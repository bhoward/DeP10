package edu.depauw.dep10.simulator2.pep10;

import edu.depauw.dep10.simulator2.State;
import edu.depauw.dep10.simulator2.common.RegisterFile.ByteRegister;
import edu.depauw.dep10.simulator2.common.RegisterFile.FlagRegister;
import edu.depauw.dep10.simulator2.common.RegisterFile.WordRegister;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

/**
 * "Mixin" interface to allow convenient access to the elements of the Pep/10
 * register file.
 */
public interface Pep10Registers extends State {
    static final int NUM_BYTE_REGISTERS = 2;

    static final ByteRegister FLAGS = new ByteRegister("Flags", 0);
    static final ByteRegister IR1 = new ByteRegister("IR1", 1);

    static final int NUM_WORD_REGISTERS = 6;

    static final WordRegister A = new WordRegister("A", 0);
    static final WordRegister X = new WordRegister("X", 1);
    static final WordRegister PC = new WordRegister("PC", 2);
    static final WordRegister SP = new WordRegister("SP", 3);
    static final WordRegister IR2 = new WordRegister("IR2", 4);
    static final WordRegister EA = new WordRegister("EA", 5);

    static final FlagRegister N = new FlagRegister("N", 0, 3);
    static final FlagRegister Z = new FlagRegister("Z", 0, 2);
    static final FlagRegister V = new FlagRegister("V", 0, 1);
    static final FlagRegister C = new FlagRegister("C", 0, 0);

    default UByte getFlags() {
        return get(FLAGS);
    }

    default void setFlags(UByte value) {
        put(FLAGS, value);
    }

    default UByte getOpCode() {
        return get(IR1);
    }

    default void setOpCode(UByte value) {
        put(IR1, value);
    }

    default Word getA() {
        return get(A);
    }

    default void setA(Word value) {
        put(A, value);
    }
    
    default void setA(UByte value) {
        put(A, UByte.of(0).with(value));
    }

    default Word getX() {
        return get(X);
    }

    default void setX(Word value) {
        put(X, value);
    }
    
    default void setX(UByte value) {
        put(X, UByte.of(0).with(value));
    }

    default Word getPC() {
        return get(PC);
    }

    default void setPC(Word value) {
        put(PC, value);
    }

    default Word getSP() {
        return get(SP);
    }

    default void setSP(Word value) {
        put(SP, value);
    }

    default Word getOperand() {
        return get(IR2);
    }

    default void setOperand(Word value) {
        put(IR2, value);
    }

    default Word getEA() {
        return get(EA);
    }

    default void setEA(Word value) {
        put(EA, value);
    }

    default boolean getN() {
        return get(N);
    }

    default void setN(boolean value) {
        put(N, value);
    }

    default boolean getZ() {
        return get(Z);
    }

    default void setZ(boolean value) {
        put(Z, value);
    }

    default boolean getV() {
        return get(V);
    }

    default void setV(boolean value) {
        put(V, value);
    }

    default boolean getC() {
        return get(C);
    }

    default void setC(boolean value) {
        put(C, value);
    }
}