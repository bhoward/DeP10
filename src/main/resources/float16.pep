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

_f_SIGN_MASK: .EQUATE 0x7FFF
_f_NEGATIVE:  .EQUATE 0x8000
_f_EXPONENT:  .EQUATE 0x7C00
_f_MANTISSA:  .EQUATE 0x03FF
_f_HIDDEN:    .EQUATE 0x0400
_f_INFINITY:  .EQUATE 0x7C00
_f_NAN:       .EQUATE 0x7FFF

; FAdd: Floating-point add
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

_fa_M:     .EQUATE 4
_fa_N:     .EQUATE 2
_fa_SUM:   .EQUATE 4
_fa_SAVEX: .EQUATE -2
_fa_TEMP:  .EQUATE -4
_fa_M_MAN: .EQUATE -4
_fa_M_EXP: .EQUATE -6

FAdd:  STWX _fa_SAVEX,s
       LDWA _fa_N,s
       ANDA _f_SIGN_MASK,i
       STWA _fa_TEMP,s
       LDWA _fa_M,s
       ANDA _f_SIGN_MASK,i
       CPWA _fa_TEMP,s
       BRGE _fa2
       ; N is larger in absolute value, so swap
       LDWA _fa_N,s
       LDWX _fa_M,s
       STWA _fa_M,s
       STWX _fa_N,s
_fa2:  ; now |N| <= |M|
       LDWX _fa_M,s
       ANDX _f_MANTISSA,i ; M mantissa in X
       LDWA _fa_M,s
       ANDA _f_EXPONENT,i
       STWA _fa_M_EXP,s ; M exponent*0400
       BREQ _fa3 ; check for subnormal M
       ORX  _f_HIDDEN,i ; add hidden bit if normal
_fa3:  STWX _fa_M_MAN,s ; M mantissa
       CPWA _f_INFINITY,i
       BREQ _fa4 ; check for infinity/NaN M
       LDWX _fa_N,s
       ANDX _f_MANTISSA,i ; N mantissa in X
       LDWA _fa_N,s
       ANDA _f_EXPONENT,i ; N exponent*0400 in A
       BREQ _fa5 ; check for subnormal N
       ORX  _f_HIDDEN,i ; add hidden bit if normal
_fa5:  CPWA _f_INFINITY,i
       BREQ _fa6 ; check for infinity/NaN N
       ; shift N until exponents match
       NEGA
       ADDA _fa_M_EXP,s
_fa7:  BREQ _fa8
       ASRX
       SUBA 0x0400,i
       BR   _fa7
_fa8:  ; now have M mantissa in -4,s and N mantissa in X
       ; both relative to exponent*0400 in -6,s
       LDWA _fa_N,s
       XORA _fa_M,s
       BRGE _fa1 ; signs differ, so subtract N from M
       NEGX
_fa1:  ADDX _fa_M_MAN,s
       LDWA _fa_M_EXP,s
       ; normalize result as needed (A = exponent*0400, X = mantissa)
       CPWX 0x0800,i
       BRLT _fa13
       ASRX ; mantissa carried into next place
       ADDA 0x0400,i
_fa13: CPWA 0,i ; check for subnormal sum; do not normalize
       BREQ _fa14
       CPWA _f_INFINITY,i ; check for infinite sum
       BRLT _fa9
       LDWX _f_HIDDEN,i ; sum is infinite, so clear mantissa
_fa9:  CPWX _f_HIDDEN,i
       BRGE _fa10 ; normalized
       ASLX
       SUBA 0x0400,i
       BRNE _fa9
_fa10: ANDX _f_MANTISSA,i ; remove hidden bit, if any
_fa14: STWX _fa_M_MAN,s
       ADDA _fa_M_MAN,s
       LDWX _fa_M,s
       BRGE _fa11
       ADDA _f_NEGATIVE,i ; negative if M negative
