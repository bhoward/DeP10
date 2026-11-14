package edu.depauw.dep10.simulatorv2;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PushbackInputStream;
import java.util.List;

import edu.depauw.dep10.simulatorv2.RegisterFile.BReg;
import edu.depauw.dep10.simulatorv2.RegisterFile.FReg;
import edu.depauw.dep10.simulatorv2.RegisterFile.WReg;
import edu.depauw.dep10.util.Word;

public class Simulator {
    private boolean running;
    private PushbackInputStream in;
    private PrintStream out;

    private RegisterFile rf;
    private State state;

    // TODO move these into a Dep10... config class
    final BReg PRE = new BReg(0);
    final BReg IR1 = new BReg(1);
    final BReg FLAGS = new BReg(2);

    final WReg A = new WReg(0);
    final WReg X = new WReg(1);
    final WReg H = new WReg(2);
    final WReg PC = new WReg(3);
    final WReg SP = new WReg(4);
    final WReg IR2 = new WReg(5);
    final WReg EA = new WReg(6);

    final FReg P = new FReg(FLAGS, 7);
    final FReg N = new FReg(FLAGS, 3);
    final FReg Z = new FReg(FLAGS, 2);
    final FReg V = new FReg(FLAGS, 1);
    final FReg C = new FReg(FLAGS, 0);
    
    /*
     * State: collection of memory, mmio devices (with supporting data), and registers
     * Machine: knows how to create initial state, decode instructions
     * Controller: connects UI (text or graphical) to process of running through states (subsumes Simulator)
     * Operation:
     * Step:
     */

    public Simulator() {
        this.running = false;
        this.in = new PushbackInputStream(System.in);
        this.out = System.out;

        final var BREGS = List.of(PRE, IR1, FLAGS);
        final var WREGS = List.of(A, X, H, PC, SP, IR2, EA);
        final var FREGS = List.of(P, N, Z, V, C);
        final var info = new RegisterFile.RFInfo(BREGS, WREGS, FREGS);

        this.rf = new RegisterFile(info);

        var s = new MMIOState(new SimpleState(rf));
        s.install(new ConsoleDevice(Word.of(0xFFFD), this));
        s.install(new ShutdownDevice(Word.of(0xFFFF), this));

        this.state = s;
    }

    public void setRunning(boolean b) {
        running = b;
    }

    public boolean isRunning() {
        return running;
    }

    public int read() {
        try {
            return in.read();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return 0;
        }
    }

    public void unread(int ch) {
        try {
            in.unread(ch);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void write(int ch) {
        out.write(ch);
    }
}
