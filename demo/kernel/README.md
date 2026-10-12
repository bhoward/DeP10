# Taking Turns on One CPU: starter kernel

Files for the time-sharing kernel exercise.

- `timeshare_starter.pep` is what students receive. They complete the block
  between the `STUDENT CODE` markers in the YIELD handler.
- `timeshare_solution.pep` is the instructor solution. Do not distribute it.

Run either file in the DeP10 simulator (bare metal mode). With the starter as
given the programs run one after the other (`A1 A2 A3 B1 B2 B3`); with YIELD
completed they take turns (`A1 B1 A2 B2 A3 B3`). A `!` in the output means a
program found its H register changed by a switch.

System call numbers: `SCALL 0` is YIELD, `SCALL 1` is EXIT. Any other number
stops the machine and leaves `0x0BAD` at address `0xF004`.

`TimeSharingKernelTest` checks both files, including that they differ only
inside the student block.
