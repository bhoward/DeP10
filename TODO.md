TODO
-------
* Settings -- allow choice of version/extensions?
* Debugging view: option to skip system code? skip over CALL/SCALL?
* Others from Pepp: watch expressions, stack trace, byte converter
* New syntax for macros
* Tests!

MMU Thoughts
-------
* Minimum: protect some regions (block size 256B? 4K?) as system-only, read-only, executable
* Next: allow per-process virtual-mode maps (v-block to p-block); system (real mode) sees full 64K address space, but user processes might have their text/data/stack segments mapped
* Could allow multiple processes to map shared read-only segments (text/data); have to be at same location across processes, because code and data contain absolute addresses
* Full: blocks might be swapped out to external storage (e.g., a 1MB "swap file" is enough for 16 simultaneous processes -- reasonable limit for process table?); in real mode, system can look at the blocks currently in memory slots (perhaps up to 56K worth, if system code can fit in 8K?) -- need way for system to use MMU mapping, e.g. for accessing data passed in a system call
* Need protection bits, plus loaded/dirty flags; stick with blocks, or allow a segment made up of multiple blocks? Hope to fit MMU table into no more than 256 words per process

Additional Ideas
-------
* Have a notion of "zero page" -- access bottom (maybe top, shadowing the system's I/O page?) 256 bytes (words?) of memory as pseudo-registers with one-byte operands. Enables interesting register assignment algorithms for compilers
* Include A, X, H, and others (PC? SP? Flags?? Constant zero??) as some of the 256 pseudo-registers?
* Have a fairly complete set of "register-register" ops in an extension table; RISC-like?
* Simulate a floating-point unit as one of the hardware devices
* Real-time clock, random number generator, network adapter??
* Simulate access to external storage, for a file system (OS might not fit in 64K anymore...)
* Relative jump instructions, for relocatable code? How do they handle jumps from outside?

Controls:
-------
[not assembled] build -> [assembled] (et seq)
Build xRun xDebug xStep xBack

[assembled] run -> [run], debug -> [debug]
Build Run Debug xStep xBack

[run] end -> [assembled] (et seq), halt -> [assembled] -- PlainController
Build End xDebug xStep xBack

[debug] pause -> [paused], halt -> [paused] -- TraceController
Build End Pause xStep xBack

[paused] resume -> [debug], step -> [paused], back -> [paused] -- SingleStepController
Build End Resume Step Back

Document the CLI options
-------
dep10 asm [OPTIONS] source
dep10 run [OPTIONS] object

OPTIONS allow specifying an OS other than default, and whether/where to produce
object, listing, error, dump files

.ORG affects current .SECTION
OS sections pack to top (absent .ORG), user (default?) section starts at 0000 (no .ORG allowed?)
make object format compatible with Pepp for user code, but extend to handle OS sections? (instead of ELF)
Or, make specific OS object format (or just always assemble the OS, as in Pepp)?
Determine what the section flags do.

Use --sys flag

-------
Assemble stuff
Usage: pepp asm [OPTIONS] user

Positionals:
  user TEXT REQUIRED

Options:
  -h,--help                   Display this help message and exit.
  --os TEXT Excludes: --bm
  --bm Excludes: --os         Use bare metal OS.
  --elf TEXT
  -o TEXT
  -e TEXT
  --os-listing TEXT
  --md,--macro-dir TEXT ...
  -s TEXT REQUIRED
-------
Run ISA3 programs
Usage: pepp run [OPTIONS] obj

Positionals:
  obj TEXT REQUIRED

Options:
  -h,--help                   Display this help message and exit.
  -i,--charIn TEXT            File whose contents are to be buffered behind charIn. The value `-` will cause charIn to be taken from stdin. When using `-`, failure to provide stdin will cause program to freeze.
  -o,--charOut TEXT [-]       File to which the contents of charOut will be written. The value `-` specifies stdout
  --mem-dump TEXT             File to which post-simulation memory-dump will be written.
  -s TEXT REQUIRED
  -m,--max UINT [125000]      Maximum number of instructions that will be executed before terminating simulator.
  --os TEXT Excludes: --bm    File from which os will be read.
  --bm Excludes: --os         Use bare metal OS.
  