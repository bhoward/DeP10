.DEFMACRO ASLA2, 0
ASLA
ASLA
.ENDMACRO

.DEFMACRO ASLA3, 0
ASLA
ASLA
ASLA
.ENDMACRO

.DEFMACRO ASLA4, 0
ASLA
ASLA
ASLA
ASLA
.ENDMACRO

.DEFMACRO ASLA5, 0
ASLA
ASLA
ASLA
ASLA
ASLA
.ENDMACRO

.DEFMACRO ASLX2, 0
ASLX
ASLX
.ENDMACRO

.DEFMACRO ASLX3, 0
ASLX
ASLX
ASLX
.ENDMACRO

.DEFMACRO ASLX4, 0
ASLX
ASLX
ASLX
ASLX
.ENDMACRO

.DEFMACRO ASLX5, 0
ASLX
ASLX
ASLX
ASLX
ASLX
.ENDMACRO

.DEFMACRO ASRA2, 0
ASRA
ASRA
.ENDMACRO

.DEFMACRO ASRA3, 0
ASRA
ASRA
ASRA
.ENDMACRO

.DEFMACRO ASRA4, 0
ASRA
ASRA
ASRA
ASRA
.ENDMACRO

.DEFMACRO ASRA5, 0
ASRA
ASRA
ASRA
ASRA
ASRA
.ENDMACRO

.DEFMACRO ASRX2, 0
ASRX
ASRX
.ENDMACRO

.DEFMACRO ASRX3, 0
ASRX
ASRX
ASRX
.ENDMACRO

.DEFMACRO ASRX4, 0
ASRX
ASRX
ASRX
ASRX
.ENDMACRO

.DEFMACRO ASRX5, 0
ASRX
ASRX
ASRX
ASRX
ASRX
.ENDMACRO

.DEFMACRO ROLA2, 0
ROLA
ROLA
.ENDMACRO

.DEFMACRO ROLA3, 0
ROLA
ROLA
ROLA
.ENDMACRO

.DEFMACRO ROLA4, 0
ROLA
ROLA
ROLA
ROLA
.ENDMACRO

.DEFMACRO ROLA5, 0
ROLA
ROLA
ROLA
ROLA
ROLA
.ENDMACRO

.DEFMACRO ROLX2, 0
ROLX
ROLX
.ENDMACRO

.DEFMACRO ROLX3, 0
ROLX
ROLX
ROLX
.ENDMACRO

.DEFMACRO ROLX4, 0
ROLX
ROLX
ROLX
ROLX
.ENDMACRO

.DEFMACRO ROLX5, 0
ROLX
ROLX
ROLX
ROLX
ROLX
.ENDMACRO

.DEFMACRO RORA2, 0
RORA
RORA
.ENDMACRO

.DEFMACRO RORA3, 0
RORA
RORA
RORA
.ENDMACRO

.DEFMACRO RORA4, 0
RORA
RORA
RORA
RORA
.ENDMACRO

.DEFMACRO RORA5, 0
RORA
RORA
RORA
RORA
RORA
.ENDMACRO

.DEFMACRO RORX2, 0
RORX
RORX
.ENDMACRO

.DEFMACRO RORX3, 0
RORX
RORX
RORX
.ENDMACRO

.DEFMACRO RORX4, 0
RORX
RORX
RORX
RORX
.ENDMACRO

.DEFMACRO RORX5, 0
RORX
RORX
RORX
RORX
RORX
.ENDMACRO

.DEFMACRO CHARI, 2
LDBA charIn, d
STBA $1, $2
.ENDMACRO

.DEFMACRO CHARO, 2
LDBA $1, $2
STBA charOut, d
.ENDMACRO

.DEFMACRO LIBC, 0
;******* printf()
;        Precondition: firstVal contains the first variadic argument
;        Postcondition: str contains a pointer to a null-terminated string.
;        Postcondition: X contains pointer to bytes
firstVal: .EQUATE  12        ;
str:      .EQUATE  10        ;Pointer to string with placeholders
tmpPtr:   .EQUATE  6         ;To provide pointer to @STRO
plceIdx:  .EQUATE  4         ;Placeholder variadic array next idx
strIdx:   .EQUATE  2         ;Current index in str
status:   .EQUATE  1         ;Have we seen % or \?
lstChar:  .EQUATE  0         ;Last loaded char from str
;Constants
lstPlce:  .EQUATE  1         ;Last char was %
printf:   SUBSP    8,i
          LDWA     0,i
          LDWX     0,i
          STWX     lstChar,s
          STWX     strIdx,s
          STWX     plceIdx,s
lpPrintf: LDBA     str, sfx
          BREQ     ePrintf
          ADDX     1,i
          STWX     strIdx, s
         ;
          LDBX     status, s
          ANDX     lstPlce, i
          BRNE     detPlce
          CPWA     '%', i
          BREQ     strtPlce
