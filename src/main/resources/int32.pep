; int32.pep -- the 32-bit (double word) integer library: DADD, DSUB, DMul,
; DDiv/UDDiv, DPRINTD/UDPRINTD and DREAD in one file. Do
;
;     .INCLUDELIB "int32"
;
; after the halt sequence to load all of them. The stack macros are in
; stack.pep and must be included BEFORE their first use.
;
; NOTE: the digit buffer and working variables of the print and read
; routines are statically allocated, not stack-based, so they are not safe
; to call from code that might be interrupted mid-routine once context
; switching exists.

;======================================================================
; ---- from daddsub.pep ----
;======================================================================
;=======================================================================
; DADD / DSUB -- 32-bit + 32-bit -> 32-bit, and 32-bit - 32-bit -> 32-bit,
; matching Java/C++ int semantics: silently wrap/truncate on overflow,
; no trap.
;
; v2 CONVENTION CHANGE (per Brian, 9/30): the result now overwrites the
; FIRST-PUSHED operand's slot (not the last-pushed one), so that a plain
; ADDSP 4,i right after CALL discards the correct leftover and chains
; cleanly:
;   PUSH A
;   PUSH B
;   CALL DADD      ; computes A + B, result replaces A's slot
;   ADDSP 4,i       ; discards B's now-unused slot
;   PUSH C
;   CALL DADD      ; computes (A+B) + C
;   ADDSP 4,i
;   POPA / POPA    ; final result
;
; For DSUB, push order is left-to-right operand order: PUSH A; PUSH B;
; CALL DSUB computes A - B (not B - A), matching how the result chains.
;
; Stack layout at entry (SP,0 is the return address):
;   SP,2  b high word (last-pushed)     SP,6  a high word (first-pushed)
;   SP,4  b low word                    SP,8  a low word
;
; Result overwrites a's slot (the first-pushed one):
;   SP,6  result high word
;   SP,8  result low word
; b's slot (SP,2/SP,4) is left untouched; caller discards it with
; ADDSP 4,i right after the call.
;
; Caller looks like:
;   LDWA  aLow,i  / PUSHA
;   LDWA  aHigh,i / PUSHA
;   LDWA  bLow,i  / PUSHA
;   LDWA  bHigh,i / PUSHA
;   CALL  DADD,i           ; or DSUB,i
;   ADDSP 4,i               ; discard b's now-unused slot
;   POPA
;   STWA  resultHigh,d
;   POPA
;   STWA  resultLow,d
;=======================================================================

DADD:
        LDWA    8,s             ; a low  (first-pushed, deep)
        ADDA    4,s             ; + b low (last-pushed, shallow); C = carry out
        STWA    8,s             ; result low -> a's slot (STWA leaves C alone)
        LDWA    6,s             ; a high (LDWA leaves C alone)
        ADCA    2,s             ; + b high + carry-in from the low word
        STWA    6,s             ; result high -> a's slot
        RET

DSUB:
        LDWA    8,s             ; a low
        SUBA    4,s             ; - b low; C=1 means no borrow, C=0 means a borrow
        STWA    8,s             ; result low -> a's slot
        LDWA    6,s             ; a high
        SBCA    2,s             ; - b high - borrow from the low word
        STWA    6,s             ; result high -> a's slot
        RET

;======================================================================
; ---- from dmul.pep ----
;======================================================================
;=======================================================================
; DMul 32-bit multiplication, truncated to 32-bit result.
;
; Take two 32-bit integers on the stack, multiply them, leave a 32-bit
; product in their place.
;
; Sample usage: multiply A * B, store product in C
;
;   LDWA Alo,d
;   PUSHA
;   LDWA Ahi,d
;   PUSHA
;   LDWA Blo,d
;   PUSHA
;   LDWA Bhi,d
;   PUSHA
;   CALL DMul
;   @DDROP      ; equivalent to ADDSP 4,i
;   POPA
;   STWA Chi,d
;   POPA
;   STWA Clo,d
;
; TODO: set N, Z, C flags (C = 1 if product does not fit in 32 bits)?
;
;=======================================================================


