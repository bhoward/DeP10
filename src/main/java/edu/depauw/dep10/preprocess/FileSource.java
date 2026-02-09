package edu.depauw.dep10.preprocess;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import edu.depauw.dep10.driver.ErrorLog;

public class FileSource extends Source {
    private String filename;
    private ErrorLog log;
    private Reader reader;

    public FileSource(String filename, ErrorLog log) {
        super(filename, null, true); // TODO allow it to be invisible?
        this.filename = filename;
        this.log = log;
        this.reader = null;
    }

    @Override
    public boolean ensureOpen() {
        try {
            if (reader == null) {
                reader = new FileReader(filename);
                setIterator(Parser.parse(reader).iterator());
            }

            return true;
        } catch (FileNotFoundException e) {
            log.error(e.getMessage());
            return false;
        }
    }

    @Override
    public void close() {
        try {
            reader.close();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}