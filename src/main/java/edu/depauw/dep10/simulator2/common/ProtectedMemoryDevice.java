package edu.depauw.dep10.simulator2.common;

import edu.depauw.dep10.simulator2.DebugState;
import edu.depauw.dep10.simulator2.Fault;
import edu.depauw.dep10.simulator2.MemoryDevice;
import edu.depauw.dep10.simulator2.Permissions;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

/**
 * An overlay on another {@link MemoryDevice} that checks r, w and x permissions
 * on ranges of addresses. It is a baby version of the MMU: it only monitors
 * permissions, and does no translation. Its first use is to make the Pep/10
 * operating system look like it is in ROM: the system code is protected as "rx"
 * (so it cannot be overwritten), and a few locations, such as the initial stack
 * pointer, are protected as "rw".
 * 
 * Addresses that have not been protected allow every kind of access. Loading an
 * image goes through {@link #loadB(Word, UByte)}, which never checks
 * permissions, so the protected ranges can still be filled in before the machine
 * runs. A forbidden access raises a {@link Fault}.
 */
public class ProtectedMemoryDevice implements MemoryDevice {
    private final MemoryDevice inner;
    private final Permissions[] permissions;

    /**
     * Construct an overlay on the given memory. At first every address allows
     * every kind of access.
     * 
     * @param inner
     */
    public ProtectedMemoryDevice(MemoryDevice inner) {
        this.inner = inner;
        this.permissions = new Permissions[inner.size()];
        java.util.Arrays.fill(permissions, Permissions.ALL);
    }

    /**
     * Set the permissions of a range of addresses, replacing any earlier setting
     * for those addresses.
     * 
     * @param start  the first address in the range
     * @param length the number of bytes in the range
     * @param what   what is allowed in the range
     * @throws IllegalArgumentException if the range does not fit in the memory
     */
    @Override
    public void protect(Word start, int length, Permissions what) {
        int first = start.value();
        if (length < 0 || first + length > permissions.length) {
            throw new IllegalArgumentException(
                    "Range %04X, length %d does not fit in %d bytes of memory".formatted(first, length, permissions.length));
        }
        java.util.Arrays.fill(permissions, first, first + length, what);
    }

    /**
     * @param address
     * @return what is allowed at the given address
     */
    public Permissions permissionsAt(Word address) {
        return permissions[address.value()];
    }

    @Override
    public boolean canFetchB(Word address) {
        return permissionsAt(address).execute();
    }

    @Override
    public boolean canReadB(Word address) {
        return permissionsAt(address).read();
    }

    @Override
    public boolean canWriteB(Word address) {
        return permissionsAt(address).write();
    }

    private static Fault denied(String what, Word address) {
        return new Fault("%s of protected memory at %04X".formatted(what, address.value()));
    }

    @Override
    public UByte fetchB(Word address) throws Fault {
        if (!canFetchB(address)) {
            throw denied("Execute", address);
        }
        return inner.fetchB(address);
    }

    @Override
    public UByte readB(Word address) throws Fault {
        if (!canReadB(address)) {
            throw denied("Read", address);
        }
        return inner.readB(address);
    }

    @Override
    public void writeB(Word address, UByte value) throws Fault {
        if (!canWriteB(address)) {
            throw denied("Write", address);
        }
        inner.writeB(address, value);
    }

    @Override
    public int size() {
        return inner.size();
    }

    @Override
    public void loadB(Word address, UByte value) {
        inner.loadB(address, value);
    }

    @Override
    public void tick() {
        inner.tick();
    }

    // These are for use in debug mode

    @Override
    public void untick() {
        inner.untick();
    }

    @Override
    public UByte fetchB(Word address, DebugState state) throws Fault {
        if (!canFetchB(address)) {
            throw denied("Execute", address);
        }
        return inner.fetchB(address, state);
    }

    @Override
    public UByte readB(Word address, DebugState state) throws Fault {
        if (!canReadB(address)) {
            throw denied("Read", address);
        }
        return inner.readB(address, state);
    }

    @Override
    public void writeB(Word address, UByte value, DebugState state) throws Fault {
        if (!canWriteB(address)) {
            throw denied("Write", address);
        }
        inner.writeB(address, value, state);
    }

    @Override
    public byte[] dumpBytes() {
        return inner.dumpBytes();
    }
}
