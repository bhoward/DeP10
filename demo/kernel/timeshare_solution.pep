; timeshare_solution.pep -- instructor solution for "Taking Turns on One CPU".
; This is timeshare_starter.pep with the YIELD code filled in. Do not hand
; it to students. Both files are checked by TimeSharingKernelTest.
;
; Two user programs, A and B, each print their letter and a count three
; times. After each print they ask the kernel to let the other program run
; (SCALL 0 = YIELD). With YIELD working the output alternates:
;
;     A1 B1 A2 B2 A3 B3     (one item per line)
;
; Without YIELD each program keeps the CPU until it exits:
;
;     A1 A2 A3 B1 B2 B3
;
; How a switch works
; ------------------
; SCALL pushes a 12-byte frame on the system stack: the flags, A, X, the
; return PC and the user SP, plus the SCALL opcode and operand. It then jumps
; to the trap handler (the address stored at 0xFFF7). SRET pops such a frame,
; resuming whatever program the frame describes, and sets the system stack
; pointer to just above the popped frame. So to resume a DIFFERENT program the
; handler only has to point SP at THAT program's frame and execute SRET.
;
; Each process has its own small system stack. The kernel starts a process by
; building a fake frame at the top of its system stack, as if the process had
; just made a system call; the first SRET then "returns" to its first
; instruction.
;
; SCALL does not save register H, so the kernel saves and restores H itself
; (SWAPHA exchanges A and H). Try deleting those lines and watch the "!" that
; the user programs print when their H comes back changed.
;
; Process table entry (10 bytes):
;     0 state    1 ready, 2 running, 3 done
;     2 saved SP (address of the process's saved SCALL frame)
;     4 saved H
;     6 next     the other process's table entry
;     8 top of the process's user stack
;    10 top of the process's system stack

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

aEnd:     .EQUATE 4             ; A prints its count for 1, 2, 3
bEnd:     .EQUATE 4             ; B does the same

cur:      .WORD   0             ; address of the running process's table entry
tmpEntry: .WORD   0
tmpPcb:   .WORD   0

pcbA:     .WORD   0             ; state
          .WORD   0             ; saved SP
          .WORD   0             ; saved H
          .WORD   pcbB          ; next
          .WORD   userAT
          .WORD   sysAT
pcbB:     .WORD   0
          .WORD   0
          .WORD   0
          .WORD   pcbA
          .WORD   userBT
          .WORD   sysBT

mainS:    .BLOCK  16
mainT:    .WORD   0
userA:    .BLOCK  32
userAT:   .WORD   0
userB:    .BLOCK  32
userBT:   .WORD   0
sysA:     .BLOCK  64
sysAT:    .WORD   0
sysB:     .BLOCK  64
sysBT:    .WORD   0

;----------------------------------------------------------------------
; Trap handler: dispatch on the SCALL operand (saved at 10,s)
;----------------------------------------------------------------------
handler:  LDWA    10,s
          BREQ    yield         ; 0 = YIELD
          CPWA    1,i
          BREQ    exit          ; 1 = EXIT
          LDWA    0x0BAD,i      ; anything else: record it and stop
          STWA    0xF004,d
          BR      halt

; YIELD: save this process, choose the next one, resume it.
yield:    MOVSPA                ; A = address of the frame SCALL just pushed
          LDWX    cur,d
          STWA    stSP,x        ; remember where this process's frame is
          SWAPHA                ; SCALL does not save H, so we do
          STWA    stH,x
; ---- BEGIN STUDENT CODE
          LDWA    READY,i
          STWA    stState,x     ; this process is ready to run again
          LDWX    stNext,x      ; X = the other process
          LDWA    stState,x
          CPWA    READY,i
          BREQ    chosen
          LDWX    cur,d         ; the other one is done: keep running this one
chosen:   STWX    cur,d
          LDWA    RUNNING,i
          STWA    stState,x
; ---- END STUDENT CODE
          BR      resume

; EXIT: this process is finished. Run the other one if it can run, else stop.
exit:     LDWX    cur,d
          LDWA    DONE,i
          STWA    stState,x
          LDWX    stNext,x
          LDWA    stState,x
          CPWA    READY,i
          BRNE    halt          ; nobody left
          STWX    cur,d
          LDWA    RUNNING,i
          STWA    stState,x
          BR      resume

; resume: restore H and SP from cur's table entry, then SRET into it
resume:   LDWX    cur,d
          LDWA    stH,x
          SWAPHA                ; H = saved H
          LDWA    stSP,x
          MOVASP                ; SP = the frame to return from
          SRET

halt:     LDBA    0,i
          STBA    pwrOff,d

;----------------------------------------------------------------------
; create: A = entry point, X = process table entry.
; Builds the first frame at the top of the process's system stack and marks
; the process ready.
;----------------------------------------------------------------------
create:   STWA    tmpEntry,d
          STWX    tmpPcb,d
          LDWA    stSys,x
          SUBA    12,i
          STWA    stSP,x        ; saved SP = frame address
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
; The two user programs. Each keeps its count on its own stack, prints a
; line, puts a marker in H, yields, and checks that H came back unchanged
; (printing "!" if not).
;----------------------------------------------------------------------
progA:    SUBSP   2,i           ; local: the count
          LDWA    1,i
          STWA    0,s
aLoop:    LDBA    'A',i
          STBA    charOut,d
          LDWA    0,s
          ADDA    '0',i
          STBA    charOut,d
          LDBA    0x0A,i
          STBA    charOut,d
          LDWA    0x00A1,i
          SWAPHA                ; H = 0x00A1
          SCALL   0,i           ; YIELD
          SWAPHA                ; A = H
          CPWA    0x00A1,i
          BREQ    aOk
          LDBA    '!',i
          STBA    charOut,d     ; H did not survive the switch
aOk:      LDWA    0,s
          ADDA    1,i
          STWA    0,s
          CPWA    aEnd,i
          BRLT    aLoop
          SCALL   1,i           ; EXIT

progB:    SUBSP   2,i
          LDWA    1,i
          STWA    0,s
bLoop:    LDBA    'B',i
          STBA    charOut,d
          LDWA    0,s
          ADDA    '0',i
          STBA    charOut,d
          LDBA    0x0A,i
          STBA    charOut,d
          LDWA    0x00B2,i
          SWAPHA                ; H = 0x00B2
          SCALL   0,i           ; YIELD
          SWAPHA
          CPWA    0x00B2,i
          BREQ    bOk
          LDBA    '!',i
          STBA    charOut,d
bOk:      LDWA    0,s
          ADDA    1,i
          STWA    0,s
          CPWA    bEnd,i
          BRLT    bLoop
          SCALL   1,i           ; EXIT
