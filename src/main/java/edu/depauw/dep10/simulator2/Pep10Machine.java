package edu.depauw.dep10.simulator2;

import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

public class Pep10Machine implements Machine {
	private static final int MEMORY_SIZE = 65536;

	private static final int NUM_BYTE_REGISTERS = 2;

	private static final ByteRegister FLAGS = new ByteRegister("Flags", 0);
	private static final ByteRegister IR1 = new ByteRegister("IR1", 1);

	private static final int NUM_WORD_REGISTERS = 5;

	private static final WordRegister A = new WordRegister("A", 0);
	private static final WordRegister X = new WordRegister("X", 1);
	private static final WordRegister PC = new WordRegister("PC", 2);
	private static final WordRegister SP = new WordRegister("SP", 3);
	private static final WordRegister IR2 = new WordRegister("IR2", 4);

	private static final FlagRegister N = new FlagRegister("N", 0, 3);
	private static final FlagRegister Z = new FlagRegister("Z", 0, 2);
	private static final FlagRegister V = new FlagRegister("V", 0, 1);
	private static final FlagRegister C = new FlagRegister("C", 0, 0);
	
	private static final Table TABLE;
	
	static {
		TABLE = new Table();
		TABLE.installPep10Base();
	}
	
	public interface Pep10Registers extends State {
		default UByte getFlags() {
			return get(FLAGS);
		}

		default void setFlags(UByte value) {
			put(FLAGS, value);
		}

		default UByte getOpCode() {
			return get(IR1);
		}

		default void setOpCode(UByte value) {
			put(IR1, value);
		}

		default Word getA() {
			return get(A);
		}

		default void setA(Word value) {
			put(A, value);
		}

		default Word getX() {
			return get(X);
		}

		default void setX(Word value) {
			put(X, value);
		}

		default Word getPC() {
			return get(PC);
		}

		default void setPC(Word value) {
			put(PC, value);
		}

		default Word getSP() {
			return get(SP);
		}

		default void setSP(Word value) {
			put(SP, value);
		}

		default Word getOperand() {
			return get(IR2);
		}

		default void setOperand(Word value) {
			put(IR2, value);
		}

		default boolean getN() {
			return get(N);
		}

		default void setN(boolean value) {
			put(N, value);
		}

		default boolean getZ() {
			return get(Z);
		}

		default void setZ(boolean value) {
			put(Z, value);
		}

		default boolean getV() {
			return get(V);
		}

		default void setV(boolean value) {
			put(V, value);
		}

		default boolean getC() {
			return get(C);
		}

		default void setC(boolean value) {
			put(C, value);
		}
	}
	
	public class Pep10State extends BaseState implements Pep10Registers {
		public Pep10State(Device memory, RegisterFile registerFile) {
			super(memory, registerFile);
		}
	}
	
	public class Pep10DebugState extends DebugBaseState implements Pep10Registers {
		public Pep10DebugState(Device memory, RegisterFile registerFile) {
			super(memory, registerFile);
		}
	}
	
	@Override
	public Pep10State init() {
		var memory = new PlainMemoryDevice(MEMORY_SIZE);
		var registerFile = new RegisterFile(NUM_BYTE_REGISTERS, NUM_WORD_REGISTERS);
		var state = new Pep10State(memory, registerFile);

		var console = new ConsoleDevice(Word.of(0xFFFD));
		var shutdown = new ShutdownDevice(Word.of(0xFFFF));

		state.mount(console);
		state.mount(shutdown);

		return state;
	}

	@Override
	public Pep10DebugState initDebug() {
		var memory = new DebugPlainMemoryDevice(MEMORY_SIZE);
		var registerFile = new DebugRegisterFile(NUM_BYTE_REGISTERS, NUM_WORD_REGISTERS);
		var state = new Pep10DebugState(memory, registerFile);

		var console = new DebugConsoleDevice(Word.of(0xFFFD));
		var shutdown = new ShutdownDevice(Word.of(0xFFFF));

		state.mount(console);
		state.mount(shutdown);

		return state;
	}

	@Override
	public Operation fetch(State state) throws Fault {
		var s = (Pep10Registers) state;
		
		s.setOpCode(s.fetchB(s.getPC()));
		s.setPC(s.getPC().plus(1));
		
		return TABLE.getOp(s.getOpCode());
	}

	@Override
	public void handle(Fault fault, State state) {
		// TODO print the fault message to error log/console?
		state.pause();
	}
}