_fa11: STWA _fa_SUM,s
       LDWX _fa_SAVEX,s
       RET
_fa4:  ; M infinite or NaN
       ; if N finite, return M
       ; if both are infinity with same sign, return M
       ; else return NaN
       LDWA _fa_M,s
       LDWX _fa_N,s
       ANDX _f_EXPONENT,i
       CPWX _f_INFINITY,i
       BRNE _fa11 ; N is finite
       CPWA _fa_N,s
       BRNE _fa12 ; M and N differ
       ANDA _f_SIGN_MASK,i
       CPWA _f_INFINITY,i
       BRNE _fa12 ; M is NaN
       LDWA _fa_M,s
       BR   _fa11 ; M and N are same infinity
_fa12: LDWA _f_NAN,i ; NaN
       LDWX _fa_M,s
       BRGE _fa11
       ADDA _f_NEGATIVE,i ; -NaN if M negative
       BR   _fa11
_fa6:  ; M finite and N infinite or NaN
       ; return N
       LDWA _fa_N,s
       BR   _fa11

; FMul: Floating-point multiply
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
FMul:  STWX -2,s
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
_fm12: SMULA -4,s
       SWAPHA ; product of mantissas in A, between 2^10 and 2^12
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

; FDiv: Floating-point divide
; Before call
;   SP+0: N (divisor)
;   SP+2: M (dividend)
; During call
;   SP-6: N exponent
;   SP-4: N mantissa
;   SP-2: save X
;   SP+0: return address
;   SP+2: N
;   SP+4: M
; After call
;   SP+2: M/N (quotient)
FDiv:  STWX -2,s
       ; M NaN => return NaN
       LDWA 4,s
       ANDA 0x7FFF,i
       BREQ _fd2 ; M is zero
       CPWA 0x7C00,i
       BRGT _fd1 ; result in A (NaN)
       BRLT _fd5 ; M is finite non-zero
       ; M infinity =>
       ;   N NaN or N infinity => return NaN
       ;   else return infinity
       LDWX 2,s
       ANDX 0x7FFF,i
       CPWX 0x7C00,i
       BRLE _fd4 ;
_fd3:  LDWA 0x7FFF,i
_fd4:  BR   _fd1 ; result in A (NaN or infinity)
       ; M zero =>
       ;   N NaN or N zero => return NaN
       ;   else return zero
_fd2:  LDWX 2,s
       ANDX 0x7FFF,i
       BREQ _fd3 ; zero / zero
       CPWX 0x7C00,i
       BRGT _fd3 ; zero / NaN
       BR   _fd1 ; result in A (zero)
       ; N NaN => return NaN
       ; N infinity => return zero
       ; N zero => return infinity
_fd5:  LDWX 2,s
       ANDX 0x7FFF,i
       BREQ _fd6 ; finite / zero
       CPWX 0x7C00,i
       BRGT _fd3 ; finite / NaN
       BRLT _fd7 ; finite / finite
_fd14: LDWA 0x0000,i
       BR   _fd1 ; result in A (zero)
_fd6:  LDWA 0x7C00,i
       BR   _fd1 ; result in A (infinity)
_fd7:  ; divide two finite numbers
       LDWA 2,s
       ANDA 0x03FF,i ; N mantissa in A
       LDWX 2,s
       ANDX 0x7C00,i ; N exponent*0400 in X
       ASRX          ; N exponent*0200 in X
       BREQ _fd11 ; check for subnormal N
       ORA  0x0400,i ; add hidden bit if normal
_fd11: CPWA 0x0800,i
       BRGE _fd12 ; shift until high bit in 2^11 place
       SUBX 0x0200,i
       ASLA
       BR   _fd11
_fd12: STWA -4,s ; N mantissa in low 12 bits
       STWX -6,s ; N exponent: -11 to 29 (times 0x0200), offset 14
       LDWA 4,s
       ANDA 0x03FF,i ; M mantissa in A
       LDWX 4,s
       ANDX 0x7C00,i ; M exponent*0400 in X
       ASRX          ; M exponent*0200 in X
       BREQ _fd9 ; check for subnormal M
       ORA  0x0400,i ; add hidden bit if normal
