package edu.depauw.dep10.driver;

import com.beust.jcommander.Parameter;

public class InitialArgs {
	@Parameter(names = {"-h", "--help"}, description = "Display this help message and exit.", help = true, order = 0)
	public boolean showHelp = false;
}