# Two Threads, One Printer: preemption exercise

Files for an exercise on code that is not safe to interrupt.

- `race_starter.pep` is what students receive. `printLong` and `uPrintLong`
  keep their working variables in static storage (`.BLOCK`), so they only work
  if nothing interrupts them. Students rewrite the two routines between the
  `STUDENT CODE` markers to use a stack frame.
- `race_solution.pep` is the instructor solution. Do not distribute it.

Two threads each print five 32-bit numbers, one per line. A timer interrupt
switches threads every `quantum` user instructions (`quantum:` near the top of
the file), so a switch can land in the middle of `printLong`.

This needs the fancy machine (user mode and the timer at `0xFFF5`):

    run --fancy --bm <object file>

Each thread writes its characters into its own buffer instead of straight to
the console, so the two threads' output cannot interleave. When both threads
have finished, the kernel prints each buffer followed by a verdict, for example:

    Thread 1 wrote:
    2147483647
    -2147483648
    0
    1234567
    -90
    Thread 1: OK

`Thread N: WRONG` means that thread's buffer differs from what it should have
printed. Thread 1 prints `2147483647`, `-2147483648`, `0`, `1234567`, `-90`;
thread 2 prints `100000`, `-1`, `2000000000`, `7`, `-305419896`.

With the starter, a quantum of 0 (no timer) reports OK for both threads, and so
do some other quanta; many quanta report WRONG because both threads share the
same digit buffer and counter. Fix `printLong` so that every quantum reports OK.

`RaceExerciseTest` checks that the starter reports OK with no timer, that it reports
WRONG for many quanta, that the solution is correct for 38 quanta between 1 and 4001,
and that the two files differ only inside the student block.
