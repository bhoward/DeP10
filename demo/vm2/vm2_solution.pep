; Address Spaces: one machine, two page tables.
;
; A tiny kernel runs two user processes, P and Q, that share the same machine
; but not the same memory. Both programs store a value at the virtual address
; 0x0400, give up the CPU with SCALL 2 (yield), and then read 0x0400 back and
; print it:
;
;   P stores 1 at 0x0400      Q stores 2 at 0x0400
;
; Each process has its own page table (tabP and tabQ). Virtual page 4 maps to
; frame 0x20 in tabP and to frame 0x21 in tabQ, so the two 0x0400s are
; different words and each process reads back what IT stored. Pages 0 (code)
; and 7 (stacks) map to the same frames in both tables.
;
; The kernel switches by pointing the stack pointer at the other process's
; trap frame and executing SRET; the frames never move. What it also has to do
; is install the right page table: the base register at 0xFFF3 holds the table
; address plus one (the odd value turns translation on).
;
; This program needs the "fancy machine" (user mode, translation).
; From a terminal:   run --fancy --bm <object file>
;
; What to do
; ----------
; In the starter, no table is ever installed, so there is no translation and
; both processes use the same physical memory: P reads back Q's value.
; Complete the code between the STUDENT CODE markers. On entry X holds the
; process number times two (0 for P, 2 for Q) and the word list tabs holds the
; two table addresses. Load the table address of the process about to run, add
; 1, and store it in the base register at 0xFFF3.
;
; Expected output:
;
;     P1
;     Q2

; ---- user code (page 0)
          BR      main

progP:    LDWA    1,i
          STWA    0x0400,d
          SCALL   2,i            ; yield
          LDBA    'P',i
          SCALL   1,i            ; SCALL 1: print the character in A
          LDWA    0x0400,d
          ADDA    '0',i
          SCALL   1,i
          LDBA    '\n',i
          SCALL   1,i
          SCALL   0,i            ; SCALL 0: exit

progQ:    LDWA    2,i
          STWA    0x0400,d
          SCALL   2,i
          LDBA    'Q',i
          SCALL   1,i
          LDWA    0x0400,d
          ADDA    '0',i
          SCALL   1,i
          LDBA    '\n',i
          SCALL   1,i
          SCALL   0,i

; ---- the user stacks: P below 0x0800, Q below 0x0780
          .SECTION "stack", "rw"
          .ORG    0x0700
userS:    .BLOCK  256

; ---- the page tables: 256 words each. Page 0 code (frame 0), page 4 data
; ---- (frame 0x20 for P, 0x21 for Q), page 7 stack (frame 7)
          .SECTION "tabP", ""
          .ORG    0x1800
tabP:     .WORD   0x0001
          .BLOCK  6              ; pages 1 to 3
          .WORD   0x2003
          .BLOCK  4              ; pages 5 and 6
          .WORD   0x0703
          .BLOCK  496            ; pages 8 to 255

          .SECTION "tabQ", ""
          .ORG    0x1A00
tabQ:     .WORD   0x0001
          .BLOCK  6
          .WORD   0x2103
          .BLOCK  4
          .WORD   0x0703
          .BLOCK  496

; ---- the kernel lives at 0x1000, where no user process may go
          .SECTION "os", ""
          .ORG    0x1000

frames:   .WORD   fP             ; the saved trap frame of each process
          .WORD   fQ
tabs:     .WORD   tabP           ; the page table of each process
          .WORD   tabQ
cur:      .WORD   0              ; 0 = P, 2 = Q
nExit:    .WORD   0
initSP:   .WORD   0

doneMsg:  .ASCII  "All processes finished\n\0"
otherMsg: .ASCII  "Unexpected trap, stopping\n\0"

fP:       .BLOCK  12
fQ:       .BLOCK  12
sysS:     .BLOCK  64
frame:    .BLOCK  12
sysT:     .WORD   0              ; top of the system stack

; mkFrame: build a first trap frame at X for a program starting at A
mkFrame:  STWA    5,x            ; PC
          LDWA    0,i
          STBA    0,x            ; flags: user mode
          STWA    1,x            ; A
          STWA    3,x            ; X
          STBA    9,x            ; opcode byte
          STWA    10,x           ; operand
          LDWA    initSP,d
          STWA    7,x            ; user SP
          RET

; ---- boot
main:     LDWA    handler,i
          STWA    0xFFF7,d       ; trap handler pointer
          LDWA    sysT,i
          STWA    0xFFFB,d       ; system stack top
          LDWA    frame,i
          MOVASP
          LDWA    0x0800,i
          STWA    initSP,d
          LDWX    fP,i
          LDWA    progP,i
          CALL    mkFrame,i
          LDWA    0x0780,i
          STWA    initSP,d
          LDWX    fQ,i
          LDWA    progQ,i
          CALL    mkFrame,i
          LDWA    0,i
          STWA    cur,d

; resume the process in cur: install its page table, point SP at its frame, SRET
resume:   LDWX    cur,d
; ---- BEGIN STUDENT CODE
          LDWA    tabs,x         ; the table of the process about to run
          ADDA    1,i            ; odd: translation on
          STWA    0xFFF3,d
; ---- END STUDENT CODE
          LDWA    frames,x
          MOVASP
          SRET

finished: LDWX    doneMsg,i
          CALL    puts,i
          LDBA    0,i
          STBA    pwrOff,d

;----------------------------------------------------------------------
; Trap handler. The frame's opcode byte is at 9,s and its operand at 10,s.
;----------------------------------------------------------------------
handler:  LDWA    0,i
          LDBA    9,s
          BRNE    scall          ; a nonzero opcode byte is a system call
          BR      other
scall:    LDWA    10,s
          BREQ    exit           ; SCALL 0 = exit
          CPWA    1,i
          BREQ    print          ; SCALL 1 = print the character in A
          CPWA    2,i
          BREQ    yield          ; SCALL 2 = yield
          BR      other
print:    LDBA    2,s            ; the low byte of the saved A
          STBA    charOut,d
          SRET
exit:     LDWA    nExit,d
          ADDA    1,i
          STWA    nExit,d
          CPWA    2,i
          BREQ    finished
yield:    LDWA    cur,d          ; switch to the other process
          BREQ    toQ
          LDWA    0,i
          BR      setCur
toQ:      LDWA    2,i
setCur:   STWA    cur,d
          BR      resume
other:    LDWX    otherMsg,i
          CALL    puts,i
          LDBA    0,i
          STBA    pwrOff,d

; puts: print the zero-terminated string at X
puts:     LDBA    0,x
          BREQ    putsEnd
          STBA    charOut,d
          ADDX    1,i
          BR      puts
putsEnd:  RET
