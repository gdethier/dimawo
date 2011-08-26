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
package dimawo.simulation.host;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

import dimawo.agents.LoggingAgent;
import dimawo.agents.UnknownAgentMessage;
import dimawo.exec.WorkerParameters;
import dimawo.exec.WorkerProcess;
import dimawo.middleware.distributedAgent.logging.ConsoleLogger;
import dimawo.simulation.host.events.AccessClosed;
import dimawo.simulation.host.events.CloseServerSocket;
import dimawo.simulation.host.events.CloseSocket;
import dimawo.simulation.host.events.ConnectEvent;
import dimawo.simulation.host.events.CreateHostProcess;
import dimawo.simulation.host.events.DataEvent;
import dimawo.simulation.host.events.HostProcessExited;
import dimawo.simulation.host.events.MiddlewareEvent;
import dimawo.simulation.host.events.NetworkEvent;
import dimawo.simulation.host.events.NewServerSocketEvent;
import dimawo.simulation.host.events.NewSocketEvent;
import dimawo.simulation.host.events.ProcessEvent;
import dimawo.simulation.host.events.RunTask;
import dimawo.simulation.host.events.SignalCloseSocket;
import dimawo.simulation.host.events.TaskCompleted;
import dimawo.simulation.middleware.VirtualTask;
import dimawo.simulation.net.NetworkException;
import dimawo.simulation.net.VirtualNetwork;
import dimawo.simulation.socket.ServerSocketInterface;
import dimawo.simulation.socket.SocketFactory;
import dimawo.simulation.socket.VirtualServerSocket;
import dimawo.simulation.socket.VirtualSocket;
import dimawo.simulation.socket.VirtualSocketAddress;


public class VirtualHost extends LoggingAgent {
	private static final int MIN_USER_PORT = 1024;
	private static final int MAX_USER_PORT = 65535;
	
	private static boolean log = false;
	
	private VirtualNetwork net;
	private String hostname;
	
	private int nextAccessID;
	private TreeMap<Integer, HostAccess> accesses;

	private TreeMap<Integer, VirtualSocket> openSocks;
	private TreeMap<Integer, VirtualServerSocket> serverSocks;

	/** The process ID to associate to next created process. */
	private int nextProcID;
	/** A table of ProcessHandles associated to all created process currently
	 * executing. */
	private TreeMap<Integer, ProcessHandle> procHandles;

	private VirtualTask runningTask;

	
	public VirtualHost(String hostname, VirtualNetwork net) throws NetworkException {
		super(null, "VirtualHost"+hostname, true);
		this.net = net;
		this.hostname = hostname;
		
		nextAccessID = 0;
		accesses = new TreeMap<Integer, HostAccess>();

		openSocks = new TreeMap<Integer, VirtualSocket>();
		serverSocks = new TreeMap<Integer, VirtualServerSocket>();
		
		nextProcID = 0;
		procHandles = new TreeMap<Integer, ProcessHandle>();
		
		net.connectHost(this);
	}
	
	public static void enableLog() {
		log = true;
	}
	
	public static void disableLog() {
		log = false;
	}
	
	public String getHostName() {
		return hostname;
	}

	public void disconnectHost() {
		net = null;
	}

	public void connectHost(VirtualNetwork net) {
		this.net = net;
	}
	
	private int useRandomPort() throws HostException {
		Random r = new Random(System.currentTimeMillis());
		int randomPort = r.nextInt(MAX_USER_PORT - MIN_USER_PORT);
		randomPort += MIN_USER_PORT;
		
		if( ! openSocks.containsKey(randomPort)) {
			return randomPort;
		}

		int stopVal = randomPort;
		++randomPort;
		while(stopVal != randomPort && openSocks.containsKey(randomPort)) {
			++randomPort;
			if(randomPort == MAX_USER_PORT + 1) {
				randomPort = MIN_USER_PORT;
			}
		}
		
		if(stopVal == randomPort) {
			throw new HostException("No port number available");
		} else {
			return randomPort;
		}
	}

