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

import java.io.IOException;
import java.io.OutputStream;

public class VirtualSocketOutputStream extends OutputStream {

	private VirtualSocket sock;
	private boolean isClosed;

	public VirtualSocketOutputStream(VirtualSocket sock) {
		this.sock = sock;
		isClosed = false;
	}

	@Override
	public void write(int val) throws IOException {
		if(isClosed)
			throw new IOException("Stream is closed");
		sock.sendData(val);
	}
	
	@Override
	public void write(byte b[], int off, int len) throws IOException {
		if(isClosed)
			throw new IOException("Stream is closed");

        if (b == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > b.length) || (len < 0) ||
                   ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }
        
        byte[] data = new byte[len];
        System.arraycopy(b, off, data, 0, len);
        sock.sendData(data);
    }
	
	@Override
	public void close() {
		isClosed = true;
	}

}
