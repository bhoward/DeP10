package edu.depauw.dep10.simulator2;

public class Fault extends Exception {
    private static final long serialVersionUID = -8917648242053214980L;

    public Fault(String message) {
        super(message);
    }
}
