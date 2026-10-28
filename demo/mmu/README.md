# Protection Faults

A memory protection exercise for the fancy machine. A small kernel runs three user programs, each with
its own protection table:

| Program | What it does | Result |
|---|---|---|
| A | updates its own data and prints `A` | runs to the end |
| B | prints `B`, then writes to page 5 (A's data) | protection fault, operand 3 |
| C | prints `C`, then jumps into page 4 (data) | protection fault, operand 4 |

The students complete `onFault` in `mmu_starter.pep` (between the `STUDENT CODE` markers) so that the
kernel reports each fault, with its address, and goes on to the next program. `mmu_solution.pep` is the
instructor version.

This needs the fancy machine:

    run --fancy --bm <object file>

The memory protection table has two bits per 256-byte page (read, write): `00` no access, `10` read only,
`11` read and write, `01` read and execute (code). The word at `0xFFF3` points to the table (0 = off),
supervisor mode ignores it, and page `0xFF` is never open to user programs. A fault undoes the
instruction and traps with opcode byte 0 and operand 3 (read or write) or 4 (instruction fetch); the
address is in the word at `0xFFF1`.

`MmuExerciseTest` checks the solution output, that the starter runs all three programs without a real
report, and that the two files differ only inside the student block.
