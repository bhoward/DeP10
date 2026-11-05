package edu.depauw.dep10.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Audit of every .pep file under demo/: the "How to run" comment at the top must say which operating
 * system the file needs and whether it needs the fancy machine, and the file must match what it says:
 * <ul>
 * <li>full OS: ends with RET, never writes to pwrOff;</li>
 * <li>bare metal: stops by writing to pwrOff, uses the header's charOut/pwrOff (does not define them);</li>
 * <li>none (System Code): ends with .INCLUDELIB "bmos" (which defines charIn/charOut/pwrOff), stops by
 * writing to pwrOff, and does not define the ports itself.</li>
 * </ul>
 * The exercises that need the fancy machine (user mode, timer, protection, translation) are listed in
 * FANCY. A starter may leave the register stores to the student, so this cannot be read off the code.
 */
class DemoHeadersTest {
    private static final List<String> FANCY = List.of("mmu", "vm", "vm2", "race");

    private static List<Path> demos() throws IOException {
        try (Stream<Path> s = Files.walk(Path.of("demo"))) {
            return s.filter(p -> p.toString().endsWith(".pep")).sorted().collect(Collectors.toList());
        }
    }

    /** The code of the file with comments removed. */
    private static String code(String text) {
        return text.lines().map(l -> l.replaceFirst(";.*", "")).collect(Collectors.joining("\n"));
    }

    private static String header(String text) {
        return text.lines().filter(l -> l.startsWith(";"))
                .collect(Collectors.joining("\n"));
    }

    @Test
    @DisplayName("Every demo .pep file says which OS it needs and whether it needs the fancy machine, and is right")
    void headersMatchFiles() throws IOException {
        var demos = demos();
        assertTrue(demos.size() >= 13, "found only " + demos.size() + " demo programs");
        for (Path p : demos) {
            String text = Files.readString(p);
            String c = code(text);
            String h = header(text);
            String name = p.toString();

            boolean fancy = FANCY.contains(p.getParent().getFileName().toString());
            if (fancy) {
                assertTrue(h.contains("Fancy:   REQUIRED"), name + " is a fancy machine exercise, so its header must say the fancy machine is required");
            } else {
                assertTrue(h.contains("Fancy:   not needed"), name + " must say the fancy machine is not needed");
            }

            boolean none = h.contains("OS:      none");
            boolean bare = h.contains("OS:      Pep/10 bare metal");
            boolean full = h.contains("OS:      Pep/10 full operating system");
            assertEquals(1, (none ? 1 : 0) + (bare ? 1 : 0) + (full ? 1 : 0), name + " must name exactly one OS in its header");

            boolean writesPwrOff = Pattern.compile("STBA\\s+pwrOff").matcher(c).find();
            boolean definesPorts = Pattern.compile("(?m)^\\s*(charOut|pwrOff|charIn)\\s*:\\s*\\.EQUATE").matcher(c).find();
            boolean lib = c.contains(".INCLUDELIB \"bmos\"");
            if (none) {
                assertTrue(lib, name + " (no OS) must .INCLUDELIB \"bmos\"");
                assertTrue(writesPwrOff, name + " (no OS) must stop by writing to pwrOff");
                assertFalse(definesPorts, name + " must not define the ports itself; bmos does");
            } else if (bare) {
                assertFalse(lib, name + " (bare metal) must not also include bmos");
                assertTrue(writesPwrOff, name + " (bare metal) must stop by writing to pwrOff");
                assertFalse(definesPorts, name + " (bare metal) gets charOut and pwrOff from the header");
            } else {
                assertFalse(lib, name + " (full OS) must not include bmos");
                assertFalse(writesPwrOff, name + " (full OS) must end with RET, not pwrOff");
                String lastOp = c.lines().map(String::strip).filter(l -> !l.isEmpty() && !l.startsWith("."))
                        .reduce((a, b) -> b).orElse("");
                assertTrue(lastOp.matches("(?s).*\\bRET\\b.*"), name + " (full OS) must end with RET, but ends with: " + lastOp);
            }
        }
    }
}
