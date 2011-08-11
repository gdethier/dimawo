/*
 * #%L
 * DiMaWo
 * %%
 * Copyright (C) 2011 DiMaWo Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package dimawo.agents;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;


/**
 * @author Gerard Dethier
 *
 * A logging agent is an agent that logs all messages into a file instead of
 * the standard output. This is particularly interesting when many logs are
 * produced and/or are be examined afterwards.
 * <p>
 * The name of the file the agent logs to is determined in function of a
 * prefix and the name of the agent. If <i>p</i> is the file name prefix,
 * <i>n</i> the name of the agent, then the file name is "<i>pn</i>.log"
 * (without quotes).
 */
public abstract class LoggingAgent extends AbstractAgent {
	/**
	 * The log file name prefix.
	 */
	private String filePrefix;
	/**
	 * The print stream the agent logs to or <i>log stream</i>.
	 */
	private PrintStream printStream;
	
	
	/**
	 * Constructs a logging agent without setting file name prefix and
	 * log stream.
	 */
	public LoggingAgent() {
		super();
	}

	
	/**
	 * Constructs a logging agent without setting file name prefix and
	 * log stream.
	 */
	public LoggingAgent(ErrorHandler parent, String name) {
		super(parent, name);
	}
	
	
	/**
	 * Constructs a logging agent without setting file name prefix and
	 * log stream.
	 */
	public LoggingAgent(ErrorHandler parent, String name, boolean daemon) {
		super(parent, name, daemon);
	}

	
	/**
	 * Constructs a logging agent without setting file name prefix and
	 * log stream.
	 */
	public LoggingAgent(ErrorHandler parent, String name, int capacity) {
		super(parent, name, capacity);
	}

	
	/**
	 * Sets the log file name prefix and creates the log stream. If the log
	 * stream could not be created, the agent logs to standard output.
	 * 
	 * @param fileNamePref The file name prefix.
	 * 
	 * @return True if the log stream could be created (and the agent is able
	 * to log to it), false otherwise.
	 */
	public boolean setPrintStream(String fileNamePref) {
		this.filePrefix = fileNamePref;
		
		File logFile = new File(fileNamePref + this.agentName
				+ ".log").getAbsoluteFile();
		File parent = logFile.getParentFile();
		if( ! parent.mkdirs() && ! parent.exists())
			new IOException("Could not create log directory "+
					parent.getAbsolutePath()).printStackTrace();
		
		try {
			printStream = new PrintStream(logFile);
			setPrintStream(printStream);
			return true;
		} catch (FileNotFoundException e) {
			return false;
		}

	}
	
	
	/**
	 * Returns the log file name prefix.
	 * 
	 * @return The log file name prefix.
	 */
	public String getFilePrefix() {
		return filePrefix;
	}
	
	
	/**
	 * Closes the log stream (and therefore ensures all logs
	 * are written into the log file). This method is final, in order to
	 * describe the operations to execute when entering into STOPPED state,
	 * {@link #logAgentExit()} method must be overridden.
	 */
	@Override
	final protected void exit() {
		logAgentExit();
		if(printStream != null)
			printStream.close();
	}

	
	/**
	 * Implements the operations to be executed when logging agent enters
	 * STOPPED state.
	 * 
	 * @see AbstractAgent#exit()
	 */
	protected abstract void logAgentExit();
}
