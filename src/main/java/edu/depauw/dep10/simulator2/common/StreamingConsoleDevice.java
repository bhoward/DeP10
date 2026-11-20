package edu.depauw.dep10.simulator2.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PushbackInputStream;

import edu.depauw.dep10.simulator2.Action;
import edu.depauw.dep10.simulator2.DebugState;
import edu.depauw.dep10.simulator2.Device;
import edu.depauw.dep10.simulator2.Fault;
import edu.depauw.dep10.simulator2.State;
import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

/**
 * An I/O device that connects to an InputStream and a PrintStream, such as
 * System.in and System.out. It uses a {@link PushbackInputStream} to provide
 * some ability to rewind input in debug mode, but it does not attempt to undo
 * output to the PrintStream.
 */
public class StreamingConsoleDevice implements Device {
    private Word base;
    private PushbackInputStream in;
    private PrintStream out;

    public StreamingConsoleDevice(Word baseAddress, InputStream in, PrintStream out) {
        this.base = baseAddress;
        this.in = new PushbackInputStream(in);
        this.out = out;
    }

    @Override
    public boolean canFetchB(Word address) {
        return false;
    }

    @Override
    public boolean canReadB(Word address) {
        return address.equals(base);
    }

    @Override
    public boolean canWriteB(Word address) {
        return address.equals(base.plus(1));
    }

    @Override
    public UByte fetchB(Word address) throws Fault {
        return null;
    }

    @Override
    public UByte readB(Word address) throws Fault {
        try {
            return UByte.of(in.read());
        } catch (IOException e) {
            throw new Fault(e.getMessage());
        }
    }

    private record ReadCharAction(StreamingConsoleDevice parent, UByte ch) implements Action {
        @Override
        public void undo(State state) {
            try {
                parent.in.unread(ch.value());
            } catch (IOException e) {
                // Silently ignore the failed attempt to unread...
            }
        }
    }

    @Override
    public UByte readB(Word address, DebugState state) throws Fault {
        var ch = readB(address);
        var action = new ReadCharAction(this, ch);
        state.record(action);
        return ch;
    }

    @Override
    public void writeB(Word address, UByte ch) throws Fault {
        out.print(ch.value());
    }

    @Override
    public void tick() {
        // Do nothing
    }
}
