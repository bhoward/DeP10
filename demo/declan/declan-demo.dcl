CONST three = 3; seven = 7;
VAR answer : INTEGER;
PROCEDURE gcd(a, b: INTEGER; VAR result: INTEGER);
  VAR m, n : INTEGER;
  BEGIN
    m := a;
    n := b;
    WHILE m # n DO
      IF m > n THEN m := m - n ELSE n := n - m END
    END;
    result := m
  END gcd;
PROCEDURE fact(n: INTEGER; VAR factn: INTEGER);
  VAR factnm1 : INTEGER;
  BEGIN
    IF n = 0 THEN factn := 1
    ELSE fact(n - 1, factnm1); factn := n * factnm1;
    END
  END fact;
PROCEDURE mulTable(lo, hi: INTEGER);
  VAR i, j : INTEGER;
  BEGIN
    FOR i := lo TO hi DO
      FOR j := lo TO hi DO
        WriteInt(i * j);
      END;
      WriteLn()
    END
  END mulTable;
PROCEDURE divTable(lo, hi: INTEGER);
  VAR i, j : INTEGER;
  BEGIN
    FOR i := lo TO hi DO
      FOR j := lo TO hi DO
        WriteInt(i DIV j);
      END;
      WriteLn()
    END
  END divTable;
PROCEDURE modTable(lo, hi: INTEGER);
  VAR i, j : INTEGER;
  BEGIN
    FOR i := lo TO hi DO
      FOR j := lo TO hi DO
        WriteInt(i MOD j);
      END;
      WriteLn()
    END
  END modTable;
BEGIN
  fact(three, answer);
  gcd(answer, seven, answer);
  answer := three * seven * (answer + answer);
  WriteInt(answer);
  WriteLn();
  mulTable(-5, 5);
  WriteLn();
  divTable(-5, 5);
  WriteLn();
  modTable(-5, 5);
  WriteLn()
END. (* Don't forget the ending period! *)