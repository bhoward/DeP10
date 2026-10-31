; Page Faults: a demand paging exercise.
;
; A tiny kernel runs one user program with address translation turned on.
; The program stores to three virtual pages, 4, 5 and 6 (0x0400, 0x0500 and
; 0x0600), that have no frame yet, and then stores to page 9, which does not
; belong to the process at all.
;
; The page table is 256 words, one per virtual page. The high byte of a word
; is the physical frame number and the low two bits are the protection bits:
;     00 not present      10 read only      11 read and write      01 read and execute
; The base register at 0xFFF3 holds the table's (even) address PLUS ONE; the
; odd value turns on translation. Supervisor mode is not translated.
;
; When the program touches a page that is not present, the machine undoes the
; instruction and traps with opcode byte 0 and operand 3 (data access) or 4
; (instruction fetch). The faulting virtual address is in the word at 0xFFF1.
; Returning with SRET runs the same instruction again.
;
; This program needs the "fancy machine" (user mode, translation).
; From a terminal:   run --fancy --bm <object file>
;
; What to do
; ----------
; In the starter, onFault only prints "(fault)" and ends the program. Rewrite
; onFault (between the STUDENT CODE markers) to handle a page fault:
;
;   * operand 4, or a page outside 4 to 6: print
;         Process killed: bad access at 0x0900
;     (puthex prints the four hex digits of A) and end with BR nextProc.
;   * otherwise give the page the next free frame: nextFrame holds its number
;     (0x20 on the first fault). Store the frame number in the high byte of
;     the page's table entry (table + 2 * page) and 3 (read and write) in the
;     low byte, add 1 to nextFrame, print
;         Page fault at 0x0400, frame 0x0020
;     and return with SRET, which retries the instruction.
;
; When onFault runs, SP points at the trap frame: the operand is at 10,s.
; Variables page and frameNo are free to use. Use puts (X = the zero-
; terminated string) with the messages pfMsg, frMsg and killMsg.
;
; Expected output:
;
;     Page fault at 0x0400, frame 0x0020
;     Page fault at 0x0500, frame 0x0021
;     Page fault at 0x0600, frame 0x0022
;     6
;     Process killed: bad access at 0x0900
;     All programs finished

; ---- user code (page 0)
          BR      main

progP:    LDWA    1,i
          STWA    0x0400,d       ; page 4 is not present: fault
          LDWA    2,i
          STWA    0x0500,d       ; page 5
          LDWA    3,i
          STWA    0x0600,d       ; page 6
          LDWA    0x0400,d
          ADDA    0x0500,d
          ADDA    0x0600,d       ; 1 + 2 + 3, no more faults
          ADDA    '0',i
          SCALL   1,i            ; SCALL 1: print the character in A
          LDBA    '\n',i
          SCALL   1,i
          LDWA    1,i
          STWA    0x0900,d       ; page 9 is not this process's: kill it
          SCALL   0,i            ; SCALL 0: exit

; ---- the user stack
          .SECTION "stack", "rw"
          .ORG    0x0700
userS:    .BLOCK  256            ; the user stack ends at 0x0800

; ---- the page table: 256 words, page 0 code in frame 0, page 7 stack in frame 7
          .SECTION "table", ""
          .ORG    0x1800
table:    .WORD   0x0001
          .BLOCK  12             ; pages 1 to 6
          .WORD   0x0703
          .BLOCK  496            ; pages 8 to 255

; ---- the kernel lives at 0x1000, where no user program may go
          .SECTION "os", ""
          .ORG    0x1000

procs:    .WORD   progP
          .WORD   0
cur:      .WORD   procs
entryPC:  .WORD   0
page:     .WORD   0
frameNo:  .WORD   0
nextFrame: .WORD  0x0020

doneMsg:  .ASCII  "All programs finished\n\0"
otherMsg: .ASCII  "Unexpected trap, stopping\n\0"
pfMsg:    .ASCII  "Page fault at 0x\0"
frMsg:    .ASCII  ", frame 0x\0"
killMsg:  .ASCII  "Process killed: bad access at 0x\0"
failMsg:  .ASCII  "(fault)\n\0"
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
          LDWA    table,i
          ADDA    1,i            ; odd: translation on
          STWA    0xFFF3,d
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
          ADDX    2,i
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
          CPWA    3,i
          BRNE    kill           ; a bad fetch is never repaired
          LDWA    0,i
          LDBA    0xFFF1,d       ; the page: high byte of the fault address
          STWA    page,d
          CPWA    4,i
          BRLT    kill
          CPWA    6,i
          BRGT    kill
          LDWA    nextFrame,d    ; give the page the next free frame
          STWA    frameNo,d
          ADDA    1,i
          STWA    nextFrame,d
          LDWX    page,d
          ASLX                   ; 2 bytes per table entry
          LDWA    frameNo,d
          STBA    table,x        ; high byte: the frame number
          ADDX    1,i
          LDWA    3,i
          STBA    table,x        ; low byte: read and write
          LDWX    pfMsg,i
          CALL    puts,i
          LDWA    0xFFF1,d
          CALL    puthex,i
          LDWX    frMsg,i
          CALL    puts,i
          LDWA    frameNo,d
          CALL    puthex,i
          LDBA    '\n',i
          STBA    charOut,d
          SRET                   ; run the same instruction again
kill:     LDWX    killMsg,i
          CALL    puts,i
          LDWA    0xFFF1,d
          CALL    puthex,i
          LDBA    '\n',i
          STBA    charOut,d
          BR      nextProc
; ---- END STUDENT CODE
