# DeP10
Exploring extensions of Pep/10 to support OS and Compilers courses at DePauw

## Building

Uses JDK 21. Update the version in `pom.xml`, and then with Maven, run `mvn package` to produce
`dep10-<VERSION>-jar-with-dependencies.jar` in the `target` directory. Rename this `dep10.jar` to match
usage below.

## Running

### GUI

Double-click on the jar, or run `java -jar dep10.jar --gui`

### Command Line

Given the following source in `test.pep`:

```
     @STRO  msg,d
     @CHARO 42,i
     @DECO  42,i
     @CHARO '\n',i
     RET
msg: .ASCII "Hello\n\0"
```

Assemble it with (second version also produces listing and error files):

```
java -jar dep10.jar asm test.pep -o test.pepo
java -jar dep10.jar asm test.pep -o test.pepo -l test.pepl -e test.pepe --os-listing
```

Run it with (second version also produces output, error, dump, and tracing files):

```
java -jar dep10.jar run test.pepo
java -jar dep10.jar run test.pepo -o test.out -e test.err -d test.pepd -t test.pept
```


## The fancy machine (operating systems projects)

The fancy machine is an optional mode of the simulator for operating systems courses. It is off by
default, so programs written for the textbook Pep/10 run unchanged. Turn it on with the **Fancy machine**
checkbox in the GUI toolbar (it applies to the next run or debug), or with `--fancy` on the command line:

```
java -jar dep10.jar run --fancy --bm exercise.pepo
```

Programs that bring their own operating system (the exercises below) assemble as System Code and run
with `--bm` (bare metal), because they provide their own trap handler.

What it adds:

| Feature | Where | Notes |
|---|---|---|
| Supervisor and user modes | P flag, bit 7 of the flags byte | The machine boots in supervisor mode. Every trap sets P and saves the old value in the trap frame; `SRET` restores it and is privileged. `BRP` branches if P is set |
| Timer | word at `0xFFF5` | Counts one per instruction in either mode; 0 = off. At zero it traps (opcode byte 0, operand 1) |
| Invalid instruction trap | opcode byte 0, operand 2 | In user mode only; in supervisor mode the machine stops with a message |
| Memory protection (stage 1) | word at `0xFFF3` (even) | A table of two bits per 256-byte page: `00` none, `10` read, `11` read/write, `01` read and execute. A violation traps with operand 3 (data) or 4 (fetch) and the address in `0xFFF1` |
| Address translation (stage 2) | word at `0xFFF3` (odd) | The table address plus one. 256 words, one per page: high byte = frame number, low two bits = the protection bits above, `00` = not present |

In the GUI state panel the **Mode**, **MMU**, **Fault** and **Timer** boxes show these registers while the
fancy machine is on. The full description is in the Pep/10 reference under "Fancy Machine".

### Exercises

Each folder in `demo` has a starter file for students, an instructor solution, a README and automated tests
(run `mvn test`). Students complete the code between the `STUDENT CODE` markers.

| Folder | Topic |
|---|---|
| `kernel` | Time-sharing: a cooperative kernel and its context switch |
| `race` | Two Threads, One Printer: making a routine safe under preemption |
| `heap` | A first-fit heap allocator: freeing a block |
| `prodcons` | Producer and consumer with semaphores (demonstration) |
| `mmu` | Protection Faults: a handler for memory protection faults |
| `vm` | Page Faults: demand paging with address translation |
| `vm2` | Address Spaces: two processes, two page tables |
