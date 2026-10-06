(* LONGINT demo: factorials and Fibonacci numbers past 16 bits.
   Prints each digit with WriteInt, so digits come out separated by spaces.
   writeLong handles non-negative values only. *)
VAR f, a, b, t : LONGINT; i : INTEGER;

PROCEDURE writeLong(v : LONGINT);
  VAR p, ten : LONGINT; d, k, started : INTEGER;
  BEGIN
    ten := 10;
    p := 10000;
    p := p * p;
    p := p * ten;
    started := 0;
    FOR k := 1 TO 10 DO
      d := 0;
      WHILE v >= p DO
        v := v - p;
        d := d + 1
      END;
      IF (started = 1) OR (d > 0) OR (k = 10) THEN
        WriteInt(d);
        started := 1
      END;
      p := p DIV ten
    END
  END writeLong;

BEGIN
  f := 1;
  FOR i := 1 TO 12 DO
    f := f * i;
    writeLong(f);
    WriteLn()
  END;
  a := 0;
  b := 1;
  FOR i := 1 TO 45 DO
    t := a + b;
    a := b;
    b := t
  END;
  writeLong(a);
  WriteLn()
END.
