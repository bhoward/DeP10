# Address Spaces

A translation exercise for the fancy machine. A small kernel runs two user processes, P and Q. Both store a
value at the virtual address `0x0400`, yield with `SCALL 2`, and then read `0x0400` back and print it:

| Process | Stores | Expected output |
|---|---|---|
| P | 1 | `P1` |
| Q | 2 | `Q2` |

Each process has its own page table, `tabP` and `tabQ`. Virtual page 4 maps to frame `0x20` in `tabP` and to
frame `0x21` in `tabQ`, so the two `0x0400`s are different words. Pages 0 (code) and 7 (stacks) map to the same
frames in both tables.

Students complete the code between the `STUDENT CODE` markers in `vm2_starter.pep`: before the kernel resumes
a process it must load that process's table address, add 1 (the odd value turns translation on) and store it
in the base register at `0xFFF3`. Without it there is no translation, both processes use the same physical
memory and P prints `P2`. `vm2_solution.pep` is the instructor version.

This needs the fancy machine:

    run --fancy --bm <object file>

The kernel switches processes by pointing the stack pointer at the other process's saved trap frame and
executing `SRET`; the frames never move. The same switch is the one in the time-sharing exercise, so this
exercise fits after it.

`AddressSpacesTest` checks the solution output and the frames, the starter's wrong output, and that the two
files differ only inside the student block.
