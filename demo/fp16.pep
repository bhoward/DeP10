main:  LDWA 0x4400,i ; 1.0 * 2^2
       STWA -2,s
       LDWA 0xC200,i ; -1.1 * 2^1
       STWA -4,s
       SUBSP 4,i
       CALL _fadd
       ADDSP 4,i
       @HEXO -2,s    ; should print 3C00 = 1.0 * 2^0
       @CHARO '\n',i
       LDWA 0x4400,i ; 1.0 * 2^2
       STWA -2,s
       LDWA 0x4200,i ; 1.1 * 2^1
       STWA -4,s
       SUBSP 4,i
       CALL _fadd
       ADDSP 4,i
       @HEXO -2,s    ; should print 4700 = 1.11 * 2^2
       @CHARO '\n',i
       RET

; 2-byte float (FP16)
;   sign: 1 (0 = +, 1 = -)
;   exponent: 5, bias 15
;   mantissa: 10, implicit leading 1 when normalized
;
; s 00000 mmmmmmmmmm -- denormalized 0.M * 2 ^ -14
; s 00001 mmmmmmmmmm -- 1.M * 2 ^ -14
; s 11110 mmmmmmmmmm -- 1.M * 2 ^ 15
; s 11111 0000000000 -- INFINITY
; s 11111 mmmmmmmmmm -- NaN (M <> 0)

; need FADD, FMUL, FDIV, convert to/from int and string
; (FNEG is just XOR with 0x8000; FSUB is just FNEG/FADD)

; Before call
;   SP+0: N (addend)
;   SP+2: M (augend)
; During call
;   SP-6: M exponent
;   SP-4: temp/M mantissa
;   SP-2: save X
;   SP+0: return address
;   SP+2: N
;   SP+4: M
; After call
;   SP+2: M+N (sum)
_fadd: STWX -2,s
       LDWA 2,s
       ANDA 0x7FFF,i
       STWA -4,s
       LDWA 4,s
       ANDA 0x7FFF,i
       CPWA -4,s
       BRGE _fa2
       ; N is larger in absolute value, so swap
       LDWA 2,s
       LDWX 4,s
       STWA 4,s
       STWX 2,s
_fa2:  ; now |N| <= |M|
       LDWX 4,s
       ANDX 0x03FF,i ; M mantissa in X
       LDWA 4,s
       ANDA 0x7C00,i
       STWA -6,s ; M exponent*0400
       BREQ _fa3 ; check for subnormal M
       ORX  0x0400 ; add hidden bit if normal
_fa3:  STWX -4,s ; M mantissa
       CPWA 0x7C00,i
       BREQ _fa4 ; check for infinity/NaN M
       LDWX 2,s
       ANDX 0x03FF,i ; N mantissa in X
       LDWA 2,s
       ANDA 0x7C00,i ; N exponent*0400 in A
       BREQ _fa5 ; check for subnormal N
       ORX  0x0400 ; add hidden bit if normal
_fa5:  CPWA 0x7C00,i
       BREQ _fa6 ; check for infinity/NaN N
       ; shift N until exponents match
       NEGA
       ADDA -6,s
_fa7:  BREQ _fa8
       ASRX
       SUBA 0x0400,i
       BR   _fa7
_fa8:  ; now have M mantissa in -4,s and N mantissa in X
       ; both relative to exponent*0400 in -6,s
       LDWA 2,s
       XORA 4,s
       BRGE _fa1 ; signs differ, so subtract N from M
       NEGX
_fa1:  ADDX -4,s
       LDWA -6,s
       ; normalize result as needed (A = exponent*0400, X = mantissa)
       CPWX 0x0800,i
       BRLT _fa9
       ASRX ; mantissa carried into next place
       ADDA 0x0400,i
_fa9:  CPWX 0x0400,i
       BRGE _fa10 ; normalized
       ASLX
       SUBA 0x0400,i
       BRNE _fa9
_fa10: ANDX 0x03FF,i ; remove hidden bit, if any
       STWX -4,s
       ADDA -4,s
       LDWX 4,s
       BRGE _fa11
       ADDA 0x8000,i ; negative if M negative
_fa11: STWA 4,s
       LDWX -2,s
       RET
_fa4:  ; M infinite or NaN
       ; if N finite, return M
       ; if both are infinity with same sign, return M
       ; else return NaN
       LDWA 4,s
       LDWX 2,s
       ANDX 0x7C00,i
       CPWX 0x7C00,i
       BRNE _fa11 ; N is finite
       XORA 2,s
       BRNE _fa12 ; M and N differ
       LDWA 4,s
       ANDA 0x7FFF,i
       CPWA 0x7C00,i
       BRNE _fa12 ; M is NaN
       LDWA 4,s
       BR   _fa11 ; M and N are same infinity
_fa12: LDWA 0x7FFF,i ; NaN
       LDWX 4,s
       BRGE _fa11
       ADDA 0x8000,i ; -NaN if M negative
       BR   _fa11
_fa6:  ; M finite and N infinite or NaN
       ; return N
       LDWA 2,s
       BR   _fa11
