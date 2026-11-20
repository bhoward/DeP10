package edu.depauw.dep10.simulator2.common;

import edu.depauw.dep10.simulator2.DebugDevice;
import edu.depauw.dep10.util.Word;

public class DebugConsoleDevice extends ConsoleDevice implements DebugDevice {
	public DebugConsoleDevice(Word baseAddress) {
		super(baseAddress);
		// TODO Auto-generated constructor stub
		
		// use a PushbackInputStream for charIn?
		// what about charOut? StringBuilder?
	}

}
