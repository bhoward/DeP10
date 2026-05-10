package edu.depauw.dep10.ui;

import java.io.PrintWriter;
import java.io.StringWriter;

import edu.depauw.dep10.assemble.Assembler;
import edu.depauw.dep10.assemble.Result;
import edu.depauw.dep10.driver.Driver;
import edu.depauw.dep10.driver.ErrorLog;
import edu.depauw.dep10.preprocess.Preprocessor;
import edu.depauw.dep10.preprocess.Sources;
import edu.depauw.dep10.simulator.Controller;
import edu.depauw.dep10.simulator.PlainController;
import edu.depauw.dep10.simulator.Simulator;
import edu.depauw.dep10.simulator.State;

public interface SourceType {
    default boolean build(String source, OutputPanel listing, OutputPanel object) {
        var log = new ErrorLog();
        Sources sources = new Sources();

        loadOSHeader(sources, log);

        sources.addString(source);

        var preprocessor = new Preprocessor(log);
        Result result = null;

        if (log.noErrors()) {
            var lines = preprocessor.preprocess(sources);
            var assembler = new Assembler(log);
            result = assembler.assemble(lines);

            // Print a listing for testing purposes
            var writer = new StringWriter();
            try (var out = new PrintWriter(writer)) {
                result.printListing(out);
            }

            listing.setContent(writer.toString());
            object.setContent(result.toObjectFile());
            
            return !result.hasErrors();
        } else {
            return false;
        }

        // TODO deal with errors; don't run on UI thread!
    }

    default void run(OutputPanel object, TerminalPanel terminal) {
        State state = new State();
        state.loadString(object.getContent());
        loadOSObject(state);

        terminal.clear();
        state.setInput(terminal.getInputStream());
        state.setOutput(terminal.getOutputStream());
        state.setError(terminal.getOutputStream());
        // TODO allow batch I/O

        Simulator sim = new Simulator(state);

        Controller control = new PlainController();
        var t = new Thread(() -> sim.run(control));
        t.start();
    }
    
    void loadOSHeader(Sources sources, ErrorLog log);
    
    void loadOSObject(State state);
    
    SourceType Pep10UserFull = new SourceType() {
        @Override
        public String toString() {
            return "Pep/10 User Code";
        }

        @Override
        public void loadOSHeader(Sources sources, ErrorLog log) {
            sources.addResource(Driver.FULL_OS_HEADER, log);
        }

        @Override
        public void loadOSObject(State state) {
            state.loadResource(Driver.FULL_OS_OBJECT);
        }
    };
    
    SourceType Pep10UserBare = new SourceType() {
        @Override
        public String toString() {
            return "Pep/10 Bare Metal";
        }

        @Override
        public void loadOSHeader(Sources sources, ErrorLog log) {
            sources.addResource(Driver.BARE_METAL_OS_HEADER, log);
        }

        @Override
        public void loadOSObject(State state) {
            state.loadResource(Driver.BARE_METAL_OS_OBJECT);
        }
    };
    
    SourceType Pep10System = new SourceType() {
        @Override
        public String toString() {
            return "Pep/10 System Code";
        }

        @Override
        public void loadOSHeader(Sources sources, ErrorLog log) {
            sources.addResource(Driver.STD_MACROS, log);
        }

        @Override
        public void loadOSObject(State state) {
            // Do nothing
        }
    };
    
    SourceType DeCLan = new SourceType() {
        @Override
        public String toString() {
            return "DeCLan";
        }

        @Override
        public boolean build(String source, OutputPanel listing, OutputPanel object) {
            var pepSource = edu.depauw.declan.DeCLan.run(source);
            return SourceType.super.build(pepSource, listing, object);
        }

        @Override
        public void loadOSHeader(Sources sources, ErrorLog log) {
            sources.addResource(Driver.FULL_OS_HEADER, log);
        }

        @Override
        public void loadOSObject(State state) {
            state.loadResource(Driver.FULL_OS_OBJECT);
        }
    };
}
