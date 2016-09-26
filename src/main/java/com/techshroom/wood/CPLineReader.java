package com.techshroom.wood;

import java.io.IOException;
import java.io.Reader;

/**
 * Code point reader and line number tracker.
 */
final class CPLineReader {

    private final Reader charSource;
    private int lineNumber = 0;
    private int codeUnit;

    public CPLineReader(Reader charSource) throws IOException {
        this.charSource = charSource;
        this.codeUnit = charSource.read();
    }

    public String errorFormatString() {
        return "On line " + this.lineNumber + ": %s";
    }

    public String formatError(String error) {
        return String.format(errorFormatString(), error);
    }

    public int getLineNumber() {
        return this.lineNumber;
    }

    public int nextCodePoint() throws IOException {
        int cp = getNextCodePoint();
        if (cp == '\n') {
            this.lineNumber++;
        }
        return cp;
    }

    private int getNextCodePoint() throws IOException {
        if (this.codeUnit == -1) {
            return -1;
        }
        try {
            char high = (char) this.codeUnit;
            if (Character.isHighSurrogate(high)) {
                int next = this.charSource.read();
                if (next == -1) {
                    throw new IOException("malformed character");
                }
                char low = (char) next;
                if (!Character.isLowSurrogate(low)) {
                    throw new IOException("malformed sequence");
                }
                return Character.toCodePoint(high, low);
            } else {
                return this.codeUnit;
            }
        } finally {
            this.codeUnit = this.charSource.read();
        }
    }
}