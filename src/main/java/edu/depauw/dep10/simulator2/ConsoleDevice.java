package edu.depauw.dep10.simulator2;

import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

public class ConsoleDevice implements Device {
	private Word charIn;
	private Word charOut;
	
	public ConsoleDevice(Word baseAddress) {
		this.charIn = baseAddress;
		this.charOut = baseAddress.plus(1);
	}
	
	@Override
	public boolean canFetchB(Word address) {
		return false;
	}

	@Override
	public boolean canReadB(Word address) {
		return address.equals(charIn);
	}

	@Override
	public boolean canWriteB(Word address) {
		return address.equals(charOut);
	}

	@Override
	public UByte fetchB(Word address, State state) throws Fault {
		return null;
	}

	@Override
	public UByte readB(Word address, State state) throws Fault {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void writeB(Word address, UByte value, State state) throws Fault {
		// TODO Auto-generated method stub

	}
}
