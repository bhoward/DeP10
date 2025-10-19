package edu.depauw.dep10;

public abstract class Operation {
	public static final Word TRAP_HANDLER_POINTER = new Word(0xFFF7);
	public static final Word DISPATCHER_POINTER = new Word(0xFFF9);
	public static final Word SYSTEM_STACK_POINTER = new Word(0xFFFB);
	public static final Word CHARIN = new Word(0xFFFD);
	public static final Word CHAROUT = new Word(0xFFFE);
	public static final Word SHUTDOWN = new Word(0xFFFF);
	
	private String mnemonic;
	
	public Operation(String mnemonic) {
		this.mnemonic = mnemonic;
	}

	public String mnemonic() {
		return mnemonic;
	}

	public abstract boolean hasOperand();

	public abstract void apply(State s, AddrMode mode);

	public abstract void install(OpTable table, int i);

	static abstract class UnaryOperation extends Operation {
		public UnaryOperation(String mnemonic) {
			super(mnemonic);
		}

		public boolean hasOperand() {
			return false;
		}

		public abstract void exec(State s);

		public void apply(State s, AddrMode mode) {
			exec(s);
		}

		public void install(OpTable table, int i) {
			table.add(i, new ModeOperation(null, this));
		}
	}
	
	static abstract class NonunaryOperation extends Operation {
		public NonunaryOperation(String mnemonic) {
			super(mnemonic);
		}
		
		public boolean hasOperand() {
			return true;
		}
	}

	static abstract class IXOperation extends NonunaryOperation {
		public IXOperation(String mnemonic) {
			super(mnemonic);
		}

		public abstract void exec(State s, Word operand);

		public void apply(State s, AddrMode mode) {
			exec(s, mode.resolve(s));
		}

		public void install(OpTable table, int i) {
			table.add(i, new ModeOperation(AddrMode.IMMEDIATE, this));
			table.add(i + 1, new ModeOperation(AddrMode.INDEX, this));
		}
	}
	
	static abstract class AllModeOperation extends NonunaryOperation {
		public AllModeOperation(String mnemonic) {
			super(mnemonic);
		}
		
		public void install(OpTable table, int i) {
			table.add(i, new ModeOperation(AddrMode.IMMEDIATE, this));
			table.add(i + 1, new ModeOperation(AddrMode.DIRECT, this));
			table.add(i + 2, new ModeOperation(AddrMode.INDIRECT, this));
			table.add(i + 3, new ModeOperation(AddrMode.STACK, this));
			table.add(i + 4, new ModeOperation(AddrMode.STACK_DEFERRED, this));
			table.add(i + 5, new ModeOperation(AddrMode.INDEX, this));
			table.add(i + 6, new ModeOperation(AddrMode.STACK_INDEXED, this));
			table.add(i + 7, new ModeOperation(AddrMode.STACK_DEFERRED_INDEXED, this));
		}
	}

	static abstract class AllModeWordOperation extends AllModeOperation {
		public AllModeWordOperation(String mnemonic) {
			super(mnemonic);
		}

		public abstract void exec(State s, Word operand);

		public void apply(State s, AddrMode mode) {
			exec(s, mode.resolve(s));
		}
	}

	static abstract class SCallOperation extends AllModeOperation {
		public SCallOperation(String mnemonic) {
			super(mnemonic);
		}
		
		public abstract void exec(State s);
		
		public void apply(State s, AddrMode mode) {
			exec(s);
		}
	}
	
	static abstract class AllModeByteOperation extends AllModeOperation {
		public AllModeByteOperation(String mnemonic) {
			super(mnemonic);
		}

		public abstract void exec(State s, UByte operand);

		public void apply(State s, AddrMode mode) {
			exec(s, mode.resolveByte(s));
		}
	}

	static abstract class NonImmedOperation extends NonunaryOperation {
		public NonImmedOperation(String mnemonic) {
			super(mnemonic);
		}

		public abstract void exec(State s, Word address);

		public void apply(State s, AddrMode mode) {
			exec(s, mode.getAddress(s));
		}

		public void install(OpTable table, int i) {
			table.add(i + 1, new ModeOperation(AddrMode.DIRECT, this));
			table.add(i + 2, new ModeOperation(AddrMode.INDIRECT, this));
			table.add(i + 3, new ModeOperation(AddrMode.STACK, this));
			table.add(i + 4, new ModeOperation(AddrMode.STACK_DEFERRED, this));
			table.add(i + 5, new ModeOperation(AddrMode.INDEX, this));
			table.add(i + 6, new ModeOperation(AddrMode.STACK_INDEXED, this));
			table.add(i + 7, new ModeOperation(AddrMode.STACK_DEFERRED_INDEXED, this));
		}
	}

