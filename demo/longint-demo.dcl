(* LONGINT demo: factorials and a Fibonacci number past 16 bits.
   Each line shows n and then n! (WriteLong prints a LONGINT, or an INTEGER widened to one).
   The last line is Fibonacci(45), which does not fit in 16 bits either. *)
VAR f, a, b, t : LONGINT; i : INTEGER;

BEGIN
  f := 1;
  FOR i := 1 TO 12 DO
    f := f * i;
    WriteLong(i);
    WriteLong(f);
    WriteLn()
  END;
  a := 0;
  b := 1;
  FOR i := 1 TO 45 DO
    t := a + b;
    a := b;
    b := t
  END;
  WriteLong(a);
  WriteLn()
END.
