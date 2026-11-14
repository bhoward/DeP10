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
    
    public static final Word TRAP_HANDLER_POINTER = Word.of(0xFFF7);
    public static final Word INITIAL_PC = Word.of(0xFFF9);
    public static final Word INITIAL_SP = Word.of(0xFFFB);
    public static final Word CHARIN = Word.of(0xFFFD);
    public static final Word CHAROUT = Word.of(0xFFFE);
    public static final Word SHUTDOWN = Word.of(0xFFFF);

    private static final Table TABLE;

    static {
        TABLE = new Table();
        Pep10Table.installPep10Base(TABLE);
    }

    @Override
    public Pep10State init() {
        var memory = new PlainMemoryDevice(MEMORY_SIZE);
        var registerFile = new RegisterFile(Pep10Registers.NUM_BYTE_REGISTERS, Pep10Registers.NUM_WORD_REGISTERS);
        var state = new Pep10State(memory, registerFile);

        var console = new ConsoleDevice(CHARIN);
        var shutdown = new ShutdownDevice(SHUTDOWN);

        state.mount(console);
        state.mount(shutdown);
        
        // TODO need to load memory contents and initialize registers
        
        return state;
    }

    @Override
    public Pep10DebugState initDebug() {
        var memory = new DebugPlainMemoryDevice(MEMORY_SIZE);
        var registerFile = new DebugRegisterFile(Pep10Registers.NUM_BYTE_REGISTERS, Pep10Registers.NUM_WORD_REGISTERS);
        var state = new Pep10DebugState(memory, registerFile);

        var console = new DebugConsoleDevice(CHARIN);
        var shutdown = new ShutdownDevice(SHUTDOWN);

        state.mount(console);
        state.mount(shutdown);
        
        // TODO need to load memory contents and initialize registers
        
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
