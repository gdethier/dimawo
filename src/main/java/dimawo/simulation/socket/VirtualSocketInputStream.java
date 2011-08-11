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
import java.io.InputStream;


public class VirtualSocketInputStream extends InputStream {
	
	private VirtualSocket sock;

	private int timeout;
	private boolean isClosed;
	
	
	public VirtualSocketInputStream(VirtualSocket sock) {
		this.sock = sock;

		isClosed = false;
		timeout = 0;
	}

	@Override
	public int read() throws IOException {
		if(isClosed)
			throw new IOException("Stream closed.");

		try {
			return sock.readByteFromBuffer(timeout);
		} catch (EOFException e) {
			return -1;
		} catch (IOException e) {
			sock.printMessage("IOException while reading, closing input stream:");
			sock.printMessage(e);
			isClosed = true;
			throw e;
		}
	}
	
	@Override
	public int read(byte b[], int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        try {
        	return sock.fillWithBytesFromBuf(b, off, len, timeout);
        } catch(EOFException e) {
        	return -1;
        }
    }

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	@Override
	public int available() {
		return sock.availableBytesInBuffer();
	}
	
	@Override
	public void close() throws IOException {
		if(isClosed)
			throw new IOException("Stream already closed.");
		
		sock.printMessage("Closing input stream.");
		
		isClosed = true;
	}

}
