/*
 * This file is part of WoodPilings, licensed under the MIT License (MIT).
 *
 * Copyright (c) TechShroom Studios <https://techshroom.com>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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