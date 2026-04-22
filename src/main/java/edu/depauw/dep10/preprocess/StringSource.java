package edu.depauw.dep10.preprocess;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

public class StringSource extends Source {
    private String string;
    private Reader reader;

    public StringSource(String string) {
        super("<string>", null, true);
        this.string = string;
        this.reader = null;
    }

    @Override
    public boolean ensureOpen() {
        if (reader == null) {
            reader = new StringReader(string);
            setIterator(Parser.parse(reader).iterator());
        }

        return true;
    }

    @Override
    public void close() {
        try {
            reader.close();
        } catch (IOException e) {
            // This should not happen
        }
    }
}