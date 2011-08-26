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
package dimawo.exec;

/**
 * This class encompasses the parameters of a DiMaWo worker.
 * 
 * @author Gerard Dethier
 */
public class WorkerParameters {
	/** Default verbosity level used by the agents instantiated in the context
	 * of the worker. */
	public int verbLevel = 0;
	/** Class name of a Master/worker factory. */
	public String factClassName = null;
	/** Parameters of the provided Master/worker factory. */
	public String[] factParams = null;
	/** Port the worker accepts connections on. */
	public int port = -1;
	/** Name of the working directory of this worker. */
	public String workDirName = null;
	/** Maximum number of children of a meta-node
	 * (see {@link dimawo.middleware.overlay.mntree.MnTreePeerAgent}) */
	public int maxNumOfChildren = 2;
	/** Minimum number of members a meta-node must contain in order to be
	 * considered as safe.
	 * (see {@link dimawo.middleware.overlay.mntree.MnTreePeerAgent}) */
	public int reliabilityThreshold = 2;
	/** Host name of the worker this worker connects to in order to join
	 * workers' overaly. */
	public String bootstrapHostName = null;
	/** Server port of the worker this worker connects to in order to join
	 * workers' overaly. */
	public int bootstrapPort = -1;
}
