; Two Threads, One Printer: a preemption exercise.
;
; Two user programs, A and B, each print a list of 32-bit numbers, one per
; line, with printLong. A timer interrupt switches between them every
; "quantum" instructions, wherever they happen to be, even in the
; middle of printLong. The numbers each program prints should come out
; whole, no matter where the switches land.
;
; Each thread's characters go into its own buffer (putc, below), not straight
; to the console, so the two threads cannot scramble each other's text. When
; both threads have finished, the kernel prints each buffer and says OK if it
; holds exactly what that thread should have printed, WRONG if not.
;
; This program needs the "fancy machine" (supervisor/user modes and a timer).
; From a terminal:   run --fancy --bm <object file>
;
; How the kernel works
; --------------------
; The machine counts instructions down in the word at 0xFFF5. When
; it reaches zero (inside the OS too, but then the trap waits until the program
; is back in user mode) it traps to the handler like a system call does, with
; opcode byte 0 and operand 1 in the trap frame (an invalid instruction is
; opcode byte = the bad opcode, operand 2). The handler saves the running
; thread, picks the other one, sets the timer again and SRETs into it.
; SCALL 1 = EXIT.
;
; What to do
; ----------
; printLong and uPrintLong between the STUDENT CODE markers keep their
; working variables in static storage (.BLOCK). Both threads share those
; words, so a switch in the middle of a print lets the other thread
; overwrite them. Run the program with a few different quanta (change
; "quantum" below, 0 means no timer) until a thread reports WRONG, then
; rewrite the two routines so that all working variables live in a stack
; frame (SUBSP on entry, ADDSP before RET). Both threads should then report
; OK for every quantum. Remember that every PUSHA moves SP, so ",s" offsets
; move with it. Characters are written with "CALL putc,i" (character in A).
;
; Process table entry (14 bytes):
;     0 state    1 ready, 2 running, 3 done
;     2 saved SP (address of the process's saved trap frame)
;     4 saved H
;     6 next     the other process's table entry
;     8 top of the process's user stack
;    10 top of the process's system stack
;    12 write pointer into the thread's output buffer

        BR      main

READY:    .EQUATE 1
RUNNING:  .EQUATE 2
DONE:     .EQUATE 3

stState:  .EQUATE 0
stSP:     .EQUATE 2
stH:      .EQUATE 4
stNext:   .EQUATE 6
stUser:   .EQUATE 8
stSys:    .EQUATE 10
stOut:    .EQUATE 12

quantum:  .EQUATE 40            ; instructions between timer traps (0 = no timer)

cur:      .WORD   0             ; address of the running process's table entry
tmpEntry: .WORD   0
tmpPcb:   .WORD   0

pcbA:     .WORD   0
          .WORD   0
          .WORD   0
          .WORD   pcbB
          .WORD   userAT
          .WORD   sysAT
          .WORD   bufA
pcbB:     .WORD   0
          .WORD   0
          .WORD   0
          .WORD   pcbA
          .WORD   userBT
          .WORD   sysBT
          .WORD   bufB

mainS:    .BLOCK  16
mainT:    .WORD   0
userA:    .BLOCK  128
userAT:   .WORD   0
userB:    .BLOCK  128
userBT:   .WORD   0
sysA:     .BLOCK  64
sysAT:    .WORD   0
sysB:     .BLOCK  64
sysBT:    .WORD   0
bufA:     .BLOCK  96            ; what thread 1 wrote
bufB:     .BLOCK  96            ; what thread 2 wrote
expA:     .ASCII  "2147483647\n-2147483648\n0\n1234567\n-90\n\0"
expB:     .ASCII  "100000\n-1\n2000000000\n7\n-305419896\n\0"
hdrA:     .ASCII  "Thread 1 wrote:\n\0"
okmA:     .ASCII  "Thread 1: OK\n\0"
badmA:    .ASCII  "Thread 1: WRONG\n\0"
hdrB:     .ASCII  "Thread 2 wrote:\n\0"
okmB:     .ASCII  "Thread 2: OK\n\0"
badmB:    .ASCII  "Thread 2: WRONG\n\0"

;----------------------------------------------------------------------
; Trap handler. The frame's opcode byte is at 9,s and its operand at 10,s.
;----------------------------------------------------------------------
handler:  LDWA    0,i
          LDBA    9,s
          BRNE    scall         ; a nonzero opcode byte is a system call
          LDWA    10,s          ; opcode 0: a trap from the machine itself
          CPWA    1,i
          BREQ    yield         ; 1 = timer: time for the other thread
          BR      exit          ; 2 = invalid instruction: end this thread
scall:    LDWA    10,s
          CPWA    1,i
          BREQ    exit          ; SCALL 1 = EXIT
          LDWA    0x0BAD,i      ; anything else: record it and stop
          STWA    0xF004,d
          BR      halt

; yield: save this thread, choose the next one, resume it.
yield:    MOVSPA                ; A = address of the frame the trap just pushed
          LDWX    cur,d
          STWA    stSP,x
          SWAPHA                ; traps do not save H, so we do
          STWA    stH,x
          LDWA    READY,i
          STWA    stState,x
          LDWX    stNext,x      ; X = the other thread
          LDWA    stState,x
          CPWA    READY,i
          BREQ    chosen
          LDWX    cur,d         ; the other one is done: keep running this one
chosen:   STWX    cur,d
          LDWA    RUNNING,i
          STWA    stState,x
          BR      resume

; exit: this thread is finished. Run the other one if it can run, else stop.
exit:     LDWX    cur,d
          LDWA    DONE,i
          STWA    stState,x
          LDWX    stNext,x
          LDWA    stState,x
          CPWA    READY,i
          BRNE    halt
          STWX    cur,d
          LDWA    RUNNING,i
          STWA    stState,x
          BR      resume

; resume: restore H and SP from cur's table entry, start the timer, SRET into it
resume:   LDWX    cur,d
          LDWA    stH,x
          SWAPHA                ; H = saved H
          LDWA    stSP,x
          MOVASP                ; SP = the frame to return from
          LDWA    quantum,i
          STWA    0xFFF5,d      ; the timer counts every instruction from here on
          SRET

; halt: report what each thread wrote, then power off
halt:
          LDWX    hdrA,i
          CALL    puts,i
          LDWX    bufA,i
          CALL    puts,i
          LDWX    0,i
ckA:     LDBA    bufA,x
          CPBA    expA,x
          BRNE    badA
          CPBA    0,i
          BREQ    okA
          ADDX    1,i
          BR      ckA
okA:     LDWX    okmA,i
          BR      sayA
badA:    LDWX    badmA,i
sayA:    CALL    puts,i
          LDWX    hdrB,i
          CALL    puts,i
          LDWX    bufB,i
          CALL    puts,i
          LDWX    0,i
ckB:     LDBA    bufB,x
          CPBA    expB,x
          BRNE    badB
          CPBA    0,i
          BREQ    okB
          ADDX    1,i
          BR      ckB
okB:     LDWX    okmB,i
          BR      sayB
badB:    LDWX    badmB,i
sayB:    CALL    puts,i
          LDBA    0,i
          STBA    pwrOff,d

; puts: print the zero-terminated string at the address in X. Keeps nothing.
puts:     LDBA    0,x
          BREQ    putsEnd
          STBA    charOut,d
          ADDX    1,i
          BR      puts
putsEnd:  RET

;----------------------------------------------------------------------
; create: A = entry point, X = process table entry.
; Builds the first frame at the top of the process's system stack.
;----------------------------------------------------------------------
create:   STWA    tmpEntry,d
          STWX    tmpPcb,d
          LDWA    stSys,x
          SUBA    12,i
          STWA    stSP,x
          LDWX    stSP,x
          LDWA    tmpEntry,d
          STWA    5,x           ; frame: return PC = entry point
          LDWX    tmpPcb,d
          LDWA    stUser,x
          LDWX    stSP,x
          STWA    7,x           ; frame: user SP = top of user stack
          LDWX    tmpPcb,d
          LDWA    READY,i
          STWA    stState,x
          RET

main:     LDWA    mainT,i
          MOVASP
          LDWA    handler,i
          STWA    0xFFF7,d      ; install the trap handler
          LDWA    progA,i
          LDWX    pcbA,i
          CALL    create
          LDWA    progB,i
          LDWX    pcbB,i
          CALL    create
          LDWA    pcbA,i        ; A runs first
          STWA    cur,d
          LDWX    pcbA,i
          LDWA    RUNNING,i
          STWA    stState,x
          BR      resume

;----------------------------------------------------------------------
; The two user programs: print five numbers each, one per line.
;----------------------------------------------------------------------
progA:
          LDWA    0xFFFF,i
          PUSHA
          LDWA    0x7FFF,i
          PUSHA
          CALL    printLong,i
          ADDSP   4,i
          LDBA    '\n',i
          CALL    putc,i
          LDWA    0x0000,i
          PUSHA
          LDWA    0x8000,i
          PUSHA
          CALL    printLong,i
          ADDSP   4,i
          LDBA    '\n',i
          CALL    putc,i
          LDWA    0x0000,i
          PUSHA
          LDWA    0x0000,i
          PUSHA
          CALL    printLong,i
          ADDSP   4,i
          LDBA    '\n',i
          CALL    putc,i
          LDWA    0xD687,i
          PUSHA
          LDWA    0x0012,i
          PUSHA
          CALL    printLong,i
          ADDSP   4,i
          LDBA    '\n',i
          CALL    putc,i
          LDWA    0xFFA6,i
          PUSHA
          LDWA    0xFFFF,i
          PUSHA
          CALL    printLong,i
          ADDSP   4,i
          LDBA    '\n',i
          CALL    putc,i
          SCALL   1,i           ; EXIT

progB:
          LDWA    0x86A0,i
          PUSHA
          LDWA    0x0001,i
          PUSHA
          CALL    printLong,i
          ADDSP   4,i
          LDBA    '\n',i
          CALL    putc,i
          LDWA    0xFFFF,i
          PUSHA
          LDWA    0xFFFF,i
          PUSHA
          CALL    printLong,i
          ADDSP   4,i
          LDBA    '\n',i
          CALL    putc,i
          LDWA    0x9400,i
          PUSHA
          LDWA    0x7735,i
          PUSHA
          CALL    printLong,i
          ADDSP   4,i
          LDBA    '\n',i
          CALL    putc,i
          LDWA    0x0007,i
          PUSHA
          LDWA    0x0000,i
          PUSHA
          CALL    printLong,i
          ADDSP   4,i
          LDBA    '\n',i
          CALL    putc,i
          LDWA    0xA988,i
          PUSHA
          LDWA    0xEDCB,i
          PUSHA
          CALL    printLong,i
          ADDSP   4,i
          LDBA    '\n',i
          CALL    putc,i
          SCALL   1,i           ; EXIT

;----------------------------------------------------------------------
; putc: appends the character in A to the running thread's output buffer.
; Keeps A and X. Uses only its own stack and the running thread's table
; entry, so it is safe to interrupt.
;----------------------------------------------------------------------
putc:     SUBSP   4,i
          STWA    0,s
          STWX    2,s
          LDWX    cur,d
          LDWX    stOut,x       ; X = where the next character goes
          LDBA    1,s
          STBA    0,x
          LDWX    cur,d
          LDWA    stOut,x
          ADDA    1,i
          STWA    stOut,x       ; move the write pointer on
          LDWA    0,s
          LDWX    2,s
          ADDSP   4,i
          RET

;----------------------------------------------------------------------
; printLong and uPrintLong. Calling convention (the same as the int32
; library): push the value's low word, then its high word, CALL, then
; ADDSP 4,i.  printLong prints a signed value, uPrintLong an unsigned one.
; Nothing is returned.
;----------------------------------------------------------------------
; ---- BEGIN STUDENT CODE
; uPrintLong frame (SP-relative right after SUBSP 16):
;   0..9 digit buffer (least significant digit first), 10 digit count,
;   12 current value high, 14 current value low
ulDigits: .EQUATE 0
ulCount:  .EQUATE 10
ulCurHi:  .EQUATE 12
ulCurLo:  .EQUATE 14
ulFrame:  .EQUATE 16

ulValHi: .EQUATE 2      ; offset from SP at uPrintLong entry: value hi word
ulValLo: .EQUATE 4      ; value lo word
ulArgHi: .EQUATE 18     ; the same operands once the frame is allocated
ulArgLo: .EQUATE 20

uPrintLong:
        LDWA    ulValHi,s
        ORA     ulValLo,s
        BRNE    ulNonzero
        LDBA    '0',i
        CALL    putc,i
        RET

ulNonzero:
        SUBSP   ulFrame,i
        LDWA    ulArgHi,s
        STWA    ulCurHi,s
        LDWA    ulArgLo,s
        STWA    ulCurLo,s
        LDWX    0,i
        STWX    ulCount,s

ulLoop:
        LDWA    ulCurHi,s
        ORA     ulCurLo,s
        BREQ    ulPrint

        ; push current value (low, high) and divisor 10 (low, high).
        ; Each PUSHA moves SP down 2, so the frame fields are read at the
        ; offsets they have at that moment (curLo at 14, curHi then at 14).
        LDWA    ulCurLo,s
        PUSHA
        LDWA    14,s            ; ulCurHi, 2 bytes further from SP now
        PUSHA
        LDWA    10,i
        PUSHA
        LDWA    0,i
        PUSHA
        CALL    UDDiv,i
        ; the 8 bytes pushed above are still on the stack, results in place:
        ; 0 remHi, 2 remLo, 4 quoHi, 6 quoLo.  The frame is 8 bytes further.
        LDWA    4,s
        STWA    20,s            ; ulCurHi + 8
        LDWA    6,s
        STWA    22,s            ; ulCurLo + 8
        LDWA    2,s             ; remainder, always below 10
        ADDA    '0',i
        LDWX    18,s            ; ulCount + 8
        STBA    8,sx            ; ulDigits + 8, indexed by the count
        ADDX    1,i
        STWX    18,s
        ADDSP   8,i

        BR      ulLoop

ulPrint:
        LDWX    ulCount,s
        SUBX    1,i
ulPrintLoop:
        BRLT    ulDone
        LDBA    ulDigits,sx
        CALL    putc,i
        SUBX    1,i
        BR      ulPrintLoop
ulDone:
        ADDSP   ulFrame,i
        RET

slValHi: .EQUATE 2
slValLo: .EQUATE 4

printLong:
        ; No cached copy of the operand is needed: each PUSHA moves SP, so
        ; the operand is re-read at its adjusted offset every time.
        LDWA    slValHi,s
        BRLT    slNegative
        ; non-negative: pass the operand straight to uPrintLong
        ; (push low, then high, matching its calling convention)
        LDWA    slValLo,s
        PUSHA
        LDWA    4,s             ; slValHi, 2 bytes further from SP now
        PUSHA
        CALL    uPrintLong,i
        ADDSP   4,i
        RET

slNegative:
        LDBA    '-',i
        CALL    putc,i
        ; compute 0 - value using the already-verified DSUB routine,
        ; which correctly handles the INT32_MIN edge case: negating
        ; 0x80000000 wraps back to 0x80000000, i.e. 2147483648
        ; unsigned, which is exactly the right magnitude to print.
        LDWA    0,i
        PUSHA                   ; a low
        PUSHA                   ; a high
        LDWA    8,s             ; slValLo, 4 bytes further from SP now
        PUSHA
        LDWA    8,s             ; slValHi, 6 bytes further from SP now
        PUSHA
        CALL    DSUB,i
        ADDSP   4,i
        ; magnitude (low, high) now on top of stack; just pass straight
        ; through to uPrintLong using the same convention
        CALL    uPrintLong,i
        ADDSP   4,i
        RET
; ---- END STUDENT CODE


        .INCLUDELIB "int32"      ; UDDiv and DSUB
        .END
