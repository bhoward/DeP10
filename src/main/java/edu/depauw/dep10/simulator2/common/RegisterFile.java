package edu.depauw.dep10.simulator2.common;

import edu.depauw.dep10.simulator2.Action;
import edu.depauw.dep10.simulator2.DebugState;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

/**
 * Represents the registers (byte, word, and individual bit flag) of a
 * particular machine configuration. Internally, each register has an id number
 * for uniform access.
 */
public class RegisterFile {
    /**
     * Selector for a particular byte register with the given id number. The name
     * may be used for visualization or tracing.
     * 
     * @param name
     * @param id
     */
    public static record ByteRegister(String name, int id) {
    }

    /**
     * Selector for a particular word register with the given id number. The name
     * may be used for visualization or tracing.
     * 
     * @param name
     * @param id
     */
    public static record WordRegister(String name, int id) {
    }

    /**
     * Selector for a particular flag within a byte register with the given id
     * number. Position 0 is the low-order bit, and position 7 is the high-order
     * bit. The name may be used for visualization or tracing.
     * 
     * @param name
     * @param id
     * @param position
     */
    public static record FlagRegister(String name, int id, int position) {
    }

    private RegisterFileDescriptor descriptor;
    private UByte[] breg;
    private Word[] wreg;

    /**
     * Construct a RegisterFile with the given number of byte and word registers.
     * Any flags should be part of one of the byte registers, so they do not need to
     * be accounted for separately.
     * 
     * @param numByteRegisters
     * @param numWordRegisters
     */
    public RegisterFile(RegisterFileDescriptor descriptor) {
        this.descriptor = descriptor;
        this.breg = new UByte[descriptor.byteRegisters().length];
        this.wreg = new Word[descriptor.wordRegisters().length];
    }

    private RegisterFile(RegisterFile registerFile) {
        this.breg = registerFile.breg.clone();
        this.wreg = registerFile.wreg.clone();
    }

    /**
     * Get a byte from the given register.
     * 
     * @param register
     * @return the byte
     */
    public UByte get(ByteRegister register) {
        return breg[register.id()];
    }

    /**
     * Put a byte into the given register.
     * 
     * @param register
     * @param value
     */
    public void put(ByteRegister register, UByte value) {
        breg[register.id()] = value;
    }

    /**
     * Get a word from the given register.
     * 
     * @param register
     * @return the word
     */
    public Word get(WordRegister register) {
        return wreg[register.id()];
    }

    /**
     * Put a word into the given register.
     * 
     * @param register
     * @param value
     */
    public void put(WordRegister register, Word value) {
        wreg[register.id()] = value;
    }

    /**
     * Get a flag bit from the given register.
     * 
     * @param flag
     * @return the bit
     */
    public boolean get(FlagRegister flag) {
        return breg[flag.id()].bit(flag.position());
    }

    /**
     * Put a flag bit into the given register.
     * 
     * @param flag
     * @param value
     */
    public void put(FlagRegister flag, boolean value) {
        breg[flag.id()] = breg[flag.id()].withBit(flag.position(), value);
    }

    /**
     * Advance the simulation by one time step.
     */
    public void tick() {
        // Do nothing
    }

    // The following are for use in debug mode

    private record ByteRegisterPutAction(RegisterFile parent, ByteRegister register, UByte newValue, UByte oldValue)
            implements Action {
        @Override
        public void undo(DebugState state) {
            parent.put(register, oldValue);
        }

        @Override
        public String getTrace() {
            return "%s: %02X->%02X".formatted(register.name, oldValue.value(), newValue.value());
        }
    }

    private record WordRegisterPutAction(RegisterFile parent, WordRegister register, Word newValue, Word oldValue)
            implements Action {
        @Override
        public void undo(DebugState state) {
            parent.put(register, oldValue);
        }

        @Override
        public String getTrace() {
            return "%s: %04X->%04X".formatted(register.name, oldValue.value(), newValue.value());
        }
    }

    private record FlagRegisterPutAction(RegisterFile parent, FlagRegister flag, boolean newValue, boolean oldValue)
            implements Action {
        @Override
        public void undo(DebugState state) {
            parent.put(flag, oldValue);
        }

        @Override
        public String getTrace() {
            return "%s: %b->%b".formatted(flag.name, oldValue, newValue);
        }
    }

    /**
     * Put a byte into the given register, and record any action that might need to
     * be undone with the given state.
     * 
     * @param register
     * @param value
     * @param state
     */
    public void put(ByteRegister register, UByte value, DebugState state) {
        var action = new ByteRegisterPutAction(this, register, value, get(register));
        put(register, value);
        state.record(action);
    }

    /**
     * Put a byte into the given register, and record any action that might need to
     * be undone with the given state.
     * 
     * @param register
     * @param value
     * @param state
     */
    public void put(WordRegister register, Word value, DebugState state) {
        var action = new WordRegisterPutAction(this, register, value, get(register));
        put(register, value);
        state.record(action);
    }

    /**
     * Put a flag bit into the given register, and record any action that might need
     * to be undone with the given state.
     * 
     * @param register
     * @param value
     * @param state
     */
    public void put(FlagRegister flag, boolean value, DebugState state) {
        var action = new FlagRegisterPutAction(this, flag, value, get(flag));
        put(flag, value);
        state.record(action);
    }

    /**
     * Reverse the last tick(), if possible (in debug mode).
     */
    public void untick() {
        // Do nothing
    }

    /**
     * @return a copy (not backed by the same arrays) of this object
     */
    public RegisterFile copy() {
        return new RegisterFile(this);
    }

    /**
     * @return a string displaying the register contents, for tracing
     */
    public String display() {
        var builder = new StringBuilder();

        for (var register : descriptor.byteRegisters()) {
            builder.append(" %s=%02X".formatted(register.name, get(register).value()));
        }

        for (var register : descriptor.wordRegisters()) {
            builder.append(" %s=%04X".formatted(register.name, get(register).value()));
        }

        for (var register : descriptor.flagRegisters()) {
            builder.append(" %s=%s".formatted(register.name, get(register) ? "T" : "F"));
        }

        return builder.toString();
    }
}
