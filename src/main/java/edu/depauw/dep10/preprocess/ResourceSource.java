package edu.depauw.dep10.preprocess;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import edu.depauw.dep10.Parser;
import edu.depauw.dep10.driver.ErrorLog;

public class ResourceSource extends Source {
    private String resource;
    private ErrorLog log;
    private Reader reader;

    public ResourceSource(String resource, ErrorLog log) {
        super(resource, null);
        this.resource = resource;
        this.log = log;
        this.reader = null;
    }

    @Override
    public boolean ensureOpen() {
        try {
            if (reader == null) {
                URL url = getClass().getResource(resource);
                if (url == null) {
                    log.error("Unable to open resource " + resource);
                    return false;
                }

                reader = new InputStreamReader(url.openStream());
                setIterator(Parser.parse(reader, false).iterator());
            }

            return true;
        } catch (IOException e) {
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