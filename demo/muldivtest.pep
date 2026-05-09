BR      start

a:          .BLOCK 2
b:          .BLOCK 2
result:     .BLOCK 2
counter:    .BLOCK 2

msgMul:     .ASCII "MUL result: \x00"
msgDiv:     .ASCII "DIV result: \x00"
newline:    .ASCII "\n\x00"

; Small numbers (2 * 3, 6 / 3)

start:  LDWA    0x0002,i
        MUL    0x0003,i
        STWA    result,d

	@STRO   msgMul,d
        @DECO	result,d
	@STRO newline, d

        LDWA    0x0006,i
	LDWX    0, i
        DIV    0x0003,i
	STWA    result,d
        
        @STRO    msgDiv,d
        @DECO   result,d
	@STRO newline, d


; (300 * 40)
        LDWA    300,i
        MUL    40,i
        STWA    result,d

        @STRO msgMul,d
	@DECO result,d
        @STRO newline,d

; (5 / 100)
        LDWA    0x0005,i
	LDWX    0, i
        DIV     0x0064,i
        STWA    result,d

        @STRO    msgDiv,d
        @DECO    result,d
        @STRO    newline,d

; (100 / 5)
        LDWA    0x0064,i
        DIV    0x0005,i
        STWA    result,d

        @STRO    msgDiv,d
        @DECO    result,d
        @STRO    newline,d

; (5!)
        LDWA    0x0001,i
        STWA    result,d

        LDWA    0x0005,i
        STWA    counter,d

factLoop: LDWA    result,d
        MUL    counter,d
        STWA    result,d

        LDWA    counter,d
        SUBA    0x0001,i
        STWA    counter,d

        CPWA    0x0001,i
        BRGE    factLoop

        @STRO    msgMul,d
        @DECO    result,d
        @STRO    newline,d

;(100 / 2 / 2 / 2)
        LDWA    0x0064,i
        DIV     0x0002,i
        STWA    a,d

        LDWA    a,d
        LDWX    0x0000,i
        DIV	0x0002,i
        STWA    a,d

        LDWA    a,d
        LDWX    0x0000,i
        DIV	0x0002,i
        STWA    result,d

        @STRO    msgDiv,d
        @DECO    result,d
        @STRO    newline,d

        RET


