package anana5.sense.logpoints;

import javax.naming.directory.AttributeInUseException;

import soot.tagkit.AttributeValueException;
import soot.tagkit.Tag;

public class SourceMapTag implements Tag {

    private String sourceName;
    private int lineNumber;
    private int columnNumber;

    public SourceMapTag(String sourceName, int lineNumber, int columnNumber) {
        this.sourceName = sourceName;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    @Override
    public String getName() {
        return "SourceMapTag";
    }

    @Override
    public byte[] getValue() throws AttributeValueException {
        return toString().getBytes();
    }

    @Override
    public String toString() {
        return sourceName + ":" + lineNumber + ":" + columnNumber;
    }
    
}
