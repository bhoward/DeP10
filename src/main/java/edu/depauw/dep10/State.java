package edu.depauw.dep10;

import java.io.IOException;

// TODO replace Word with short and UByte with byte

public class State {
	private Word A;
	private Word X;
	private Word PC;
	private Word SP;
	private UByte IR1;
	private Word IR2;
	private UByte Flags;
	private boolean running;

	// TODO initialization; loading; ...

	private UByte[] memory = new UByte[65536];

	public UByte mem1(Word addr) {
		// TODO check permissions
		if (addr.equals(Operation.CHARIN)) {
			// TODO read from somewhere other than the console
			try {
				return new UByte(System.in.read());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return memory[addr.value()];
	}

	public Word mem2(Word addr) {
		var hi = mem1(addr);
		var lo = mem1(addr.plus(1));
		return new Word(hi.value() << 16 + lo.value());
	}

	public void setMem1(Word addr, UByte n) {
		// TODO check permissions
		if (addr.equals(Operation.SHUTDOWN)) {
			running = false;
			System.out.flush();
		} else if (addr.equals(Operation.CHAROUT)) {
			// TODO write to somewhere other than the console
			System.out.write(n.value());
		}

		memory[addr.value()] = n;
	}

	public void setMem2(Word addr, Word n) {
		setMem1(addr, n.hi());
		setMem1(addr.plus(1), n.lo());
	}

	public Word getA() {
		return A;
	}

	public Word getX() {
		return X;
	}

	public Word getPC() {
		return PC;
	}

	public Word getSP() {
		return SP;
	}

	public UByte getOpCode() {
		return IR1;
	}

	public Word getOperand() {
		return IR2;
	}

	public void setA(Word n) {
		this.A = n;
	}

	public void setA(UByte n) {
		this.A = new Word(n.value());
	}

	public void setX(Word n) {
		this.X = n;
	}

	public void setX(UByte n) {
		this.X = new Word(n.value());
	}

	public void setPC(Word n) {
		this.PC = n;
	}

	public void setSP(Word n) {
		this.SP = n;
	}

	public void setOpCode(UByte n) {
		this.IR1 = n;
	}

	public void setOperand(Word n) {
		this.IR2 = n;
	}

	public boolean getN() {
		return Flags.bit(3);
	}

	public boolean getZ() {
		return Flags.bit(2);
	}

	public boolean getV() {
		return Flags.bit(1);
	}

	public boolean getC() {
		return Flags.bit(0);
	}

	public void setN(boolean b) {
		Flags = Flags.withBit(3, b);
	}

	public void setZ(boolean b) {
		Flags = Flags.withBit(2, b);
	}

	public void setV(boolean b) {
		Flags = Flags.withBit(1, b);
	}

	public void setC(boolean b) {
		Flags = Flags.withBit(0, b);
	}

	public void setFlags(UByte flags) {
		Flags = new UByte(flags.value() & 0x0F);
	}

	public UByte getFlags() {
		return Flags;
	}

	public boolean isRunning() {
		return running;
	}
}
