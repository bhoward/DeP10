package edu.depauw.dep10;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;

// TODO add a validator
class CommandAsm {
	@Parameter
	public List<String> parameters = new ArrayList<>();

	@Parameter(names = { "-h", "--help" }, description = "Display this help message and exit.", help = true)
	public boolean showHelp = false;

	@Parameter(names = "--os", description = "Specify OS.")
	public String osName = "default";

	@Parameter(names = "--bm", description = "Use bare metal OS.")
	public boolean bareMetal = false;
	
	// TODO -s option for source? --md for macro directory?

	@Parameter(names = "-o", description = "Output object file.")
	public String objectFile = null;
	
	@Parameter(names = "-e", description = "Output error file.")
	public String errorFile = null;
	
	@Parameter(names = {"-l", "--os-listing"}, description = "Output listing file.")
	public String listingFile = null;
}

class CommandRun {
	@Parameter
	public List<String> parameters = new ArrayList<>();

	@Parameter(names = { "-h", "--help" }, description = "Display this help message and exit.", help = true)
	public boolean showHelp = false;

	@Parameter(names = "--os", description = "Specify OS.")
	public String osName = "default";

	@Parameter(names = "--bm", description = "Use bare metal OS.")
	public boolean bareMetal = false;
	
	// TODO -s option for source??
	
	@Parameter(names = {"-i", "--charIn"}, description = "Console input.")
	public String consoleIn = null;
	
	@Parameter(names = {"-o", "--charOut"}, description = "Console output.")
	public String consoleOut = null;
	
	@Parameter(names = {"-d", "--mem-dump"}, description = "Output memory dump.")
	public String memDump = null;
	
	@Parameter(names = {"-m", "--max"}, description = "Maximum number of instructions.")
	public int max = 125000;
}
