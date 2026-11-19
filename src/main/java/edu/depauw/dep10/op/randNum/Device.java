package edu.depauw.dep10.op.randNum;

import edu.depauw.dep10.simulator.State;
import edu.depauw.dep10.util.UByte;

public interface Device {
    int base();                        // first address of this device's block
    int size();                        // number of bytes it owns
    UByte read(int offset);            // offset is relative to base(); may have side effects
    void write(int offset, UByte b);
    void tick(State s);                // called once per instruction
    boolean interruptPending();        // for the shared trap path
    void reset();                      // return to power-on state

    // added for reversibility (back-stepping in debug mode)
    Object saveState();                // snapshot of everything that changes
    void restoreState(Object snapshot);
    boolean reversible();              // false means back-stepping must stop
}