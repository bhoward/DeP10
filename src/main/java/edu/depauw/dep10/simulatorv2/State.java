package edu.depauw.dep10.simulatorv2;

import edu.depauw.dep10.simulatorv2.RegisterFile.BReg;
import edu.depauw.dep10.simulatorv2.RegisterFile.FReg;
import edu.depauw.dep10.simulatorv2.RegisterFile.WReg;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

public interface State {
    /**
     * Read a byte from the given address. Might require read permission.
     * 
     * @param address
     * @return
     */
    UByte readB(Word address);

    /**
     * Read a byte to be executed from the given address. Might require execute
     * permission.
     * 
     * @param address
     * @return
     */
    UByte fetchB(Word address);

    /**
     * Write a byte to the given address. Might require write permission.
     * 
     * @param address
     * @param b
     */
    void writeB(Word address, UByte b);

    default Word readW(Word address) {
        var hi = readB(address);
        var lo = readB(address.plus(1));
        return hi.with(lo);
    }

    default Word fetchW(Word address) {
        var hi = fetchB(address);
        var lo = fetchB(address.plus(1));
        return hi.with(lo);
    }

    default void writeW(Word address, Word w) {
        writeB(address, w.hi());
        writeB(address.plus(1), w.lo());
    }

    Word getWReg(WReg reg);

    UByte getBReg(BReg reg);

    boolean getFlag(FReg flag);

    void setWReg(WReg reg, Word w);

    void setBReg(BReg reg, UByte b);

    void setFlag(FReg flag, boolean b);

    Step commit();
    
    void rollback();

    void undo(Step step);
}
