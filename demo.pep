		BR		Start
		.block	0x59
; comment line, followed by empy line

Start:	LDWA	42
		ROLA
		STWA	0,d
		.ORG	100
		.ASCII	"Hello, world!\n\0"