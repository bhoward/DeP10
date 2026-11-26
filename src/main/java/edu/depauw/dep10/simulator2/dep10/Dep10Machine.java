package edu.depauw.dep10.simulator2.dep10;

import edu.depauw.dep10.simulator2.Fault;
import edu.depauw.dep10.simulator2.Image;
import edu.depauw.dep10.simulator2.Machine;
import edu.depauw.dep10.simulator2.Operation;
import edu.depauw.dep10.simulator2.State;
import edu.depauw.dep10.simulator2.common.BufferedConsoleDevice;
import edu.depauw.dep10.simulator2.common.ClockDevice;
import edu.depauw.dep10.simulator2.common.PlainMemoryDevice;
import edu.depauw.dep10.simulator2.common.ProtectedMemoryDevice;
import edu.depauw.dep10.simulator2.common.RegisterFile;
import edu.depauw.dep10.simulator2.common.ShutdownDevice;
import edu.depauw.dep10.simulator2.common.StreamingConsoleDevice;
import edu.depauw.dep10.simulator2.pep10.Pep10Machine;
import edu.depauw.dep10.simulator2.pep10.Pep10Table;
import edu.depauw.dep10.util.Word;

/**
 * Configuration class for the DeP10 machine: Pep/10 with the extension
 * instructions, the H register, and the real time clock.
 */
public class Dep10Machine implements Machine {
    private static final int MEMORY_SIZE = 65536;

    /** The real time clock occupies the 16-byte slice 0xFF00 to 0xFF0F of the I/O page. */
    public static final Word CLOCK = Word.of(0xFF00);

    private static final ExtensibleTable TABLE;

    static {
        TABLE = new ExtensibleTable(Pep10Table.UNIMPLEMENTED);
        Dep10Table.installDep10(TABLE);
    }

    @Override
    public Dep10State init(Image image) {
        var memory = new ProtectedMemoryDevice(new PlainMemoryDevice(MEMORY_SIZE));
        var registerFile = new RegisterFile(Dep10Registers.DESCRIPTOR);
        var state = new Dep10State(memory, registerFile);

        var console = new StreamingConsoleDevice(Pep10Machine.CHARIN, System.in, System.out);
        var shutdown = new ShutdownDevice(Pep10Machine.SHUTDOWN);

        state.mount(Pep10Machine.CHARIN, console);
        state.mount(Pep10Machine.SHUTDOWN, shutdown);
        state.mount(CLOCK, new ClockDevice(CLOCK));

        image.loadInto(memory);
        initializeRegisters(state);

        return state;
    }

    @Override
    public Dep10DebugState initDebug(Image image) {
        var memory = new ProtectedMemoryDevice(new PlainMemoryDevice(MEMORY_SIZE));
        var registerFile = new RegisterFile(Dep10Registers.DESCRIPTOR);
        var state = new Dep10DebugState(memory, registerFile);

        var console = new BufferedConsoleDevice(Pep10Machine.CHARIN);
        var shutdown = new ShutdownDevice(Pep10Machine.SHUTDOWN);

        state.mount(Pep10Machine.CHARIN, console);
        state.mount(Pep10Machine.SHUTDOWN, shutdown);
        state.mount(CLOCK, new ClockDevice(CLOCK));

        image.loadInto(memory);
        initializeRegisters(state);

        return state;
    }

    private void initializeRegisters(Dep10Registers state) {
        try {
            state.setPC(state.readW(Pep10Machine.INITIAL_PC));
            state.setSP(state.readW(Pep10Machine.INITIAL_SP));
        } catch (Fault fault) {
            // TODO where should this be handled?
            System.err.println("Unable to initialize machine registers");
        }
    }

    @Override
    public Operation fetch(State state) throws Fault {
        var s = (Dep10Registers) state;

        var first = s.fetchB(s.getPC());
        s.setPC(s.getPC().plus(1));
        s.setOpCode(first);

        var op = TABLE.getOp(first);
        if (op instanceof ExtensibleTable.Prefix prefix) {
            var second = s.fetchB(s.getPC());
            s.setPC(s.getPC().plus(1));
            s.setIR1(Word.of((first.value() << 8) | second.value()));
            op = prefix.getTable().getOp(second);
        }

        return op;
    }

    @Override
    public void handle(Fault fault, State state) {
        // TODO print the fault message to error log/console?
        state.pause();
    }
}
