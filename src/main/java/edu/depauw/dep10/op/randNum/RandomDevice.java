package edu.depauw.dep10.op.randNum;
import edu.depauw.dep10.simulator.State;
import edu.depauw.dep10.util.UByte;

public final class RandomDevice implements Device {
    public static final int BASE = 0xFF40;   // placeholder address from the design doc

    private RandSource src;
    private int latch;
    private int seedHi;                      // holds the high seed byte until the low byte arrives

    public RandomDevice(RandSource src) { this.src = src; }

    public int base() { return BASE; }
    public int size() { return 16; }

    // offsets: 0-1 data (read), 2-3 seed (write), 4 status (read)
    public UByte read(int offset) {
        switch (offset) {
            case 0:  latch = src.nextWord(); return UByte.of(latch >> 8);
            case 1:  return UByte.of(latch);
            case 4:  return UByte.of(1);     // always ready
            default: return UByte.of(0);
        }
    }

    public void write(int offset, UByte b) {
        if (offset == 2) {
            seedHi = b.value();
        } else if (offset == 3 && src instanceof SeededSource s) {
            s.setSeed((seedHi << 8) | b.value());
        }
    }

    public void tick(State s) { }
    public boolean interruptPending() { return false; }
    public void reset() { latch = 0; seedHi = 0; }

    public Object saveState() { return new Object[] { src.saveState(), latch, seedHi }; }
    public void restoreState(Object o) {
        Object[] a = (Object[]) o;
        src.restoreState(a[0]);
        latch = (Integer) a[1];
        seedHi = (Integer) a[2];
    }
    public boolean reversible() { return src.reversible(); }
}