		BR		Start
		.block	0x57
; comment line, followed by empty line

Start:	LDWA	42,i
.export Start
		ROLA
		STWA	0,d
		@ASLA3
		.ORG	0x0100
		.ASCII	"Hello, world!\n\0"
		FOO
		.SECTION "test", "rwx"
		BR		Start