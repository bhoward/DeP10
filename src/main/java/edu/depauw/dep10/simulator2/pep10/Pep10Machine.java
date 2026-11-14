package edu.depauw.dep10.simulator2.pep10;

import edu.depauw.dep10.simulator2.Fault;
import edu.depauw.dep10.simulator2.Machine;
import edu.depauw.dep10.simulator2.Operation;
import edu.depauw.dep10.simulator2.State;
import edu.depauw.dep10.simulator2.Table;
import edu.depauw.dep10.simulator2.common.ConsoleDevice;
import edu.depauw.dep10.simulator2.common.DebugConsoleDevice;
import edu.depauw.dep10.simulator2.common.DebugPlainMemoryDevice;
import edu.depauw.dep10.simulator2.common.DebugRegisterFile;
import edu.depauw.dep10.simulator2.common.PlainMemoryDevice;
import edu.depauw.dep10.simulator2.common.RegisterFile;
import edu.depauw.dep10.simulator2.common.ShutdownDevice;
import edu.depauw.dep10.util.Word;

/**
 * Configuration class for the basic Pep/10 machine from Warford.
 */
public class Pep10Machine implements Machine {
    private static final int MEMORY_SIZE = 65536;

    private static final Table TABLE;

    static {
        TABLE = new Table();
        TABLE.installPep10Base();
    }

    @Override
    public Pep10State init() {
        var memory = new PlainMemoryDevice(MEMORY_SIZE);
        var registerFile = new RegisterFile(Pep10Registers.NUM_BYTE_REGISTERS, Pep10Registers.NUM_WORD_REGISTERS);
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
        var registerFile = new DebugRegisterFile(Pep10Registers.NUM_BYTE_REGISTERS, Pep10Registers.NUM_WORD_REGISTERS);
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
