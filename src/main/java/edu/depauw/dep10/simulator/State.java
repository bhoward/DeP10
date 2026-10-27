package edu.depauw.dep10.simulator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.Scanner;

import edu.depauw.dep10.op.Operation;
import edu.depauw.dep10.op.Pep10;
import edu.depauw.dep10.op.Table;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

public class State {
    private Word A;
    private Word X;
    private Word H;
    private Word PC;
    private Word SP;
    private UByte PRE;
    private UByte IR1;
    private Word IR2;
    private Word EA;
    private UByte Flags;
    private boolean running;
    private Operation op;

    // Fancy machine: supervisor (protected) mode, timer and invalid-instruction trap.
    // Off by default, so bare-metal programs see the original machine.
    private boolean fancy = false;
    private boolean protectedMode = true; // the machine boots in the OS
    private boolean startedInUser = false;   // the current instruction began in user mode
    private boolean timerExpired = false;
    // Memory protection: what to undo if the current user-mode instruction faults.
    private final java.util.ArrayList<int[]> undoLog = new java.util.ArrayList<>();
    private Word saveA, saveX, saveH, saveSP;
    private UByte saveFlags;    // the timer hit zero but the trap waits for user mode

    private UByte[] memory = new UByte[65536];
    
    private InputStream in = System.in;
    private PrintStream out = System.out;
    private PrintStream err = System.err;

    public State() {
        A = Word.of(0);
        X = Word.of(0);
        H = Word.of(0);
        PC = Word.of(0);
        SP = Word.of(0);
        PRE = UByte.of(0);
        IR1 = UByte.of(0);
        IR2 = Word.of(0);
        EA = Word.of(0);
        Flags = UByte.of(0);
        running = false;
        op = null;

        for (int i = 0; i < memory.length; i++) {
            memory[i] = UByte.of(0);
        }
    }

    void initialize(Word initPC, Word initSP) {
        if (fancy) {
            // The device registers (fault address, table base, timer: 0xFFF1 to 0xFFF6) start at 0.
            // The stock OS images keep the end of a text message there.
            for (int a = Pep10.FAULT_ADDRESS.value(); a < Pep10.TRAP_HANDLER_POINTER.value(); a++) {
                memory[a] = UByte.of(0);
            }
        }
        setPC(mem2(initPC));
        setSP(mem2(initSP));
        running = true;
    }
    
    public UByte mem1(Word addr) {
        checkAccess(addr, false);
        if (addr.equals(Pep10.CHARIN)) {
            try {
                return UByte.of(in.read());
            } catch (IOException e) {
                haltWithError("Unable to read from console input");
            }
        }

        return memory[addr.value()];
    }

    /** Reads an instruction byte; in user mode the page must be executable. */
    public UByte fetch1(Word addr) {
        checkFetch(addr);
        return mem1(addr);
    }

    /** Reads an operand specifier from the instruction stream. */
    public Word fetch2(Word addr) {
        checkFetch(addr);
        checkFetch(addr.plus(1));
        return mem2(addr);
    }

    /**
     * The two protection bits for a page, read from the table the word at 0xFFF3 points to:
     * 0 (00) no user access, 2 (10) read, 3 (11) read and write, 1 (01) read and execute.
     */
    private int protection(int page) {
        int base = mem2Safe(Pep10.MMU_BASE).value();
        int entry = memory[(base + page / 4) & 0xFFFF].value();
        return (entry >> (6 - 2 * (page % 4))) & 3;
    }

    /** True while the memory protection table is being enforced: user mode on the fancy machine with a table set. */
    public boolean isProtecting() {
        return fancy && !protectedMode && mem2Safe(Pep10.MMU_BASE).value() != 0;
    }

    private void checkAccess(Word addr, boolean write) {
        if (!isProtecting()) {
            return;
        }
        int page = addr.value() >> 8;
        int bits = protection(page);
        boolean ok = page != 0xFF && (write ? bits == 3 : bits != 0);
        if (!ok) {
            throw new MemoryFault(addr, MemoryFault.DATA);
        }
    }

