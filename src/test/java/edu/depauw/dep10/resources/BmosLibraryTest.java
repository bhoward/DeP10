package edu.depauw.dep10.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The bmos library: a program with no OS that includes it can print and power off. */
class BmosLibraryTest {
    private static final String PROGRAM = """
            main:     LDWA    stackTop,i
                      MOVASP
                      LDWX    hello,i
                      CALL    puts,i
                      LDWA    0xBEEF,i
                      CALL    puthex,i
                      LDWA    0x0A05,i
                      CALL    puthex,i
                      LDBA    '\\n',i
                      STBA    charOut,d
                      LDBA    0,i
                      STBA    pwrOff,d
            hello:    .ASCII  "hex: \\0"
                      .BLOCK  64
            stackTop: .BLOCK  0
                      .INCLUDELIB "bmos"
                      .END
            """;

    @Test
    @DisplayName("puts, puthex, charOut and pwrOff work as System Code with no OS image")
    void printsAndPowersOff() {
        var s = PepHarness.loadSystem(PepHarness.assembleSystem(PROGRAM), null);
        var out = new java.io.ByteArrayOutputStream();
        s.setOutput(out);
        new edu.depauw.dep10.simulator.Simulator(s).run(new edu.depauw.dep10.simulator.StepCountController(
                new edu.depauw.dep10.simulator.PlainController(), 10_000));
        assertFalse(s.isRunning(), "machine should have powered off");
        assertEquals("hex: BEEF0A05\n", out.toString(java.nio.charset.StandardCharsets.US_ASCII));
    }
}
