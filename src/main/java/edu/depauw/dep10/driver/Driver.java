package edu.depauw.dep10.driver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Properties;

import com.beust.jcommander.JCommander;

import edu.depauw.dep10.assemble.Assembler;
import edu.depauw.dep10.assemble.Result;
import edu.depauw.dep10.preprocess.Preprocessor;
import edu.depauw.dep10.preprocess.Sources;
import edu.depauw.dep10.simulator.Controller;
import edu.depauw.dep10.simulator.DebugState;
import edu.depauw.dep10.simulator.PlainController;
import edu.depauw.dep10.simulator.Simulator;
import edu.depauw.dep10.simulator.State;
import edu.depauw.dep10.simulator.StepController;
import edu.depauw.dep10.simulator.TracingController;

public class Driver {
	private static final String STD_MACROS = "/stdmacro.pep";
	private static final String FORTH_MACROS = "/forthmacro.pep";
	private static final String BARE_METAL_OS = "/pep10baremetal.pep";
	private static final String FULL_OS = "/pep10os.pep";
	private static final String FORTH_OS = "/assembler.pep";
	private static final String PROPERTIES = "project.properties";
	private static final String VERSION_PROP = "version";

    public static void main(String[] argv) throws IOException {
		ErrorLog log = new ErrorLog();

		InitialArgs init = new InitialArgs();
		CommandAsm asm = new CommandAsm();
		CommandRun run = new CommandRun();
		JCommander jc = JCommander.newBuilder()
				.addObject(init)
				.addCommand("asm", asm)
				.addCommand("run", run)
				.build();
		jc.parse(argv);

		if (init.showVersion) {
		    var properties = new Properties();
		    properties.load(Driver.class.getClassLoader().getResourceAsStream(PROPERTIES));
		    System.out.println(properties.getProperty(VERSION_PROP));
		} else if (init.showHelp || jc.getParsedCommand() == null) {
            jc.usage();
        } else {
			switch (jc.getParsedCommand()) {
			case "asm":
				if (asm.showHelp) {
					jc.usage("asm");
				} else {
					doAsm(asm, log);
				}
				break;
			case "run":
				if (run.showHelp) {
					jc.usage("run");
				} else {
					doRun(run, log);
				}
				break;
			default:
			    jc.usage();
			    break;
			}
		}
	}

	private static void doAsm(CommandAsm asm, ErrorLog log) {
	    Sources sources = new Sources();
	    
	    // First add standard macros
	    sources.addResource(STD_MACROS, log); // TODO option to substitute?
	    
	    // After loading macros, set option to generate OS listing if desired
	    sources.setVisibleResources(asm.osListing);
	    
	    // Next add the distinguished source file, if any
	    if (asm.sourceFile != null) {
	        sources.addFile(asm.sourceFile, log);
	    }
	    
	    // Add any other source files from the command line
	    for (var name : asm.sourceList) {
	        sources.addFile(name, log);
	    }
	    
	    // If no sources specified, add standard input
	    if (asm.sourceFile == null && asm.sourceList.isEmpty()) {
	        sources.addStdIn();
	    }
	    
	    // Add the appropriate OS
		if (asm.bareMetal && asm.osName != null) {
			log.error("Bare metal excludes specifying OS.");
			return;
		}
		
		if (asm.bareMetal) {
		    sources.addResource(BARE_METAL_OS, log);
		} else if (asm.osName != null) {
		    if (asm.osName.equals("FORTH")) {
		        // special case for the FORTH OS
		        sources.addResource(FORTH_MACROS, log);
		        sources.addResource(FORTH_OS, log);
		    } else {
		        sources.addFile(asm.osName, log);
		    }
		} else {
		    sources.addResource(FULL_OS, log);
		}

		var preprocessor = new Preprocessor(log);
		Result result = null;
		
		if (log.noErrors()) {
    		var lines = preprocessor.preprocess(sources);
    		// all macros and includes have been expanded
    		
    		var assembler = new Assembler(log);
    		result = assembler.assemble(lines);
    		
    		if (asm.objectFile != null) {
    		    try (var out = new BufferedWriter(new FileWriter(asm.objectFile))) {
    		        out.write(result.toObjectFile());
    		    } catch (IOException e) {
                    log.error(e.getMessage());
                }
    		} else {
    		    System.out.println(result.toObjectFile());
    		}
    		
    		if (asm.listingFile != null) {
    		    try (var out = new PrintWriter(new BufferedWriter(new FileWriter(asm.listingFile)))) {
    		        result.printListing(out);
    		    } catch (IOException e) {
    		        log.error(e.getMessage());
    		    }
    		}
		}
		
		if (asm.errorFile != null) {
		    try (var out = new PrintWriter(new BufferedWriter(new FileWriter(asm.errorFile)))) {
		        for (var message : log.getMessages()) {
		            out.println(message);
		        }
		        
		        if (result != null) {
		            result.printErrors(out);
		        }
		    } catch (IOException e) {
		        System.err.println("Error writing error file!");
		    }
		} else {
		    for (var message : log.getMessages()) {
		        System.err.println(message);
		    }
		}
	}

    private static void doRun(CommandRun run, ErrorLog log) {
	    State state = (run.trace == null) ? new State() : new DebugState();
	    
	    for (var param : run.parameters) {
	        state.loadFile(param);
	    }
	    
	    state.setInput(run.consoleIn);
	    state.setOutput(run.consoleOut);
	    state.setError(run.errOut);
	    
	    Simulator sim = new Simulator(state);
	    
	    Controller control = new PlainController();
	    if (run.max > 0) {
	        control = new StepController(control, run.max);
	    }
	    
	    TracingController tc = null;
	    if (run.trace != null) {
	        tc = new TracingController(control);
	        control = tc;
	    }
	    
	    // TODO for interactive use, also support breakpoints and single-stepping
	    sim.run(control);
	    
	    if (run.memDump != null) {
	        state.dump(run.memDump);
	    }
	    
	    if (run.trace != null) {
	        try {
	            var output = new PrintStream(new File(run.trace));
	            tc.printTrace(output);
	            output.close();
	        } catch (FileNotFoundException e) {
	            System.err.println("Unable to open tracing output: " + run.trace);
	        }
	    }
	}
}