    private void checkFetch(Word addr) {
        if (!isProtecting()) {
            return;
        }
        int page = addr.value() >> 8;
        if (page == 0xFF || protection(page) != 1) {
            throw new MemoryFault(addr, MemoryFault.FETCH);
        }
    }

    public UByte mem1Safe(Word addr) {
        return memory[addr.value()];
    }
    
    public Word mem2(Word addr) {
        checkAccess(addr, false);
        checkAccess(addr.plus(1), false);
        var hi = mem1(addr);
        var lo = mem1(addr.plus(1));
        return Word.of((hi.value() << 8) + lo.value());
    }

    public Word mem2Safe(Word addr) {
        var hi = mem1Safe(addr);
        var lo = mem1Safe(addr.plus(1));
        return Word.of((hi.value() << 8) + lo.value());
    }

    public void setMem1(Word addr, UByte n) {
        checkAccess(addr, true);
        if (fancy && !protectedMode) {
            undoLog.add(new int[] { addr.value(), memory[addr.value()].value() });
        }
        if (addr.equals(Pep10.SHUTDOWN)) {
            stop();
        } else if (addr.equals(Pep10.CHAROUT)) {
            out.write(n.value());
        }

        memory[addr.value()] = n;
    }

    public void setMem2(Word addr, Word n) {
        checkAccess(addr, true); // both bytes are checked before either is written
        checkAccess(addr.plus(1), true);
        setMem1(addr, n.hi());
        setMem1(addr.plus(1), n.lo());
    }

    public Word getA() {
        return A;
    }

    public Word getX() {
        return X;
    }
    
    public Word getH() {
        return H;
    }

    public Word getPC() {
        return PC;
    }

    public Word getSP() {
        return SP;
    }

    public UByte getPrefix() {
        return PRE;
    }

    public UByte getOpCode() {
        return IR1;
    }

    public Word getOperand() {
        return IR2;
    }

    public Word getEA() {
        return EA;
    }

    public void setA(Word n) {
        this.A = n;
    }

    public void setA(UByte n) {
        this.A = Word.of(n.value());
    }

    public void setX(Word n) {
        this.X = n;
    }

    public void setX(UByte n) {
        this.X = Word.of(n.value());
    }
    
    public void setH(Word n) {
        this.H = n;
    }

    public void setPC(Word n) {
        this.PC = n;
    }

    public void setSP(Word n) {
        this.SP = n;
    }

    public void setPrefix(UByte opCode) {
        PRE = opCode;
    }

    public void setOpCode(UByte n) {
        this.IR1 = n;
    }

    public void setOperand(Word n) {
        this.IR2 = n;
    }

    public void setEA(Word address) {
        EA = address;
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
        Flags = UByte.of(flags.value() & 0x0F);
    }

    /**
     * NZVC in the low four bits. On the fancy machine bit 7 is P: 1 in protected (supervisor) mode,
     * 0 in user mode. A trap sets it and pushes the old value in the frame; SRET takes it back from
     * the frame (restoreMode). MOVAFLG cannot change it (setFlags ignores everything but NZVC).
     */
    public UByte getFlags() {
        return fancy && protectedMode ? UByte.of(Flags.value() | 0x80) : Flags;
    }

    public boolean isRunning() {
        return running;
    }
    
    public Operation getOp() {
        return op;
    }
    
    public void setOp(Operation op) {
        this.op = op;
    }
    
    public void pause() {
        running = false;
        flushIO();
    }
    
    public void stop() {
        pause();
        shutdownIO();
    }
    
    public void resume() {
        running = true;
    }
    
    public void loadFile(String param) {
        try (var scanner = new Scanner(new File(param))) {
            load(scanner);
        } catch (FileNotFoundException e) {
            haltWithError("Unable to load " + param);
        }
    }
    
