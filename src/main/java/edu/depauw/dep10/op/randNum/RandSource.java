package edu.depauw.dep10.op.randNum;

public interface RandSource {
    int nextWord();               
    Object saveState();           
    void restoreState(Object s);
    boolean reversible();
}
