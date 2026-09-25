;=======================================================================
; DADD / DSUB -- 32-bit + 32-bit -> 32-bit, and 32-bit - 32-bit -> 32-bit,
; matching Java/C++ int semantics: silently wrap/truncate on overflow,
; no trap. Same stack calling convention as DDiv/UDDiv (PR #12) and DMul:
; for each 32-bit operand, push low word first, then high word.
;
; DeP10 has no ADC/SBC (add/subtract-with-carry) instruction, so the
; high-word add/subtract is done twice -- once assuming no carry/borrow
; from the low word, once assuming there was one -- and BRC picks which
; result to keep. This avoids needing any scratch memory.
;
; Stack layout at entry (SP,0 is the return address):
;   SP,2  a high word      SP,6  b high word
;   SP,4  a low word       SP,8  b low word
;
; DADD computes a + b, DSUB computes a - b. The result (32-bit) overwrites
; a's slot in place:
;   SP,2  result high word
;   SP,4  result low word
; b's slot (SP,6 / SP,8) is left untouched; the caller discards those two
; words (e.g. with ADDSP 4,i after popping the result) since only one
; 32-bit result comes out of two 32-bit operands.
;
; Caller looks like:
;   LDWA  bLow,i  / PUSHA
;   LDWA  bHigh,i / PUSHA
;   LDWA  aLow,i  / PUSHA
;   LDWA  aHigh,i / PUSHA
;   CALL  DADD,i           ; or DSUB,i
;   POPA
;   STWA  resultHigh,d
;   POPA
;   STWA  resultLow,d
;   ADDSP 4,i               ; discard b's now-unused slot
;=======================================================================

DADD:
        LDWA    4,s             ; a low
        ADDA    8,s             ; + b low; C = carry out of the low word
        STWA    4,s             ; result low
        BRC     _DAddCarryIn    ; C=1: high word must add one extra for the carry
        LDWA    2,s             ; a high
        ADDA    6,s             ; + b high (no carry-in)
        STWA    2,s             ; result high
        RET
_DAddCarryIn:
        LDWA    2,s             ; a high
        ADDA    6,s             ; + b high
        ADDA    1,i             ; + carry-in from the low word
        STWA    2,s             ; result high
        RET

DSUB:
        LDWA    4,s             ; a low
        SUBA    8,s             ; - b low; C=1 means "no borrow needed" (Issue #11 fix),
        STWA    4,s             ; result low       C=0 means a borrow was needed
        BRC     _DSubNoBorrow   ; C=1: high word subtracts normally, no adjustment
        LDWA    2,s             ; a high
        SUBA    6,s             ; - b high
        SUBA    1,i             ; - the borrow from the low word
        STWA    2,s             ; result high
        RET
_DSubNoBorrow:
        LDWA    2,s             ; a high
        SUBA    6,s             ; - b high (no borrow-in)
        STWA    2,s             ; result high
        RET
