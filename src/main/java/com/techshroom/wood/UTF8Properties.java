package com.techshroom.wood;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Similar to {@link Properties}, but encoded in UTF-8 by default. It also
 * supports attaching javadoc-like comments to each entry.
 */
public final class UTF8Properties extends ConcurrentHashMap<String, String> {

    private static final long serialVersionUID = -5306083496118278080L;
    private final Map<String, String> javadoc = new ConcurrentHashMap<>();

    private static final class PropertiesReader {

        private enum State {
            DEFAULT, READING_KEY, READING_VALUE, AWAITING_COMMENT, AWAITING_JAVADOC, READING_JAVADOC,
            POTENTIAL_CLOSE_JAVADOC;
        }

        private final UTF8Properties readInto;
        private final StringBuilder strBuilder = new StringBuilder();
        private State state = State.DEFAULT;
        private String currentJavadoc;
        private String currentKey;

        private PropertiesReader(UTF8Properties readInto) {
            this.readInto = readInto;
        }

        private void readProperties(Reader reader) throws IOException {
            this.state = State.DEFAULT;
            CPLineReader cpIn = new CPLineReader(reader);
            int cp;
            while ((cp = cpIn.nextCodePoint()) != -1) {
                switch (this.state) {
                    case DEFAULT:
                        if (cp == '/') {
                            this.state = State.AWAITING_COMMENT;
                        } else if (!Character.isJavaIdentifierStart(cp)) {
                            throw new IllegalStateException(cpIn.formatError(
                                    String.valueOf(Character.toChars(cp)) + " is an invalid key starting character"));
                        } else {
                            this.state = State.READING_KEY;
                            this.strBuilder.setLength(0);
                            this.strBuilder.append(Character.toChars(cp));
                        }
                        break;
                    case READING_KEY:
                        if (cp == '=') {
                            this.currentKey = this.strBuilder.toString();
                            this.state = State.READING_VALUE;
                            this.strBuilder.setLength(0);
                        } else if (!Character.isJavaIdentifierPart(cp)) {
                            throw new IllegalStateException(cpIn.formatError(
                                    String.valueOf(Character.toChars(cp)) + " is an invalid key character"));
                        } else {
                            this.strBuilder.append(Character.toChars(cp));
                        }
                        break;
                    case READING_VALUE:
                        if (cp == '\n') {
                            insertValue();
                        } else {
                            this.strBuilder.append(Character.toChars(cp));
                        }
                        break;
                    case AWAITING_COMMENT:
                        if (cp == '*') {
                            this.state = State.AWAITING_JAVADOC;
                        } else if (cp == '/') {
                            // A double-slash single-line comment
                            // Read until \n or EOF
                            while ((cp = cpIn.nextCodePoint()) != -1 && cp != '\n') {
                            }
                            this.state = State.DEFAULT;
                        } else {
                            // Invalid, we don't support / as a key.
                            throw new IllegalStateException(cpIn.formatError("Invalid comment"));
                        }
                        break;
                    case AWAITING_JAVADOC:
                        // This state corresponds to potential javadoc comments
                        // We just ignore /* */ style comments, but we retain
                        // javadoc
                        if (cp == '*') {
                            this.state = State.READING_JAVADOC;
                            this.strBuilder.setLength(0);
                        } else {
                            // Regular comment, discard until "*/"
                            boolean seenStar = cp == '*';
                            while ((cp = cpIn.nextCodePoint()) != -1) {
                                if (cp == '*') {
                                    seenStar = true;
                                } else if (cp == '/' && seenStar) {
                                    // "*/" matched
                                    this.state = State.DEFAULT;
                                    break;
                                } else {
                                    seenStar = false;
                                }
                            }
                            checkState(cp != -1, cpIn.errorFormatString(), "missing \"*/\" for multi-line comment");
                        }
                        break;
                    case READING_JAVADOC:
                        if (cp == '*') {
                            this.state = State.POTENTIAL_CLOSE_JAVADOC;
                        } else {
                            this.strBuilder.append(Character.toChars(cp));
                        }
                        break;
                    case POTENTIAL_CLOSE_JAVADOC:
                        if (cp == '/') {
                            // Javadoc closed.
                            this.currentJavadoc = this.strBuilder.toString();
                            this.state = State.DEFAULT;
                        } else {
                            // Javadoc didn't close, just a regular *
                            this.strBuilder.append('*').append(Character.toChars(cp));
                            this.state = State.READING_JAVADOC;
                        }
                        break;
                    default:
                        throw new IllegalStateException("Unhandled state " + this.state);
                }
            }
            if (this.currentKey != null && this.state == State.READING_VALUE) {
                insertValue();
            }
            checkState(!inInvalidState(), cpIn.errorFormatString(), "invalid properties file");
        }

        private void insertValue() {
            checkNotNull(this.currentKey, "impossible: null key");
            if (this.currentJavadoc != null) {
                this.readInto.javadoc.put(this.currentKey, this.currentJavadoc);
            }
            this.readInto.put(this.currentKey, this.strBuilder.toString());
            this.state = State.DEFAULT;
        }

        private boolean inInvalidState() {
            return this.state != State.DEFAULT;
        }

    }

    private final PropertiesReader reader = new PropertiesReader(this);

    /**
     * Loads properties from the given stream using UTF-8 character decoding.
     * 
     * @param stream
     *            - The input stream, will not be closed
     * @throws IOException
     */
    public UTF8Properties load(InputStream stream) throws IOException {
        load(new InputStreamReader(stream));
        return this;
    }

    /**
     * Loads properties from the given reader.
     * 
     * @param reader
     *            - The reader, will not be closed
     * @throws IOException
     */
    public UTF8Properties load(Reader reader) throws IOException {
        // sync over this.reader since it uses non-thread-safe state
        synchronized (this.reader) {
            this.reader.readProperties(reader);
        }
        return this;
    }

}