	private void handleNetworkEvent(NetworkEvent ne) throws NetworkException {
		if(ne instanceof ConnectEvent) {
			handleConnectEvent((ConnectEvent) ne);
		} else if(ne instanceof DataEvent) {
			handleDataEvent((DataEvent) ne);
		} else if(ne instanceof CloseServerSocket) {
			handleCloseServerSocket((CloseServerSocket) ne);
		} else if(ne instanceof CloseSocket) {
			handleCloseSocket((CloseSocket) ne);
		} else if(ne instanceof SignalCloseSocket) {
			handleSignalCloseSocket((SignalCloseSocket) ne);
		} else if(ne instanceof NewServerSocketEvent) {
			handleNewServerSocket((NewServerSocketEvent) ne);
		} else if(ne instanceof NewSocketEvent) {
			handleNewSocket((NewSocketEvent) ne);
		} else {
			throw new NetworkException("Unknown event: "+ne.getClass().getName());
		}
	}
	
	private void handleCloseServerSocket(CloseServerSocket css) {
		printMessage("Closing server socket on port "+css.getPort());
		if(serverSocks.remove(css.getPort()) != null)
			css.signalSuccess();
		else
			css.signalError(new NetworkException("No server socket bound to port "+css.getPort()));
	}
	
	private void handleCloseSocket(CloseSocket css) {
		printMessage("Closing socket on port "+css.getPort());
		if(openSocks.remove(css.getPort()) == null)
			css.signalError(new NetworkException("No server socket bound to port "+css.getPort()));
		else
			css.signalSuccess();
	}
	
	private void handleSignalCloseSocket(SignalCloseSocket scs) {
		int port = scs.getPort();
		printMessage("Signal close to socket on port "+port);
		VirtualSocket sock = openSocks.get(port);
		if(sock != null) {
			sock.putRemoteClose();
		}
		scs.signalSuccess();
	}
	
	private void handleDataEvent(DataEvent ne) {
		int port = ne.getPort();
		printMessage("Putting data to socket on port "+port+" ("+ne.getNumberOfBytes()+" bytes)");
		VirtualSocket sock = openSocks.get(port);
		if(sock == null) {
			ne.signalError(new NetworkException("Port "+port+" unreachable on "+hostname));
		} else {
			sock.putData(ne); // event is signaled further to implement a form of flow control
		}
	}

	private void handleConnectEvent(ConnectEvent ne) {
		int port = ne.getPort();
		printMessage("Connecting to server socket on port "+port);
		VirtualServerSocket serv = serverSocks.get(port);
		if(serv == null) {
			ne.signalError(new NetworkException("Unreachable port"));
		} else {
			serv.addPendingConnection(ne); // VirtualServerSocket signals event
		}
	}

	private boolean isUp() {
		return getState().equals(AgentState.RUNNING);
	}

	private void cancelSubmittedEvents() {
		LinkedList<Object> incomingEvents = flushPendingMessages();
		while( ! incomingEvents.isEmpty()) {
			Object o = incomingEvents.poll();
			if(o instanceof NetworkEvent) {
				NetworkEvent ne = (NetworkEvent) o;
				ne.signalError(new NetworkException("Host unreachable"));
			} else if(o instanceof RunTask) {
				RunTask rt = (RunTask) o;
				rt.getTask().signalInterrupted(hostname);
			}
		}
	}

