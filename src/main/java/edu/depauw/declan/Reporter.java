package edu.depauw.declan;

import java.io.PrintStream;

public class Reporter {
    private boolean hadError = false;
    private boolean hadRuntimeError = false;
    
    private PrintStream err;
    
    public Reporter(PrintStream err) {
        this.err = err;
    }

    public void error(int line, String message) {
        report(line, "", message);
    }

    public void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }

    public void runtimeError(RuntimeError error) {
        err.println(error.getMessage() + "\n[line " + error.token.line + "]");
        hadRuntimeError = true;
    }

    private void report(int line, String where, String message) {
        err.println(";[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }

    public void reset() {
        hadError = false;
        hadRuntimeError = false;
    }

    public boolean hadError() {
        return hadError;
    }

    public boolean hadRuntimeError() {
        return hadRuntimeError;
    }
}