_fd9:  CPWA 0x0400,i
       BRGE _fd10 ; shift until high bit in 2^10 place
       SUBX 0x0200,i
       ASLA
       BR   _fd9
_fd10: ; M exponent in X: -10 to 30 (times 0x0200), offset 15
       ; M mantissa in A (low 11 bits)
       ; Want to divide unsigned 32-bit dividend = M in high word, low 11 bits
       ; by 16-bit divisor = N in low 12 bits
       ; Largest possible quotient will then just fit in (high end of) 16 bits
       SWAPHA
       LDWA 0,i
       UDMA -4,s ; A is 1.qqq... or 0.1qq...
       SUBX -6,s ; difference of exponents in X, times 0x0200 (range -39 to 41, offset 1)
       CPWA 0,i
       BRGE _fd16
       ASRA
       ANDA 0x7FFF,i ; if high bit was 1, need special treatment for first ASRA
       ADDX 0x0200,i
_fd16: ASRA
       ASRA
       ASRA
       ASRA ; now bit 11 is 1 and higher bits are 0
       ADDX 0x1A00,i ; restore bias 15 to exponent; max is 55 times 0x0200
       BRGT _fd15 ; number is normal or infinite
       BRLT _fd13 ; exponent is negative
       ASRA
       BR _fd15 ; exponent is 0
_fd13: ASRA ; need to increase exponent to 0 for subnormal/zero case
       ADDX 0x0200,i
       BRLT _fd13
_fd15: CPWX 0x3E00,i
       BRGE _fd6 ; infinite result
       ANDA 0x03FF,i ; remove hidden bit, if still present
       ASLX
       STWX -6,s
       ADDA -6,s
_fd1:  LDWX 2,s
       XORX 4,s
       BRGE _fd8
       ORA  0x8000,i
_fd8:  STWA 4,s
       LDWX -2,s
       RET

; FPrint: Print float to console
; Before call
;   SP+0: N (argument)
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
FPrint: STWX -2,s
       LDWA 2,s
       BRGE _fp1
       LDBX '-',i
       STBX charOut,d
_fp1:  ANDA 0x7FFF,i
       CPWA 0x7C00,i
       BRLT _fp3
       BREQ _fp2
       ; @STRO _NaN,d
       LDWX _NaN,i
_fp11: LDBA 0,x
       BREQ _fp0
       STBA charOut,d
       ADDX 1,i
       BR   _fp11
_fp2:  ; @STRO _Inf,d
       LDWX _Inf,i
       BR   _fp11
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
_fp7:  SWAPHA
       LDWA 0,i ; clear upper 16 bits of dividend (need CLRH? SEHr to sign-extend r into H?)
       SWAPHA
       UDMA 10,i
       SWAPHA
       ORA  '0',i
       LDWX -15,s
       SUBX 1,i
       STWX -15,s
       STBA -15,sf
       SWAPHA
       STWA -7,s
       CPWA 0,i
       BRNE _fp7
       ; @STRO -15,sf
       LDWX 0,i
_fp12: LDBA -15,sfx
       BREQ _fp9
       STBA charOut,d
       ADDX 1,i
       BR   _fp12
_fp8:  @CHARO '0',i
_fp9:  LDWA -4,s ; prepare to print the decimal part
       ORA  -6,s
       BREQ _fp0
       @CHARO '.',i
_fp10: LDWA -4,s
       UMULA 10,i
       STWA -4,s
       SWAPHX    ; save high word in X
       LDWA -6,s
       UMULA 10,i
       STWA -6,s
       ADDX -6,s ; add saved high word from multiplying -4,s by 10
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

