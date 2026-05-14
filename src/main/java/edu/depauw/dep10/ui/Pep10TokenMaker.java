package edu.depauw.dep10.ui;

import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMaker;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;

public class Pep10TokenMaker extends AbstractTokenMaker {
    private static final int SINGLE_QUOTE_ESCAPE = Token.LITERAL_CHAR + 100;
    private static final int DOUBLE_QUOTE_ESCAPE = Token.LITERAL_STRING_DOUBLE_QUOTE + 100;

    // See
    // https://github.com/bobbylight/RSyntaxTextArea/wiki/Adding-Syntax-Highlighting-for-a-new-Language

    /**
     * Returns a list of tokens representing the given text.
     *
     * @param text           The text to break into tokens.
     * @param startTokenType The token with which to start tokenizing.
     * @param startOffset    The offset at which the line of tokens begins.
     * @return A linked list of tokens representing <code>text</code>.
     */
    public Token getTokenList(Segment text, int startTokenType, int startOffset) {
        resetTokenList();

        char[] array = text.array;
        int offset = text.offset;
        int count = text.count;
        int end = offset + count;

        // Token starting offsets are always of the form:
        // 'startOffset + (currentTokenStart-offset)', but since startOffset and
        // offset are constant, tokens' starting positions become:
        // 'newStartOffset+currentTokenStart'.
        int newStartOffset = startOffset - offset;

        int currentTokenStart = offset;
        int currentTokenType = startTokenType;

        for (int i = offset; i < end; i++) {
            char c = array[i];

            switch (currentTokenType) {
            case Token.NULL:
                currentTokenStart = i; // Starting a new token here.

                switch (c) {
                case ' ':
                case '\t':
                case '\n':
                case '\13': // VTAB
                case '\f':
                case '\r':
                    currentTokenType = Token.WHITESPACE;
                    break;

                case '"':
                    currentTokenType = Token.LITERAL_STRING_DOUBLE_QUOTE;
                    break;

                case '\'':
                    currentTokenType = Token.LITERAL_CHAR;
                    break;

                case ';':
                    currentTokenType = Token.COMMENT_EOL;
                    break;

                default:
                    if (RSyntaxUtilities.isDigit(c) || c == '-') {
                        currentTokenType = Token.LITERAL_NUMBER_DECIMAL_INT;
                        break;
                    } else if (RSyntaxUtilities.isLetter(c)) {
                        currentTokenType = Token.IDENTIFIER;
                        break;
                    }

                    // Anything not currently handled - mark as an identifier (includes _@.,)
                    currentTokenType = Token.IDENTIFIER;
                    break;
                } // End of switch (c).

                break;

            case Token.WHITESPACE:
                switch (c) {

                case ' ':
                case '\t':
                case '\n':
                case '\13': // VTAB
                case '\f':
                case '\r':
                    break; // Still whitespace.

                case '"':
                    addToken(text, currentTokenStart, i - 1, Token.WHITESPACE, newStartOffset + currentTokenStart);
                    currentTokenStart = i;
                    currentTokenType = Token.LITERAL_STRING_DOUBLE_QUOTE;
                    break;

                case '\'':
                    addToken(text, currentTokenStart, i - 1, Token.WHITESPACE, newStartOffset + currentTokenStart);
                    currentTokenStart = i;
                    currentTokenType = Token.LITERAL_CHAR;
                    break;

                case ';':
                    addToken(text, currentTokenStart, i - 1, Token.WHITESPACE, newStartOffset + currentTokenStart);
                    currentTokenStart = i;
                    currentTokenType = Token.COMMENT_EOL;
                    break;

                default: // Add the whitespace token and start anew.
                    addToken(text, currentTokenStart, i - 1, Token.WHITESPACE, newStartOffset + currentTokenStart);
                    currentTokenStart = i;

                    if (RSyntaxUtilities.isDigit(c) || c == '-') {
                        currentTokenType = Token.LITERAL_NUMBER_DECIMAL_INT;
                        break;
                    } else if (RSyntaxUtilities.isLetter(c)) {
                        currentTokenType = Token.IDENTIFIER;
                        break;
                    }

                    // Anything not currently handled - mark as identifier
                    currentTokenType = Token.IDENTIFIER;
                } // End of switch (c).

                break;

            default: // Should never happen
            case Token.IDENTIFIER:
                switch (c) {
                case ' ':
                case '\t':
                case '\n':
                case '\13': // VTAB
                case '\f':
                case '\r':
                    addToken(text, currentTokenStart, i - 1, Token.IDENTIFIER, newStartOffset + currentTokenStart);
                    currentTokenStart = i;
                    currentTokenType = Token.WHITESPACE;
                    break;

                case '"':
                    addToken(text, currentTokenStart, i - 1, Token.IDENTIFIER, newStartOffset + currentTokenStart);
                    currentTokenStart = i;
                    currentTokenType = Token.LITERAL_STRING_DOUBLE_QUOTE;
                    break;

                case '\'':
                    addToken(text, currentTokenStart, i - 1, Token.IDENTIFIER, newStartOffset + currentTokenStart);
                    currentTokenStart = i;
                    currentTokenType = Token.LITERAL_CHAR;
                    break;

               default:
                    if (RSyntaxUtilities.isLetterOrDigit(c)) {
                        break; // Still an identifier of some type.
                    }
                    // Otherwise, we're still an identifier (?).
                } // End of switch (c).

                break;

            case Token.LITERAL_NUMBER_DECIMAL_INT:
                switch (c) {
                case ' ':
                case '\t':
                case '\n':
                case '\13': // VTAB
                case '\f':
                case '\r':
                    addToken(text, currentTokenStart, i - 1, Token.LITERAL_NUMBER_DECIMAL_INT,
                            newStartOffset + currentTokenStart);
                    currentTokenStart = i;
                    currentTokenType = Token.WHITESPACE;
                    break;

                case '"':
                    addToken(text, currentTokenStart, i - 1, Token.LITERAL_NUMBER_DECIMAL_INT,
                            newStartOffset + currentTokenStart);
                    currentTokenStart = i;
                    currentTokenType = Token.LITERAL_STRING_DOUBLE_QUOTE;
                    break;

                case '\'':
                    addToken(text, currentTokenStart, i - 1, Token.LITERAL_NUMBER_DECIMAL_INT,
                            newStartOffset + currentTokenStart);
                    currentTokenStart = i;
                    currentTokenType = Token.LITERAL_CHAR;
                    break;

                default:
                    if (RSyntaxUtilities.isHexCharacter(c) || c == 'x' || c == 'X') {
                        break; // Still a literal number (sloppy handling of hex literals...)
                    }

                    // Otherwise, remember this was a number and start over.
                    addToken(text, currentTokenStart, i - 1, Token.LITERAL_NUMBER_DECIMAL_INT,
                            newStartOffset + currentTokenStart);
                    i--;
                    currentTokenType = Token.NULL;
                } // End of switch (c).

                break;

            case Token.COMMENT_EOL:
                i = end - 1;
                addToken(text, currentTokenStart, i, currentTokenType, newStartOffset + currentTokenStart);
                // We need to set token type to null so at the bottom we don't add one more
                // token.
                currentTokenType = Token.NULL;
                break;

            case Token.LITERAL_STRING_DOUBLE_QUOTE:
                if (c == '"') {
                    addToken(text, currentTokenStart, i, Token.LITERAL_STRING_DOUBLE_QUOTE,
                            newStartOffset + currentTokenStart);
                    currentTokenType = Token.NULL;
                } else if (c == '\\') {
                    currentTokenType = DOUBLE_QUOTE_ESCAPE;
                }
                break;

            case Token.LITERAL_CHAR:
                if (c == '\'') {
                    addToken(text, currentTokenStart, i, Token.LITERAL_CHAR,
                            newStartOffset + currentTokenStart);
                    currentTokenType = Token.NULL;
                } else if (c == '\\') {
                    currentTokenType = SINGLE_QUOTE_ESCAPE;
                }
                break;
                
            case DOUBLE_QUOTE_ESCAPE:
                currentTokenType = Token.LITERAL_STRING_DOUBLE_QUOTE;
                break;
                
            case SINGLE_QUOTE_ESCAPE:
                currentTokenType = Token.LITERAL_CHAR;
                break;
            } // End of switch (currentTokenType).
        } // End of for (int i=offset; i<end; i++).

        switch (currentTokenType) {
        // Remember what token type to begin the next line with.
        case Token.LITERAL_STRING_DOUBLE_QUOTE:
            addToken(text, currentTokenStart, end - 1, currentTokenType, newStartOffset + currentTokenStart);
            break;

        // Do nothing if everything was okay.
        case Token.NULL:
            addNullToken();
            break;

        // All other token types don't continue to the next line...
        default:
            addToken(text, currentTokenStart, end - 1, currentTokenType, newStartOffset + currentTokenStart);
            addNullToken();
        }

        // Return the first token in our linked list.
        return firstToken;
    }

