# Page Faults

A demand paging exercise for the fancy machine. A small kernel runs one user program with address
translation turned on. The program stores to three virtual pages that have no frame yet, and then to a page
that does not belong to it:

| Step | What happens | Handler must |
|---|---|---|
| store to `0x0400`, `0x0500`, `0x0600` | page not present, trap with operand 3 | give the page the next free frame, print a line, `SRET` (the store runs again) |
| add the three values and print `6` | no more faults | nothing |
| store to `0x0900` | page 9 is not in the process's range (pages 4 to 6) | print `Process killed: bad access at 0x0900`, go on to the next process |

Students complete `onFault` in `vm_starter.pep` (between the `STUDENT CODE` markers). `vm_solution.pep` is the
instructor version, and its output is

    Page fault at 0x0400, frame 0x0020
    Page fault at 0x0500, frame 0x0021
    Page fault at 0x0600, frame 0x0022
    6
    Process killed: bad access at 0x0900
    All programs finished

This needs the fancy machine:

    run --fancy --bm <object file>

## The hardware

The page table is 256 words, one per virtual page. In each word the high byte is the physical frame number
and the low two bits are the protection bits: `00` not present, `10` read only, `11` read and write, `01`
read and execute. The word at `0xFFF3` is the base register. An even value points to a stage 1 protection
table (64 bytes, protection only); an **odd** value selects translation, and the table is at the value minus 1.
Supervisor mode is never translated, and page `0xFF` is never open to user programs. A page with bits `00`
traps with operand 3 (data) or 4 (instruction fetch) and the **virtual** address in the word at `0xFFF1`; the
instruction is undone first, so `SRET` retries it.

`VmExerciseTest` checks the solution output and that the data really lands in the new frames, that the
starter runs with only a placeholder, and that the two files differ only inside the student block.
