package edu.depauw.dep10.simulator2.pep10;

import edu.depauw.dep10.simulator2.Fault;
import edu.depauw.dep10.simulator2.Image;
import edu.depauw.dep10.simulator2.Machine;
import edu.depauw.dep10.simulator2.Operation;
import edu.depauw.dep10.simulator2.State;
import edu.depauw.dep10.simulator2.Table;
import edu.depauw.dep10.simulator2.common.StreamingConsoleDevice;
import edu.depauw.dep10.simulator2.common.BufferedConsoleDevice;
import edu.depauw.dep10.simulator2.common.ClockDevice;
import edu.depauw.dep10.simulator2.common.PlainMemoryDevice;
import edu.depauw.dep10.simulator2.common.RegisterFile;
import edu.depauw.dep10.simulator2.common.ShutdownDevice;
import edu.depauw.dep10.util.Word;

/**
 * Configuration class for the basic Pep/10 machine from Warford.
 */
public class Pep10Machine implements Machine {
    private static final int MEMORY_SIZE = 65536;

    public static final Word TRAP_HANDLER_POINTER = Word.of(0xFFF7);
    public static final Word INITIAL_PC = Word.of(0xFFF9);
    public static final Word INITIAL_SP = Word.of(0xFFFB);
    public static final Word CHARIN = Word.of(0xFFFD);
    public static final Word CHAROUT = Word.of(0xFFFE);
    public static final Word SHUTDOWN = Word.of(0xFFFF);

    /** The real time clock occupies the 16-byte slice 0xFF00 to 0xFF0F of the I/O page. */
    public static final Word CLOCK = Word.of(0xFF00);

    private static final Table TABLE;

    static {
        TABLE = new Table(Pep10Table.UNIMPLEMENTED);
        Pep10Table.installPep10Base(TABLE);
    }

    @Override
    public Pep10State init(Image image) {
        var memory = new PlainMemoryDevice(MEMORY_SIZE);
        var registerFile = new RegisterFile(Pep10Registers.DESCRIPTOR);
        var state = new Pep10State(memory, registerFile);

        var console = new StreamingConsoleDevice(CHARIN, System.in, System.out); // TODO pass these in?
        var shutdown = new ShutdownDevice(SHUTDOWN);

        state.mount(CHARIN, console);
        state.mount(SHUTDOWN, shutdown);
        state.mount(CLOCK, new ClockDevice(CLOCK));

        image.loadInto(memory);
        initializeRegisters(state);

        return state;
    }

    @Override
    public Pep10DebugState initDebug(Image image) {
        var memory = new PlainMemoryDevice(MEMORY_SIZE);
        var registerFile = new RegisterFile(Pep10Registers.DESCRIPTOR);
        var state = new Pep10DebugState(memory, registerFile);

        var console = new BufferedConsoleDevice(CHARIN);
        var shutdown = new ShutdownDevice(SHUTDOWN);

        state.mount(CHARIN, console);
        state.mount(SHUTDOWN, shutdown);
        state.mount(CLOCK, new ClockDevice(CLOCK));

        image.loadInto(memory);
        initializeRegisters(state);

        return state;
    }

    private void initializeRegisters(Pep10Registers state) {
        try {
            state.setPC(state.readW(INITIAL_PC));
            state.setSP(state.readW(INITIAL_SP));
        } catch (Fault fault) {
            // TODO where should this be handled?
            System.err.println("Unable to initialize machine registers");
        }
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
