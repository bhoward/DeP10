package edu.depauw.dep10.preprocess;

import java.util.Iterator;

public class Source implements Iterator<Line> {
    private String sourceName;
    private Iterator<Line> it;
    private boolean visible;
    private int lineNumber;
    
    public Source(String sourceName, Iterator<Line> it, boolean visible) {
        this.sourceName = sourceName;
        this.it = it;
        this.visible = visible;
        this.lineNumber = 0;
    }
    
    protected void setIterator(Iterator<Line> it) {
        this.it = it;
    }
    
    /**
     * Return true if either this source has already been opened, or
     * if opening is successful. Override this for nontrivial cases.
     * 
     * @return
     */
    public boolean ensureOpen() {
        return true;
    }
    
    /**
     * Close this source. Override this for nontrivial cases.
     */
    public void close() {
    }

    public boolean hasNext() {
        if (ensureOpen()) {
            if (it.hasNext()) {
                return true;
            }
            
            close();
        }
        
        return false;
    }

    public Line next() {
        var line = it.next();
        lineNumber++;
        line.setLocation(sourceName, lineNumber);
        line.setVisible(visible);
        return line;
    }
    
    public boolean isVisible() {
        return visible;
    }
}
