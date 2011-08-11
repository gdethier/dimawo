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
package dimawo.simulation.socket;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import dimawo.simulation.host.events.DataEvent;



/**
 * A buffer to put and get bytes. Bytes retrieval is a blocking operation. Bytes
 * insertion is not.
 * 
 * @author Gérard Dethier
 */
public class BlockingByteBuffer {
	
	private VirtualSocket sock;

	private Semaphore available; // Number of available packets
	private LinkedList<DataEvent> queuedData;
	private int availableBytes;
	private boolean eof, error;
	
	private int out;
	private byte[] buffer;
	private int byteVal;


	public BlockingByteBuffer(VirtualSocket sock) {
		this.sock = sock;
		
		available = new Semaphore(0);
		queuedData = new LinkedList<DataEvent>();
		availableBytes = 0;
		eof = false;
		error = false;
		
		buffer = null;
	}

	public int getByte(int timeout) throws SocketTimeoutException, IOException {
		if(error)
			throw new IOException("Socket closed");
		
		if(eof)
			throw new EOFException();
		
		if(buffer != null) {
			int val = (0xFF & buffer[out]);
			++out;
			--availableBytes;
			
			if(out == buffer.length) {
				buffer = null;
			}
			
			return val;
		}
		
		try {
			sock.printMessage("Waiting byte...");
			if(timeout == 0) {
				available.acquire();
			} else {
				if( ! available.tryAcquire((long) timeout, TimeUnit.MILLISECONDS)) {
					throw new SocketTimeoutException("After "+timeout+"ms (available permits="+available.availablePermits()+")");
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			return -1;
		}
		
		if(buffer == null) {
			DataEvent e;
			synchronized(queuedData) {
				e = queuedData.removeFirst();
				--availableBytes;
			}
			if(e == null) {
				if(error) {
					throw new IOException("Socket closed");
				}
				eof = true;
				// Remote socket is closed.
				throw new EOFException();
			}
			
			if(e.isByte()) {
				byteVal = e.getByte();
			} else {
				buffer = e.getData();
				out = 0;
			}
			
			e.signalSuccess();
		}
		
		int val;
		if(buffer != null) {
			val = (0xFF & buffer[out]);
			++out;
			
			if(out == buffer.length) {
				buffer = null;
			}
		
		} else {
			val = byteVal;
		}
		
		assert val >= 0;
		return val;
	}
	
	public int fill(byte[] b, int off, int len, int timeout) throws IOException {
		if(error)
			throw new IOException("Socket closed");

		if(eof)
			return -1;


		int rem = len;
		int curPos = off;
		if(buffer != null) {
			int toCopy = Math.min(rem, buffer.length - out);
			assert toCopy > 0;
			System.arraycopy(buffer, out, b, curPos, toCopy);
			out += toCopy;
			curPos += toCopy;
			rem -= toCopy;
			availableBytes -= toCopy;

			if(out == buffer.length) {
				buffer = null;
			}
			
			if(rem == 0)
				return len;
		}
		int num = 1;
		while(rem > 0) {
			
			if(len != rem && available.availablePermits() == 0)
				return len - rem;

			try {
				sock.printMessage("Waiting packet (rem="+rem+") #"+num);
				if(timeout == 0) {
					available.acquire();
				} else {
					if( ! available.tryAcquire((long) timeout, TimeUnit.MILLISECONDS)) {
						if(rem == len)
							throw new SocketTimeoutException("After "+timeout+"ms");
						else
							return len - rem;
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				return -1;
			}

			if(buffer == null) {
				DataEvent e;
				synchronized(queuedData) {
					e = queuedData.removeFirst();
				}
				if(e == null) {
					if(error) {
						throw new IOException("Socket closed");
					}
					eof = true;
					
					if(len == rem)
						return -1;
					else
						return len - rem; // number of read bytes
				}

				if(e.isByte()) {
					byteVal = e.getByte();
				} else {
					buffer = e.getData();
					out = 0;
				}

				e.signalSuccess();
			}

			if(buffer != null) {
				int toCopy = Math.min(rem, buffer.length - out);
				assert toCopy > 0;
				System.arraycopy(buffer, out, b, curPos, toCopy);
				out += toCopy;
				curPos += toCopy;
				rem -= toCopy;
				availableBytes -= toCopy;

				if(out == buffer.length) {
					buffer = null;
				}

			} else {
				b[curPos] = (byte) byteVal;
				++curPos;
				--rem;
				--availableBytes;
			}
			
			++num;

		}

		return len;
	}

	public void putBytes(DataEvent data) {
		synchronized(queuedData) {
			queuedData.addLast(data);
			availableBytes += data.getNumberOfBytes();
		}
		available.release();
	}

	public void putClose() {
		synchronized(queuedData) {
			queuedData.addLast(null);
			++availableBytes;
		}
		available.release();
	}

	public synchronized int availableBytes() {
		return availableBytes;
	}

	public void putError() {
		synchronized(queuedData) {
			queuedData.addLast(null);
			error = true;
		}
		available.release();
	}

}
