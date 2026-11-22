package edu.depauw.dep10.simulator2.common;

import edu.depauw.dep10.simulator2.Action;
import edu.depauw.dep10.simulator2.DebugState;
import edu.depauw.dep10.simulator2.Device;
import edu.depauw.dep10.simulator2.Fault;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

/**
 * The countdown timer of the fancy machine, as a memory-mapped device. The
 * operating system loads the number of instructions to run into the timer;
 * every tick (one per executed instruction, in either mode) takes one off, and
 * when it reaches zero the timer is <em>expired</em>. Loading zero turns the
 * timer off.
 * <p>
 * Layout, as byte offsets from the base address:
 * <ul>
 * <li>0 and 1: the countdown, one word, high byte first, read and write. A read
 * gives the instructions left; a write loads a new value.</li>
 * <li>2: status, read only. Bit 0 is set while the timer is expired.</li>
 * </ul>
 * The device only counts. Raising the timer trap (and making it wait until the
 * program is back in user mode and has run one instruction) is the job of the
 * machine, which asks {@link #isExpired()} after each step. As in the original
 * fancy machine, an expiry stays pending until the OS loads a non-zero value, so
 * loading zero does not cancel a trap that is already waiting.
 * <p>
 * The state is the loaded value, the number of ticks since it was loaded, and
 * whether an expiry is being carried over from before a load of zero. The value
 * a program reads is computed from these, so {@link #untick()} (one tick back)
 * and undoing a write in debug mode put everything back exactly.
 */
public class TimerDevice implements Device {
    public static final int SIZE = 3;
    public static final int COUNTDOWN = 0;
    public static final int STATUS = 2;

    private final Word base;
    private int loaded = 0;
    private long elapsed = 0;
    private boolean carried = false;

    /**
     * Construct a timer to be mounted at the given base address.
     * 
     * @param baseAddress
     */
    public TimerDevice(Word baseAddress) {
        this.base = baseAddress;
    }

    private int offset(Word address) {
        return (address.value() - base.value()) & 0xFFFF;
    }

    private int remaining() {
        return (int) Math.max(0, loaded - elapsed);
    }

    /** @return true if the timer has counted down to zero and the OS has not loaded a new value since */
    public boolean isExpired() {
        return carried || (loaded > 0 && elapsed >= loaded);
    }

    @Override
    public void tick() {
        elapsed++;
    }

    @Override
    public void untick() {
        if (elapsed > 0) {
            elapsed--;
        }
    }

    @Override
    public boolean canFetchB(Word address) {
        return false;
    }

    @Override
    public boolean canReadB(Word address) {
        return offset(address) <= STATUS;
    }

    @Override
    public boolean canWriteB(Word address) {
        return offset(address) < STATUS;
    }

    @Override
    public UByte fetchB(Word address) throws Fault {
        throw new Fault("Cannot execute from the timer");
    }

    @Override
    public UByte readB(Word address) throws Fault {
        int off = offset(address);
        if (off == COUNTDOWN) {
            return UByte.of(remaining() >> 8);
        } else if (off == COUNTDOWN + 1) {
            return UByte.of(remaining());
        } else if (off == STATUS) {
            return UByte.of(isExpired() ? 1 : 0);
        }
        throw new Fault("Nothing to read in the timer at offset " + off);
    }

    private record Reload(TimerDevice timer, int oldLoaded, long oldElapsed, boolean oldCarried, int newLoaded)
            implements Action {
        @Override
        public void undo(DebugState state) {
            timer.loaded = oldLoaded;
            timer.elapsed = oldElapsed;
            timer.carried = oldCarried;
        }

        @Override
        public String getTrace() {
            return "timer: %d->%d".formatted(oldLoaded, newLoaded);
        }
    }

    @Override
    public void writeB(Word address, UByte value) throws Fault {
        int off = offset(address);
        int current = remaining();
        int next;
        if (off == COUNTDOWN) {
            next = (value.value() << 8) | (current & 0xFF);
        } else if (off == COUNTDOWN + 1) {
            next = (current & 0xFF00) | value.value();
        } else {
            throw new Fault("Cannot write to the timer at offset " + off);
        }

        // A pending expiry survives loading zero, but not loading a real value
        carried = next == 0 && isExpired();
        loaded = next;
        elapsed = 0;
    }

    @Override
    public void writeB(Word address, UByte value, DebugState state) throws Fault {
        var oldLoaded = loaded;
        var oldElapsed = elapsed;
        var oldCarried = carried;
        writeB(address, value);
        state.record(new Reload(this, oldLoaded, oldElapsed, oldCarried, loaded));
    }
}
