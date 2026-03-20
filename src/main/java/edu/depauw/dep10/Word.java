package edu.depauw.dep10;

import java.util.Objects;

public class Word {
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
		Word other = (Word) obj;
		return value == other.value;
	}

	private static final int MAX_UNSIGNED = 0xFFFF;

    private static final Word ZERO = new Word(0);
	
	private final int value;
	
	private Word(int value) {
		this.value = value & MAX_UNSIGNED;
	}
	
	public static Word of(int value) {
	    if (value == 0) {
	        return ZERO;
	    } else {
	        return new Word(value);
	    }
	}
	
	public int value() {
		return value;
	}

	public Word plus(int i) {
		return new Word(value + i);
	}
	
	public Word plus(Word n) {
		return new Word(value + n.value);
	}
	
	public Word minus(Word n) {
		return new Word(value - n.value);
	}
	
	public UByte minus(UByte n) {
		return UByte.of(value - n.value());
	}
	
	public Word and(Word n) {
		return new Word(value & n.value);
	}
	
	public Word or(Word n) {
		return new Word(value | n.value);
	}
	
	public Word xor(Word n) {
		return new Word(value ^ n.value);
	}
	
	public UByte hi() {
		return UByte.of(value >> 8);
	}

	public UByte lo() {
		return UByte.of(value);
	}

	public boolean bit(int i) {
		return (value & (1 << i)) != 0;
	}

	public Word negate() {
		return new Word(-value);
	}

	public boolean isNegative() {
		return bit(15);
	}

	public boolean isZero() {
		return value == 0;
	}
	
	public boolean lessThan(Word n) {
		return value < n.value;
	}

	public Word shiftLeft(boolean carry) {
		return new Word((value << 1) | (carry ? 1 : 0));
	}

	public Word shiftRight(boolean carry) {
		return new Word((value >> 1) | (carry ? 1 << 15 : 0));
	}

	public Word not() {
		return new Word(~value);
	}
	
	@Override
	public String toString() {
	    return Integer.toHexString(value);
	}
}
