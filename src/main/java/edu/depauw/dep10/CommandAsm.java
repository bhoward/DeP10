package edu.depauw.dep10;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;

public class CommandAsm {
	@Parameter(description = "source")
	public List<String> sourceList = new ArrayList<>();

	@Parameter(names = { "-h", "--help" }, description = "Display this help message and exit.", help = true, order = 0)
	public boolean showHelp = false;

	@Parameter(names = "-s", description = "Name of source file.", order = 1)
	public String sourceFile = null;

	@Parameter(names = "--os", description = "Name of OS.", order = 2)
	public String osName = null;

	@Parameter(names = "--bm", description = "Use bare metal OS.", order = 3)
	public boolean bareMetal = false;
	
	@Parameter(names = "-o", description = "Name of object file.", order = 4)
	public String objectFile = null;
	
	@Parameter(names = "-e", description = "Name of error file.", order = 5)
	public String errorFile = null;
	
	@Parameter(names = {"-l", "--os-listing"}, description = "Name of listing file.", order = 6)
	public String listingFile = null;
	
    // TODO --md for macro directory?
}