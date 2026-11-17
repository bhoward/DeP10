package edu.depauw.dep10.simulator2;

/**
 * Represents a memory image, such as a collection of sections with various
 * properties to be loaded at various locations in memory.
 */
public interface Image {
    /**
     * Store the contents of this image in the given memory device.
     * 
     * @param memory
     */
    void loadInto(MemoryDevice memory);
}
