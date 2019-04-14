package org.blendee.cli;

@SuppressWarnings("serial")
public class CommandException extends RuntimeException {

	CommandException(Throwable t) {
		super(t);
	}
}
