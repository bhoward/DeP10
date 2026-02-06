package edu.depauw.dep10.driver;

import java.util.ArrayList;
import java.util.List;

public class ErrorLog {
    private List<String> messages;

    public ErrorLog() {
        this.messages = new ArrayList<String>();
    }
    
	public void error(String message) {
		messages.add(message);
	}

	// TODO get the messages back out...
}
