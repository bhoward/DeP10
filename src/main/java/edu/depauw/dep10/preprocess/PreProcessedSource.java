package edu.depauw.dep10.preprocess;

import java.util.Iterator;
import java.util.List;

public class PreProcessedSource extends Source {
    private Iterator<Line> it;
    private Line current;
    
    public PreProcessedSource(List<Line> lines) {
        super(null, null, false);
        this.it = lines.iterator();
    }

    @Override
    public boolean hasNext() {
        return it.hasNext();
    }

    @Override
    public Line next() {
        current = it.next();
        return current;
    }

    @Override
    public boolean isVisible() {
        if (current != null) {
            return current.isVisible();
        } else {
            return false;
        }
    }
}
