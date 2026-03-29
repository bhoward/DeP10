package edu.depauw.dep10.simulator;

import java.util.ArrayList;
import java.util.List;

import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

public class DebugState extends State {
    private List<MemoryAccess> accesses;
    
    public DebugState() {
        this.accesses = new ArrayList<>();
    }

    @Override
    public UByte mem1(Word addr) {
        var value = super.mem1(addr);
        accesses.add(new MemoryAccess.RB(addr, value));
        return value;
    }

    @Override
    public void setMem1(Word addr, UByte n) {
        super.setMem1(addr, n);
        accesses.add(new MemoryAccess.WB(addr, n));
    }

    public Trace trace() {
        var t = new Trace(getA(), getX(), getSP(), getFlags(), List.copyOf(accesses));
        clearAccesses();
        return t;
    }

    public void clearAccesses() {
        accesses.clear();
    }
}
