main:  LDWA 0x4400,i ; 1.0 * 2^2 = 4
       STWA -2,s
       LDWA 0xC200,i ; -1.1 * 2^1 = -3
       STWA -4,s
       SUBSP 4,i
       CALL _fmul
       ADDSP 4,i
       SUBSP 2,i
       CALL _fprint
       ADDSP 2,i
       @CHARO '\n',i
       @HEXO -2,s    ; should print CA00 = -1.1 * 2^3 = -12
       @CHARO '\n',i
       
       LDWA 0x4400,i ; 1.0 * 2^2
       STWA -2,s
       LDWA 0xC200,i ; -1.1 * 2^1
       STWA -4,s
       SUBSP 4,i
       CALL _fadd
       ADDSP 4,i
       SUBSP 2,i
       CALL _fprint
       ADDSP 2,i
       @CHARO '\n',i
       @HEXO -2,s    ; should print 3C00 = 1.0 * 2^0 = 1
       @CHARO '\n',i
       
       LDWA 0x4400,i ; 1.0 * 2^2
       STWA -2,s
       LDWA 0x4200,i ; 1.1 * 2^1
       STWA -4,s
       SUBSP 4,i
       CALL _fadd
       ADDSP 4,i
       SUBSP 2,i
       CALL _fprint
       ADDSP 2,i
       @CHARO '\n',i
       @HEXO -2,s    ; should print 4700 = 1.11 * 2^2 = 7
       @CHARO '\n',i
       
       LDWA 0x7C00,i ; +Infinity
       STWA -2,s
       LDWA 0x7C00,i ; +Infinity
       STWA -4,s
       SUBSP 4,i
       CALL _fadd
       ADDSP 4,i
       SUBSP 2,i
       CALL _fprint
       ADDSP 2,i
       @CHARO '\n',i
       @HEXO -2,s    ; should print 7C00 = +Infinity
       @CHARO '\n',i
       
       LDWA 0x7C00,i ; +Infinity
       STWA -2,s
       LDWA 0xFC00,i ; -Infinity
       STWA -4,s
       SUBSP 4,i
       CALL _fadd
       ADDSP 4,i
       SUBSP 2,i
       CALL _fprint
       ADDSP 2,i
       @CHARO '\n',i
       @HEXO -2,s    ; should print 7FFF = NaN
       @CHARO '\n',i
       
       LDWA 0xBA00,i ; -1.1*2^-1 = -0.75
       STWA -2,s
       SUBSP 2,i
       CALL _fprint
       ADDSP 2,i
       @CHARO '\n',i

       LDWA 0x3E00,i ; 1.1*2^0 = 1.5
       STWA -2,s
       SUBSP 2,i
       CALL _fprint
       ADDSP 2,i
       @CHARO '\n',i

       RET

; 2-byte float (FP16)
;   sign: 1 bit (0 = +, 1 = -)
;   exponent: 5 bits, bias 15
;   mantissa: 10 bits, implicit leading 1 when normalized
;
; s 00000 mmmmmmmmmm -- denormalized 0.M * 2 ^ -14
; s 00001 mmmmmmmmmm -- 1.M * 2 ^ -14
; s 01111 0000000000 -- 1.0 * 2 ^ 0
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
       ORX  0x0400,i ; add hidden bit if normal
_fa3:  STWX -4,s ; M mantissa
       CPWA 0x7C00,i
       BREQ _fa4 ; check for infinity/NaN M
       LDWX 2,s
       ANDX 0x03FF,i ; N mantissa in X
       LDWA 2,s
       ANDA 0x7C00,i ; N exponent*0400 in A
       BREQ _fa5 ; check for subnormal N
       ORX  0x0400,i ; add hidden bit if normal
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
       CPWA 2,s
       BRNE _fa12 ; M and N differ
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

; Before call
;   SP+0: N (multiplier)
;   SP+2: M (multiplicand)
; During call
;   SP-6: M exponent
;   SP-4: M mantissa
;   SP-2: save X
;   SP+0: return address
;   SP+2: N
;   SP+4: M
; After call
;   SP+2: M*N (product)
_fmul: STWX -2,s
       ; M NaN => return NaN
       LDWA 4,s
       ANDA 0x7FFF,i
       BREQ _fm2 ; M is zero
       CPWA 0x7C00,i
       BRGT _fm1 ; result in A (NaN)
       BRLT _fm5 ; M is finite non-zero
       ; M infinity =>
       ;   N NaN or N zero => return NaN
       ;   else return infinity
       LDWX 2,s
       ANDX 0x7FFF,i
       BREQ _fm3 ; infinity * zero
       CPWX 0x7C00,i
       BRLE _fm4 ;
_fm3:  LDWA 0x7FFF,i
_fm4:  BR   _fm1 ; result in A (NaN or infinity)
       ; M zero =>
       ;   N NaN or N infinity => return NaN
       ;   else return zero
_fm2:  LDWX 2,s
       ANDX 0x7FFF,i
       CPWX 0x7C00,i
       BRGE _fm3 ; zero * infinity or NaN
       BR   _fm1 ; result in A (zero)
       ; N NaN => return NaN
       ; N infinity => return infinity
       ; N zero => return zero
