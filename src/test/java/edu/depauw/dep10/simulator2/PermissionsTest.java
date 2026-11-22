package edu.depauw.dep10.simulator2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class PermissionsTest {
    @Test
    public void parseReadsRwxInAnyOrderAndCase() {
        var p = Permissions.parse("XwR");
        assertTrue(p.read());
        assertTrue(p.write());
        assertTrue(p.execute());
        assertEquals(Permissions.parse("rwx"), p);
    }

    @Test
    public void parseIgnoresOtherLetters() {
        assertEquals(Permissions.parse("rw"), Permissions.parse("zrwz"));
        assertEquals(Permissions.NONE, Permissions.parse("z"));
        assertEquals(Permissions.NONE, Permissions.parse(""));
        assertEquals(Permissions.NONE, Permissions.parse(null));
    }

    @Test
    public void protectionBitsMatchTheTableFormat() {
        assertEquals(0, Permissions.parse("").protectionBits()); // no access
        assertEquals(2, Permissions.parse("r").protectionBits()); // read only
        assertEquals(3, Permissions.parse("rw").protectionBits()); // read and write
        assertEquals(3, Permissions.parse("w").protectionBits());
        assertEquals(1, Permissions.parse("rx").protectionBits()); // read and execute
        assertEquals(1, Permissions.parse("rwx").protectionBits()); // code wins over write
        assertEquals(1, Permissions.parse("x").protectionBits());
    }

    @Test
    public void allAllowsEverything() {
        assertTrue(Permissions.ALL.read() && Permissions.ALL.write() && Permissions.ALL.execute());
        assertFalse(Permissions.NONE.read() || Permissions.NONE.write() || Permissions.NONE.execute());
    }

    @Test
    public void toStringShowsTheThreeFlags() {
        assertEquals("rw-", Permissions.parse("rw").toString());
        assertEquals("r-x", Permissions.parse("rx").toString());
        assertEquals("---", Permissions.NONE.toString());
    }
}
