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
package dimawo.master.messages;

import dimawo.middleware.distributedAgent.DAId;

public class WorkerExitMessage extends MasterMessage {

	private static final long serialVersionUID = 1L;
	
	private Throwable error;
	private Object result;


	public WorkerExitMessage(DAId sourceDaId, Throwable error, Object result) {

		super(sourceDaId);

		this.error = error;
		this.result = result;

	}

	public Throwable getError() {

		return error;

	}

	public Object getResult() {

		return result;

	}

}
