package edu.depauw.dep10.op;

import edu.depauw.dep10.simulator.State;
import edu.depauw.dep10.util.Word;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Dep10PushPop (PUSHA/PUSHX/POPA/POPX/SWAPAX/SWAPHA/SWAPHX), merged into
 * main as part of the h_register/0.9.0 work. These instructions previously had no
 * test coverage at all.
 */
public class PushPopTest {

    @Nested
    @DisplayName("PUSHA / PUSHX")
    class Push {

        @Test
        @DisplayName("PUSHA decrements SP by 2 and stores A at the new SP")
        void pusha() {
            State s = new State();
            s.setA(Word.of(0x1234));
            s.setSP(Word.of(0x1000));

            Dep10PushPop.PUSHA.exec(s);

            assertEquals(0x0FFE, s.getSP().value());
            assertEquals(0x1234, s.mem2(s.getSP()).value());
        }

        @Test
        @DisplayName("PUSHX decrements SP by 2 and stores X at the new SP")
        void pushx() {
            State s = new State();
            s.setX(Word.of(0x5678));
            s.setSP(Word.of(0x1000));

            Dep10PushPop.PUSHX.exec(s);

            assertEquals(0x0FFE, s.getSP().value());
            assertEquals(0x5678, s.mem2(s.getSP()).value());
        }
    }

    @Nested
    @DisplayName("POPA / POPX")
    class Pop {

        @Test
        @DisplayName("POPA loads A from the current SP and increments SP by 2")
        void popa() {
            State s = new State();
            s.setSP(Word.of(0x0FFE));
            s.setMem2(Word.of(0x0FFE), Word.of(0xABCD));

            Dep10PushPop.POPA.exec(s);

            assertEquals(0xABCD, s.getA().value());
            assertEquals(0x1000, s.getSP().value());
        }

        @Test
        @DisplayName("POPX loads X from the current SP and increments SP by 2")
        void popx() {
            State s = new State();
            s.setSP(Word.of(0x0FFE));
            s.setMem2(Word.of(0x0FFE), Word.of(0xCAFE));

            Dep10PushPop.POPX.exec(s);

            assertEquals(0xCAFE, s.getX().value());
            assertEquals(0x1000, s.getSP().value());
        }

        @Test
        @DisplayName("PUSHA followed by POPA round-trips A through the stack")
        void pushThenPopRoundTrips() {
            State s = new State();
            s.setA(Word.of(0x4242));
            s.setSP(Word.of(0x1000));

            Dep10PushPop.PUSHA.exec(s);
            s.setA(Word.of(0)); // clobber A to prove POPA actually restores it
            Dep10PushPop.POPA.exec(s);

            assertEquals(0x4242, s.getA().value());
            assertEquals(0x1000, s.getSP().value(), "SP should be back where it started");
        }
    }

    @Nested
    @DisplayName("SWAPAX / SWAPHA / SWAPHX")
    class Swap {

        @Test
        @DisplayName("SWAPAX exchanges A and X")
        void swapax() {
            State s = new State();
            s.setA(Word.of(0x1111));
            s.setX(Word.of(0x2222));

            Dep10PushPop.SWAPAX.exec(s);

            assertEquals(0x2222, s.getA().value());
            assertEquals(0x1111, s.getX().value());
        }

        @Test
        @DisplayName("SWAPHA exchanges H and A, leaving X untouched")
        void swapha() {
            State s = new State();
            s.setA(Word.of(0x1111));
            s.setX(Word.of(0x9999));
            s.setH(Word.of(0x3333));

            Dep10PushPop.SWAPHA.exec(s);

            assertEquals(0x3333, s.getA().value());
            assertEquals(0x1111, s.getH().value());
            assertEquals(0x9999, s.getX().value());
        }

        @Test
        @DisplayName("SWAPHX exchanges H and X, leaving A untouched")
        void swaphx() {
            State s = new State();
            s.setX(Word.of(0x2222));
            s.setA(Word.of(0x9999));
            s.setH(Word.of(0x4444));

            Dep10PushPop.SWAPHX.exec(s);

            assertEquals(0x4444, s.getX().value());
            assertEquals(0x2222, s.getH().value());
            assertEquals(0x9999, s.getA().value());
        }

        @Test
        @DisplayName("SWAPHA is its own inverse")
        void swaphaIsInvolution() {
            State s = new State();
            s.setA(Word.of(0x1111));
            s.setH(Word.of(0x3333));

            Dep10PushPop.SWAPHA.exec(s);
            Dep10PushPop.SWAPHA.exec(s);

            assertEquals(0x1111, s.getA().value());
            assertEquals(0x3333, s.getH().value());
        }

        @Test
        @DisplayName("SWAPHA then SWAPHX moves A's old value through H into X (documents the shared-H convention)")
        void swapChainDocumentsSharedHConvention() {
            // This is the "finish one before starting the other" pattern Brian described:
            // move A's value into H, then move H's value into X.
            State s = new State();
            s.setA(Word.of(0xAAAA));
            s.setX(Word.of(0xBBBB));
            s.setH(Word.of(0));

            Dep10PushPop.SWAPHA.exec(s); // H<->A: H=0xAAAA, A=0
            Dep10PushPop.SWAPHX.exec(s); // H<->X: H=0xBBBB, X=0xAAAA

            assertEquals(0xAAAA, s.getX().value());
            assertEquals(0xBBBB, s.getH().value());
            assertEquals(0, s.getA().value());
        }
    }
}
