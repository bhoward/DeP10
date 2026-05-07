BR      start

; vars to store registers
resultA: .BLOCK 2
resultHi: .BLOCK 2
rowVal: .BLOCK 2 

; vars for array and indexing
values:	 .WORD -20000
	 .WORD -10000
	 .WORD 0
	 .WORD 10000
	 .WORD 20000
count:   .WORD 5
row: 	 .BLOCK 2
col: 	 .BLOCK 2

msgRow:  .ASCII "New row: \x00"
newline: .ASCII "\n\x00"
space:   .ASCII " \x00"

; mul matrix
; HEXO hi/low - -20000 -10000 -10 0 10 10000 20000

start:   LDWA 0,i
	 STWA row,d
; exit loop if row > count
outloop: LDWA row,d
	 CPWA count,d
	 BRGE done

	 @STRO msgRow,d
; index to values[row]
	 LDWX row,d
; offset values by 2 
	 ASLX
	 LDWA values,x
	 STWA resultA,d
	 @HEXO resultA,d
	 @STRO newline,d

; instantiate col
	 LDWA 0,i
	 STWA col,d
; check if col > count
inloop:  LDWA col,d
	 CPWA count,d
	 BRGE nextRow
	 LDWX row,d
	 ASLX
	 LDWA values,x
	 STWA rowVal,d
	 LDWX col,d
	 ASLX
	 MULA values,x
	 STWA resultA,d
	 ; now do high bits
	 LDWA rowVal,d
	 LDWX col,d
	 ASLX
	 MULHA values,x
	 STWA resultHi,d
	 @HEXO resultHi,d
	 @STRO space,d
	 @HEXO resultA,d
	 @STRO newline,d

; increment col
	 LDWA col,d
	 ADDA 1,i
	 STWA col,d
	 BR inloop

; increment row
nextRow: LDWA row,d
	 ADDA 1,i
	 STWA row,d
	 BR outloop

done:    RET


