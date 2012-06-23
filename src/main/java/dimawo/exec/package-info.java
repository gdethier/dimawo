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
/**
 * This package provides classes used to execute DiMaWo Master and Workers.
 * <p>
 * We call <i>launcher</i> a class that provides a static main method and which
 * can therefore be used as entry point for the execution of an application.
 * This package provides two types of launchers, giving access to different
 * implementations of DiMaWo:
 * <ul>
 * <li>Centralized implementation launchers</li>
 * <li>Decentralized implementation launchers</li>
 * </ul>
 * <p>
 * Centralized implementation launchers clearly distinguish Master and Workers
 * launchers: the Master is executed using {@link dimawo.exec.GenericMasterLauncher GenericMasterLauncher}
 * launcher and Workers are executed using {@link dimawo.exec.GenericCentralDALauncher GenericCentralDALauncher}.
 * <p>
 * Decentralized implementation is organized in a P2P fashion: a launcher
 * executes a peer. However, a bootstrap is needed when executing the first peer.
 * A {@link dimawo.exec.GenericBootstrapLauncher bootstrap launcher}
 * is therefore provided. {@link dimawo.exec.GenericLauncher GenericPeerLauncher launcher}
 * is used to execute next peers.
 * <p>
 * For more details about centralized and decentralized implementations, see
 * {@link dimawo.middleware.overlay.impl Overlay implementation package}.
 */
package dimawo.exec;