_fm5:  LDWX 2,s
       ANDX 0x7FFF,i
       BREQ _fm6 ; finite * zero
       CPWX 0x7C00,i
       BRGT _fm3 ; finite * NaN
       BRLT _fm7 ; finite * finite
_fm14: LDWA 0x7C00,i
       BR   _fm1 ; result in A (infinity)
_fm6:  LDWA 0x0000,i
       BR   _fm1 ; result in A (zero)
_fm7:  ; multiply two finite numbers
       LDWX 4,s
       ANDX 0x03FF,i ; M mantissa in X
       LDWA 4,s
       ANDA 0x7C00,i ; M exponent*0400 in A
       ASRA          ; M exponent*0200 in A
       BREQ _fm9 ; check for subnormal M
       ORX  0x0400,i ; add hidden bit if normal
_fm9:  CPWX 0x2000,i ; shift until high bit in 2^13 place
       BRGE _fm10
       ASLX
       SUBA 0x0200,i
       BR   _fm9
_fm10: STWA -6,s ; M exponent: -13 to 27 (times 0x0200), offset 12
       STWX -4,s
       LDWA 2,s
       ANDA 0x03FF,i ; N mantissa in A
       LDWX 2,s
       ANDX 0x7C00,i ; N exponent*0400 in X
       ASRX          ; N exponent*0200 in X
       BREQ _fm11 ; check for subnormal N
       ORA  0x0400,i ; add hidden bit if normal
_fm11: CPWA 0x2000,i ; shift until high bit in 2^13 place
       BRGE _fm12
       ASLA
       SUBX 0x0200,i
       BR   _fm11
_fm12: MULHA -4,s ; product of mantissas in A, between 2^10 and 2^12
       ADDX -6,s ; sum of exponents in X, times 0x0200 (range -26 to 54, offset 24)
       SUBX 0x1200,i ; now exponent is range -35 to 45, offset 15
       CPWA 0x0800,i
       BRLT _fm13
_fm15: ASRA
       ADDX 0x0200,i
_fm13: CPWX 0,i
       BRLT _fm15
       ; now exponent is range 0 to 45, offset 15
       CPWX 0x3E00,i
       BRGE _fm14 ; infinite result
       ANDA 0x03FF,i
       ASLX
       STWX -6,s
       ADDA -6,s
_fm1:  LDWX 2,s
       XORX 4,s
       BRGE _fm8
       ORA  0x8000,i
_fm8:  STWA 4,s
       LDWX -2,s
       RET

; Before call
;   SP+0: N (multiplier)
; During call
;   SP-15: pointer into string buffer
;   SP-13: string buffer (5 bytes)
;   SP-8: zero (1 byte)
;   SP-7: N integer part
;   SP-5: N fractional part (3 bytes)
;   SP-2: save X
;   SP+0: return address
;   SP+2: N
; After call
;   SP+0: N (unchanged)
_NaN:  .ASCII "NaN\0"
_Inf:  .ASCII "Infinity\0"
_fprint: STWX -2,s
       LDWA 2,s
       BRGE _fp1
       LDBX '-',i
       STBX charOut,d
_fp1:  ANDA 0x7FFF,i
       CPWA 0x7C00,i
       BRLT _fp3
       BREQ _fp2
       @STRO _NaN,d
       BR   _fp0
_fp2:  @STRO _Inf,d
       BR   _fp0
_fp3:  ANDA 0x3FF,i
       LDWX 2,s
       ANDX 0x7C00,i
       BREQ _fp4
       ORA  0x0400,i ; restore hidden bit
_fp4:  STWA -4,s
       LDWA 0,i
       STWA -6,s
       STWA -8,s
_fp5:  SUBX 0x0400,i
       BRLE _fp6
       LDWA -4,s
       ASLA
       STWA -4,s
       LDWA -6,s
       ROLA
       STWA -6,s
       LDWA -8,s
       ROLA
       STWA -8,s
       BR   _fp5
_fp6:  MOVSPA ; prepare to print the integer part
       SUBA 8,i ; NUL at end of string buffer
       STWA -15,s
       LDWA -7,s
       BREQ _fp8
_fp7:  UMODA 10,i
       ORA  '0',i
       LDWX -15,s
       SUBX 1,i
       STWX -15,s
       STBA -15,sf
       LDWA -7,s
       UDIVA 10,i
       STWA -7,s
       BRNE _fp7
       @STRO -15,sf
       BR   _fp9
_fp8:  @CHARO '0',i
_fp9:  LDWA -4,s ; prepare to print the decimal part
       ORA  -6,s
       BREQ _fp0
       @CHARO '.',i
_fp10: LDWA -4,s
       MULA 10,i
       STWA -4,s
       LDWX -4,s
       UMULHX 10,i
       LDWA -6,s
       MULA 10,i
       STWA -6,s
       ADDX -6,s
       STWX -6,s
       LDBA -6,s
       ORA  '0',i
       STBA charOut,d
       LDBA 0,i
       STBA -6,s
       LDWA -4,s
       ORA -6,s
       BRNE _fp10
_fp0:  LDWX -2,s
       RET