; Producer and consumer with a two-slot buffer, using semaphores.
;
; The producer makes the items 1 to 6 and puts each into a buffer that holds
; only two. The consumer takes them out in order. Two counting semaphores keep
; them in step:
;
;   empty  free slots in the buffer (starts at 2)
;   full   filled slots (starts at 0)
;
; The producer does P(empty), stores the item, then V(full). The consumer does
; P(full), takes the item, then V(empty). A P on a count of 0 blocks the caller
; until the other side does a V. With one producer and one consumer no extra
; lock is needed: each side owns its own index.
;
; Output: "pN" when the producer has stored item N, "cN" when the consumer has
; taken it. The buffer never holds more than 2 items, so the producer never
; gets more than two items ahead:
;
;     p1 p2 c1 c2 p3 p4 c3 c4 p5 p6 c5 c6      (one item per line)
;
; System calls: SCALL 0 YIELD, 1 EXIT, 2 P, 3 V. For P and V the semaphore's
; address is in A. A semaphore is one word holding its count. A process
; control block (PCB) is 18 bytes:
;     0 state (0 free, 1 ready, 2 running, 3 done, 4 blocked)
;     2 saved SP (address of the saved trap frame)
;     4 next PCB in the ring
;     6 pid      8 top of user stack     10 top of system stack
;    12 unused  14 saved H     16 waitingOn (semaphore, if blocked)

        BR      main

FREE:    .EQUATE 0
READY:   .EQUATE 1
RUNNING: .EQUATE 2
DONE:    .EQUATE 3
BLOCKED: .EQUATE 4
items:   .EQUATE 7              ; the producer makes 1 .. items-1

cur:     .WORD   0              ; address of the running PCB
tmpSem:  .WORD   0
tmpEntry: .WORD  0
tmpPcb:  .WORD   0

empty:   .WORD   2
full:    .WORD   0
buf:     .BLOCK  4              ; two one-word slots
inIdx:   .WORD   0              ; byte offset of the producer's next slot
outIdx:  .WORD   0              ; byte offset of the consumer's next slot

pcbP:    .WORD   0
         .WORD   0
         .WORD   pcbC
         .WORD   0
         .WORD   userPT
         .WORD   sysPT
         .WORD   0
         .WORD   0
         .WORD   0
pcbC:    .WORD   0
         .WORD   0
         .WORD   pcbP
         .WORD   1
         .WORD   userCT
         .WORD   sysCT
         .WORD   0
         .WORD   0
         .WORD   0

mainS:   .BLOCK  16
mainT:   .WORD   0
userP:   .BLOCK  32
userPT:  .WORD   0
userC:   .BLOCK  32
userCT:  .WORD   0
sysP:    .BLOCK  64
sysPT:   .WORD   0
sysC:    .BLOCK  64
sysCT:   .WORD   0

;----------------------------------------------------------------------
; Trap handler: dispatch on the SCALL operand
;----------------------------------------------------------------------
handler: LDWA    10,s
         BREQ    yield
         CPWA    1,i
         BREQ    exit
         CPWA    2,i
         BREQ    semP
         CPWA    3,i
         BREQ    semV
         LDWA    0x0BAD,i       ; unknown system call: record it and stop
         STWA    0xF004,d
         BR      halt

yield:   MOVSPA
         LDWX    cur,d
         STWA    2,x            ; save the frame address
         SWAPHA                 ; H is not in the frame
         STWA    14,x
         LDWA    READY,i
         STWA    0,x
         BR      pick

exit:    LDWX    cur,d
         LDWA    DONE,i
         STWA    0,x
         BR      pick

; P: take a permit, or block the caller on the semaphore in its saved A
semP:    LDWX    1,s
         LDWA    0,x
         BREQ    block
         SUBA    1,i
         STWA    0,x
         SRET
block:   STWX    tmpSem,d
         MOVSPA
         LDWX    cur,d
         STWA    2,x
         SWAPHA
         STWA    14,x
         LDWA    BLOCKED,i
         STWA    0,x
         LDWA    tmpSem,d
         STWA    16,x           ; waitingOn
         BR      pick

; V: hand the permit to a blocked process, or bump the count
semV:    LDWX    1,s
         STWX    tmpSem,d
         LDWX    cur,d
         LDWX    4,x
