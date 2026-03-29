package edu.depauw.dep10.util;

import java.util.Objects;

public class UByte {
	@Override
	public int hashCode() {
		return Objects.hash(value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UByte other = (UByte) obj;
		return value == other.value;
	}

	private static final int MAX_UNSIGNED = 0xFF;

    private static final UByte ZERO = new UByte(0);
	
	private final int value;
	
	private UByte(int value) {
		this.value = value & MAX_UNSIGNED;
	}
	
	public static UByte of(int value) {
	    if (value == 0) {
	        return ZERO;
	    } else {
	        return new UByte(value);
	    }
	}
	
	public int value() {
		return value;
	}
	
	public boolean bit(int i) {
		return (value & (1 << i)) != 0;
	}

	public UByte withBit(int i, boolean b) {
		if (b) {
			return new UByte(value | (1 << i));
		} else {
			return new UByte(value & ~(1 << i));
		}
	}
	
	public boolean isNegative() {
		return bit(7);
	}
	
	public boolean isZero() {
		return value == 0;
	}
    
    @Override
    public String toString() {
        return Integer.toHexString(value);
    }
}
