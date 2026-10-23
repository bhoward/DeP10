CONST pi = 3.14159265;
VAR x, y: REAL;
  i, j: INTEGER;
BEGIN
  x := 355;
  x := x / 113;
  WriteReal(x);
  WriteReal(pi);
  WriteLn();
  
  Round(x, i);
  WriteInt(i);
  Round(32767, i);
  WriteInt(i);
  WriteLn();

  FOR i := -5 TO 5 DO
    x := i;
    FOR j := -5 TO 5 DO
      y := j;
      IF x < y THEN WriteInt(1) ELSE WriteInt(0) END
    END;
    WriteLn();
  END
END.