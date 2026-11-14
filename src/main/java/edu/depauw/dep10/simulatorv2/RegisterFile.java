package edu.depauw.dep10.simulatorv2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.depauw.dep10.util.UByte;
import edu.depauw.dep10.util.Word;

public class RegisterFile {
    public record BReg(int id) {}
    
    public record WReg(int id) {}
    
    public record FReg(BReg bReg, int position) {}
    
    public record RFInfo(List<BReg> bregs, List<WReg> wregs, List<FReg> fregs) {}
    
    private Map<BReg, UByte> bReg;
    private Map<WReg, Word> wReg;
    
    public RegisterFile(RFInfo info) {
        this.bReg = new HashMap<>();
        this.wReg = new HashMap<>();
        
        for (var reg : info.bregs) {
            bReg.put(reg, UByte.of(0));
        }
        
        for (var reg : info.wregs) {
            wReg.put(reg, Word.of(0));
        }
    }
    
    public UByte get(BReg reg) {
        return bReg.get(reg);
    }
    
    public Word get(WReg reg) {
        return wReg.get(reg);
    }
    
    public boolean get(FReg reg) {
        var flags = bReg.get(reg.bReg());
        return flags.bit(reg.position());
    }
    
    public void set(BReg reg, UByte b) {
        bReg.put(reg, b);
    }
    
    public void set(WReg reg, Word w) {
        wReg.put(reg, w);
    }
    
    public void set(FReg reg, boolean b) {
        var flags = bReg.get(reg.bReg());
        bReg.put(reg.bReg(), flags.withBit(reg.position(), b));
    }
}