	public static final Operation RET = new UnaryOperation("RET") {
		public void exec(State s) {
			s.setPC(s.mem2(s.getSP()));
			s.setSP(s.getSP().plus(2));
		}
	};

	public static final Operation SRET = new UnaryOperation("SRET") {
		public void exec(State s) {
			s.setFlags(s.mem1(s.getSP()));
			s.setA(s.mem2(s.getSP().plus(1)));
			s.setX(s.mem2(s.getSP().plus(3)));
			s.setPC(s.mem2(s.getSP().plus(5)));
			s.setSP(s.mem2(s.getSP().plus(7)));
		}
	};

	public static final Operation MOVSPA = new UnaryOperation("MOVSPA") {
		public void exec(State s) {
			s.setA(s.getSP());
		}
	};

	public static final Operation MOVASP = new UnaryOperation("MOVASP") {
		public void exec(State s) {
			s.setSP(s.getA());
		}
	};

	public static final Operation MOVFLGA = new UnaryOperation("MOVFLGA") {
		public void exec(State s) {
			s.setA(s.getFlags());
		}
	};

	public static final Operation MOVAFLG = new UnaryOperation("MOVAFLG") {
		public void exec(State s) {
			s.setFlags(s.getA().lo());
		}
	};

	public static final Operation NOP = new UnaryOperation("NOP") {
		public void exec(State s) {
		}
	};

	public static final Operation NEGA = new UnaryOperation("NEGA") {
		public void exec(State s) {
			var a1 = s.getA();
			var sign1 = a1.isNegative();
			var zero1 = a1.isZero();

			var a2 = a1.negate();
			var sign2 = a2.isNegative();
			var zero2 = a2.isZero();

			s.setA(a2);
			s.setN(sign2);
			s.setZ(zero2);
			s.setV(sign1 == sign2);
			s.setC(zero1);
		}
	};

	public static final Operation NEGX = new UnaryOperation("NEGX") {
		public void exec(State s) {
			var x1 = s.getX();
			var sign1 = x1.isNegative();
			var zero1 = x1.isZero();

			var x2 = x1.negate();
			var sign2 = x2.isNegative();
			var zero2 = x2.isZero();

			s.setX(x2);
			s.setN(sign2);
			s.setZ(zero2);
			s.setV(sign1 == sign2);
			s.setC(zero1);
		}
	};

	public static final Operation ASLA = new UnaryOperation("ASLA") {
		public void exec(State s) {
			var a1 = s.getA();
			var sign1 = a1.isNegative();

			var a2 = a1.shiftLeft(false);
			var sign2 = a2.isNegative();
			var zero2 = a2.isZero();

			s.setC(sign1);
			s.setA(a2);
			s.setN(sign2);
			s.setZ(zero2);
			s.setV(sign1 != sign2);
		}
	};

	public static final Operation ASLX = new UnaryOperation("ASLX") {
		public void exec(State s) {
			var x1 = s.getX();
			var sign1 = x1.isNegative();

			var x2 = x1.shiftLeft(false);
			var sign2 = x2.isNegative();
			var zero2 = x2.isZero();

			s.setC(sign1);
			s.setX(x2);
			s.setN(sign2);
			s.setZ(zero2);
			s.setV(sign1 != sign2);
		}
	};

	public static final Operation ASRA = new UnaryOperation("ASRA") {
		public void exec(State s) {
			var a1 = s.getA();
			var sign1 = a1.isNegative();
			var carry = a1.bit(0);

			var a2 = a1.shiftRight(sign1);
			var zero2 = a2.isZero();

			s.setC(carry);
			s.setA(a2);
			s.setN(sign1);
			s.setZ(zero2);
			s.setV(false);
		}
	};

	public static final Operation ASRX = new UnaryOperation("ASRX") {
		public void exec(State s) {
			var x1 = s.getX();
			var sign1 = x1.isNegative();
			var carry = x1.bit(0);

			var x2 = x1.shiftRight(sign1);
			var zero2 = x2.isZero();

			s.setC(carry);
			s.setX(x2);
			s.setN(sign1);
			s.setZ(zero2);
			s.setV(false);
		}
	};

	public static final Operation NOTA = new UnaryOperation("NOTA") {
		public void exec(State s) {
			var a1 = s.getA();

			var a2 = a1.not();
			var sign2 = a2.isNegative();
			var zero2 = a2.isZero();

			s.setA(a2);
			s.setN(sign2);
			s.setZ(zero2);
		}
	};