vLoop:   LDWA    0,x
         CPWA    BLOCKED,i
         BRNE    vNext
         LDWA    16,x
         CPWA    tmpSem,d
         BREQ    vWake
vNext:   CPWX    cur,d
         BREQ    vNone
         LDWX    4,x
         BR      vLoop
vWake:   LDWA    READY,i
         STWA    0,x
         SRET
vNone:   LDWX    tmpSem,d
         LDWA    0,x
         ADDA    1,i
         STWA    0,x
         SRET

; pick the next ready PCB after cur and resume it
pick:    LDWX    cur,d
         LDWX    4,x
pickLp:  LDWA    0,x
         CPWA    READY,i
         BREQ    run
         CPWX    cur,d
         BREQ    halt           ; went all the way round: nobody is ready
         LDWX    4,x
         BR      pickLp
run:     STWX    cur,d
         LDWA    RUNNING,i
         STWA    0,x
         LDWA    14,x
         SWAPHA
         LDWA    2,x
         MOVASP
         SRET

halt:    LDBA    0,i
         STBA    pwrOff,d

; create: A = entry point, X = PCB; builds the first frame, marks ready
create:  STWA    tmpEntry,d
         STWX    tmpPcb,d
         LDWA    10,x
         SUBA    12,i
         STWA    2,x
         LDWX    2,x
         LDWA    tmpEntry,d
         STWA    5,x
         LDWX    tmpPcb,d
         LDWA    8,x
         LDWX    2,x
         STWA    7,x
         LDWX    tmpPcb,d
         LDWA    READY,i
         STWA    0,x
         RET

main:    LDWA    mainT,i
         MOVASP
         LDWA    handler,i
         STWA    0xFFF7,d
         LDWA    producer,i
         LDWX    pcbP,i
         CALL    create
         LDWA    consumer,i
         LDWX    pcbC,i
         CALL    create
         LDWA    pcbC,i         ; start as if the consumer had just exited
         STWA    cur,d
         BR      pick

;----------------------------------------------------------------------
; Producer: 0,s = the next item
;----------------------------------------------------------------------
producer: SUBSP  2,i
         LDWA    1,i
         STWA    0,s
pLoop:   LDWA    empty,i
         SCALL   2,i            ; P(empty): wait for a free slot
         LDWX    inIdx,d
         LDWA    0,s
         STWA    buf,x          ; put the item in the slot
         LDBA    'p',i
         STBA    charOut,d
         LDWA    0,s
         ADDA    '0',i
         STBA    charOut,d
         LDBA    '\n',i
         STBA    charOut,d
         LDWX    inIdx,d
         ADDX    2,i
         CPWX    4,i
         BRLT    pKeep
         LDWX    0,i            ; wrap round to slot 0
pKeep:   STWX    inIdx,d
         LDWA    full,i
         SCALL   3,i            ; V(full): one more item to take
         LDWA    0,s
         ADDA    1,i
         STWA    0,s
         CPWA    items,i
         BRLT    pLoop
         SCALL   1,i            ; EXIT

;----------------------------------------------------------------------
; Consumer: 0,s = how many items are still to take
;----------------------------------------------------------------------
consumer: SUBSP  2,i
         LDWA    items,i
         SUBA    1,i
         STWA    0,s
cLoop:   LDWA    full,i
         SCALL   2,i            ; P(full): wait for an item
         LDWX    outIdx,d
         LDWA    buf,x          ; take it out of the slot
         STWA    tmpPcb,d       ; (any scratch word will do; the item is printed below)
         LDBA    'c',i
         STBA    charOut,d
         LDWA    tmpPcb,d
         ADDA    '0',i
         STBA    charOut,d
         LDBA    '\n',i
         STBA    charOut,d
         LDWX    outIdx,d
         ADDX    2,i
         CPWX    4,i
         BRLT    cKeep
         LDWX    0,i
cKeep:   STWX    outIdx,d
         LDWA    empty,i
         SCALL   3,i            ; V(empty): one more free slot
         LDWA    0,s
         SUBA    1,i
         STWA    0,s
         BRGT    cLoop
         SCALL   1,i            ; EXIT
         .END
