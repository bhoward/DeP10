;=======================================================================
; DDIV32 -- 32-bit / 32-bit division for DeP10.
; See "Design Proposal: 32-Bit Division (DDIV)", section 5.
;
; Provides:
;   .DEFMACRO UDIV32, 16   unsigned 32/32 divide
;   .DEFMACRO SDIV32, 16   signed 32/32 divide
;
; Both take the same parameter shape -- 8 input operand/mode pairs
; followed by 8 output operand/mode pairs:
;   $1,$2    dividend high word    (input)
;   $3,$4    dividend low word     (input)
;   $5,$6    divisor high word     (input)
;   $7,$8    divisor low word      (input)
;   $9,$10   quotient high word    (output)
;   $11,$12  quotient low word     (output)
;   $13,$14  remainder high word   (output)
;   $15,$16  remainder low word    (output)
;
; Precondition: divisor is nonzero (not checked here -- matching the
; design proposal's separation of the 32/32 macro from divide-by-zero
; handling, which belongs to whatever wraps this for a given opcode).
;
; The macros are thin wrappers (no internal labels), so they can be
; invoked any number of times in one program without label collisions.
; The actual algorithm lives once, in the _UDiv32/_SDiv32/_UCmp16
; subroutines below, which should be assembled into the program exactly
; once (e.g. via an .INCLUDE, once stdmacro.pep or an equivalent has
; one, or simply pasted once at the end of a program for now).
;=======================================================================

.DEFMACRO UDIV32, 16
        LDWA    $1,$2
        STWA    udivDvdHi,d
        LDWA    $3,$4
        STWA    udivDvdLo,d
        LDWA    $5,$6
        STWA    udivDvrHi,d
        LDWA    $7,$8
        STWA    udivDvrLo,d
        CALL    _UDiv32
        LDWA    udivQuoHi,d
        STWA    $9,$10
        LDWA    udivQuoLo,d
        STWA    $11,$12
        LDWA    udivRemHi,d
        STWA    $13,$14
        LDWA    udivRemLo,d
        STWA    $15,$16
.ENDMACRO

.DEFMACRO SDIV32, 16
        LDWA    $1,$2
        STWA    udivDvdHi,d
        LDWA    $3,$4
        STWA    udivDvdLo,d
        LDWA    $5,$6
        STWA    udivDvrHi,d
        LDWA    $7,$8
        STWA    udivDvrLo,d
        CALL    _SDiv32
        LDWA    udivQuoHi,d
        STWA    $9,$10
        LDWA    udivQuoLo,d
        STWA    $11,$12
        LDWA    udivRemHi,d
        STWA    $13,$14
        LDWA    udivRemLo,d
        STWA    $15,$16
.ENDMACRO

;=======================================================================
; _UCmp16: compare two unsigned 16-bit values.
; Precondition: udivCmpX, udivCmpY hold the two values.
; Postcondition: A = 1 if X>Y, 0 if X==Y, 0xFFFF (-1) if X<Y.
;
; Does not use CPWA's Carry flag: testing found it is wrong whenever the
; value compared against is exactly 0 (github.com/bhoward/DeP10/issues/11),
; which is common here since a divisor's high word is very often 0. Y==0
; is special-cased directly instead. The general case uses the XOR-0x8000
; unsigned-via-signed trick on N (via CPWA's overflow-corrected N, not C),
; which testing did not find any zero-operand problem with -- as long as
; Y (the value XOR'd and then negated internally by CPWA) is never
; exactly 0, which the Y==0 special case guarantees.
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

        CALL    _UDiv32

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
