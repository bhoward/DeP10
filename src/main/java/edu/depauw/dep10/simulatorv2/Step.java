package edu.depauw.dep10.simulatorv2;

public interface Step {
    /**
     * Commit to all of the side-effecting actions of this step.
     */
    void commit();
    
    /**
     * Cancel any side-effecting actions of this step.
     */
    void rollback();
    
    /**
     * Reverse the effect of any actions of this step.
     */
    void undo();
}