	public static final Operation NOTX = new UnaryOperation("NOTX") {
		public void exec(State s) {
			var x1 = s.getX();

			var x2 = x1.not();
			var sign2 = x2.isNegative();
			var zero2 = x2.isZero();

			s.setX(x2);
			s.setN(sign2);
			s.setZ(zero2);
		}
	};

	public static final Operation ROLA = new UnaryOperation("ROLA") {
		public void exec(State s) {
			var a1 = s.getA();
			var carry = a1.isNegative();

			var a2 = a1.shiftLeft(s.getC());
			var sign2 = a2.isNegative();
			var zero2 = a2.isZero();

			s.setC(carry);
			s.setA(a2);
			s.setN(sign2);
			s.setZ(zero2);
		}
	};

	public static final Operation ROLX = new UnaryOperation("ROLX") {
		public void exec(State s) {
			var x1 = s.getX();
			var carry = x1.isNegative();

			var x2 = x1.shiftLeft(s.getC());
			var sign2 = x2.isNegative();
			var zero2 = x2.isZero();

			s.setC(carry);
			s.setX(x2);
			s.setN(sign2);
			s.setZ(zero2);
		}
	};

	public static final Operation RORA = new UnaryOperation("RORA") {
		public void exec(State s) {
			var a1 = s.getA();
			var carry = a1.bit(0);

			var a2 = a1.shiftRight(s.getC());
			var sign2 = a2.isNegative();
			var zero2 = a2.isZero();

			s.setC(carry);
			s.setA(a2);
			s.setN(sign2);
			s.setZ(zero2);
		}
	};

	public static final Operation RORX = new UnaryOperation("RORX") {
		public void exec(State s) {
			var x1 = s.getX();
			var carry = x1.bit(0);

			var x2 = x1.shiftRight(s.getC());
			var sign2 = x2.isNegative();
			var zero2 = x2.isZero();

			s.setC(carry);
			s.setX(x2);
			s.setN(sign2);
			s.setZ(zero2);
		}
	};

	public static final Operation BR = new IXOperation("BR") {
		public void exec(State s, Word operand) {
			s.setPC(operand);
		}
	};

	public static final Operation BRLE = new IXOperation("BRLE") {
		public void exec(State s, Word operand) {
			if (s.getN() || s.getZ()) {
				s.setPC(operand);
			}
		}
	};

	public static final Operation BRLT = new IXOperation("BRLT") {
		public void exec(State s, Word operand) {
			if (s.getN()) {
				s.setPC(operand);
			}
		}
	};

	public static final Operation BREQ = new IXOperation("BREQ") {
		public void exec(State s, Word operand) {
			if (s.getZ()) {
				s.setPC(operand);
			}
		}
	};

	public static final Operation BRNE = new IXOperation("BRNE") {
		public void exec(State s, Word operand) {
			if (!s.getZ()) {
				s.setPC(operand);
			}
		}
	};

	public static final Operation BRGE = new IXOperation("BRGE") {
		public void exec(State s, Word operand) {
			if (!s.getN()) {
				s.setPC(operand);
			}
		}
	};

	public static final Operation BRGT = new IXOperation("BRGT") {
		public void exec(State s, Word operand) {
			if (!s.getN() && !s.getZ()) {
				s.setPC(operand);
			}
		}
	};

	public static final Operation BRV = new IXOperation("BRV") {
		public void exec(State s, Word operand) {
			if (s.getV()) {
				s.setPC(operand);
			}
		}
	};

	public static final Operation BRC = new IXOperation("BRC") {
		public void exec(State s, Word operand) {
			if (s.getC()) {
				s.setPC(operand);
			}
		}
	};

	public static final Operation CALL = new IXOperation("CALL") {
		public void exec(State s, Word operand) {
			s.setSP(s.getSP().plus(-2));
			s.setMem2(s.getSP(), s.getPC());
			s.setPC(operand);
		}
	};

	public static final Operation SCALL = new SCallOperation("SCALL") {
		public void exec(State s) {
			var y = s.mem2(Operation.SYSTEM_STACK_POINTER);

			s.setMem2(y.plus(-2), s.getOperand());
			s.setMem1(y.plus(-3), s.getOpCode());
			s.setMem2(y.plus(-5), s.getSP());
			s.setMem2(y.plus(-7), s.getPC());
			s.setMem2(y.plus(-9), s.getX());
			s.setMem2(y.plus(-11), s.getA());
			s.setMem1(y.plus(-12), s.getFlags());

			s.setSP(y.plus(-12));
			s.setPC(s.mem2(Operation.TRAP_HANDLER_POINTER));
		}
	};

