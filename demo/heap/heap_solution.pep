; Heap Allocator: a first-fit allocator for a 64-byte heap.
;
; The heap is one block of memory, cut into blocks. Every block starts with a
; 2-byte header: its payload size in bytes (always even) with the lowest bit
; used as the flag "allocated". Blocks follow one another with no gaps, so
; the block after the one at address p starts at p + 2 + size.
;
;     heap:  [hdr|payload.....][hdr|payload..][hdr|payload.........]
;
; HInit   makes the whole heap one free block.
; HAlloc  A = bytes wanted. Returns X = address of the payload, or X = 0 if
;         no free block is big enough. First fit: take the first free block
;         that is large enough. Requests are rounded up to an even number of
;         bytes (minimum 2). If at least 4 bytes would be left over, the block
;         is split and the rest stays free; otherwise the whole block is used.
; HFree   X = address of a payload returned by HAlloc. Makes that block free.
;
; All three use A and X freely. They keep a few working words in static
; storage, so they are not safe to interrupt (see the Two Threads exercise).
;
; Your job: write HFree in the block marked STUDENT CODE. Until you do, the
; driver at the end of the file shows memory never coming back: its fourth
; request fails (p4=0) because the 20 bytes freed in the middle are not reused.
; With HFree working, p4 gets the same address as p2.
;
; Extension: when HFree makes a block free, also merge it with the free block
; that follows it, so two small free neighbours can serve one large request.

        BR      main

heapSize: .EQUATE 64

heap:     .BLOCK  64            ; the heap itself
heapEnd:  .WORD   0             ; (the address just past the heap)

haNeed:   .WORD   0
haTmp:    .WORD   0
haHdr:    .WORD   0

HInit:    LDWA    heapSize,i
          SUBA    2,i           ; the one block's payload: everything but its header
          STWA    heap,d        ; size 62, bit 0 clear = free
          RET

HAlloc:   ADDA    1,i           ; round the request up to an even number
          ANDA    0xFFFE,i
          CPWA    2,i
          BRGE    haSized
          LDWA    2,i           ; at least 2 bytes
haSized:  STWA    haNeed,d
          LDWX    heap,i        ; X = address of the first block
haLoop:   CPWX    heapEnd,i
          BRGE    haFail        ; walked off the end: nothing fits
          LDWA    0,x           ; A = header
          ANDA    1,i
          BRNE    haNext        ; allocated: skip it
          LDWA    0,x           ; free: A = its size
          CPWA    haNeed,d
          BRGE    haFit
haNext:   LDWA    0,x
          ANDA    0xFFFE,i
          ADDA    2,i           ; payload size + header
          STWA    haTmp,d
          ADDX    haTmp,d       ; on to the next block
          BR      haLoop
haFit:    SUBA    haNeed,d      ; A = bytes left over if we take just what was asked
          CPWA    4,i
          BRLT    haWhole       ; too little left for another block: use it all
          SUBA    2,i           ; the new free block's payload
          STWA    haTmp,d
          STWX    haHdr,d
          ADDX    haNeed,d
          ADDX    2,i           ; X = where the new free block's header goes
          LDWA    haTmp,d
          STWA    0,x
          LDWX    haHdr,d
          LDWA    haNeed,d
          ORA     1,i           ; size and the allocated bit
          STWA    0,x
          ADDX    2,i           ; X = payload
          RET
haWhole:  LDWA    0,x
          ORA     1,i
          STWA    0,x
          ADDX    2,i
          RET
haFail:   LDWX    0,i
          RET

; HFree: X = payload address.
; ---- BEGIN STUDENT CODE
HFree:    SUBX    2,i           ; X = the block's header
          LDWA    0,x
          ANDA    0xFFFE,i      ; clear the allocated bit; the size stays
          STWA    0,x
          RET
; ---- END STUDENT CODE

; report: prints X as an offset from the start of the heap, 0 to 99, then a
; newline (prints 0 if X is 0). Uses A and X.
report:   CPWX    0,i
          BREQ    rpValue
          SUBX    heap,i
rpValue:  STWX    haTmp,d
          LDWA    haTmp,d
          LDWX    0,i
rpLoop:   CPWA    10,i
          BRLT    rpTens
          SUBA    10,i
          ADDX    1,i
          BR      rpLoop
rpTens:   STWA    haTmp,d       ; the ones digit
          CPWX    0,i
          BREQ    rpOnes
          ADDX    '0',i
          STBX    charOut,d
rpOnes:   LDWA    haTmp,d
          ADDA    '0',i
          STBA    charOut,d
          LDBA    '\n',i
          STBA    charOut,d
          RET

; ---- DRIVER BELOW
p1:       .WORD   0
p2:       .WORD   0
p3:       .WORD   0
p4:       .WORD   0

main:     CALL    HInit,i
          LDWA    10,i
          CALL    HAlloc,i
          STWX    p1,d
          LDWA    20,i
          CALL    HAlloc,i
          STWX    p2,d
          LDWA    8,i
          CALL    HAlloc,i
          STWX    p3,d
          LDWX    p2,d
          CALL    HFree,i       ; give the 20 bytes back
          LDWA    20,i
          CALL    HAlloc,i      ; ...and ask for 20 again
          STWX    p4,d
          LDBA    'p',i
          STBA    charOut,d
          LDBA    '1',i
          STBA    charOut,d
          LDBA    '=',i
          STBA    charOut,d
          LDWX    p1,d
          CALL    report,i
          LDBA    'p',i
          STBA    charOut,d
          LDBA    '2',i
          STBA    charOut,d
          LDBA    '=',i
          STBA    charOut,d
          LDWX    p2,d
          CALL    report,i
          LDBA    'p',i
          STBA    charOut,d
          LDBA    '3',i
          STBA    charOut,d
          LDBA    '=',i
          STBA    charOut,d
          LDWX    p3,d
          CALL    report,i
          LDBA    'p',i
          STBA    charOut,d
          LDBA    '4',i
          STBA    charOut,d
          LDBA    '=',i
          STBA    charOut,d
          LDWX    p4,d
          CALL    report,i
          LDBA    0,i
          STBA    pwrOff,d
          .END
