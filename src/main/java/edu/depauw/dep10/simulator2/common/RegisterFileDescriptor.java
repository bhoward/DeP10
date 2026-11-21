package edu.depauw.dep10.simulator2.common;

/**
 * Describes the collection of registers in a particular {@link RegisterFile}.
 */
public interface RegisterFileDescriptor {
    RegisterFile.ByteRegister[] byteRegisters();

    RegisterFile.WordRegister[] wordRegisters();

    RegisterFile.FlagRegister[] flagRegisters();
}
