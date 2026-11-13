package edu.depauw.dep10.simulator2;

import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

public interface State {
    boolean isRunning();

    void run();

    void pause();

    void resume();

    UByte fetchB(Word address) throws Fault;

    UByte readB(Word address) throws Fault;

    void writeB(Word address, UByte value) throws Fault;

    Word readW(Word address) throws Fault;

    Word fetchW(Word address) throws Fault;

    void writeW(Word address, Word value) throws Fault;
    
    UByte get(ByteRegister register);
    
    void put(ByteRegister register, UByte value);
    
    Word get(WordRegister register);
    
    void put(WordRegister register, Word value);
    
    boolean get(FlagRegister flag);
    
    void put(FlagRegister flag, boolean value);
}