	public static final Operation LDWA = new AllModeWordOperation("LDWA") {
		public void exec(State s, Word operand) {
			var sign = operand.isNegative();
			var zero = operand.isZero();

			s.setA(operand);
			s.setN(sign);
			s.setZ(zero);
		}
	};

	public static final Operation LDWX = new AllModeWordOperation("LDWX") {
		public void exec(State s, Word operand) {
			var sign = operand.isNegative();
			var zero = operand.isZero();

			s.setX(operand);
			s.setN(sign);
			s.setZ(zero);
		}
	};

	public static final Operation LDBA = new AllModeByteOperation("LDBA") {
		public void exec(State s, UByte operand) {
			var zero = operand.isZero();

			s.setA(operand);
			s.setN(false);
			s.setZ(zero);
		}
	};

	public static final Operation LDBX = new AllModeByteOperation("LDBX") {
		public void exec(State s, UByte operand) {
			var zero = operand.isZero();

			s.setX(operand);
			s.setN(false);
			s.setZ(zero);
		}
	};

	public static final Operation STWA = new NonImmedOperation("STWA") {
		public void exec(State s, Word address) {
			s.setMem2(address, s.getA());
		}
	};

	public static final Operation STWX = new NonImmedOperation("STWX") {
		public void exec(State s, Word address) {
			s.setMem2(address, s.getX());
		}
	};

	public static final Operation STBA = new NonImmedOperation("STBA") {
		public void exec(State s, Word address) {
			s.setMem1(address, s.getA().lo());
		}
	};

	public static final Operation STBX = new NonImmedOperation("STBX") {
		public void exec(State s, Word address) {
			s.setMem1(address, s.getX().lo());
		}
	};

	public static final Operation CPWA = new AllModeWordOperation("CPWA") {
		public void exec(State s, Word operand) {
			var a1 = s.getA();
			var sign1 = a1.isNegative();

			var op = operand.negate();
			var signo = op.isNegative();

			var a2 = a1.plus(op);
			var sign2 = a2.isNegative();
			var zero2 = a2.isZero();
			var overflow = (sign1 == signo) && (sign1 != sign2);
			var carry = a2.lessThan(a1) || a2.lessThan(op);

			s.setN(sign2 ^ overflow);
			s.setZ(zero2);
			s.setV(overflow);
			s.setC(carry);
		}
	};

	public static final Operation CPWX = new AllModeWordOperation("CPWX") {
		public void exec(State s, Word operand) {
			var x1 = s.getX();
			var sign1 = x1.isNegative();

			var op = operand.negate();
			var signo = op.isNegative();

			var x2 = x1.plus(op);
			var sign2 = x2.isNegative();
			var zero2 = x2.isZero();
			var overflow = (sign1 == signo) && (sign1 != sign2);
			var carry = x2.lessThan(x1) || x2.lessThan(op);

			s.setN(sign2 ^ overflow);
			s.setZ(zero2);
			s.setV(overflow);
			s.setC(carry);
		}
	};

	public static final Operation CPBA = new AllModeByteOperation("CPBA") {
		public void exec(State s, UByte operand) {
			var a1 = s.getA();

			var a2 = a1.minus(operand);
			var sign2 = a2.isNegative();
			var zero2 = a2.isZero();

			s.setN(sign2);
			s.setZ(zero2);
			s.setV(false);
			s.setC(false);
		}
	};

	public static final Operation CPBX = new AllModeByteOperation("CPBX") {
		public void exec(State s, UByte operand) {
			var x1 = s.getX();

			var x2 = x1.minus(operand);
			var sign2 = x2.isNegative();
			var zero2 = x2.isZero();

			s.setN(sign2);
			s.setZ(zero2);
			s.setV(false);
			s.setC(false);
		}
	};

	public static final Operation ADDA = new AllModeWordOperation("ADDA") {
		public void exec(State s, Word operand) {
			var a1 = s.getA();
			var sign1 = a1.isNegative();

			var signo = operand.isNegative();

			var a2 = a1.plus(operand);
			var sign2 = a2.isNegative();
			var zero2 = a2.isZero();
			var overflow = (sign1 == signo) && (sign1 != sign2);
			var carry = a2.lessThan(a1) || a2.lessThan(operand);

			s.setA(a2);
			s.setN(sign2);
			s.setZ(zero2);
			s.setV(overflow);
			s.setC(carry);
		}
	};

