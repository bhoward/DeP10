package edu.depauw.dep10.op;

import edu.depauw.dep10.simulator.State;

public class Dep10PushPop {
    public static final Table table = new Table();

    public static final Operation PUSHX = new Operation.Unary("PUSHX") {
        public void exec(State s) {
            s.setSP(s.getSP().plus(-2));
            s.setMem2(s.getSP(), s.getX());
        }
    };

    public static final Operation PUSHA = new Operation.Unary("PUSHA") {
        public void exec(State s) {
            s.setSP(s.getSP().plus(-2));
            s.setMem2(s.getSP(), s.getA());
        }
    };

    public static final Operation POPX = new Operation.Unary("POPX") {
        public void exec(State s) {
            s.setX(s.mem2(s.getSP()));
            s.setSP(s.getSP().plus(2));
        }
    };

    public static final Operation POPA = new Operation.Unary("POPA") {
        public void exec(State s) {
            s.setA(s.mem2(s.getSP()));
            s.setSP(s.getSP().plus(2));
        }
    };
    
    public static final Operation SWAPAX = new Operation.Unary("SWAPAX") {
        public void exec(State s) {
            var a = s.getA();
            var x = s.getX();
            s.setA(x);
            s.setX(a);
        }
    };
    
    public static final Operation SWAPHA = new Operation.Unary("SWAPHA") {
        public void exec(State s) {
            var h = s.getH();
            var a = s.getA();
            s.setH(a);
            s.setA(h);
        }
    };
    
    public static final Operation SWAPHX = new Operation.Unary("SWAPHX") {
        public void exec(State s) {
            var h = s.getH();
            var x = s.getX();
            s.setH(x);
            s.setX(h);
        }
    };
    
    static {
        table.install(1, SWAPAX);
        table.install(2, SWAPHA);
        table.install(3, SWAPHX);
        table.install(4, PUSHA);
        table.install(5, PUSHX);
        table.install(6, POPA);
        table.install(7, POPX);
    }
}
