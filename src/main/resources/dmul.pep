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


sAHigh: .EQUATE 2
sALow:  .EQUATE 4
sBHigh: .EQUATE 6
sBLow:  .EQUATE 8

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
