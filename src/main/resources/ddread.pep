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
