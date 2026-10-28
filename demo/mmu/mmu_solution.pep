; Protection Faults: a memory protection exercise.
;
; A tiny kernel runs three user programs one after another. Each program has
; its own protection table (two bits per 256-byte page, see below) that the
; kernel installs before it starts the program:
;
;   A  well behaved: updates its own data (page 5) and prints "A".
;   B  buggy: prints "B", then writes to page 5, which belongs to A.
;   C  buggy: prints "C", then jumps into page 4, which holds data.
;
; The protection hardware stops B and C. It undoes the offending
; instruction and traps to the kernel's handler with opcode byte 0 in the
; frame and the operand
;   3   a read or write the table does not allow, or
;   4   an instruction fetch from a page that is not code.
; The faulting address is in the word at 0xFFF1, and the saved PC in the
; frame is the instruction that faulted.
;
; This program needs the "fancy machine" (user mode, protection).
; From a terminal:   run --fancy --bm <object file>
;
; Page table entry bits (read, write): 00 no access, 10 read only,
; 11 read and write, 01 read and execute (a code page). The table base
; register is the word at 0xFFF3; the kernel points it at the running
; program's table. Page 0xFF (I/O) is never open to user programs.
;
; What to do
; ----------
; In the starter the handler calls onFault, which only prints "(fault)" and
; goes on to the next program. Rewrite onFault (between the STUDENT CODE
; markers) so that for each fault it prints one line:
;
;     Process killed: bad data access at 0x0500        (operand 3)
;     Process killed: bad instruction fetch at 0x0400  (operand 4)
;
; with the address from the word at 0xFFF1 (puthex prints the four hex
; digits of the word in A) and then ends the program with BR nextProc.
; When onFault runs, SP points at the trap frame: the operand is at 10,s.
; Use puts (X = the zero-terminated string) to print the messages
; badData and badFetch.
;
; Expected output:
;
;     B
;     Process killed: bad data access at 0x0500
;     C
;     Process killed: bad instruction fetch at 0x0400
;     A
;     All programs finished

; ---- user code (page 0): read and execute only
          BR      main

progA:    LDWA    secretA,d
          ADDA    1,i
          STWA    secretA,d      ; A's own data, so this is allowed
          LDBA    'A',i
          SCALL   1,i            ; SCALL 1: print the character in A
          LDBA    '\n',i
          SCALL   1,i
          SCALL   0,i            ; SCALL 0: exit

progB:    LDBA    'B',i
          SCALL   1,i
          LDBA    '\n',i
          SCALL   1,i
          LDWA    1,i
          STWA    secretA,d      ; page 5 is not B's: this faults
          SCALL   0,i

progC:    LDBA    'C',i
          SCALL   1,i
          LDBA    '\n',i
          SCALL   1,i
          BR      0x0400         ; page 4 is data, not code: this faults
          SCALL   0,i

; ---- data pages
          .SECTION "text", "r"
          .ORG    0x0400
note:     .WORD   0xBEEF

          .SECTION "dataA", "rw"
          .ORG    0x0500
secretA:  .WORD   0x1234

          .SECTION "dataB", "rw"
          .ORG    0x0600
dataB:    .WORD   0

          .SECTION "stack", "rw"
          .ORG    0x0700
userS:    .BLOCK  256            ; user stacks end at 0x0800

; ---- the kernel lives at 0x1000, where no user program may go
          .SECTION "os", ""
          .ORG    0x1000

; Tables: pages 0 to 3 in the first byte, 4 to 7 in the second
tableA:   .BYTE   0x40           ; page 0 code
          .BYTE   0xB3           ; page 4 read, 5 read/write, 6 none, 7 read/write
          .BLOCK  62
tableB:   .BYTE   0x40           ; page 0 code
          .BYTE   0x8F           ; page 4 read, 5 none, 6 read/write, 7 read/write
          .BLOCK  62

; Program list: entry point and table, ended by 0
procs:    .WORD   progB
          .WORD   tableB
          .WORD   progC
          .WORD   tableB
          .WORD   progA
          .WORD   tableA
          .WORD   0
cur:      .WORD   procs
entryPC:  .WORD   0

doneMsg:  .ASCII  "All programs finished\n\0"
otherMsg: .ASCII  "Unexpected trap, stopping\n\0"
badData:  .ASCII  "Process killed: bad data access at 0x\0"
badFetch: .ASCII  "Process killed: bad instruction fetch at 0x\0"
hexTab:   .ASCII  "0123456789ABCDEF"

sysS:     .BLOCK  64
frame:    .BLOCK  12             ; the trap frame lives here
sysT:     .WORD   0              ; top of the system stack

; ---- boot
main:     LDWA    handler,i
          STWA    0xFFF7,d       ; trap handler pointer
          LDWA    sysT,i
          STWA    0xFFFB,d       ; system stack top
          LDWA    frame,i
          MOVASP

; start the next program: build its first frame, install its table, SRET
start:    LDWX    cur,d
          LDWA    0,x
          BREQ    finished
          STWA    entryPC,d
          LDWA    2,x
          STWA    0xFFF3,d       ; protection on, with this program's table
          LDWX    frame,i
          LDWA    0,i
          STBA    0,x            ; flags
          STWA    1,x            ; A
          STWA    3,x            ; X
          LDWA    entryPC,d
          STWA    5,x            ; PC
          LDWA    0x0800,i
          STWA    7,x            ; user SP
          LDWA    0,i
          STBA    9,x            ; opcode byte
          STWA    10,x           ; operand
          LDWA    frame,i
          MOVASP
          SRET

nextProc: LDWX    cur,d
          ADDX    4,i
          STWX    cur,d
          BR      start

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
          LDWA    10,s           ; opcode 0: a trap from the machine itself
          CPWA    3,i
          BREQ    fault
          CPWA    4,i
          BREQ    fault
          BR      other
scall:    LDWA    10,s
          BREQ    nextProc       ; SCALL 0 = exit
          CPWA    1,i
          BRNE    other
          LDBA    2,s            ; SCALL 1 = print the character in A (low byte at +2)
          STBA    charOut,d
          SRET
fault:    BR      onFault
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

; puthex: print the four hex digits of A
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

; ---- BEGIN STUDENT CODE
onFault:  LDWA    10,s           ; 3 = data access, 4 = instruction fetch
          CPWA    4,i
          BREQ    isFetch
          LDWX    badData,i
          BR      show
isFetch:  LDWX    badFetch,i
show:     CALL    puts,i
          LDWA    0xFFF1,d       ; the fault address register
          CALL    puthex,i
          LDBA    '\n',i
          STBA    charOut,d
          BR      nextProc
; ---- END STUDENT CODE
.END
