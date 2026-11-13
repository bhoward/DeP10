package edu.depauw.dep10.simulator2;

import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

public interface Device {
    boolean canFetchB(Word address);

    boolean canReadB(Word address);

    boolean canWriteB(Word address);

    // TODO Note that the State arguments are for debug versions of devices
    // to record their actions in a Step for undoing
    
    UByte fetchB(Word address, State state) throws Fault;

    UByte readB(Word address, State state) throws Fault;

    void writeB(Word address, UByte value, State state) throws Fault;

    // Override the following if accessing a word for a device
    // shouldn't be done one byte at a time
    default boolean canFetchW(Word address) {
        return canFetchB(address) && canFetchB(address.plus(1));
    }

    default boolean canReadW(Word address) {
        return canReadB(address) && canReadB(address.plus(1));
    }

    default boolean canWriteW(Word address) {
        return canWriteB(address) && canWriteB(address.plus(1));
    }

    default Word fetchW(Word address, State state) throws Fault {
        var hi = fetchB(address, state);
        var lo = fetchB(address.plus(1), state);
        return hi.with(lo);
    }

    default Word readW(Word address, State state) throws Fault {
        var hi = readB(address, state);
        var lo = readB(address.plus(1), state);
        return hi.with(lo);
    }

    default void writeW(Word address, Word value, State state) throws Fault {
        writeB(address, value.hi(), state);
        writeB(address.plus(1), value.lo(), state);
    }
}
