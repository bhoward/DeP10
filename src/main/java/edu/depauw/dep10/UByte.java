package edu.depauw.dep10;

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
	
	private final int value;
	
	public UByte(int value) {
		this.value = value & MAX_UNSIGNED;
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
}