printCh:  STBA     charOut,d
          ;printf reset status
printfRs: LDBA     0,i
          STBA     status,s
printfNx: LDWX     strIdx, s
          BR       lpPrintf
;Determine what to do with placeholder
strtPlce: LDBX     lstPlce, i
          STBX     status, s
          BR       printfNx
detPlce:  LDWX     plceIdx,s
          CPWA     '%',i
          BREQ     printCh
          ANDA     '_',i   ;Convert to uppercase
          CPWA     'H',i
          BREQ     printfH
          CPWA     'D',i
          BREQ     printfD
          CPWA     'S',i
          BREQ     printfS
          CPWA     'C',i
          BREQ     printfC
          ; Illegal placeholder, print message and shutdown
          STBA     lstChar,s ;Must cache last character, overriden by prntMsg
          @STRO    mBadPlce, d
          LDBA     lstChar,s
          STBA     charOut,d
          LDBA     0xDE, i
          STBA     pwrOff,d
hang:     BR       hang
ePrintf:  ADDSP    8,i
          RET
;
printfC:  LDBA     firstVal, sx
          STBA     charOut, d
          ADDX     1,i
          BR       stPlcIdx
printfH:  @HEXO    firstVal, sx
          BR       plceFix
printfD:  @DECO    firstVal, sx
          BR       plceFix
          ;Must do addressing manually.
          ;Compute the offset to the char* on the stack (a char**)
printfS:  MOVSPA
          ADDA     firstVal,i
          ADDA     plceIdx,s
          ;Derefernece the char** to char*
          STWA     tmpPtr, s
          LDWA     tmpPtr, sf
          STWA     tmpPtr, s
          ;Print the contents of the char*
          @STRO    tmpPtr, sf
plceFix:  ADDX     2,i
stPlcIdx: STWX     plceIdx,s
          BR       printfRs
;
mBadPlce: .ASCII "Illegal placeholder %\0"
.ENDMACRO

.DEFMACRO MALLOC, 0
;******* malloc()
;        Precondition: @MALLOC is the last statment in the program
;        Precondition: A contains number of bytes
;        Postcondition: X contains pointer to bytes
malloc:  LDWX    hpPtr,d     ;returned pointer
         ADDA    hpPtr,d     ;allocate from heap
         STWA    hpPtr,d     ;update hpPtr
         RET                 
hpPtr:   .WORD heap          ;address of next free byte
heap:    .BLOCK  1           ;first byte in the heap
.ENDMACRO

.DEFMACRO SWAPAX, 0
; Swap the values of A and X.
         SUBSP   4, i        ;Allocate temporaries #2h#2h
         STWA    0, s        ;Save A
         STWX    2, s        ;Save X
         LDWX    0, s        ;Move X <- A
         LDWA    2, s        ;Move A <- old X
         ADDSP   4, i        ;Deallocate temporaries #2h#2h
.ENDMACRO

.DEFMACRO XCHGA, 2
;$1: Operand specifier
;$2: Addressing mode
         SUBSP   4,i        ;Allocate temporaries #2h#2h
         STWA    0,s        ;Save A, Mem[SP] <- A
         LDWA    $1,$2      ;A <- Mem[Opr]
         STWA    2,s        ;Mem[SP+2] <- A;
         LDWA    0,s        ;A<-Mem[SP]
         STWA    $1,$2      ;Mem[Opr]<-Mem[SP]
         LDWA    2,s        ;A<-Mem[SP+2]
         ADDSP   4,i        ;Deallocate temporaries #2h#2h
.ENDMACRO

.DEFMACRO XCHGX, 2
; $1: Operand specifier
; $2: Addressing mode
         SUBSP   6, i        ;Allocate temporaries #2h#2h#2h
         STWA    2,s         ;Save A, Mem[SP] <- A
         STWX    0,s         ;Save X, Mem[SP+2] <- X
         LDWA    $1,$2       ;A <- Mem[Opr]
         STWA    4,s         ;Mem[SP+4] <- A;
         LDWA    0,s         ;A<-Mem[SP] (which is X)
         STWA    $1,$2       ;Mem[Opr]<-Mem[SP]
         LDWA    2, s        ;Restore A, A <- Mem[SP+2]
         LDWX    4,s         ;X<-Mem[SP+4]. Perform last so NZVCS are correct.
         ADDSP   6, i        ;Deallocate temporaries #2h#2h#2h
.ENDMACRO

.DEFMACRO XNORA, 2
XORA $1, $2
NOTA
.ENDMACRO

.DEFMACRO XNORX, 2
XORX $1, $2
NOTX
.ENDMACRO
