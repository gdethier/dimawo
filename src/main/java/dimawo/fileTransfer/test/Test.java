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
package dimawo.fileTransfer.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Semaphore;

import dimawo.agents.AgentException;
import dimawo.agents.ErrorHandler;
import dimawo.fileTransfer.FileTransferException;
import dimawo.fileTransfer.client.FileTransferClientAgent;
import dimawo.fileTransfer.client.FileTransferClientCallBack;
import dimawo.fileTransfer.client.GetFileCallBack;
import dimawo.fileTransfer.server.FileProvider;
import dimawo.fileTransfer.server.FileTransferServerAgent;




public class Test implements FileProvider, ErrorHandler, FileTransferClientCallBack {
	
	final static String[] testFileName = {
		"test0.bin",
		"test1.bin",
		"test2.bin",
		"test3.bin",
		"test4.bin"
	};

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileTransferException 
	 */
	public static void main(String[] args) throws IOException, FileTransferException {
		Test test = new Test(testFileName);
		
		// Create files
		File[] files = new File[testFileName.length];
		for(int i = 0; i < testFileName.length; ++i) {
			files[i] = new File(testFileName[i]);
			files[i].deleteOnExit();
			FileOutputStream fos = new FileOutputStream(testFileName[i]);
			byte[] data = new byte[235];
			Random r = new Random(System.currentTimeMillis());
			for(int j = 0; j < 30; ++j) {
				r.nextBytes(data);
				fos.write(data);
			}
			fos.close();
		}
		
		test.run();
	}
	
	private FileTransferServerAgent server;
	private FileTransferClientAgent client;
	
	private Semaphore sync;
	
	
	public Test(String[] fileName) {
		server = new FileTransferServerAgent(this, "TestFTPServer");
		server.setDaemon(true);
		server.setFileProvider(this);
		server.setChunkSize(512);

		client = new FileTransferClientAgent(this, "TestFTPClient");
		client.setDaemon(true);
		
		server.setCommunicator(new ServerCommunicator(server, client));
		client.setCommunicator(new ClientCommunicator(server, client));
		
		sync = new Semaphore(0);
	}
	
	private void run() throws FileTransferException, IOException {
		
		System.out.println("Starting server...");
		try {
			server.start();
		} catch (AgentException e) {
			e.printStackTrace();
			return;
		}
		
		System.out.println("Starting client...");
		try {
			client.start();
		} catch (AgentException e) {
			e.printStackTrace();
		}
		
		for(int i = 0; i < testFileName.length; ++i) {
			System.out.println("Getting file "+testFileName[i]);

			File f = File.createTempFile("test-", ".dat");
			client.getFile(new TestGetFile(TestCommunicator.SERVERDAID, testFileName[i], true, f, this));
			
			System.out.println("Waiting for file...");
			try {
				sync.acquire();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.println("File size: "+f.length());
			f.delete();
		}
		
		System.out.println("Test successful.");
	}
	
	@Override
	public File getFile(String fileUID) {
		return new File(fileUID);
	}

	@Override
	public void signalChildError(Throwable t, String errorSourceId) {
		t.printStackTrace(System.out);
		System.exit(-1);
	}

	@Override
	public void submitFile(GetFileCallBack cb) {
		if(! cb.isSuccessful()) {
			System.err.println("Could not retrieve file.");
		}
		sync.release();
	}

}
