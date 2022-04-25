package anana5.sense.logpoints;

import java.io.Serializable;

import soot.tagkit.AttributeValueException;
import soot.tagkit.Tag;

public class SourceMapTag implements Tag, Serializable {

    private final String methodName;
    private final String sourceFile;
    private final int  lineNumber;

    public SourceMapTag(String methodName, String sourceFile, int lineNumber) {
        this.methodName = methodName;
        this.sourceFile = sourceFile;
        this.lineNumber = lineNumber;
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
        return methodName;
    }

    public String source() {
        return sourceFile;
    }

    public int line() {
        return lineNumber;
    }

    @Override
    public String toString() {
        return String.format("%s(%s:%d)", methodName, sourceFile, lineNumber);
    }
    
}
