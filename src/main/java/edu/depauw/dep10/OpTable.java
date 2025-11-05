package edu.depauw.dep10;

import java.util.HashMap;
import java.util.Map;

public class OpTable {
	private ModeOperation[] ops;
	private Map<Pair<String, String>, Integer> codes;
	
	public OpTable() {
		this.ops = new ModeOperation[256];
		this.codes = new HashMap<>();
		
		addUnused(0, 1);
		Operation.RET.install(this, 1);
		Operation.SRET.install(this, 2);
		Operation.MOVFLGA.install(this, 3);
		Operation.MOVAFLG.install(this, 4);
		Operation.MOVSPA.install(this, 5);
		Operation.MOVASP.install(this, 6);
		Operation.NOP.install(this, 7);
		
		addUnused(8, 16);
		
		Operation.NEGA.install(this, 24);
		Operation.NEGX.install(this, 25);
		Operation.ASLA.install(this, 26);
		Operation.ASLX.install(this, 27);
		Operation.ASRA.install(this, 28);
		Operation.ASRX.install(this, 29);
		Operation.NOTA.install(this, 30);
		Operation.NOTX.install(this, 31);
		Operation.ROLA.install(this, 32);
		Operation.ROLX.install(this, 33);
		Operation.RORA.install(this, 34);
		Operation.RORX.install(this, 35);
		
		Operation.BR.install(this, 36);
		Operation.BRLE.install(this, 38);
		Operation.BRLT.install(this, 40);
		Operation.BREQ.install(this, 42);
		Operation.BRNE.install(this, 44);
		Operation.BRGE.install(this, 46);
		Operation.BRGT.install(this, 48);
		Operation.BRV.install(this, 50);
		Operation.BRC.install(this, 52);
		Operation.CALL.install(this, 54);

		Operation.SCALL.install(this, 56);
		Operation.ADDSP.install(this, 64);
		Operation.SUBSP.install(this, 72);
		
		Operation.ADDA.install(this, 80);
		Operation.ADDX.install(this, 88);
		Operation.SUBA.install(this, 96);
		Operation.SUBX.install(this, 104);
		Operation.ANDA.install(this, 112);
		Operation.ANDX.install(this, 120);
		Operation.ORA.install(this, 128);
		Operation.ORX.install(this, 136);
		Operation.XORA.install(this, 144);
		Operation.XORX.install(this, 152);
		Operation.CPWA.install(this, 160);
		Operation.CPWX.install(this, 168);
		Operation.CPBA.install(this, 176);
		Operation.CPBX.install(this, 184);
		Operation.LDWA.install(this, 192);
		Operation.LDWX.install(this, 200);
		Operation.LDBA.install(this, 208);
		Operation.LDBX.install(this, 216);
		Operation.STWA.install(this, 224);
		Operation.STWX.install(this, 232);
		Operation.STBA.install(this, 240);
		Operation.STBX.install(this, 248);
	}
	
	public ModeOperation get(UByte n) {
		return ops[n.value()];
	}
	
	public ModeOperation get(int n) {
		return ops[n];
	}
	
	public int lookup(String mnemonic, String mode) {
		return codes.getOrDefault(new Pair<>(mnemonic.toLowerCase(), mode.toLowerCase()), 0);
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
}
