package edu.depauw.dep10.preprocess;

import java.util.Iterator;

public class Source implements Iterator<Line> {
    // TODO add source name and line number tracking
    private Iterator<Line> it;
    
    public Source(Iterator<Line> it) {
        this.it = it;
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
        return it.next();
    }
}
