		BR		Start
		.block	90
Start:	LDWA	42
		ROLA
		STWA	0,d
		.ORG	100
		.ASCII	"Hello, world!\n\0"