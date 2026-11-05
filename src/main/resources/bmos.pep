;=======================================================================
; bmos.pep -- DeP10's own tiny "bare metal OS", as a library.
;
; Use it in a program that runs with NO operating system: assemble the
; program as "Pep/10 System Code" (the editor picks that type when the file
; has .SECTION lines; from a terminal use  asm --sys ... ) and put
;
;       .INCLUDELIB "bmos"
;
; at the very END of the file, after the code that powers the machine off.
; (Execution starts at address 0, so a library placed first would be run
; instead of jumped into.)
;
; What it provides
;   charIn, charOut, pwrOff   the addresses of the console input port, the
;                             console output port and the power-off port
;   puts     print the zero-terminated string at the address in X
;            (changes A and X)
;   puthex   print the four hex digits of the word in A (changes A and X)
;   hexTab   the table of hex digits that puthex uses
;
; What it does NOT provide: a trap handler. The program installs its own
; (store its address in the word at 0xFFF7 and the top of the system stack
; in the word at 0xFFFB), because writing the handler is the point of the
; exercises. A program ends by storing any byte to pwrOff, for example
;       STBA    pwrOff,d
; It never ends with RET: there is no operating system to return to.
;=======================================================================

charIn:   .EQUATE 0xFFFD         ; console input port
charOut:  .EQUATE 0xFFFE         ; console output port
pwrOff:   .EQUATE 0xFFFF         ; write here to power off

; puts: print the zero-terminated string at the address in X
puts:     LDBA    0,x
          BREQ    putsEnd
          STBA    charOut,d
          ADDX    1,i
          BR      puts
putsEnd:  RET

; puthex: print the four hex digits of the word in A
puthex:   SUBSP   6,i            ; 0,s value  2,s digits left  4,s digit
          STWA    0,s
          LDWA    4,i
          STWA    2,s
phLoop:   LDWA    0,s
          @ASRA4
          @ASRA4
          @ASRA4                 ; top nibble to the bottom (the sign bit smears, so mask)
          ANDA    0x000F,i
          STWA    4,s
          LDWX    4,s
          LDBA    hexTab,x
          STBA    charOut,d
          LDWA    0,s
          @ASLA4                 ; next nibble to the top
          STWA    0,s
          LDWA    2,s
          SUBA    1,i
          STWA    2,s
          BRNE    phLoop
          ADDSP   6,i
          RET

hexTab:   .ASCII  "0123456789ABCDEF"
