package edu.depauw.declan.ast;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import edu.depauw.declan.Type;

public abstract class Scope {
    private Map<String, VarInfo> variables;
    private Map<String, Type> types;
    private Program parent;
    private int slotNumber;

    public Scope() {
        this.variables = new HashMap<>();
        this.types = new HashMap<>();
        this.parent = null;
        this.slotNumber = 0;
    }

    public void setParent(Program parent) {
        this.parent = parent;
    }

    public int getNumberOfSlots() {
        return slotNumber;
    }

    public void add(String name, VarInfo info) {
        info.setSlot(slotNumber);
        slotNumber += info.width();
        variables.put(name, info);
    }

    public VarInfo lookup(String name) {
        if (variables.containsKey(name)) {
            return variables.get(name);
        } else if (parent != null) {
            return parent.lookup(name);
        } else {
            return null;
        }
    }

    public void addType(String name, Type type) {
        types.put(name, type);
    }

    public Type lookupType(String name) {
        if (types.containsKey(name)) {
            return types.get(name);
        } else if (parent != null) {
            return parent.lookupType(name);
        } else {
            return null;
        }
    }

    public boolean containsType(String name) {
        return types.containsKey(name);
    }

    public boolean contains(String name) {
        return variables.containsKey(name);
    }

    public void printSymbolTable(PrintStream out) {
        for (var entry : variables.entrySet()) {
            out.println(entry.getKey() + ": " + entry.getValue());
        }
    }

    public Procedure getProc(String name) {
        if (parent != null) {
            return parent.getProc(name);
        } else {
            Program program = (Program) this;
            return program.lookupProc(name);
        }
    }

    public Set<Map.Entry<String, VarInfo>> entries() {
        return variables.entrySet();
    }
}
