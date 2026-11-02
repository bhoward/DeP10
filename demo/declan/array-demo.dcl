(* The sieve of Eratosthenes: print the primes below 100 *)
CONST n = 100;
TYPE Flags = ARRAY n OF BOOLEAN;
VAR composite : Flags; i, j : INTEGER;

BEGIN
  FOR i := 2 TO n - 1 DO
    IF ~composite[i] THEN
      WriteLong(i);
      j := i * i;
      WHILE j < n DO
        composite[j] := TRUE;
        j := j + i
      END
    END
  END;
  WriteLn()
END.
