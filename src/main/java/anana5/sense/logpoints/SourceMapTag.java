package anana5.sense.logpoints;

import java.io.Serializable;

import soot.tagkit.AttributeValueException;
import soot.tagkit.Tag;

public class SourceMapTag implements Tag, Serializable {
    private final String name;
    private final String file;
    private final int  line;

    public SourceMapTag(String name, String file, int line) {
        this.name = name;
        this.file = file;
        this.line = line;
    }

    @Override
    public String getName() {
        return "SourceMapTag";
    }

    @Override
    public byte[] getValue() throws AttributeValueException {
        return toString().getBytes();
    }

    public String method() {
        return name;
    }

    public String source() {
        return file;
    }

    public int line() {
        return line;
    }

    @Override
    public String toString() {
        return String.format("%s(%s:%d)", name, file, line);
    }
    
}
