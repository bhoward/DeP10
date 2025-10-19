package edu.depauw.dep10;

public abstract class AddrMode {
	private String modeString;
	
	public AddrMode(String modeString) {
		this.modeString = modeString;
	}
	
	public String modeString() {
		return modeString;
	}
	
	public abstract Word getAddress(State s);

	public Word resolve(State s) {
		return s.mem2(getAddress(s));
	}

	public UByte resolveByte(State s) {
		return s.mem1(getAddress(s));
	}

	public static final AddrMode IMMEDIATE = new AddrMode("i") {
		public Word getAddress(State s) {
			throw new IllegalStateException();
		}

		@Override
		public Word resolve(State s) {
			return s.getOperand();
		}

		@Override
		public UByte resolveByte(State s) {
			return s.getOperand().lo();
		}
	};

	public static final AddrMode DIRECT = new AddrMode("d") {
		public Word getAddress(State s) {
			return s.getOperand();
		}
	};

	public static final AddrMode INDIRECT = new AddrMode("n") {
		public Word getAddress(State s) {
			return s.mem2(s.getOperand());
		}
	};

	public static final AddrMode STACK = new AddrMode("s") {
		public Word getAddress(State s) {
			return s.getSP().plus(s.getOperand());
		}
	};

	public static final AddrMode STACK_DEFERRED = new AddrMode("sf") {
		public Word getAddress(State s) {
			return s.mem2(s.getSP().plus(s.getOperand()));
		}
	};

	public static final AddrMode INDEX = new AddrMode("x") {
		public Word getAddress(State s) {
			return s.getOperand().plus(s.getX());
		}
	};

	public static final AddrMode STACK_INDEXED = new AddrMode("sx") {
		public Word getAddress(State s) {
			return s.getSP().plus(s.getOperand()).plus(s.getX());
		}
	};

	public static final AddrMode STACK_DEFERRED_INDEXED = new AddrMode("sfx") {
		public Word getAddress(State s) {
			return s.mem2(s.getSP().plus(s.getOperand())).plus(s.getX());
		}
	};
}
