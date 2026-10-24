# Heap Allocator: first-fit allocator exercise

Files for the heap allocator exercise (from Jess's project outline).

- `heap_starter.pep` is what students receive. `HInit` and `HAlloc` are
  written; students write `HFree` between the `STUDENT CODE` markers. As given,
  `HFree` does nothing, so the driver's fourth request fails (`p4=0`).
- `heap_solution.pep` is the instructor solution. Do not distribute it.

The heap is 64 bytes. Each block has a 2-byte header holding its payload size
(even) with the low bit set when the block is allocated. `HAlloc` (A = bytes,
returns X = payload address or 0) takes the first free block that is large
enough, rounds requests up to an even size (minimum 2), and splits the block
when at least 4 bytes would be left over. `HFree` (X = payload address) must
make the block free again.

Output of the driver with the solution: `p1=2`, `p2=14`, `p3=36`, `p4=14`
(offsets from the start of the heap; `p4` reuses `p2`'s space).

Extension for students: have `HFree` merge the freed block with the free block
that follows it. This is not tested.

`HeapAllocatorTest` checks both files, runs the solution's library with other
drivers (layout, rounding, running out of memory, first fit, splitting, exact
fit), and checks that the files differ only inside the student block.
