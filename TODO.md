dep10 asm [OPTIONS] source
dep10 run [OPTIONS] object

OPTIONS allow specifying an OS other than default, and whether/where to produce
object, listing, error, dump files

.ORG affects current .SECTION
OS sections pack to top (absent .ORG), user (default?) section starts at 0000 (no .ORG allowed)
make object format compatible with Pepp for user code, but extend to handle OS sections? (instead of ELF)
Or, make specific OS object format (or just always assemble the OS, as in Pepp)?
Determine what the section flags do.

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
  