; Operands are pushed A low, A high, B low, B high, so after CALL the
; return address is at 0, B high at 2, B low at 4, A high at 6, A low at 8.
; The product overwrites A's slot (the first-pushed operand), matching
; DADD/DSUB/DDiv/UDDiv, so ADDSP 4,i after the call discards B.
sBHigh: .EQUATE 2
sBLow:  .EQUATE 4
sAHigh: .EQUATE 6
sALow:  .EQUATE 8

DMul:   LDWA  sAHigh, s
        MULA  sBLow, s
        STWA  sAHigh, s
        LDWA  sALow, s
        MULA  sBHigh, s
        ADDA  sAHigh, s
        STWA  sAHigh, s

        LDWA  sALow, s
        UMULA sBLow, s          ; A = resultLow, H = high16(L1*L2)
        STWA  sALow, s          ; save resultLow now, before H gets clobbered

        SWAPHA                  ; A = high16(L1*L2)
        ADDA  sAHigh, s
        STWA  sAHigh, s
        RET

;======================================================================
; ---- from ddiv32.pep ----
;======================================================================
;=======================================================================
; DDiv / UDDiv -- 32-bit / 32-bit division, matching the stack calling
; convention Prof. Howard gave Jess for DMul (see "DMUL - checking if
; on the right track" thread, 9/14/2026).
;=======================================================================

_UCmp16:
        LDWA    udivCmpY,d
        BREQ    _UCmp16YZero
        LDWA    udivCmpX,d
        XORA    0x8000,i
        STWA    udivTmp1,d
        LDWA    udivCmpY,d
        XORA    0x8000,i
        STWA    udivTmp2,d
        LDWA    udivTmp1,d
        CPWA    udivTmp2,d
        BREQ    _UCmp16Eq
        BRLT    _UCmp16Lt
        LDWA    1,i
        RET
_UCmp16Lt:
        LDWA    0xFFFF,i
        RET
_UCmp16Eq:
        LDWA    0,i
        RET
_UCmp16YZero:
        LDWA    udivCmpX,d
        BREQ    _UCmp16Eq
        LDWA    1,i
        RET

;=======================================================================
; _UDiv32: unsigned 32-bit / 32-bit division, restoring shift-subtract.
; Precondition:  udivDvdHi:udivDvdLo = dividend, udivDvrHi:udivDvrLo = divisor.
; Postcondition: udivQuoHi:udivQuoLo = quotient, udivRemHi:udivRemLo = remainder.
; Postcondition: udivDvdHi:udivDvdLo is destroyed (used as scratch).
;=======================================================================
_UDiv32:
        LDWA    0,i
        STWA    udivRemHi,d
        STWA    udivRemLo,d
        STWA    udivQuoHi,d
        STWA    udivQuoLo,d
        LDWA    32,i
        STWA    udivCnt,d

_UDiv32Loop:
        LDWA    udivCnt,d
        BREQ    _UDiv32Done

        ; Shift the still-unconsumed dividend left by 1; the bit shifted
        ; out of its top (the next bit of the original dividend, MSB
        ; first) lands in Carry for the next step to consume. (This use
        ; of Carry is safe -- ASLA/ROLA aren't subject to issue #11,
        ; which is specific to SUBA/SUBX/CPWA/CPWX.)
        LDWA    udivDvdLo,d
        ASLA
        STWA    udivDvdLo,d
        LDWA    udivDvdHi,d
        ROLA
        STWA    udivDvdHi,d

        ; Shift that bit into the bottom of the remainder.
        LDWA    udivRemLo,d
        ROLA
        STWA    udivRemLo,d
        LDWA    udivRemHi,d
        ROLA
        STWA    udivRemHi,d

        ; Shift the quotient left by 1 too, injecting a 0 that gets
        ; upgraded to 1 below if this iteration's subtraction succeeds.
        LDWA    udivQuoLo,d
        ASLA
        STWA    udivQuoLo,d
        LDWA    udivQuoHi,d
        ROLA
        STWA    udivQuoHi,d

        ; Is Rem (32-bit) >= Dvr (32-bit), unsigned?
        LDWA    udivRemHi,d
        STWA    udivCmpX,d
        LDWA    udivDvrHi,d
        STWA    udivCmpY,d
        CALL    _UCmp16
        BRLT    _UDiv32NoSub
        BRGT    _UDiv32DoSub
        LDWA    udivRemLo,d
        STWA    udivCmpX,d
        LDWA    udivDvrLo,d
        STWA    udivCmpY,d
        CALL    _UCmp16
        BRLT    _UDiv32NoSub

_UDiv32DoSub:
        ; Determine (before subtracting) whether the low-word subtraction
        ; will need to borrow.
        LDWA    udivRemLo,d
        STWA    udivCmpX,d
        LDWA    udivDvrLo,d
        STWA    udivCmpY,d
        CALL    _UCmp16
        BRLT    _UDiv32Borrows
        LDWA    0,i
        BR      _UDiv32BorrowDone
_UDiv32Borrows:
        LDWA    1,i
_UDiv32BorrowDone:
        STWA    udivTmp3,d

        ; The raw difference values from SUBA are correct regardless of
        ; issue #11 (only its flags are wrong), so we use the values and
        ; supply the borrow computed above by hand.
        LDWA    udivRemLo,d
        SUBA    udivDvrLo,d
        STWA    udivRemLo,d
        LDWA    udivRemHi,d
        SUBA    udivDvrHi,d
        SUBA    udivTmp3,d
        STWA    udivRemHi,d

        LDWA    udivQuoLo,d
        ORA     1,i
        STWA    udivQuoLo,d

_UDiv32NoSub:
        LDWA    udivCnt,d
        SUBA    1,i
        STWA    udivCnt,d
        BR      _UDiv32Loop

_UDiv32Done:
        RET

;=======================================================================
; _Negate32: two's complement negation of a 32-bit value.
; Precondition/postcondition: sNegHi:sNegLo.
; Deliberately avoids the Carry flag (issue #11): whether the low word's
; +1 carries into the high word is decided by checking if the low word
; became exactly 0 (BREQ/BRNE on the Z flag), not by Carry.
;=======================================================================
_Negate32:
        LDWA    sNegHi,d
        NOTA
        STWA    sNegHi,d
        LDWA    sNegLo,d
        NOTA
        STWA    sNegLo,d
        LDWA    sNegLo,d
        ADDA    1,i
        STWA    sNegLo,d
        BRNE    _Negate32Done
        LDWA    sNegHi,d
        ADDA    1,i
        STWA    sNegHi,d
_Negate32Done:
        RET

;=======================================================================
; _SDiv32: signed 32-bit / 32-bit division.
; Precondition/postcondition storage same as _UDiv32 (they share it).
; Per design proposal section 5: extract signs, run the unsigned
; routine on the absolute values, fix up the signs of the results
; afterward. Remainder takes the dividend's sign, matching Java/C and
; the existing 16-bit MODA.
;=======================================================================
_SDiv32:
        LDWA    udivDvdHi,d
        ANDA    0x8000,i
        STWA    sdivSignDvd,d
        LDWA    udivDvrHi,d
        ANDA    0x8000,i
        STWA    sdivSignDvr,d

        LDWA    sdivSignDvd,d
        STWA    sdivNegR,d              ; nonzero (0x8000) iff dividend negative

        LDWA    sdivSignDvd,d
        XORA    sdivSignDvr,d
        STWA    sdivNegQ,d              ; nonzero iff signs differ

        ; |dividend|
        LDWA    sdivSignDvd,d
        BREQ    _SDiv32DvdAbsDone
        LDWA    udivDvdHi,d
        STWA    sNegHi,d
        LDWA    udivDvdLo,d
        STWA    sNegLo,d
        CALL    _Negate32
        LDWA    sNegHi,d
        STWA    udivDvdHi,d
        LDWA    sNegLo,d
        STWA    udivDvdLo,d
_SDiv32DvdAbsDone:

        ; |divisor|
        LDWA    sdivSignDvr,d
        BREQ    _SDiv32DvrAbsDone
        LDWA    udivDvrHi,d
        STWA    sNegHi,d
        LDWA    udivDvrLo,d
        STWA    sNegLo,d
        CALL    _Negate32
        LDWA    sNegHi,d
        STWA    udivDvrHi,d
        LDWA    sNegLo,d
        STWA    udivDvrLo,d
_SDiv32DvrAbsDone:

        ; Same fast-path-with-fallback as UDDiv, now operating on the
        ; magnitudes (sign already handled above).
        LDWA    udivDvrHi,d
        BRNE    _SDiv32General
        LDWA    udivDvdHi,d
        STWA    f16DvdHi,d
        LDWA    udivDvdLo,d
        STWA    f16DvdLo,d
        LDWA    udivDvrLo,d
        STWA    f16Dvr,d
        CALL    _UDiv32By16
        LDWA    f16Overflow,d
        BRNE    _SDiv32General
        LDWA    0,i
        STWA    udivQuoHi,d
        LDWA    f16Quo,d
        STWA    udivQuoLo,d
        LDWA    0,i
        STWA    udivRemHi,d
        LDWA    f16Rem,d
        STWA    udivRemLo,d
        BR      _SDiv32AfterDivide
_SDiv32General:
        CALL    _UDiv32
_SDiv32AfterDivide:

        ; fix up quotient sign
        LDWA    sdivNegQ,d
        BREQ    _SDiv32QDone
        LDWA    udivQuoHi,d
        STWA    sNegHi,d
        LDWA    udivQuoLo,d
        STWA    sNegLo,d
        CALL    _Negate32
        LDWA    sNegHi,d
        STWA    udivQuoHi,d
        LDWA    sNegLo,d
        STWA    udivQuoLo,d
_SDiv32QDone:

        ; fix up remainder sign
        LDWA    sdivNegR,d
        BREQ    _SDiv32RDone
        LDWA    udivRemHi,d
        STWA    sNegHi,d
        LDWA    udivRemLo,d
        STWA    sNegLo,d
        CALL    _Negate32
        LDWA    sNegHi,d
        STWA    udivRemHi,d
        LDWA    sNegLo,d
        STWA    udivRemLo,d
_SDiv32RDone:
        RET

udivDvdHi:   .BLOCK 2
udivDvdLo:   .BLOCK 2
udivDvrHi:   .BLOCK 2
udivDvrLo:   .BLOCK 2
udivRemHi:   .BLOCK 2
udivRemLo:   .BLOCK 2
udivQuoHi:   .BLOCK 2
udivQuoLo:   .BLOCK 2
udivCnt:     .BLOCK 2
udivTmp1:    .BLOCK 2
udivTmp2:    .BLOCK 2
udivTmp3:    .BLOCK 2
udivCmpX:    .BLOCK 2
udivCmpY:    .BLOCK 2
sNegHi:      .BLOCK 2
sNegLo:      .BLOCK 2
sdivSignDvd: .BLOCK 2
sdivSignDvr: .BLOCK 2
sdivNegQ:    .BLOCK 2
sdivNegR:    .BLOCK 2

;=======================================================================
; DDiv / UDDiv -- 32-bit / 32-bit division, matching the stack template
; Prof. Howard gave Jess for DMul.
;
; Take two 32-bit integers on the stack, divide them, leave a 32-bit
; quotient and a 32-bit remainder in their place. On return, the Carry
; bit is set if the divisor was 0 (quotient and remainder are left as
; 0 in that case, matching the existing 16-bit divide-by-zero contract
; from PR #8: N=0, Z=1, V=1, C=1).
;
; DDiv:  signed.   UDDiv: unsigned.
;
; v2 CONVENTION CHANGE (matching the DADD/DSUB fix): the quotient now
; overwrites the FIRST-pushed operand's slot (the dividend), and the
; remainder overwrites the LAST-pushed operand's slot (the divisor), so
; that a plain ADDSP 4,i right after CALL discards the remainder and
; chains cleanly for a sequence of divisions:
;   PUSH dividend
;   PUSH divisor
;   CALL UDDiv     ; computes dividend / divisor, quotient replaces dividend's slot
;   ADDSP 4,i       ; discards divisor's now-unused slot (remainder, if unneeded)
;   PUSH nextDivisor
;   CALL UDDiv     ; computes (first quotient) / nextDivisor
;   ADDSP 4,i
;   POPA / POPA    ; final quotient
;
; Push order is dividend first, then divisor, matching the same
; left-to-right operand order used by DADD/DSUB/DMUL (PUSH A; PUSH B).
;
; Stack layout at entry (SP,0 is the return address, since the caller
; just did CALL DDiv,i / CALL UDDiv,i):
;   SP,6  dividend high word (first-pushed)   SP,2  divisor high word (last-pushed)
;   SP,8  dividend low word                   SP,4  divisor low word
;
; On return, in place:
;   SP,6  quotient high word                  SP,2  remainder high word
;   SP,8  quotient low word                   SP,4  remainder low word
;
; Caller looks like:
;   LDWA  dividendLow,i / PUSHA
;   LDWA  dividendHigh,i / PUSHA
;   LDWA  divisorLow,i / PUSHA
;   LDWA  divisorHigh,i / PUSHA
;   CALL  DDiv,i          ; or UDDiv,i
;   ADDSP 4,i               ; discard divisor's now-unused slot, if remainder not needed
;   POPA
;   STWA  quotientHigh,i
;   POPA
;   STWA  quotientLow,i
;   BRC   ... handle divide-by-zero, if needed ...
;=======================================================================

UDDiv:
        LDWA    6,s
        STWA    udivDvdHi,d
        LDWA    8,s
        STWA    udivDvdLo,d
        LDWA    2,s
        STWA    udivDvrHi,d
        LDWA    4,s
        STWA    udivDvrLo,d

        LDWA    udivDvrHi,d
        ORA     udivDvrLo,d
        BREQ    _DDivByZero

        ; Divisor fits in one word? Try the fast path (16 iterations)
        ; first -- but its quotient is only one word wide, so if this
        ; particular dividend would overflow that (a real possibility
        ; here, since UDDiv accepts a full 32-bit dividend), fall back
        ; to the general routine instead of losing precision.
        LDWA    udivDvrHi,d
        BRNE    _UDDivGeneral
        LDWA    udivDvdHi,d
        STWA    f16DvdHi,d
        LDWA    udivDvdLo,d
        STWA    f16DvdLo,d
        LDWA    udivDvrLo,d
        STWA    f16Dvr,d
        CALL    _UDiv32By16
        LDWA    f16Overflow,d
        BRNE    _UDDivGeneral
        LDWA    0,i
        STWA    udivQuoHi,d
        LDWA    f16Quo,d
        STWA    udivQuoLo,d
        LDWA    0,i
        STWA    udivRemHi,d
        LDWA    f16Rem,d
        STWA    udivRemLo,d
        BR      _DDivWriteBack

_UDDivGeneral:
        CALL    _UDiv32
        BR      _DDivWriteBack

_DDivSigned:
        LDWA    6,s
        STWA    udivDvdHi,d
        LDWA    8,s
        STWA    udivDvdLo,d
        LDWA    2,s
        STWA    udivDvrHi,d
        LDWA    4,s
        STWA    udivDvrLo,d

        LDWA    udivDvrHi,d
        ORA     udivDvrLo,d
        BREQ    _DDivByZero

        CALL    _SDiv32

_DDivWriteBack:
        LDWA    udivQuoHi,d
        STWA    6,s
        LDWA    udivQuoLo,d
        STWA    8,s
        LDWA    udivRemHi,d
        STWA    2,s
        LDWA    udivRemLo,d
        STWA    4,s

        ; N = sign of quotient, Z = quotient is 0, V = 0, C = 0 (success)
        LDWA    0,i
        STWA    dflagsN,d
        STWA    dflagsZ,d
        LDWA    udivQuoHi,d
        ANDA    0x8000,i
        BREQ    _DDivNDone
        LDWA    1,i
        STWA    dflagsN,d
_DDivNDone:
        LDWA    udivQuoHi,d
        ORA     udivQuoLo,d
        BRNE    _DDivZDone
        LDWA    1,i
        STWA    dflagsZ,d
_DDivZDone:
        LDWA    0,i
        STWA    dflagsNibble,d
        LDWA    dflagsN,d
        BREQ    _DDivBuildZ
        LDWA    dflagsNibble,d
        ORA     8,i
        STWA    dflagsNibble,d
_DDivBuildZ:
        LDWA    dflagsZ,d
        BREQ    _DDivBuildDone
        LDWA    dflagsNibble,d
        ORA     4,i
        STWA    dflagsNibble,d
_DDivBuildDone:
        LDWA    dflagsNibble,d
        MOVAFLG
        RET

_DDivByZero:
        LDWA    0,i
        STWA    2,s
        STWA    4,s
        STWA    6,s
        STWA    8,s
        LDWA    7,i             ; N=0,Z=1,V=1,C=1 -> 0111
        MOVAFLG
        RET

DDiv:
        BR      _DDivSigned

dflagsN:      .BLOCK 2
dflagsZ:      .BLOCK 2
dflagsNibble: .BLOCK 2
;=======================================================================
; _UDiv32By16: unsigned 32-bit dividend / 16-bit divisor, the genuine
; fast path (16 iterations, not 32), with an early-exit overflow check
; that skips the loop entirely when the quotient wouldn't fit anyway.
;
; This is the piece that was discussed but never actually implemented:
; UDDiv/DDiv currently always run the general 32-iteration _UDiv32
; even when the divisor fits in one word.
;
; Precondition: f16DvdHi:f16DvdLo = dividend (32-bit).
;               f16Dvr = divisor (16-bit, nonzero).
; Postcondition: f16Overflow = 1 if the quotient doesn't fit in 16 bits
;                (f16Quo/f16Rem are NOT meaningful in that case), else 0.
;                f16Quo = quotient, f16Rem = remainder (both 16-bit).
;                f16DvdLo is destroyed (used as scratch).
;=======================================================================
_UDiv32By16:
        ; Early-exit overflow check: quotient fits in 16 bits iff
        ; dividendHi < divisor (unsigned). No iterations needed either way
        ; to know this.
        LDWA    f16DvdHi,d
        STWA    udivCmpX,d
        LDWA    f16Dvr,d
        STWA    udivCmpY,d
        CALL    _UCmp16
        BRLT    _UDiv32By16NoOverflow
        LDWA    1,i
        STWA    f16Overflow,d
        RET
_UDiv32By16NoOverflow:
        LDWA    0,i
        STWA    f16Overflow,d
        STWA    f16Quo,d
        LDWA    f16DvdHi,d
        STWA    f16Rem,d        ; remainder starts as dividendHi, already < divisor
        LDWA    16,i
        STWA    f16Cnt,d

_UDiv32By16Loop:
        LDWA    f16Cnt,d
        BREQ    _UDiv32By16Done

        ; shift dividendLo left 1; the bit shifted out (next bit of the
        ; original dividend) lands in Carry.
        LDWA    f16DvdLo,d
        ASLA
        STWA    f16DvdLo,d

        ; shift that dividend bit into the remainder. The bit shifted
        ; OUT of the remainder (its own old top bit) means the true
        ; (unmasked) remainder is 65536 higher than the stored value,
        ; which is unconditionally >= any 16-bit divisor -- so a carry
        ; here means "definitely subtract," skipping the compare.
        LDWA    f16Rem,d
        ROLA
        STWA    f16Rem,d
        BRC     _UDiv32By16DoSub

        LDWA    f16Rem,d
        STWA    udivCmpX,d
        LDWA    f16Dvr,d
        STWA    udivCmpY,d
        CALL    _UCmp16
        BRLT    _UDiv32By16NoSub

_UDiv32By16DoSub:
        LDWA    f16Rem,d
        SUBA    f16Dvr,d
        STWA    f16Rem,d
        ; shift the quotient left 1, with a fresh 0 (independent of the
        ; dividend/remainder carry chain above), then upgrade to 1.
        LDWA    f16Quo,d
        ASLA
        ORA     1,i
        STWA    f16Quo,d
        BR      _UDiv32By16NextIter

_UDiv32By16NoSub:
        LDWA    f16Quo,d
        ASLA
        STWA    f16Quo,d

_UDiv32By16NextIter:
        LDWA    f16Cnt,d
        SUBA    1,i
        STWA    f16Cnt,d
        BR      _UDiv32By16Loop

_UDiv32By16Done:
        RET

f16DvdHi:   .BLOCK 2
f16DvdLo:   .BLOCK 2
f16Dvr:     .BLOCK 2
f16Quo:     .BLOCK 2
f16Rem:     .BLOCK 2
f16Cnt:     .BLOCK 2
f16Overflow: .BLOCK 2

;======================================================================
; ---- from ddprint.pep ----
;======================================================================
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

;======================================================================
; ---- from ddread.pep ----
;======================================================================
;=======================================================================
; ddread.pep -- read a signed 32-bit decimal value from charIn. This is
; the input side of ddprint.pep; the Declan compiler uses DREAD for
; ReadLong.
;
; Like @DECI, DREAD skips leading blanks and line ends, accepts an
; optional + or - sign, then reads digits up to the first character
; that is not a digit (that character is consumed). Anything that is
; not a number reads as 0. Values outside the 32-bit range wrap
; silently, the same as LONGINT arithmetic.
;
; DREAD takes no stack arguments. It leaves the result in the static
; words rdlHi (high word) and rdlLo (low word); the caller copies them
; where they belong.
;
; The multiply and add use DMul and DADD, and the negation uses DSUB,
; so daddsub.pep and dmul.pep must also be included (the Declan
; compiler does this itself, so they are not included here).
;
; NOTE (same concurrency caveat as ddprint.pep): the working variables
; are statically allocated, not stack-based.
;=======================================================================

DREAD:
        LDWA    0,i
        STWA    rdlHi,d
        STWA    rdlLo,d
        STWA    rdlNeg,d
rdlSkip:
        LDBA    charIn,d
        CPWA    ' ',i
        BREQ    rdlSkip
        CPWA    '\n',i
        BREQ    rdlSkip
        CPWA    9,i
        BREQ    rdlSkip
        CPWA    13,i
        BREQ    rdlSkip
        CPWA    '-',i
        BRNE    rdlPlus
        LDWA    1,i
        STWA    rdlNeg,d
        BR      rdlNext
rdlPlus:
        CPWA    '+',i
        BRNE    rdlTest
rdlNext:
        LDBA    charIn,d
rdlTest:
        SUBA    '0',i
        BRLT    rdlFinish
        CPWA    9,i
        BRGT    rdlFinish
        STWA    rdlDigit,d

        ; value = value * 10 + digit
        LDWA    rdlLo,d
        PUSHA
        LDWA    rdlHi,d
        PUSHA
        LDWA    10,i
        PUSHA
        LDWA    0,i
        PUSHA
        CALL    DMul,i
        ADDSP   4,i
        LDWA    rdlDigit,d
        PUSHA
        LDWA    0,i
        PUSHA
        CALL    DADD,i
        ADDSP   4,i
        POPA
        STWA    rdlHi,d
        POPA
        STWA    rdlLo,d
        BR      rdlNext

rdlFinish:
        LDWA    rdlNeg,d
        BREQ    rdlDone
        ; value = 0 - value
        LDWA    0,i
        PUSHA
        LDWA    0,i
        PUSHA
        LDWA    rdlLo,d
        PUSHA
        LDWA    rdlHi,d
        PUSHA
        CALL    DSUB,i
        ADDSP   4,i
        POPA
        STWA    rdlHi,d
        POPA
        STWA    rdlLo,d
rdlDone:
        RET

rdlHi:    .BLOCK 2
rdlLo:    .BLOCK 2
rdlNeg:   .BLOCK 2
rdlDigit: .BLOCK 2