; FToI: Convert float to int
; Before call
;   SP+0: N (input float)
; During call
;   SP-8: zero (1 byte)
;   SP-7: N integer part
;   SP-5: N fractional part (3 bytes)
;   SP-2: save X
;   SP+0: return address
;   SP+2: N
; After call
;   SP+0: N (output int)
; Arbitrary decisions:
; * NaN converts to 0
; * Infinity and -Infinity convert to MININT = -2^15
; * Finite values outside of the signed range wrap around
; * Truncates toward 0 (no attempt at rounding)
FToI:  STWX -2,s
       LDWA 2,s
       ANDA 0x7FFF,i
       CPWA 0x7C00,i
       BRLT _ft3
       BREQ _ft2
       ; NaN -> 0
       LDWA 0,i
       BR   _ft7
       ; Infinity -> 2^15 (= -2^15 as signed int)
_ft2:  LDWA 0x8000,i
       BR   _ft7
_ft3:  ANDA 0x3FF,i
       LDWX 2,s
       ANDX 0x7C00,i
       BREQ _ft4
       ORA  0x0400,i ; restore hidden bit
_ft4:  STWA -4,s
       LDWA 0,i
       STWA -6,s
       STWA -8,s
_ft5:  SUBX 0x0400,i
       BRLE _ft6
       LDWA -4,s
       ASLA
       STWA -4,s
       LDWA -6,s
       ROLA
       STWA -6,s
       LDWA -8,s
       ROLA
       STWA -8,s
       BR   _ft5
_ft6:  LDWA -7,s
_ft7:  LDWX 2,s
       BRGE _ft1
       NEGA
_ft1:  STWA 2,s
       LDWX -2,s
       RET
   
; IToF: Convert int to float
; Before call
;   SP+0: N (input int)
; During call
;   SP-4: temp
;   SP-2: save X
;   SP+0: return address
;   SP+2: N
; After call
;   SP+0: N (output float)
; Arbitrary decisions:
; * If N > 2048, returns closest float <= N
IToF:  STWX -2,s
       LDWA 2,s
       BREQ _it1 ; zero
       BRGT _it5
       NEGA
       BRGT _it5
       ; handle -2^15 here as a special case
       LDWA 0xF800,i
       BR   _it1
_it5:  LDWX 0x6400,i ; exponent for 1024
_it2:  CPWA 0x0400,i
       BRGE _it3
       ASLA
       SUBX 0x0400,i
       BR   _it2
_it3:  CPWA 0x0800,i
       BRLT _it4
       ASRA
       ADDX 0x0400,i
       BR   _it3
_it4:  STWX -4,s
       ANDA 0x03FF,i ; clear hidden bit
       ORA -4,s
       LDWX 2,s
       BRGE _it1
       ORA  0x8000,i
_it1:  STWA 2,s
       LDWX -2,s
       RET
       
; FEq: Compare floats for equality
; Before call
;   SP+0: N
;   SP+2: M
; During call
;   SP+0: return address
;   SP+2: N
;   SP+4: M
; After call
;   SP+2: 0 if false, 1 if true
FEq:   LDWA 2,s
       CPWA 0x8000,i ; check for N = negative zero
       BRNE _fe1
       LDWA 0,i
       STWA 2,s
       BR _fe2
_fe1:  ANDA 0x7FFF,i
       CPWA 0x7C00,i ; check for N = NaN
       BRLE _fe2
_fe3:  LDWA 0,i
       BR _fe6 ; return false
_fe2:  LDWA 4,s
       CPWA 0x8000,i ; check for M = negative zero
       BRNE _fe4
       LDWA 0,i
       STWA 4,s
       BR _fe5
_fe4:  ANDA 0x7FFF,i
       CPWA 0x7C00,i ; check for M = NaN
       BRLE _fe5
       BR _fe3 ; return false
_fe5:  ; Ordinary comparison
       LDWA 4,s
       CPWA 2,s
       BRNE _fe3
       LDWA 1,i
