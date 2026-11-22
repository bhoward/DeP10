package edu.depauw.dep10.simulator2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.depauw.dep10.simulator2.pep10.Pep10DebugState;
import edu.depauw.dep10.simulator2.pep10.Pep10Machine;
import edu.depauw.dep10.simulator2.pep10.Pep10State;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

/**
 * Tests that devices are mounted in 16-byte slices of the I/O page, and that
 * only addresses in a device's slice are ever offered to it.
 */
public class IoPageSlicesTest {
    /** A device that answers every read in its own slice with a fixed byte, and counts the questions. */
    private static class Probe implements Device {
        int asked = 0;
        int ticks = 0;
        int unticks = 0;
        private final Word base;
        private final int value;

        Probe(Word base, int value) {
            this.base = base;
            this.value = value;
        }

        @Override
        public boolean canReadB(Word address) {
            asked++;
            return address.value() >= base.value() && address.value() < base.value() + 16;
        }

        @Override
        public boolean canFetchB(Word address) {
            return false;
        }

        @Override
        public boolean canWriteB(Word address) {
            return false;
        }

        @Override
        public UByte readB(Word address) throws Fault {
            return UByte.of(value);
        }

        @Override
        public UByte fetchB(Word address) throws Fault {
            return null;
        }

        @Override
        public void writeB(Word address, UByte value) throws Fault {
        }

        @Override
        public void tick() {
            ticks++;
        }

        @Override
        public void untick() {
            unticks++;
        }
    }

    private static void zeroMemory(MemoryDevice memory) {
        for (int i = 0; i < memory.size(); i++) {
            memory.loadB(Word.of(i), UByte.of(0));
        }
    }

    private Pep10State state;

    @BeforeEach
    public void setUp() {
        state = new Pep10Machine().init(IoPageSlicesTest::zeroMemory);
    }

    @Test
    public void deviceAnswersInsideItsSlice() throws Fault {
        state.mount(Word.of(0xFF20), new Probe(Word.of(0xFF20), 0x5A));
        assertEquals(0x5A, state.readB(Word.of(0xFF20)).value());
        assertEquals(0x5A, state.readB(Word.of(0xFF2F)).value());
        assertEquals(0, state.readB(Word.of(0xFF1F)).value());
        assertEquals(0, state.readB(Word.of(0xFF30)).value());
    }

    @Test
    public void mountAddressNeedNotBeTheFirstInTheSlice() throws Fault {
        state.mount(Word.of(0xFF27), new Probe(Word.of(0xFF20), 0x77));
        assertEquals(0x77, state.readB(Word.of(0xFF20)).value());
    }

    @Test
    public void deviceIsNotAskedAboutOtherSlicesOrOtherPages() throws Fault {
        var probe = new Probe(Word.of(0xFF20), 1);
        state.mount(Word.of(0xFF20), probe);
        state.readB(Word.of(0x1234));
        state.readB(Word.of(0xFE20));
        state.readB(Word.of(0xFF10));
        state.readB(Word.of(0xFF30));
        assertEquals(0, probe.asked);
        state.readB(Word.of(0xFF25));
        assertEquals(1, probe.asked);
    }

    @Test
    public void devicesSharingASliceAreAllConsulted() throws Fault {
        state.mount(Word.of(0xFF40), new Probe(Word.of(0xFF48), 0x11)); // answers only 0xFF48 to 0xFF4F
        state.mount(Word.of(0xFF40), new Probe(Word.of(0xFF40), 0x22)); // answers 0xFF40 to 0xFF4F
        assertEquals(0x22, state.readB(Word.of(0xFF40)).value());
        assertEquals(0x11, state.readB(Word.of(0xFF48)).value()); // the first one mounted wins
    }

    @Test
    public void mountingOutsideTheIoPageIsRefused() {
        var probe = new Probe(Word.of(0x1000), 1);
        assertThrows(IllegalArgumentException.class, () -> state.mount(Word.of(0x1000), probe));
        assertThrows(IllegalArgumentException.class, () -> state.mount(Word.of(0xFEFF), probe));
    }

    @Test
    public void everyMountedDeviceIsTicked() {
        var a = new Probe(Word.of(0xFF20), 1);
        var b = new Probe(Word.of(0xFF50), 2);
        state.mount(Word.of(0xFF20), a);
        state.mount(Word.of(0xFF50), b);
        state.tick();
        state.tick();
        assertEquals(2, a.ticks);
        assertEquals(2, b.ticks);
    }

    @Test
    public void debugStateUntickReachesDevicesInSlices() {
        var debug = new Pep10Machine().initDebug(IoPageSlicesTest::zeroMemory);
        var probe = new Probe(Word.of(0xFF60), 3);
        debug.mount(Word.of(0xFF60), probe);
        debug.tick();
        debug.untick();
        assertEquals(1, probe.ticks);
        assertEquals(1, probe.unticks);
    }

    @Test
    public void clockIsMountedAtFF00InThePep10Machine() throws Fault {
        Pep10DebugState debug = new Pep10Machine().initDebug(IoPageSlicesTest::zeroMemory);
        for (int i = 0; i < 7; i++) {
            debug.tick();
        }
        assertEquals(7, debug.readW(Pep10Machine.CLOCK.plus(6)).value());
        debug.untick();
        assertEquals(6, debug.readW(Pep10Machine.CLOCK.plus(6)).value());
    }
}