    public void loadResource(String resource) {
        URL url = getClass().getResource(resource);
        if (url == null) {
            haltWithError("Unable to open resource " + resource);
        }

        try (var scanner = new Scanner(url.openStream())) {
            load(scanner);
        } catch (IOException e) {
            haltWithError("Unable to open resource " + resource);
        }
    }
    
    public void loadString(String object) {
    	try (var scanner = new Scanner(object)) {
    		load(scanner);
    	}
    }

    private void load(Scanner scanner) {
        int addr = 0;

        while (scanner.hasNext()) {
            String token = scanner.next();
            if (token.startsWith("[")) {
                addr = Integer.parseInt(token.substring(1, 5), 16);
                // TODO also handle protection bits
            } else {
                var value = Integer.parseInt(token, 16);
                memory[addr] = UByte.of(value);
                addr = (addr + 1) % memory.length;
            }
        }
    }

    public void dump(String memDump) {
        try (var dumpOut = new FileOutputStream(memDump)) {
            var mem = new byte[memory.length];
            for (int i = 0; i < memory.length; i++) {
                mem[i] = (byte) memory[i].value();
            }
            
            dumpOut.write(mem);
        } catch (IOException e) {
            err.println("Error writing memory dump: " + e.getMessage());
        }
    }

    private void haltWithError(String message) {
        err.println(message);
        stop();
    }
    
    private void flushIO() {
        out.flush();
        if (err != System.err) {
            err.flush(); // System.err is autoflushing
        }
    }

    private void shutdownIO() {
        if (in != System.in) {
            try {
                in.close();
            } catch (IOException e) {
                err.println("Unable to close console input");
            }
        }
        
        if (out != System.out) {
            out.close();
        }
        
        if (err != System.err) {
            err.close();
        }
    }

    public void setInput(String consoleIn) {
        if (consoleIn != null) {
            try {
                in = new FileInputStream(new File(consoleIn));
            } catch (FileNotFoundException e) {
                haltWithError("Unable to open console input: " + consoleIn);
            }
        } else {
            in = System.in;
        }
    }
    
    public void setInput(InputStream is) {
        in = is;
    }
    
    public void setOutput(String consoleOut) {
        if (consoleOut != null) {
            try {
                out = new PrintStream(new File(consoleOut));
            } catch (FileNotFoundException e) {
                haltWithError("Unable to open console output: " + consoleOut);
            }
        } else {
            out = System.out;
        }
    }
    
    public void setOutput(OutputStream os) {
        out = new PrintStream(os);
    }
    
    public void setError(String errOut) {
        if (errOut != null) {
            try {
                err = new PrintStream(new File(errOut));
            } catch (FileNotFoundException e) {
                haltWithError("Unable to open error output: " + errOut);
            }
        } else {
            err = System.err;
        }
    }
    
    public void setError(OutputStream os) {
        err = new PrintStream(os);
    }

    public boolean isFancy() {
        return fancy;
    }

    /** Turns the fancy machine (protected/user modes, timer, invalid-instruction trap) on or off. */
    public void setFancy(boolean fancy) {
        this.fancy = fancy;
        this.timerExpired = false;
    }

    /** True while the OS is running (supervisor mode); false while a user program runs. Meaningful only on the fancy machine. */
    public boolean isProtectedMode() {
        return protectedMode;
    }

    /**
     * Called by SRET with the flags byte it popped from the trap frame: bit 7 (P) says which mode
     * the frame came from, and that is the mode SRET returns to. A frame pushed by a system call
     * made from the OS therefore returns to the OS (system calls can nest), and a frame pushed
     * from a user program returns to user mode. Does nothing on the plain machine.
     */
    public void restoreMode(UByte savedFlags) {
        if (fancy) {
            protectedMode = (savedFlags.value() & 0x80) != 0;
        }
    }

