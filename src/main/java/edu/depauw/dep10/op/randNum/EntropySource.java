package edu.depauw.dep10.op.randNum;
import java.security.SecureRandom;

public final class EntropySource implements RandSource {
    private final SecureRandom rng = new SecureRandom();
    public int nextWord()              { return rng.nextInt(1 << 16); }
    public Object saveState()          { return null; }
    public void restoreState(Object s) { }
    public boolean reversible()        { return false; }
}
