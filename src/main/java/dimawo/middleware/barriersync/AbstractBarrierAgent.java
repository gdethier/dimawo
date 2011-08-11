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
package dimawo.middleware.barriersync;

import dimawo.agents.ErrorHandler;
import dimawo.agents.LoggingAgent;
import dimawo.middleware.barriersync.events.BarrierWait;
import dimawo.middleware.barriersync.events.DestroyBarrier;
import dimawo.middleware.overlay.BarrierID;
import dimawo.middleware.overlay.BarrierSyncCallBackInterface;
import dimawo.middleware.overlay.BarrierSyncInterface;

public abstract class AbstractBarrierAgent extends LoggingAgent implements
		BarrierSyncInterface {

	public AbstractBarrierAgent(ErrorHandler parent, String name) {
		super(parent, name);
	}

	@Override
	public void barrierDestroy(BarrierID id) {
		try {
			submitMessage(new DestroyBarrier(id));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void barrierWait(BarrierID id, BarrierSyncCallBackInterface cb) {
		if(cb == null)
			throw new NullPointerException();

		try {
			submitMessage(new BarrierWait(id, cb));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void handleMessage(Object o) throws Throwable {
		if(o instanceof DestroyBarrier) {
			handleDestroyBarrier((DestroyBarrier) o);
		} else if(o instanceof BarrierWait) {
			handleBarrierWait((BarrierWait) o);
		} else {
			handleSpecializedMessage(o);
		}
	}

	private void handleDestroyBarrier(DestroyBarrier destroy) {
		BarrierID id = destroy.getBarrierId();
		
		destroyBarrier(id);
	}

	private void handleBarrierWait(BarrierWait wait) {
		BarrierID id = wait.getBarrierId();
		BarrierSyncCallBackInterface cbInt = wait.getCallBackInterface();
		
		BarrierWaitCallBack cb = new BarrierWaitCallBack(id);
		try {
			wait(id, cbInt);
		} catch(Throwable t) {
			cb.setError(t);
			cbInt.barrierWaitCB(cb);
		}
	}

	protected abstract void destroyBarrier(BarrierID id);
	protected abstract void wait(BarrierID id, BarrierSyncCallBackInterface cbInt) throws Exception;
	protected abstract void handleSpecializedMessage(Object o) throws Exception;
}