    /**
     * Pushes the 12-byte trap frame on the system stack (flags at +0, A +1, X +3, PC +5, SP +7,
     * opcode +9, operand +10), enters protected mode and jumps to the trap handler.
     * SCALL uses its own opcode and raw operand. The machine's own traps use opcode 0:
     * operand 1 is the timer, operand 2 an invalid instruction (the bad opcode is then in the
     * frame's opcode byte, so an invalid opcode 0 is told apart from the timer by its operand).
     */
    public void trap(UByte opcode, Word operand) {
        var savedFlags = getFlags(); // includes P, the mode that is being interrupted
        if (fancy) {
            protectedMode = true; // the trap itself runs as the OS, so the protection table does not apply to it
        }
        var t = mem2(Pep10.SYSTEM_STACK_POINTER);

        setMem2(t.plus(-2), operand);
        setMem1(t.plus(-3), opcode);
        setMem2(t.plus(-5), SP);
        setMem2(t.plus(-7), PC);
        setMem2(t.plus(-9), X);
        setMem2(t.plus(-11), A);
        setMem1(t.plus(-12), savedFlags);

        setSP(t.plus(-12));
        setPC(mem2(Pep10.TRAP_HANDLER_POINTER));
    }

    /** Called when the current instruction is not defined. */
    public void invalidInstruction() {
        if (!fancy) {
            throw new IllegalStateException("Unimplemented operation");
        }
        if (protectedMode) {
            err.println(String.format("Invalid instruction 0x%02X at 0x%04X in supervisor mode; stopping",
                    IR1.value(), PC.plus(-1).value()));
            stop();
        } else {
            trap(IR1, Word.of(2));
        }
    }

    /**
     * Called after each instruction that actually ran. The timer counts down on every instruction,
     * in supervisor mode too, like a device that does not care what mode the CPU is in. If it
     * reaches zero while the OS is running, the trap waits until the program is back in user mode
     * and has run one instruction (so the OS can always make progress), which stops a program
     * from getting a bigger slice by making many system calls.
     */
    public void instructionDone() {
        if (!fancy) {
            return;
        }
        int t = mem2Safe(Pep10.TIMER).value();
        if (t != 0) {
            timerExpired = false; // the OS has set a fresh timer
            t--;
            memory[Pep10.TIMER.value()] = UByte.of(t >> 8);
            memory[Pep10.TIMER.value() + 1] = UByte.of(t & 0xFF);
            if (t == 0) {
                timerExpired = true;
            }
        }
        if (timerExpired && startedInUser && !protectedMode) {
            timerExpired = false;
            trap(UByte.of(0), Word.of(1));
        }
    }

    public void doStep(Controller control, Table table) {
        var origPC = PC;
        startedInUser = fancy && !protectedMode;
        if (startedInUser) {
            // Remember enough to undo this instruction if memory protection stops it
            saveA = A;
            saveX = X;
            saveH = H;
            saveSP = SP;
            saveFlags = Flags;
            undoLog.clear();
        }

        PRE = UByte.of(0);
        try {
            IR1 = fetch1(PC);
            PC = PC.plus(1);

            var op = table.getOp(IR1);
            op.perform(this, origPC, control);
        } catch (MemoryFault fault) {
            if (!startedInUser) {
                throw fault;
            }
            protectionTrap(fault, origPC);
        }
    }

    /** Undoes the faulting instruction and traps: opcode byte 0, operand 3 (data) or 4 (fetch), address at 0xFFF1. */
    private void protectionTrap(MemoryFault fault, Word origPC) {
        for (int i = undoLog.size() - 1; i >= 0; i--) {
            memory[undoLog.get(i)[0]] = UByte.of(undoLog.get(i)[1]);
        }
        undoLog.clear();
        A = saveA;
        X = saveX;
        H = saveH;
        SP = saveSP;
        Flags = saveFlags;
        PRE = UByte.of(0);
        PC = origPC;
        memory[Pep10.FAULT_ADDRESS.value()] = fault.address().hi();
        memory[Pep10.FAULT_ADDRESS.value() + 1] = fault.address().lo();
        trap(UByte.of(0), Word.of(fault.operand()));
    }
}
