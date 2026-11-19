package edu.depauw.dep10.op.randNum;

public final class SeededSource implements RandSource {
    private static final long MULT = 0x5DEECE66DL, ADD = 0xBL;
    private static final long MASK = (1L << 48) - 1;
    private long state;

    public SeededSource(long seed) { setSeed(seed); }

    public void setSeed(long seed) { state = (seed ^ MULT) & MASK; }

    public int nextWord() {
        state = (state * MULT + ADD) & MASK;
        return (int) (state >>> 32) & 0xFFFF;   // same as Random.next(16)
    }

    public Object saveState()            { return state; }
    public void restoreState(Object s)   { state = (Long) s; }
    public boolean reversible()          { return true; }
}