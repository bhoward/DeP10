;=======================================================================
; ddprint.pep -- print a 32-bit value in decimal. Flagged in the 10/1
; meeting: we have @DECO for 16-bit values but nothing for the 32-bit
; library. The Declan compiler uses DPRINTD for WriteLong.
; Hex printing is trivial (two @HEXO calls, high word then low word);
; this file covers the harder decimal case.
;
; Calling convention matches the rest of the double-word library: push
; the value low word, then high word, CALL, then the caller cleans up
; with ADDSP 4,i. Neither routine returns a result on the stack; both
; are pure output, like @DECO itself.
;
; UDPRINTD: unsigned decimal print of a 32-bit value.
; DPRINTD:  signed decimal print; prints a leading '-' for negative
;           values, then the magnitude via UDPRINTD.
;
; NOTE (concurrency caveat, same as ddiv32.pep): the digit buffer and
; working variables below are statically allocated, not stack-based.
; See the separate design note on this; not safe to call from code
; that might be interrupted mid-routine once context switching exists.
;=======================================================================

.INCLUDELIB "daddsub"
.INCLUDELIB "ddiv32"

udpValHi: .EQUATE 2      ; offset from SP at UDPRINTD entry: value hi word
udpValLo: .EQUATE 4      ; value lo word

UDPRINTD:
        LDWA    udpValHi,s
        ORA     udpValLo,s
        BRNE    udpNonzero
        LDBA    '0',i
        STBA    charOut,d
        RET

udpNonzero:
        LDWA    udpValHi,s
        STWA    udpCurHi,d
        LDWA    udpValLo,s
        STWA    udpCurLo,d
        LDWX    0,i
        STWX    udpCount,d

udpLoop:
        LDWA    udpCurHi,d
        ORA     udpCurLo,d
        BREQ    udpPrint

        ; push current value (low, high) and divisor 10 (low, high)
        LDWA    udpCurLo,d
        PUSHA
        LDWA    udpCurHi,d
        PUSHA
        LDWA    10,i
        PUSHA
        LDWA    0,i
        PUSHA
        CALL    UDDiv,i
        ; stack top-to-bottom after RET: remHi, remLo, quoHi, quoLo
        POPA
        STWA    udpRemHi,d
        POPA
        STWA    udpRemLo,d
        POPA
        STWA    udpCurHi,d
        POPA
        STWA    udpCurLo,d

        LDWX    udpCount,d
        LDWA    udpRemLo,d
        ADDA    '0',i
        STBA    udpDigits,x
        ADDX    1,i
        STWX    udpCount,d

        BR      udpLoop

udpPrint:
        LDWX    udpCount,d
        SUBX    1,i
udpPrintLoop:
        BRLT    udpDone
        LDBA    udpDigits,x
        STBA    charOut,d
        SUBX    1,i
        BR      udpPrintLoop
udpDone:
        RET

dpValHi: .EQUATE 2
dpValLo: .EQUATE 4

DPRINTD:
        ; Cache the operand into statics BEFORE any PUSHA below, since
        ; ",s" offsets are relative to the CURRENT SP: once we start
        ; pushing, dpValHi/dpValLo (offsets measured at entry) no
        ; longer point at the right place.
        LDWA    dpValHi,s
        STWA    dpSaveHi,d
        LDWA    dpValLo,s
        STWA    dpSaveLo,d

        LDWA    dpSaveHi,d
        BRLT    dpNegative
        ; non-negative: pass the cached operand straight to UDPRINTD
        ; (push low, then high, matching its calling convention)
        LDWA    dpSaveLo,d
        PUSHA
        LDWA    dpSaveHi,d
        PUSHA
        CALL    UDPRINTD,i
        ADDSP   4,i
        RET

dpNegative:
        LDBA    '-',i
        STBA    charOut,d
        ; compute 0 - value using the already-verified DSUB routine,
        ; which correctly handles the INT32_MIN edge case: negating
        ; 0x80000000 wraps back to 0x80000000, i.e. 2147483648
        ; unsigned, which is exactly the right magnitude to print.
        LDWA    0,i
        PUSHA
        LDWA    0,i
        PUSHA
        LDWA    dpSaveLo,d
        PUSHA
        LDWA    dpSaveHi,d
        PUSHA
        CALL    DSUB,i
        ADDSP   4,i
        ; magnitude (low, high) now on top of stack; just pass straight
        ; through to UDPRINTD using the same convention
        CALL    UDPRINTD,i
        ADDSP   4,i
        RET

; ---- static scratch (see concurrency caveat above) ----
udpCurHi:  .BLOCK 2
udpCurLo:  .BLOCK 2
udpRemHi:  .BLOCK 2
udpRemLo:  .BLOCK 2
udpCount:  .BLOCK 2
udpDigits: .BLOCK 10
dpSaveHi:  .BLOCK 2
dpSaveLo:  .BLOCK 2
