; int32.pep -- the 32-bit (double word) integer library: DADD, DSUB, DMul,
; DDiv/UDDiv, DPRINTD/UDPRINTD and DREAD in one file. Do
;
;     .INCLUDELIB "int32"
;
; after the halt sequence to load all of them. The stack macros are in
; stack.pep and must be included BEFORE their first use.
;
; REENTRANT: no routine in this file uses static storage. Working
; variables live in stack frames and small helpers take their operands in
; registers, so the routines are safe to call from several threads and
; from code that might be interrupted mid-routine by a context switch.
; The one interface change this required: DREAD returns its result in
; registers (A = high word, X = low word) instead of the old static
; words rdlHi and rdlLo.

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
;======================================================================
; ---- from ddiv32.pep ----
;======================================================================
;=======================================================================
; DDiv / UDDiv -- 32-bit / 32-bit division, matching the stack calling
; convention Prof. Howard gave Jess for DMul (see "DMUL - checking if
; on the right track" thread, 9/14/2026).
;
; REENTRANT: none of the division code uses static storage. UDDiv and DDiv
; allocate one working frame on the stack (DVFRAME bytes) and the helpers
; _UDiv32 and _UDiv32By16 read and write it through ",s" addressing. The
; small helpers _UCmp16 and _Negate32 take their operands in registers.
; Any number of threads may therefore be inside these routines at once.
;
; Frame layout (offsets from SP right after SUBSP DVFRAME,i in UDDiv/DDiv):
;    0 dividend high   2 dividend low    (shifted away as the division runs)
;    4 divisor high    6 divisor low
;    8 remainder high 10 remainder low
;   12 quotient high  14 quotient low
;   16 loop counter   18 scratch
;   20 negate-quotient flag   22 negate-remainder flag
; The helpers are entered with a return address on top of the frame, so they
; see every offset above plus 2 (the h... names below).
;=======================================================================

DVFRAME:   .EQUATE 24

dvDvdHi:   .EQUATE 0
dvDvdLo:   .EQUATE 2
dvDvrHi:   .EQUATE 4
dvDvrLo:   .EQUATE 6
dvRemHi:   .EQUATE 8
dvRemLo:   .EQUATE 10
dvQuoHi:   .EQUATE 12
dvQuoLo:   .EQUATE 14
dvCnt:     .EQUATE 16
dvTmp:     .EQUATE 18
dvNegQ:    .EQUATE 20
dvNegR:    .EQUATE 22

hDvdHi:    .EQUATE 2
hDvdLo:    .EQUATE 4
hDvrHi:    .EQUATE 6
hDvrLo:    .EQUATE 8
hRemHi:    .EQUATE 10
hRemLo:    .EQUATE 12
hQuoHi:    .EQUATE 14
hQuoLo:    .EQUATE 16
hCnt:      .EQUATE 18
hTmp:      .EQUATE 20

; The caller's operands, as seen from inside UDDiv/DDiv after the frame has
; been allocated (entry offsets 6,8 and 2,4 plus DVFRAME).
argDvdHi:  .EQUATE 30
argDvdLo:  .EQUATE 32
argDvrHi:  .EQUATE 26
argDvrLo:  .EQUATE 28

;=======================================================================
; _UCmp16: unsigned 16-bit compare.  In: A = x, X = y.
; Out: A = 1 if x > y, 0 if x = y, 0xFFFF if x < y, with N and Z set to
; match (BRLT, BREQ and BRGT can be used right after the CALL).
; Uses 4 bytes of stack for the duration of the call; no static storage.
;=======================================================================
_UCmp16:
        SUBSP   4,i
        STWA    2,s             ; x
        STWX    0,s             ; y
        LDWA    0,s
        BREQ    _UCmp16YZero
        LDWA    2,s
        XORA    0x8000,i
        STWA    2,s
        LDWA    0,s
        XORA    0x8000,i
        STWA    0,s
        LDWA    2,s
        CPWA    0,s
        BREQ    _UCmp16Eq
        BRLT    _UCmp16Lt
_UCmp16Gt:
        ADDSP   4,i
        LDWA    1,i
        RET
_UCmp16Lt:
        ADDSP   4,i
        LDWA    0xFFFF,i
        RET
_UCmp16Eq:
        ADDSP   4,i
        LDWA    0,i
        RET
_UCmp16YZero:
        LDWA    2,s
        BREQ    _UCmp16Eq
        BR      _UCmp16Gt

;=======================================================================
; _UDiv32: unsigned 32-bit / 32-bit division, restoring shift-subtract.
; Operates on the caller's frame (see the layout above), entered with the
; return address on top of it.
; Precondition:  hDvdHi:hDvdLo = dividend, hDvrHi:hDvrLo = divisor.
; Postcondition: hQuoHi:hQuoLo = quotient, hRemHi:hRemLo = remainder.
; Postcondition: hDvdHi:hDvdLo is destroyed (used as scratch).
;=======================================================================
_UDiv32:
        LDWA    0,i
        STWA    hRemHi,s
        STWA    hRemLo,s
        STWA    hQuoHi,s
        STWA    hQuoLo,s
        LDWA    32,i
        STWA    hCnt,s

_UDiv32Loop:
        LDWA    hCnt,s
        BREQ    _UDiv32Done

        ; Shift the still-unconsumed dividend left by 1; the bit shifted
        ; out of its top (the next bit of the original dividend, MSB
        ; first) lands in Carry for the next step to consume. (This use
        ; of Carry is safe -- ASLA/ROLA aren't subject to issue #11,
        ; which is specific to SUBA/SUBX/CPWA/CPWX.)
        LDWA    hDvdLo,s
        ASLA
        STWA    hDvdLo,s
        LDWA    hDvdHi,s
        ROLA
        STWA    hDvdHi,s

        ; Shift that bit into the bottom of the remainder.
        LDWA    hRemLo,s
        ROLA
        STWA    hRemLo,s
        LDWA    hRemHi,s
        ROLA
        STWA    hRemHi,s

        ; Shift the quotient left by 1 too, injecting a 0 that gets
        ; upgraded to 1 below if this iteration's subtraction succeeds.
        LDWA    hQuoLo,s
        ASLA
        STWA    hQuoLo,s
        LDWA    hQuoHi,s
        ROLA
        STWA    hQuoHi,s

        ; Is Rem (32-bit) >= Dvr (32-bit), unsigned?
        LDWA    hRemHi,s
        LDWX    hDvrHi,s
        CALL    _UCmp16
        BRLT    _UDiv32NoSub
        BRGT    _UDiv32DoSub
        LDWA    hRemLo,s
        LDWX    hDvrLo,s
        CALL    _UCmp16
        BRLT    _UDiv32NoSub

_UDiv32DoSub:
        ; Determine (before subtracting) whether the low-word subtraction
        ; will need to borrow.
        LDWA    hRemLo,s
        LDWX    hDvrLo,s
        CALL    _UCmp16
        BRLT    _UDiv32Borrows
        LDWA    0,i
        BR      _UDiv32BorrowDone
_UDiv32Borrows:
        LDWA    1,i
_UDiv32BorrowDone:
        STWA    hTmp,s

        ; The raw difference values from SUBA are correct regardless of
        ; issue #11 (only its flags are wrong), so we use the values and
        ; supply the borrow computed above by hand.
        LDWA    hRemLo,s
        SUBA    hDvrLo,s
        STWA    hRemLo,s
        LDWA    hRemHi,s
        SUBA    hDvrHi,s
        SUBA    hTmp,s
        STWA    hRemHi,s

        LDWA    hQuoLo,s
        ORA     1,i
        STWA    hQuoLo,s

_UDiv32NoSub:
        LDWA    hCnt,s
        SUBA    1,i
        STWA    hCnt,s
        BR      _UDiv32Loop

_UDiv32Done:
        RET

;=======================================================================
; _Negate32: two's complement negation of a 32-bit value.
; In: A = high word, X = low word.  Out: A = high word, X = low word of
; the negated value.  No memory is used.
; Deliberately avoids the Carry flag (issue #11): whether the low word's
; +1 carries into the high word is decided by checking if the low word
; became exactly 0 (BREQ/BRNE on the Z flag), not by Carry.
;=======================================================================
_Negate32:
        NOTA
        NOTX
        ADDX    1,i
        BRNE    _Negate32Done
        ADDA    1,i
_Negate32Done:
        RET

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
        SUBSP   DVFRAME,i
        LDWA    argDvdHi,s
        STWA    dvDvdHi,s
        LDWA    argDvdLo,s
        STWA    dvDvdLo,s
        LDWA    argDvrHi,s
        STWA    dvDvrHi,s
        LDWA    argDvrLo,s
        STWA    dvDvrLo,s
        ORA     dvDvrHi,s
        BREQ    _DDivByZero

        ; Unsigned: nothing to negate afterwards.
        LDWA    0,i
        STWA    dvNegQ,s
        STWA    dvNegR,s
        BR      _DDivCompute

DDiv:
        SUBSP   DVFRAME,i
        LDWA    argDvdHi,s
        STWA    dvDvdHi,s
        LDWA    argDvdLo,s
        STWA    dvDvdLo,s
        LDWA    argDvrHi,s
        STWA    dvDvrHi,s
        LDWA    argDvrLo,s
        STWA    dvDvrLo,s
        ORA     dvDvrHi,s
        BREQ    _DDivByZero

        ; Signed: extract the signs, divide the magnitudes, fix up the
        ; signs afterward. The remainder takes the dividend's sign, matching
        ; Java/C and the existing 16-bit MODA.
        LDWA    dvDvdHi,s
        ANDA    0x8000,i
        STWA    dvNegR,s                ; nonzero iff dividend negative
        LDWA    dvDvrHi,s
        ANDA    0x8000,i
        STWA    dvTmp,s                 ; nonzero iff divisor negative
        XORA    dvNegR,s
        STWA    dvNegQ,s                ; nonzero iff signs differ

        LDWA    dvNegR,s
        BREQ    _DDivDvdAbsDone
        LDWA    dvDvdHi,s
        LDWX    dvDvdLo,s
        CALL    _Negate32
        STWA    dvDvdHi,s
        STWX    dvDvdLo,s
_DDivDvdAbsDone:

        LDWA    dvTmp,s
        BREQ    _DDivDvrAbsDone
        LDWA    dvDvrHi,s
        LDWX    dvDvrLo,s
        CALL    _Negate32
        STWA    dvDvrHi,s
        STWX    dvDvrLo,s
_DDivDvrAbsDone:

        ; Both entry points arrive here with magnitudes in the frame and the
        ; two sign flags set. Divisor fits in one word? Try the fast path
        ; (16 iterations) first -- but its quotient is only one word wide,
        ; so if this particular dividend would overflow that (a real
        ; possibility, since a full 32-bit dividend is accepted), fall back
        ; to the general routine instead of losing precision.
_DDivCompute:
        LDWA    dvDvrHi,s
        BRNE    _DDivGeneral
        CALL    _UDiv32By16
        BRNE    _DDivGeneral
        BR      _DDivSigns
_DDivGeneral:
        CALL    _UDiv32

_DDivSigns:
        LDWA    dvNegQ,s
        BREQ    _DDivQDone
        LDWA    dvQuoHi,s
        LDWX    dvQuoLo,s
        CALL    _Negate32
        STWA    dvQuoHi,s
        STWX    dvQuoLo,s
_DDivQDone:
        LDWA    dvNegR,s
        BREQ    _DDivRDone
        LDWA    dvRemHi,s
        LDWX    dvRemLo,s
        CALL    _Negate32
        STWA    dvRemHi,s
        STWX    dvRemLo,s
_DDivRDone:

        ; Write the results back over the operands.
        LDWA    dvQuoHi,s
        STWA    argDvdHi,s
        LDWA    dvQuoLo,s
        STWA    argDvdLo,s
        LDWA    dvRemHi,s
        STWA    argDvrHi,s
        LDWA    dvRemLo,s
        STWA    argDvrLo,s

        ; N = sign of quotient, Z = quotient is 0, V = 0, C = 0 (success).
        ; Build the flag nibble in X, then move it through A.
        LDWX    0,i
        LDWA    dvQuoHi,s
        BRGE    _DDivNDone
        LDWX    8,i
_DDivNDone:
        LDWA    dvQuoHi,s
        ORA     dvQuoLo,s
        BRNE    _DDivZDone
        ORX     4,i
_DDivZDone:
        SWAPAX
        ADDSP   DVFRAME,i
        MOVAFLG
        RET

_DDivByZero:
        LDWA    0,i
        STWA    argDvrHi,s
        STWA    argDvrLo,s
        STWA    argDvdHi,s
        STWA    argDvdLo,s
        ADDSP   DVFRAME,i
        LDWA    7,i             ; N=0,Z=1,V=1,C=1 -> 0111
        MOVAFLG
        RET

;=======================================================================
; _UDiv32By16: unsigned 32-bit dividend / 16-bit divisor, the genuine
; fast path (16 iterations, not 32), with an early-exit overflow check
; that skips the loop entirely when the quotient wouldn't fit anyway.
;
; Works on the caller's frame, entered with the return address on top of it.
; Precondition: hDvdHi:hDvdLo = dividend (32-bit).
;               hDvrLo = divisor (16-bit, nonzero; hDvrHi must be 0).
; Out: A = 1 if the quotient doesn't fit in 16 bits (nothing in the frame
;      is changed then), else A = 0 with N=0, Z=1, and
;      hQuoHi:hQuoLo = quotient, hRemHi:hRemLo = remainder (high words 0).
;      hDvdLo is destroyed (used as scratch), as are hRemLo/hCnt.
;=======================================================================
_UDiv32By16:
        ; Early-exit overflow check: quotient fits in 16 bits iff
        ; dividendHi < divisor (unsigned). No iterations needed either way
        ; to know this.
        LDWA    hDvdHi,s
        LDWX    hDvrLo,s
        CALL    _UCmp16
        BRLT    _UDiv32By16NoOverflow
        LDWA    1,i
        RET
_UDiv32By16NoOverflow:
        LDWA    0,i
        STWA    hQuoHi,s
        STWA    hQuoLo,s
        STWA    hRemHi,s
        LDWA    hDvdHi,s
        STWA    hRemLo,s        ; remainder starts as dividendHi, already < divisor
        LDWA    16,i
        STWA    hCnt,s

_UDiv32By16Loop:
        LDWA    hCnt,s
        BREQ    _UDiv32By16Done

        ; shift dividendLo left 1; the bit shifted out (next bit of the
        ; original dividend) lands in Carry.
        LDWA    hDvdLo,s
        ASLA
        STWA    hDvdLo,s

        ; shift that dividend bit into the remainder. The bit shifted
        ; OUT of the remainder (its own old top bit) means the true
        ; (unmasked) remainder is 65536 higher than the stored value,
        ; which is unconditionally >= any 16-bit divisor -- so a carry
        ; here means "definitely subtract," skipping the compare.
        LDWA    hRemLo,s
        ROLA
        STWA    hRemLo,s
        BRC     _UDiv32By16DoSub

        LDWA    hRemLo,s
        LDWX    hDvrLo,s
        CALL    _UCmp16
        BRLT    _UDiv32By16NoSub

_UDiv32By16DoSub:
        LDWA    hRemLo,s
        SUBA    hDvrLo,s
        STWA    hRemLo,s
        ; shift the quotient left 1, with a fresh 0 (independent of the
        ; dividend/remainder carry chain above), then upgrade to 1.
        LDWA    hQuoLo,s
        ASLA
        ORA     1,i
        STWA    hQuoLo,s
        BR      _UDiv32By16NextIter

_UDiv32By16NoSub:
        LDWA    hQuoLo,s
        ASLA
        STWA    hQuoLo,s

_UDiv32By16NextIter:
        LDWA    hCnt,s
        SUBA    1,i
        STWA    hCnt,s
        BR      _UDiv32By16Loop

_UDiv32By16Done:
        LDWA    0,i
        RET

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
; REENTRANT: the digit buffer and working variables live in a stack frame
; (UDPRINTD) or are not needed at all (DPRINTD), so several threads may
; print at once. Their characters still interleave on the console, because
; each thread's output goes to the same charOut device.
;=======================================================================

; UDPRINTD frame (SP-relative right after SUBSP 16):
;   0..9 digit buffer (least significant digit first), 10 digit count,
;   12 current value high, 14 current value low
udpDigits: .EQUATE 0
udpCount:  .EQUATE 10
udpCurHi:  .EQUATE 12
udpCurLo:  .EQUATE 14
udpFrame:  .EQUATE 16

udpValHi: .EQUATE 2      ; offset from SP at UDPRINTD entry: value hi word
udpValLo: .EQUATE 4      ; value lo word
udpArgHi: .EQUATE 18     ; the same operands once the frame is allocated
udpArgLo: .EQUATE 20

UDPRINTD:
        LDWA    udpValHi,s
        ORA     udpValLo,s
        BRNE    udpNonzero
        LDBA    '0',i
        STBA    charOut,d
        RET

udpNonzero:
        SUBSP   udpFrame,i
        LDWA    udpArgHi,s
        STWA    udpCurHi,s
        LDWA    udpArgLo,s
        STWA    udpCurLo,s
        LDWX    0,i
        STWX    udpCount,s

udpLoop:
        LDWA    udpCurHi,s
        ORA     udpCurLo,s
        BREQ    udpPrint

        ; push current value (low, high) and divisor 10 (low, high).
        ; Each PUSHA moves SP down 2, so the frame fields are read at the
        ; offsets they have at that moment (curLo at 14, curHi then at 14).
        LDWA    udpCurLo,s
        PUSHA
        LDWA    14,s            ; udpCurHi, 2 bytes further from SP now
        PUSHA
        LDWA    10,i
        PUSHA
        LDWA    0,i
        PUSHA
        CALL    UDDiv,i
        ; the 8 bytes pushed above are still on the stack, results in place:
        ; 0 remHi, 2 remLo, 4 quoHi, 6 quoLo.  The frame is 8 bytes further.
        LDWA    4,s
        STWA    20,s            ; udpCurHi + 8
        LDWA    6,s
        STWA    22,s            ; udpCurLo + 8
        LDWA    2,s             ; remainder, always below 10
        ADDA    '0',i
        LDWX    18,s            ; udpCount + 8
        STBA    8,sx            ; udpDigits + 8, indexed by the count
        ADDX    1,i
        STWX    18,s
        ADDSP   8,i

        BR      udpLoop

udpPrint:
        LDWX    udpCount,s
        SUBX    1,i
udpPrintLoop:
        BRLT    udpDone
        LDBA    udpDigits,sx
        STBA    charOut,d
        SUBX    1,i
        BR      udpPrintLoop
udpDone:
        ADDSP   udpFrame,i
        RET

dpValHi: .EQUATE 2
dpValLo: .EQUATE 4

DPRINTD:
        ; No cached copy of the operand is needed: each PUSHA moves SP, so
        ; the operand is re-read at its adjusted offset every time.
        LDWA    dpValHi,s
        BRLT    dpNegative
        ; non-negative: pass the operand straight to UDPRINTD
        ; (push low, then high, matching its calling convention)
        LDWA    dpValLo,s
        PUSHA
        LDWA    4,s             ; dpValHi, 2 bytes further from SP now
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
        PUSHA                   ; a low
        PUSHA                   ; a high
        LDWA    8,s             ; dpValLo, 4 bytes further from SP now
        PUSHA
        LDWA    8,s             ; dpValHi, 6 bytes further from SP now
        PUSHA
        CALL    DSUB,i
        ADDSP   4,i
        ; magnitude (low, high) now on top of stack; just pass straight
        ; through to UDPRINTD using the same convention
        CALL    UDPRINTD,i
        ADDSP   4,i
        RET

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
; DREAD takes no stack arguments. It returns the result in registers:
; A = high word, X = low word. (It used to leave it in static words; the
; registers keep the routine reentrant.)
;
; The multiply and add use DMul and DADD, and the negation uses DSUB,
; so the double-word routines above must be in the same program (they are,
; because they are all in this file).
;
; REENTRANT: the working variables are in a stack frame.
;=======================================================================

; DREAD frame (SP-relative right after SUBSP 8):
rdlNeg:   .EQUATE 0
rdlDigit: .EQUATE 2
rdlHi:    .EQUATE 4
rdlLo:    .EQUATE 6
rdlFrame: .EQUATE 8

DREAD:
        SUBSP   rdlFrame,i
        LDWA    0,i
        STWA    rdlHi,s
        STWA    rdlLo,s
        STWA    rdlNeg,s
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
        STWA    rdlNeg,s
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
        STWA    rdlDigit,s

        ; value = value * 10 + digit.  Offsets below are the frame offsets
        ; plus the bytes pushed so far.
        LDWA    rdlLo,s
        PUSHA
        LDWA    6,s             ; rdlHi + 2
        PUSHA
        LDWA    10,i
        PUSHA
        LDWA    0,i
        PUSHA
        CALL    DMul,i
        ADDSP   4,i             ; the product (low, high) stays on the stack
        LDWA    6,s             ; rdlDigit + 4
        PUSHA
        LDWA    0,i
        PUSHA
        CALL    DADD,i
        ADDSP   4,i
        POPA
        STWA    6,s             ; rdlHi + 2 (the low word is still pushed)
        POPA
        STWA    rdlLo,s
        BR      rdlNext

rdlFinish:
        LDWA    rdlNeg,s
        BREQ    rdlDone
        ; value = 0 - value
        LDWA    0,i
        PUSHA                   ; a low
        PUSHA                   ; a high
        LDWA    10,s            ; rdlLo + 4
        PUSHA
        LDWA    10,s            ; rdlHi + 6
        PUSHA
        CALL    DSUB,i
        ADDSP   4,i
        POPA
        STWA    6,s             ; rdlHi + 2
        POPA
        STWA    rdlLo,s
rdlDone:
        LDWA    rdlHi,s
        LDWX    rdlLo,s
        ADDSP   rdlFrame,i
        RET