_fe6:  STWA 4,s
       RET
       
; FLt: Compare floats for M less than N
; Before call
;   SP+0: N
;   SP+2: M
; During call
;   SP+0: return address
;   SP+2: N
;   SP+4: M
; After call
;   SP+2: 0 if false, 1 if true
FLt:   LDWA 2,s
       CPWA 0x8000,i ; check for N = negative zero
       BRNE _fl1
       LDWA 0,i
       STWA 2,s
       BR _fe2
_fl1:  ANDA 0x7FFF,i
       CPWA 0x7C00,i ; check for N = NaN
       BRLE _fl2
_fl3:  LDWA 0,i
       BR _fl6 ; return false
_fl2:  LDWA 4,s
       CPWA 0x8000,i ; check for M = negative zero
       BRNE _fl4
       LDWA 0,i
       STWA 4,s
       BR _fl5
_fl4:  ANDA 0x7FFF,i
       CPWA 0x7C00,i ; check for M = NaN
       BRLE _fl5
       BR _fl3 ; return false
_fl5:  ; Ordinary comparison; first check signs
       LDWA 2,s
       BRGE _fl7 ; N is non-negative
       ; N is negative here
       LDWA 4,s
       BRGE _fl3 ; M is non-negative and N is negative; return false
       ; M and N are both negative here
       CPWA 2,s
       BRLE _fl3 ; if M <= N _as ints_, then M >= N as floats; return false
       BR _fl8 ; return true
_fl7:  ; N is non-negative here
       LDWA 4,s
       BRLT _fl8 ; M is negative and N is non-negative; return true
       ; M and N are both non-negative here
       CPWA 2,s
       BRGE _fl3 ; if M >= N, then return false
_fl8:  LDWA 1,i ; return true
_fl6:  STWA 4,s
       RET
       
; FRead: Read float from console
; Before call
;   SP+0: empty
; During call
;   SP-8: temporary space for calling other functions
;   SP-6: fractional place value
;   SP-4: sign
;   SP-2: save X
;   SP+0: return address
;   SP+2: partial result
; After call
;   SP+0: return value
FRead: STWX -2,s
       LDWA 0,i
       STWA 2,s
       STWA -4,s
       LDBA charIn,d
       CPBA '-',i
       BRNE _fr1
       LDWA 0x8000,i
       STWA -4,s
_fr2:  LDBA charIn,d
_fr1:  CPBA '.',i
       BREQ _fr3
       CPBA '0',i
       BRLT _fr4
       CPBA '9',i
       BRGT _fr4
       SUBA '0',i
       STWA -8,s
       SUBSP 8,i
       CALL IToF,i
       LDWA 10,s
       STWA -2,s
       LDWA 0x4900,i ; = 10.0 decimal
       STWA -4,s
       SUBSP 4,i
       CALL FMul,i
       ADDSP 2,i
       CALL FAdd,i
       ADDSP 10,i
       LDWA -8,s
       STWA 2,s
       BR _fr2
_fr3:  LDWA 0x3C00,i ; = 1.0 decimal
       STWA -6,s
_fr5:  LDBX charIn,d
       CPBX '0',i
       BRLT _fr4
       CPBX '9',i
       BRGT _fr4
       SUBX '0',i
       LDWA 0x4900,i ; = 10.0 decimal
       STWA -8,s
       SUBSP 8,i
       CALL FDiv,i
       STWX 0,s
       CALL IToF,i
       LDWA 2,s
       STWA -2,s
       SUBSP 2,i
       CALL FMul,i
       LDWA 12,s
       STWA 0,s
       CALL FAdd,i
       ADDSP 10,i
       LDWA -8,s
       STWA 2,s
       BR _fr5
_fr4:  LDWA 2,s
       ORA -4,s
       STWA 2,s
       LDWX -2,s
       RET
       
; TODO what about converting LONGINT to REAL?