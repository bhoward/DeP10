package edu.depauw.dep10.simulator2.common;

import edu.depauw.dep10.simulator2.Action;
import edu.depauw.dep10.simulator2.DebugState;
import edu.depauw.dep10.simulator2.Device;
import edu.depauw.dep10.simulator2.Fault;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

/**
 * A real time clock, mounted over a 16-byte block of memory. Time is simulated:
 * the clock counts calls to {@link #tick()}, so one tick is one executed
 * instruction, and {@link #untick()} takes one tick back when the debugger
 * steps backward. Every value the program can read is computed from the tick
 * count, so it is correct again after an undo.
 * <p>
 * Layout, as byte offsets from the base address:
 * <ul>
 * <li>0 to 7: counter, the 64-bit tick count, read only, high byte first.
 * Reading the byte at offset 0 copies the whole counter into a hidden latch,
 * and the other bytes are read from the latch. A program that reads the four
 * words in order gets one consistent value; reading them in another order can
 * give a torn value.</li>
 * <li>8 to 11: compare, a 32-bit value, high byte first.</li>
 * <li>12: control. Bit 0 enables the clock; bit 1 is the interrupt enable,
 * which is stored but not used yet (the first version is poll only).</li>
 * <li>13: status, read only. Bit 0 is set while the clock is enabled and the
 * low 32 bits of the counter are at least the compare value (unsigned). It is
 * computed, not stored.</li>
 * <li>14 and 15: unused.</li>
 * </ul>
 * In debug mode (when the state is a {@link DebugState}) writes are recorded so
 * that stepping back restores them. The latch is hidden state and is cleared on
 * {@link #untick()}.
 */
public class ClockDevice implements Device {
    public static final int SIZE = 16;
    public static final int COUNTER = 0;
    public static final int COMPARE = 8;
    public static final int CONTROL = 12;
    public static final int STATUS = 13;

    private final Word base;
    private long ticks = 0;
    private Long latch = null;
    private long compare = 0; // only the low 32 bits are used
    private int control = 0;

    /**
     * Construct a clock to be mounted at the given base address.
     * 
     * @param baseAddress
     */
    public ClockDevice(Word baseAddress) {
        this.base = baseAddress;
    }

    /** @return the number of ticks so far */
    public long getTicks() {
        return ticks;
    }

    private int offset(Word address) {
        return address.value() - base.value(); // Only use if address >= base
    }

    private boolean inBlock(Word address) {
        return base.value() <= address.value() && offset(address) < SIZE;
    }

    private boolean statusBit() {
        boolean enabled = (control & 1) != 0;
        return enabled && Long.compareUnsigned(ticks & 0xFFFFFFFFL, compare & 0xFFFFFFFFL) >= 0;
    }

    @Override
    public void tick() {
        ticks++;
    }

    @Override
    public void untick() {
        if (ticks > 0) {
            ticks--;
        }
        latch = null;
    }

    @Override
    public boolean canFetchB(Word address) {
        return false;
    }

    @Override
    public boolean canReadB(Word address) {
        int off = offset(address);
        return inBlock(address) && off <= STATUS;
    }

    @Override
    public boolean canWriteB(Word address) {
        int off = offset(address);
        return inBlock(address) && (off >= COMPARE && off < CONTROL + 1);
    }

    @Override
    public UByte fetchB(Word address) throws Fault {
        throw new Fault("Cannot execute from the clock");
    }

    @Override
    public UByte readB(Word address) throws Fault {
        int off = offset(address);
        if (off < COMPARE) {
            if (off == COUNTER) {
                latch = ticks;
            }
            long value = (latch != null) ? latch : ticks;
            int shift = 8 * (7 - off);
            return UByte.of((int) ((value >>> shift) & 0xFF));
        } else if (off < CONTROL) {
            int shift = 8 * (3 - (off - COMPARE));
            return UByte.of((int) ((compare >>> shift) & 0xFF));
        } else if (off == CONTROL) {
            return UByte.of(control);
        } else if (off == STATUS) {
            return UByte.of(statusBit() ? 1 : 0);
        }
        throw new Fault("Nothing to read in the clock at offset " + off);
    }
    
    private record ChangeCompare(ClockDevice clock, long oldCompare, long newCompare) implements Action {
        @Override
        public void undo(DebugState state) {
            clock.compare = oldCompare;
        }

        @Override
        public String getTrace() {
            return "clock/compare: %d->%d".formatted(oldCompare, newCompare);
        }
    }
    
    private record ChangeControl(ClockDevice clock, int oldControl, int newControl) implements Action {
        @Override
        public void undo(DebugState state) {
            clock.control = oldControl;
        }

        @Override
        public String getTrace() {
            return "clock/control: %02X->%02X".formatted(oldControl, newControl);
        }
    }

    @Override
    public void writeB(Word address, UByte value) throws Fault {
        int off = offset(address);

        if (off >= COMPARE && off < CONTROL) {
            int shift = 8 * (3 - (off - COMPARE));
            long mask = 0xFFL << shift;
            compare = (compare & ~mask) | ((long) value.value() << shift);
        } else if (off == CONTROL) {
            control = value.value() & 0x03;
        } else {
            throw new Fault("Cannot write to the clock at offset " + off);
        }
    }

    @Override
    public void writeB(Word address, UByte value, DebugState state) throws Fault {
        var oldCompare = compare;
        var oldControl = control;
        writeB(address, value);
        if (compare != oldCompare) {
            state.record(new ChangeCompare(null, oldCompare, compare));
        } else if (control != oldControl) {
            state.record(new ChangeControl(null, oldControl, control));
        }
    }
}