	public static final Operation ADDX = new AllModeWordOperation("ADDX") {
		public void exec(State s, Word operand) {
			var x1 = s.getX();
			var sign1 = x1.isNegative();

			var signo = operand.isNegative();

			var x2 = x1.plus(operand);
			var sign2 = x2.isNegative();
			var zero2 = x2.isZero();
			var overflow = (sign1 == signo) && (sign1 != sign2);
			var carry = x2.lessThan(x1) || x2.lessThan(operand);

			s.setX(x2);
			s.setN(sign2);
			s.setZ(zero2);
			s.setV(overflow);
			s.setC(carry);
		}
	};

	public static final Operation SUBA = new AllModeWordOperation("SUBA") {
		public void exec(State s, Word operand) {
			var a1 = s.getA();
			var sign1 = a1.isNegative();

			var op = operand.negate();
			var signo = op.isNegative();

			var a2 = a1.plus(op);
			var sign2 = a2.isNegative();
			var zero2 = a2.isZero();
			var overflow = (sign1 == signo) && (sign1 != sign2);
			var carry = a2.lessThan(a1) || a2.lessThan(op);

			s.setA(a2);
			s.setN(sign2);
			s.setZ(zero2);
			s.setV(overflow);
			s.setC(carry);
		}
	};

	public static final Operation SUBX = new AllModeWordOperation("SUBX") {
		public void exec(State s, Word operand) {
			var x1 = s.getX();
			var sign1 = x1.isNegative();

			var op = operand.negate();
			var signo = op.isNegative();

			var x2 = x1.plus(op);
			var sign2 = x2.isNegative();
			var zero2 = x2.isZero();
			var overflow = (sign1 == signo) && (sign1 != sign2);
			var carry = x2.lessThan(x1) || x2.lessThan(op);

			s.setX(x2);
			s.setN(sign2);
			s.setZ(zero2);
			s.setV(overflow);
			s.setC(carry);
		}
	};

	public static final Operation ANDA = new AllModeWordOperation("ANDA") {
		public void exec(State s, Word operand) {
			var a1 = s.getA();

			var a2 = a1.and(operand);
			var sign2 = a2.isNegative();
			var zero2 = a2.isZero();

			s.setA(a2);
			s.setN(sign2);
			s.setZ(zero2);
		}
	};

	public static final Operation ANDX = new AllModeWordOperation("ANDX") {
		public void exec(State s, Word operand) {
			var x1 = s.getX();

			var x2 = x1.and(operand);
			var sign2 = x2.isNegative();
			var zero2 = x2.isZero();

			s.setX(x2);
			s.setN(sign2);
			s.setZ(zero2);
		}
	};

	public static final Operation ORA = new AllModeWordOperation("ORA") {
		public void exec(State s, Word operand) {
			var a1 = s.getA();

			var a2 = a1.or(operand);
			var sign2 = a2.isNegative();
			var zero2 = a2.isZero();

			s.setA(a2);
			s.setN(sign2);
			s.setZ(zero2);
		}
	};

	public static final Operation ORX = new AllModeWordOperation("ORX") {
		public void exec(State s, Word operand) {
			var x1 = s.getX();

			var x2 = x1.or(operand);
			var sign2 = x2.isNegative();
			var zero2 = x2.isZero();

			s.setX(x2);
			s.setN(sign2);
			s.setZ(zero2);
		}
	};

	public static final Operation XORA = new AllModeWordOperation("XORA") {
		public void exec(State s, Word operand) {
			var a1 = s.getA();

			var a2 = a1.xor(operand);
			var sign2 = a2.isNegative();
			var zero2 = a2.isZero();

			s.setA(a2);
			s.setN(sign2);
			s.setZ(zero2);
		}
	};

	public static final Operation XORX = new AllModeWordOperation("XORX") {
		public void exec(State s, Word operand) {
			var x1 = s.getX();

			var x2 = x1.xor(operand);
			var sign2 = x2.isNegative();
			var zero2 = x2.isZero();

			s.setX(x2);
			s.setN(sign2);
			s.setZ(zero2);
		}
	};

	public static final Operation ADDSP = new AllModeWordOperation("ADDSP") {
		public void exec(State s, Word operand) {
			var sp1 = s.getSP();

			var sp2 = sp1.plus(operand);

			s.setSP(sp2);
		}
	};

	public static final Operation SUBSP = new AllModeWordOperation("SUBSP") {
		public void exec(State s, Word operand) {
			var sp1 = s.getSP();

			var op = operand.negate();

			var sp2 = sp1.plus(op);

			s.setSP(sp2);
		}
	};
}
