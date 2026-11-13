package edu.depauw.dep10.simulator2;

import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

public class RegisterFile {
    private UByte[] breg;
    private Word[] wreg;
    
    public RegisterFile(int numByteRegisters, int numWordRegisters) {
        this.breg = new UByte[numByteRegisters];
        this.wreg = new Word[numWordRegisters];
    }

    public UByte get(ByteRegister register) {
        return breg[register.id()];
    }

    public void put(ByteRegister register, UByte value, State state) {
        breg[register.id()] = value;
    }

    public Word get(WordRegister register) {
        return wreg[register.id()];
    }

    public void put(WordRegister register, Word value, State state) {
        wreg[register.id()] = value;
    }

    public boolean get(FlagRegister flag) {
        return breg[flag.id()].bit(flag.position());
    }

    public void put(FlagRegister flag, boolean value, State state) {
        breg[flag.id()] = breg[flag.id()].withBit(flag.position(), value);
    }
}
