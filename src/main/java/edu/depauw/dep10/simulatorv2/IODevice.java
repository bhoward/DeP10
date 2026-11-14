package edu.depauw.dep10.simulatorv2;

import edu.depauw.dep10.simulatorv2.MMIOState.MStep;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

public interface IODevice {
    boolean canRead(Word address);

    boolean canFetch(Word address);

    boolean canWrite(Word address);

    default UByte readB(Word offset, MStep current) {
        return UByte.of(0);
    }

    default UByte fetchB(Word offset, MStep current) {
        return UByte.of(0);
    }

    default void writeB(Word offset, UByte b, MStep current) {
        // do nothing
    }
}
