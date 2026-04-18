package edu.depauw.dep10.simulator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.Scanner;

import edu.depauw.dep10.Operation;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

public class State {
    private Word A;
    private Word X;
    private Word PC;
    private Word SP;
    private UByte IR1;
    private Word IR2;
    private UByte Flags;
    private boolean running;

    private UByte[] memory = new UByte[65536];
    
    private InputStream in = System.in;
    private PrintStream out = System.out;
    private PrintStream err = System.err;

    public State() {
        A = Word.of(0);
        X = Word.of(0);
        PC = Word.of(0);
        SP = Word.of(0);
        IR1 = UByte.of(0);
        IR2 = Word.of(0);
        Flags = UByte.of(0);
        running = true;

        for (int i = 0; i < memory.length; i++) {
            memory[i] = UByte.of(0);
        }
    }

    public UByte mem1(Word addr) {
        // TODO check permissions
        if (addr.equals(Operation.CHARIN)) {
            try {
                return UByte.of(in.read());
            } catch (IOException e) {
                haltWithError("Unable to read from console input");
            }
        }

        return memory[addr.value()];
    }

    public Word mem2(Word addr) {
        var hi = mem1(addr);
        var lo = mem1(addr.plus(1));
        return Word.of((hi.value() << 8) + lo.value());
    }

    public void setMem1(Word addr, UByte n) {
        // TODO check permissions
        if (addr.equals(Operation.SHUTDOWN)) {
            stop();
        } else if (addr.equals(Operation.CHAROUT)) {
            out.write(n.value());
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
        this.A = Word.of(n.value());
    }

    public void setX(Word n) {
        this.X = n;
    }

    public void setX(UByte n) {
        this.X = Word.of(n.value());
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
        Flags = UByte.of(flags.value() & 0x0F);
    }

    public UByte getFlags() {
        return Flags;
    }

    public boolean isRunning() {
        return running;
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
}
