package edu.depauw.dep10.ui;

import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMaker;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;

public class DeCLanTokenMaker extends AbstractTokenMaker {
    @Override
    public Token getTokenList(Segment text, int initialTokenType, int startOffset) {
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
        int currentTokenType = initialTokenType;

        for (int i = offset; i < end; i++) {
            char c = array[i];
            switch (currentTokenType) {
            case Token.NULL:
                switch (c) {
                case ')':
                case ';':
                case ',':
                case '.':
                    addToken(text, currentTokenStart, i, Token.SEPARATOR, newStartOffset + currentTokenStart);
                    currentTokenStart = i + 1;
                    currentTokenType = Token.NULL;
                    continue;

                case '-':
                case '+':
                case '/':
                case '*':
                case '&':
                case '~':
                case '=':
                case '#':
                    addToken(text, currentTokenStart, i, Token.OPERATOR, newStartOffset + currentTokenStart);
                    currentTokenStart = i + 1;
                    currentTokenType = Token.NULL;
                    continue;

                case ':':
                case '<':
                case '>':
                    if (i + 1 < end && array[i + 1] == '=') {
                        // Handle :=, <=, and >= as well
                        i++;
                    }
                    addToken(text, currentTokenStart, i, Token.OPERATOR, newStartOffset + currentTokenStart);
                    currentTokenStart = i + 1;
                    currentTokenType = Token.NULL;
                    continue;

                case '(':
                    if (i + 1 < end && array[i + 1] == '*') {
                        // Start of a comment
                        i++;
                        currentTokenType = Token.COMMENT_MULTILINE;
                        continue;
                    }
                    addToken(text, currentTokenStart, i, Token.OPERATOR, newStartOffset + currentTokenStart);
                    currentTokenStart = i + 1;
                    currentTokenType = Token.NULL;
                    continue;

                case ' ':
                case '\n':
                case '\r':
                case '\t':
                    currentTokenType = Token.WHITESPACE;
                    continue;

                default:
                    if (RSyntaxUtilities.isDigit(c)) {
                        currentTokenType = Token.LITERAL_NUMBER_DECIMAL_INT;
                        continue;
                    } else {
                        currentTokenType = Token.IDENTIFIER;
                        continue;
                    }
                }

            case Token.WHITESPACE:
                switch (c) {
                case ' ':
                case '\n':
                case '\r':
                case '\t':
                    continue;

                default:
                    i--;
                    addToken(text, currentTokenStart, i, currentTokenType, newStartOffset + currentTokenStart);
                    currentTokenStart = i + 1;
                    currentTokenType = Token.NULL;
                    continue;
                }

            case Token.COMMENT_MULTILINE:
                // Note: unable to handle multiline nested comments, because there is no way to
                // track the nesting
                if (c == '*' && i + 1 < end && array[i + 1] == ')') {
                    i++;
                    addToken(text, currentTokenStart, i, currentTokenType, newStartOffset + currentTokenStart);
                    currentTokenStart = i + 1;
                    currentTokenType = Token.NULL;
                    continue;
                } else {
                    continue;
                }

            case Token.IDENTIFIER:
                if (RSyntaxUtilities.isLetterOrDigit(c) || c == '_') {
                    continue;
                } else {
                    i--;
                    addToken(text, currentTokenStart, i, currentTokenType, newStartOffset + currentTokenStart);
                    currentTokenStart = i + 1;
                    currentTokenType = Token.NULL;
                    continue;
                }

            case Token.LITERAL_NUMBER_DECIMAL_INT:
                if (RSyntaxUtilities.isDigit(c)) {
                    continue;
                } else if (RSyntaxUtilities.isHexCharacter(c)) {
                    currentTokenType = Token.LITERAL_NUMBER_HEXADECIMAL;
                    continue;
                } else if (c == 'H') {
                    i--;
                    currentTokenType = Token.LITERAL_NUMBER_HEXADECIMAL;
                    continue;
                } else if (c == '.') {
                    currentTokenType = Token.LITERAL_NUMBER_FLOAT;
                    continue;
                } else {
                    i--;
                    addToken(text, currentTokenStart, i, currentTokenType, newStartOffset + currentTokenStart);
                    currentTokenStart = i + 1;
                    currentTokenType = Token.NULL;
                    continue;
                }

            case Token.LITERAL_NUMBER_HEXADECIMAL:
                if (RSyntaxUtilities.isHexCharacter(c)) {
                    continue;
                } else if (c == 'H') {
                    addToken(text, currentTokenStart, i, currentTokenType, newStartOffset + currentTokenStart);
                    currentTokenStart = i + 1;
                    currentTokenType = Token.NULL;
                    continue;
                } else {
                    i--;
                    addToken(text, currentTokenStart, i, currentTokenType, newStartOffset + currentTokenStart);
                    currentTokenStart = i + 1;
                    currentTokenType = Token.NULL;
                    continue;
                }

            case Token.LITERAL_NUMBER_FLOAT:
                if (RSyntaxUtilities.isDigit(c)) {
                    continue;
                } else if (c == 'E') {
                    if (i + 1 < end && (array[i + 1] == '+' || array[i + 1] == '-')) {
                        i++;
                    }
                    
                    // Go ahead and scan the rest of the exponent here...
                    while (i + 1 < end && RSyntaxUtilities.isDigit(array[i + 1])) {
                        i++;
                    }
                    
                    addToken(text, currentTokenStart, i, currentTokenType, newStartOffset + currentTokenStart);
                    currentTokenStart = i + 1;
                    currentTokenType = Token.NULL;
                    continue;
                } else {
                    i--;
                    addToken(text, currentTokenStart, i, currentTokenType, newStartOffset + currentTokenStart);
                    currentTokenStart = i + 1;
                    currentTokenType = Token.NULL;
                    continue;
                }
            }
        }
        
        switch (currentTokenType) {
        // Remember what token type to begin the next line with.
        case Token.COMMENT_MULTILINE:
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

        tokenMap.put("BEGIN", Token.RESERVED_WORD);
        tokenMap.put("BY", Token.RESERVED_WORD);
        tokenMap.put("CONST", Token.RESERVED_WORD);
        tokenMap.put("DIV", Token.RESERVED_WORD);
        tokenMap.put("DO", Token.RESERVED_WORD);
        tokenMap.put("ELSE", Token.RESERVED_WORD);
        tokenMap.put("ELSIF", Token.RESERVED_WORD);
        tokenMap.put("END", Token.RESERVED_WORD);
        tokenMap.put("FOR", Token.RESERVED_WORD);
        tokenMap.put("IF", Token.RESERVED_WORD);
        tokenMap.put("MOD", Token.RESERVED_WORD);
        tokenMap.put("OR", Token.RESERVED_WORD);
        tokenMap.put("PROCEDURE", Token.RESERVED_WORD);
        tokenMap.put("REPEAT", Token.RESERVED_WORD);
        tokenMap.put("THEN", Token.RESERVED_WORD);
        tokenMap.put("TO", Token.RESERVED_WORD);
        tokenMap.put("UNTIL", Token.RESERVED_WORD);
        tokenMap.put("VAR", Token.RESERVED_WORD);
        tokenMap.put("WHILE", Token.RESERVED_WORD);

        tokenMap.put("BOOLEAN", Token.DATA_TYPE);
        tokenMap.put("INTEGER", Token.DATA_TYPE);
        tokenMap.put("REAL", Token.DATA_TYPE);

        tokenMap.put("FALSE", Token.RESERVED_WORD_2);
        tokenMap.put("TRUE", Token.RESERVED_WORD_2);

        return tokenMap;
    }
}
