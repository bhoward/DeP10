package edu.depauw.dep10;

import java.util.HashMap;
import java.util.Map;

import edu.depauw.dep10.util.Pair;
import edu.depauw.dep10.util.UByte;

public class OpTable {
	private OpTableEntry[] ops;
	private Map<Pair<String, String>, Integer> codes;
	
	public OpTable() {
		this.ops = new OpTableEntry[256];
		this.codes = new HashMap<>();
	}
	
	public OpTableEntry get(UByte n) {
		return ops[n.value()];
	}
	
	public ModeOperation get(int n) {
	    if (n < 256) {
	        return (ModeOperation) ops[n];
	    } else {
	        int prefix = n >> 8;
	        var entry = (PrefixEntry) ops[prefix];
	        return (ModeOperation) entry.get(UByte.of(n));
	    }
	}
	
	public int lookup(String mnemonic, String mode) {
		return codes.getOrDefault(new Pair<>(mnemonic.toLowerCase(), mode.toLowerCase()), -1);
	}
	
	public void addUnused(int code, int n) {
		for (int i = 0; i < n; i++) {
			ops[code + i] = null;
		}
	}
	
	public void add(int code, ModeOperation mop) {
		ops[code] = mop;
		String mnemonic = mop.op().mnemonic().toLowerCase();
		String modeString = (mop.mode() != null) ? mop.mode().modeString().toLowerCase() : "";
		codes.put(new Pair<>(mnemonic, modeString), code);
	}
	
	public void add(int code, PrefixEntry prefix) {
	    ops[code] = prefix;
	    for (var entry : prefix.table().codes.entrySet()) {
	        codes.put(entry.getKey(), (code << 8) + entry.getValue());
	    }
	}
}