    @Override
    public void addToken(Segment segment, int start, int end, int tokenType, int startOffset) {
        // This assumes all keywords, etc. were parsed as "identifiers."
        if (tokenType == Token.IDENTIFIER) {
            int value = wordsToHighlight.get(segment, start, end);
            if (value != -1) {
                tokenType = value;
            }
        }

        super.addToken(segment, start, end, tokenType, startOffset);
    }

    @Override
    public TokenMap getWordsToHighlight() {
        TokenMap tokenMap = new TokenMap();

        tokenMap.put("RET", Token.FUNCTION);
        tokenMap.put("SRET", Token.FUNCTION);
        tokenMap.put("MOVSPA", Token.FUNCTION);
        tokenMap.put("MOVASP", Token.FUNCTION);
        tokenMap.put("MOVFLGA", Token.FUNCTION);
        tokenMap.put("MOVAFLG", Token.FUNCTION);
        tokenMap.put("NOP", Token.FUNCTION);
        tokenMap.put("NEGA", Token.FUNCTION);
        tokenMap.put("NEGX", Token.FUNCTION);
        tokenMap.put("ASLA", Token.FUNCTION);
        tokenMap.put("ASLX", Token.FUNCTION);
        tokenMap.put("ASRA", Token.FUNCTION);
        tokenMap.put("ASRX", Token.FUNCTION);
        tokenMap.put("NOTA", Token.FUNCTION);
        tokenMap.put("NOTX", Token.FUNCTION);
        tokenMap.put("ROLA", Token.FUNCTION);
        tokenMap.put("ROLX", Token.FUNCTION);
        tokenMap.put("RORA", Token.FUNCTION);
        tokenMap.put("RORX", Token.FUNCTION);
        tokenMap.put("BR", Token.FUNCTION);
        tokenMap.put("BRLE", Token.FUNCTION);
        tokenMap.put("BRLT", Token.FUNCTION);
        tokenMap.put("BREQ", Token.FUNCTION);
        tokenMap.put("BRNE", Token.FUNCTION);
        tokenMap.put("BRGE", Token.FUNCTION);
        tokenMap.put("BRGT", Token.FUNCTION);
        tokenMap.put("BRV", Token.FUNCTION);
        tokenMap.put("BRC", Token.FUNCTION);
        tokenMap.put("CALL", Token.FUNCTION);
        tokenMap.put("SCALL", Token.FUNCTION);
        tokenMap.put("LDWA", Token.FUNCTION);
        tokenMap.put("LDWX", Token.FUNCTION);
        tokenMap.put("LDBA", Token.FUNCTION);
        tokenMap.put("LDBX", Token.FUNCTION);
        tokenMap.put("STWA", Token.FUNCTION);
        tokenMap.put("STWX", Token.FUNCTION);
        tokenMap.put("STBA", Token.FUNCTION);
        tokenMap.put("STBX", Token.FUNCTION);
        tokenMap.put("CPWA", Token.FUNCTION);
        tokenMap.put("CPWX", Token.FUNCTION);
        tokenMap.put("CPBA", Token.FUNCTION);
        tokenMap.put("CPBX", Token.FUNCTION);
        tokenMap.put("ADDA", Token.FUNCTION);
        tokenMap.put("ADDX", Token.FUNCTION);
        tokenMap.put("SUBA", Token.FUNCTION);
        tokenMap.put("SUBX", Token.FUNCTION);
        tokenMap.put("ANDA", Token.FUNCTION);
        tokenMap.put("ANDX", Token.FUNCTION);
        tokenMap.put("ORA", Token.FUNCTION);
        tokenMap.put("ORX", Token.FUNCTION);
        tokenMap.put("XORA", Token.FUNCTION);
        tokenMap.put("XORX", Token.FUNCTION);
        tokenMap.put("ADDSP", Token.FUNCTION);
        tokenMap.put("SUBSP", Token.FUNCTION);

        return tokenMap;
    }
}