	public synchronized void putEvent(NetworkEvent ne) {
		if(getState().equals(AgentState.STOPPED)) {
			ne.signalError(new NetworkException("Host unreachable"));
		}
		
		try {
			submitMessage(ne);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private VirtualSocket buildSocket() throws HostException {
		int port = useRandomPort();
		VirtualSocket sock = new VirtualSocket(this, port);
		openSocks.put(port, sock);
		return sock;
	}
	
	private VirtualSocket buildSocket(VirtualSocketAddress remoteAddr) throws HostException {
		int port = useRandomPort();
		VirtualSocket sock = new VirtualSocket(this, port, remoteAddr);
		openSocks.put(port, sock);
		return sock;
	}

	private VirtualServerSocket buildServerSocket(int port) throws HostException {
		if(openSocks.containsKey(port) || serverSocks.containsKey(port)) {
			throw new HostException("Port already in use");
		}
		VirtualServerSocket sock = new VirtualServerSocket(this, port);
		serverSocks.put(port, sock);
		return sock;
	}

	public void unregisterServerSocket(int port) throws NetworkException {
		if( ! isUp())
			throw new NetworkException("Host is down");

		CloseServerSocket css = new CloseServerSocket(port);
		try {
			submitMessage(css);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		try {
			css.waitOn();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if( ! css.isSuccessful())
			throw css.getException();
	}
	
	public void unregisterSocket(int port) throws NetworkException {
		if( ! isUp())
			throw new NetworkException("Host is down");

		CloseSocket cs = new CloseSocket(port);
		try {
			submitMessage(cs);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		try {
			cs.waitOn();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if( ! cs.isSuccessful())
			throw cs.getException();
		
	}
	
	public void signalSocketClose(VirtualSocketAddress remoteAddr) throws NetworkException {
		SignalCloseSocket scs = new SignalCloseSocket(remoteAddr);
		try {
			net.routeEvent(scs);
		} catch (NetworkException e) {
			scs.signalError(e);
		}

		try {
			scs.waitOn();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return;
		}
		
		if( ! scs.isSuccessful()) {
			throw scs.getException();
		}
	}

	public void sendData(VirtualSocketAddress remoteAddr, int val) throws NetworkException {
		DataEvent data = new DataEvent(remoteAddr, val);
		net.routeEvent(data);
		try {
			data.waitOn();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if( ! data.isSuccessful()) {
			throw data.getException();
		}
	}
	
	public void sendData(VirtualSocketAddress remoteAddr, byte[] a) throws NetworkException {
		DataEvent data = new DataEvent(remoteAddr, a);
		net.routeEvent(data);
		try {
			data.waitOn();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if( ! data.isSuccessful()) {
			throw data.getException();
		}
	}

	/**
	 * Tries to connect to a remote server socket. If sucessful, the port of
	 * remote socket is returned.
	 * 
	 * @param endpoint Address of server socket.
	 * @param timeout Maximum connection time.
	 * @param localPort Port of local socket.
	 * 
	 * @return The port of remote socket (in case of success).
	 * 
	 * @throws NetworkException
	 * @throws InterruptedException
	 */
	public int connectTo(VirtualSocketAddress endpoint, int timeout, int localPort) throws NetworkException, InterruptedException {
		if( ! isUp())
			throw new NetworkException("Host is down");

		ConnectEvent ce = new ConnectEvent(endpoint,
				new VirtualSocketAddress(hostname, localPort));
		net.routeEvent(ce);
		if(timeout == 0) {
			ce.waitOn();
		} else {
			ce.waitOn(timeout);
		}

		if(ce.isSuccessful()) {
			return ce.getSocketPort();
		} else {
			throw ce.getException();
		}
	}

	public VirtualSocket newSocket() throws IOException {
		return newSocket(null);
	}

	public VirtualSocket newSocket(VirtualSocketAddress remoteAddr) throws IOException {
		NewSocketEvent nse = new NewSocketEvent(remoteAddr);
		try {
			submitMessage(nse);
			nse.waitOn();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}
		
		if(nse.isSuccessful()) {
			return nse.getSocket();
		} else {
			throw new IOException(nse.getException());
		}
	}
	
	private void handleNewSocket(NewSocketEvent e) {
		if( ! isUp()) {
			e.signalError(new NetworkException("Host is down"));
			return;
		}
		
		try {
			VirtualSocketAddress remoteAddr = e.getRemoteAddress();
			VirtualSocket sock;
			if(remoteAddr == null) {
				sock = buildSocket();
				printMessage("Built new unconnected socket on port "+sock.getLocalPort());
			} else {
				sock = buildSocket(remoteAddr);
				printMessage("Built new socket on port "+sock.getLocalPort()+" for remote socket "+remoteAddr);
			}
			e.setSocket(sock);
			e.signalSuccess();
		} catch (HostException ex) {
			e.signalError(new NetworkException("Could not create socket", ex));
		}
	}
	
	public ServerSocketInterface newServerSocket(int port) throws IOException {
		NewServerSocketEvent nse = new NewServerSocketEvent(port);
		try {
			submitMessage(nse);
			nse.waitOn();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}
		
		if(nse.isSuccessful()) {
			return nse.getSocket();
		} else {
			throw new IOException(nse.getException());
		}
	}

	private void handleNewServerSocket(NewServerSocketEvent e) {
		if( ! isUp()) {
			e.signalError(new NetworkException("Host is down"));
			return;
		}
		
		int port = e.getPort();
		try {
			VirtualServerSocket sock = buildServerSocket(port);
			printMessage("Built new server socket bound to port "+port);
			e.setSocket(sock);
			e.signalSuccess();
		} catch (HostException ex) {
			e.signalError(new NetworkException("Could not create socket", ex));
		}
	}

	public synchronized void printMessage(String msg) {
		if( ! log)
			return;
		agentPrintMessage(System.currentTimeMillis()+"-- [VirtualHost "+hostname+"] "+msg);
	}

	public void printMessage(Throwable t) {
		if( ! log)
			return;
		agentPrintMessage(System.currentTimeMillis()+"-- [VirtualHost "+hostname+"] Exception:");
		agentPrintMessage(t);
	}

	public synchronized void killTask() {
		if(runningTask != null) {
			runningTask.kill();
			
			VirtualTask t = runningTask;
			runningTask = null;

			t.signalInterrupted(hostname);
		}
	}

	public void runTask(VirtualTask task) {
		try {
			submitMessage(new RunTask(task));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Requests the creation of a process on the virtual host. This produces
	 * a ProcessHandle instance that can be used to retrieve information about
	 * processe's state (see {@link ProcessHandle}). Note that created handle
	 * contains only undefined information until actual creation of the
	 * process.
	 * 
	 * @param params The parameters of the worker the process will execute.
	 * 
	 * @return A ProcessHandle instance.
	 */
	public ProcessHandle createHostProcess(WorkerParameters params) {
		ProcessHandle handle = new ProcessHandle();
		try {
			submitMessage(new CreateHostProcess(handle, params));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return handle;
	}

	private synchronized void handleRunTask(RunTask rt) {
		VirtualTask task = rt.getTask();
		killTask();
		
		agentPrintMessage("Running task "+task.getTaskID()+" from job "+
				task.getJobID());
		
		runningTask = task;
		try {
			runningTask.start(getAccess());
		} catch (NetworkException e) {
			agentPrintMessage("Host is down, task is interrupted");
			runningTask.signalInterrupted(hostname);
			runningTask = null;
			return;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void signalTaskCompleted(VirtualTask task) {
		try {
			submitMessage(new TaskCompleted(task));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected synchronized void logAgentExit() {
		agentPrintMessage("exit");
		
		try {
			net.disconnectHost(hostname);
		} catch (NetworkException e) {
		}
		
		for(HostAccess a : accesses.values()) {
			a.close();
		}
		accesses.clear();
		
		for(Entry<Integer, VirtualSocket> e : openSocks.entrySet()) {
			VirtualSocket sock = e.getValue();
			try {
				sock.getInputStream().close();
				sock.getOutputStream().close();
			} catch (IOException e1) {
			}

			VirtualSocketAddress addr = sock.getRemoteSocketAddress();
			if(addr != null) { // Socket may not be already connected
				SignalCloseSocket scs = new SignalCloseSocket(addr);
				try {
					net.routeEvent(scs);
				} catch (NetworkException e2) {
				}
			}
		}
		openSocks.clear();
		
		for(Entry<Integer, VirtualServerSocket> e : serverSocks.entrySet()) {
			VirtualServerSocket sock = e.getValue();
			try {
				sock.close();
			} catch (IOException e1) {
			}
		}
		serverSocks.clear();
		
		cancelSubmittedEvents();
		
		if(runningTask != null) {
			runningTask.kill();
			runningTask.signalInterrupted(hostname);
			runningTask = null;
		}
		
		for(ProcessHandle h : procHandles.values()) {
			h.killProcess();
			h.signalEndOfExecution(new Exception("Host is down"));
		}
	}

	@Override
	protected void handleMessage(Object o) throws Throwable {
		if(o instanceof NetworkEvent) {
			handleNetworkEvent((NetworkEvent) o);
		} else if(o instanceof MiddlewareEvent) {
			handleMiddlewareEvent((MiddlewareEvent) o);
		} else if(o instanceof AccessClosed) {
			handleAccessClosed((AccessClosed) o);
		} else if(o instanceof ProcessEvent) {
			handleProcessEvent((ProcessEvent) o);
		} else {
			throw new UnknownAgentMessage(o);
		}
	}

	private void handleProcessEvent(ProcessEvent o) throws UnknownAgentMessage {
		if(o instanceof CreateHostProcess) {
			handleCreateHostProcess((CreateHostProcess) o);
		} else if(o instanceof HostProcessExited) {
			handleHostProcessExited((HostProcessExited) o);
		} else {
			throw new UnknownAgentMessage(o);
		}
	}

	private void handleHostProcessExited(HostProcessExited o) {
		ProcessHandle handle = o.getProcessHandle();

		// Remove handle from table
		int procID = handle.getProcID();
		printMessage("Removing process "+procID);
		ProcessHandle removedHandle = procHandles.remove(procID);

		if(removedHandle != handle)
			throw new Error("Process was not registered before.");
		
		// Close sockets associated to exited process
		// TODO

		handle.signalEndOfExecution(o.getError());
	}

	private void handleCreateHostProcess(CreateHostProcess o) {
		final ProcessHandle handle = o.getProcessHandle();
		
		// Create process
		HostAccess access = null;
		try {
			access = getAccess();
		} catch (NetworkException e2) {
			handle.signalProcessCreated(e2); // Signal error on creation
			return;
		}

		WorkerProcess proc = null;
		try {
			proc = new WorkerProcess(o.getWorkerParameters(),
					new ConsoleLogger(), new SocketFactory(access));
		} catch (Exception e2) {
			handle.signalProcessCreated(e2); // Signal error on creation
			return;
		}
		
		printMessage("Worker process instantiated.");
		
		// Insert handle into table
		while(procHandles.containsKey(nextProcID))
			++nextProcID;
		handle.setProcId(nextProcID);
		handle.setWorkerProcess(proc);
		procHandles.put(nextProcID, handle);
		printMessage("Creating process "+nextProcID);
		++nextProcID;

		handle.signalProcessCreated(null);

		final WorkerProcess finalProc = proc;
		Thread t = new Thread() {
			public void run() {
				try {
					finalProc.executeProcess();
					try {
						submitMessage(new HostProcessExited(handle, null));
					} catch (Exception e) {
						e.printStackTrace();
					}
				} catch (Exception e) {
					try {
						submitMessage(new HostProcessExited(handle, e));
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
			}
		};
		t.setDaemon(true);
		t.start();
	}

	private void handleAccessClosed(AccessClosed o) {
		accesses.remove(o.getAccessID());
	}

	@Override
	protected void init() throws Throwable {
		printMessage("init");
	}
	
	private void handleMiddlewareEvent(MiddlewareEvent me) throws UnknownAgentMessage {
		if(me instanceof TaskCompleted) {
			handleTaskCompleted((TaskCompleted) me);
		} else if(me instanceof RunTask) {
			handleRunTask((RunTask) me);
		} else {
			throw new UnknownAgentMessage(me);
		}
	}
	
	private synchronized void handleTaskCompleted(TaskCompleted tc) {
		VirtualTask cTask = tc.getTask();
		if(cTask == runningTask) {
			runningTask = null;
			
			cTask.signalCompleted(hostname);
		}
	}

	public synchronized HostAccess getAccess() throws NetworkException {
		if( ! isUp())
			throw new NetworkException("Host is down");
		return new HostAccess(nextAccessID++, this);
	}
	
	public void signalClosedAccess(int accessID) {
		try {
			submitMessage(new AccessClosed(accessID));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
