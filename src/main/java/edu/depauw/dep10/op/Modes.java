package edu.depauw.dep10.op;

public class Modes {
    public static Mode[] All = new Mode[] { Mode.I, Mode.D, Mode.N, Mode.S, Mode.SF, Mode.X, Mode.SX, Mode.SFX };
    public static Mode[] NotI = new Mode[] { Mode.D, Mode.N, Mode.S, Mode.SF, Mode.X, Mode.SX, Mode.SFX };
    public static Mode[] IX = new Mode[] { Mode.I, Mode.X1 };
}
