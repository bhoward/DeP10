       BR main

.INCLUDELIB "float16"

main:  LDWA 0x4500,i ; 1.01 * 2^2 = 5
       STWA -2,s
       LDWA 0x4200,i ; 1.1 * 2^1 = 3
       STWA -4,s
       SUBSP 4,i
       CALL FDiv
       ADDSP 4,i
       SUBSP 2,i
       CALL FPrint
       ADDSP 2,i
       @CHARO '\n',i
       @HEXO -2,s ; should print 3EAA = 1.1010101010 * 2^0 = 1.666...
       @CHARO '\n',i

       LDWA 0x4400,i ; 1.0 * 2^2 = 4
       STWA -2,s
       LDWA 0xC200,i ; -1.1 * 2^1 = -3
       STWA -4,s
       SUBSP 4,i
       CALL FMul
       ADDSP 4,i
       SUBSP 2,i
       CALL FPrint
       ADDSP 2,i
       @CHARO '\n',i
       @HEXO -2,s    ; should print CA00 = -1.1 * 2^3 = -12
       @CHARO '\n',i
       
       LDWA 0x4400,i ; 1.0 * 2^2
       STWA -2,s
       LDWA 0xC200,i ; -1.1 * 2^1
       STWA -4,s
       SUBSP 4,i
       CALL FAdd
       ADDSP 4,i
       SUBSP 2,i
       CALL FPrint
       ADDSP 2,i
       @CHARO '\n',i
       @HEXO -2,s    ; should print 3C00 = 1.0 * 2^0 = 1
       @CHARO '\n',i
       
       LDWA 0x4400,i ; 1.0 * 2^2
       STWA -2,s
       LDWA 0x4200,i ; 1.1 * 2^1
       STWA -4,s
       SUBSP 4,i
       CALL FAdd
       ADDSP 4,i
       SUBSP 2,i
       CALL FPrint
       ADDSP 2,i
       @CHARO '\n',i
       @HEXO -2,s    ; should print 4700 = 1.11 * 2^2 = 7
       @CHARO '\n',i
       
       LDWA 0x7C00,i ; +Infinity
       STWA -2,s
       LDWA 0x7C00,i ; +Infinity
       STWA -4,s
       SUBSP 4,i
       CALL FAdd
       ADDSP 4,i
       SUBSP 2,i
       CALL FPrint
       ADDSP 2,i
       @CHARO '\n',i
       @HEXO -2,s    ; should print 7C00 = +Infinity
       @CHARO '\n',i
       
       LDWA 0x7C00,i ; +Infinity
       STWA -2,s
       LDWA 0xFC00,i ; -Infinity
       STWA -4,s
       SUBSP 4,i
       CALL FAdd
       ADDSP 4,i
       SUBSP 2,i
       CALL FPrint
       ADDSP 2,i
       @CHARO '\n',i
       @HEXO -2,s    ; should print 7FFF = NaN
       @CHARO '\n',i
       
       LDWA 0xBA00,i ; -1.1*2^-1 = -0.75
       STWA -2,s
       SUBSP 2,i
       CALL FPrint
       ADDSP 2,i
       @CHARO '\n',i

       LDWA 0x3E00,i ; 1.1*2^0 = 1.5
       STWA -2,s
       SUBSP 2,i
       CALL FPrint
       ADDSP 2,i
       @CHARO '\n',i

       LDWA 0x0000,i ; 0
       STWA -2,s
       SUBSP 2,i
       CALL FPrint
       ADDSP 2,i
       @CHARO '\n',i

       LDWA 0x0001,i ; 0.0000000001*2^-14 = 1*2^-24 = smallest possible (0.000000059604644775390625)
       STWA -2,s
       SUBSP 2,i
       CALL FPrint
       ADDSP 2,i
       @CHARO '\n',i

       LDWA 0x0002,i ; 0.000000001*2^-14 = second smallest possible (0.00000011920928955078125)
       STWA -2,s
       SUBSP 2,i
       CALL FPrint
       ADDSP 2,i
       @CHARO '\n',i

       LDWA 0x03FF,i ; 0.1111111111*2^-14 = largest denormalized (0.000060975551605224609375)
       STWA -2,s
       SUBSP 2,i
       CALL FPrint
       ADDSP 2,i
       @CHARO '\n',i

       LDWA 0x0400,i ; 1.0*2^-14 = smallest normal (0.00006103515625)
       STWA -2,s
       SUBSP 2,i
       CALL FPrint
       ADDSP 2,i
       @CHARO '\n',i

       LDWA 0x7BFF,i ; 1.1111111111*2^15 = largest normal (65504)
       STWA -2,s
       SUBSP 2,i
       CALL FPrint
       ADDSP 2,i
       @CHARO '\n',i

       LDWA 0x67FF,i ; 1.1111111111*2^10 = 2047
       STWA -2,s
       SUBSP 2,i
       CALL FPrint
       ADDSP 2,i
       @CHARO '\n',i

       LDWA 0x6800,i ; 1.0*2^11 = 2048
       STWA -2,s
       SUBSP 2,i
       CALL FPrint
       ADDSP 2,i
       @CHARO '\n',i

       LDWA 0x6801,i ; 1.0000000001*2^11 = 2050
       STWA -2,s
       SUBSP 2,i
       CALL FPrint
       ADDSP 2,i
       @CHARO '\n',i

       LDWA 0x7C00,i ; +Infinity
       STWA -2,s
       SUBSP 2,i
       CALL FToI
       ADDSP 2,i
       @DECO -2,s
       @CHARO '\n',i

       LDWA 0x7C01,i ; +NaN
       STWA -2,s
       SUBSP 2,i
       CALL FToI
       ADDSP 2,i
       @DECO -2,s
       @CHARO '\n',i

       LDWA 0x0400,i ; 1.0*2^-14 = smallest normal (0.00006103515625)
       STWA -2,s
       SUBSP 2,i
       CALL FToI
       ADDSP 2,i
       @DECO -2,s
       @CHARO '\n',i

       LDWA 0x7BFF,i ; 1.1111111111*2^15 = largest normal (65504)
       STWA -2,s
       SUBSP 2,i
       CALL FToI
       ADDSP 2,i
       @DECO -2,s
       @CHARO '\n',i

       LDWA 0xE800,i ; -1.0*2^11 = -2048
       STWA -2,s
       SUBSP 2,i
       CALL FToI
       ADDSP 2,i
       @DECO -2,s
       @CHARO '\n',i

       LDWA 0x6801,i ; 1.0000000001*2^11 = 2050
       STWA -2,s
       SUBSP 2,i
       CALL FToI
       ADDSP 2,i
       @DECO -2,s
       @CHARO '\n',i

       LDWA 0,i
       STWA -2,s
       SUBSP 2,i
       CALL IToF
       CALL FPrint
       ADDSP 2,i
       @CHARO '\n',i
       
       LDWA 1,i
       STWA -2,s
       SUBSP 2,i
       CALL IToF
       CALL FPrint
       ADDSP 2,i
       @CHARO '\n',i
       
       LDWA -1,i
       STWA -2,s
       SUBSP 2,i
       CALL IToF
       CALL FPrint
       ADDSP 2,i
       @CHARO '\n',i
       
       LDWA 1023,i
       STWA -2,s
       SUBSP 2,i
       CALL IToF
       CALL FPrint
       ADDSP 2,i
       @CHARO '\n',i
       
       LDWA -1023,i
       STWA -2,s
       SUBSP 2,i
       CALL IToF
       CALL FPrint
       ADDSP 2,i
       @CHARO '\n',i
       
       LDWA 1024,i
       STWA -2,s
       SUBSP 2,i
       CALL IToF
       CALL FPrint
       ADDSP 2,i
       @CHARO '\n',i
       
       LDWA -1024,i
       STWA -2,s
       SUBSP 2,i
       CALL IToF
       CALL FPrint
       ADDSP 2,i
       @CHARO '\n',i
       
       LDWA 32767,i
       STWA -2,s
       SUBSP 2,i
       CALL IToF
       CALL FPrint
       ADDSP 2,i
       @CHARO '\n',i
       
       LDWA -32767,i
       STWA -2,s
       SUBSP 2,i
       CALL IToF
       CALL FPrint
       ADDSP 2,i
       @CHARO '\n',i
       
       LDWA -32768,i
       STWA -2,s
       SUBSP 2,i
       CALL IToF
       CALL FPrint
       ADDSP 2,i
       @CHARO '\n',i
       
       RET
