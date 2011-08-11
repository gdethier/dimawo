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
package dimawo.middleware.fileSystem.fileDB;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dimawo.middleware.fileSystem.FileSystemAgent;
import dimawo.middleware.fileSystem.fileDB.FileDescriptor.FileMode;




public class LocalFilesDB {
	
	private FileSystemAgent fs;
	private TreeMap<String, FileDescriptor> files;
	
	
	public LocalFilesDB(FileSystemAgent fs) {
		this.fs = fs;
		files = new TreeMap<String, FileDescriptor>();
	}

	
	public LinkedList<String> getFileList(Pattern p) {
		LinkedList<String> listedFiles = new LinkedList<String>();
		Iterator<String> fileIt = files.keySet().iterator();
		while(fileIt.hasNext()) {
			String fileUID = fileIt.next();
			Matcher m = p.matcher(fileUID);
			if(m.matches()) {
				listedFiles.add(fileUID);
			}
		}
		return listedFiles;
	}


	/**
	 * @param p
	 * @return The list of fileUIDs of deleted files.
	 */
	public LinkedList<String> deleteFiles(Pattern p) {

		LinkedList<String> fileUIDs = new LinkedList<String>();

		Iterator<Entry<String, FileDescriptor>> fileIt = files.entrySet().iterator();
		while(fileIt.hasNext()) {

			Entry<String, FileDescriptor> e = fileIt.next();
			FileDescriptor fd = e.getValue();

			if( ! fd.isReadOnly()) {

				Matcher m = p.matcher(e.getKey());
				if(m.matches()) {
					
					printMessage("Deleting file "+e.getKey()+" ("+fd.getFile().getName()+").");
					fileUIDs.add(e.getKey());

					File f = fd.getFile();
					if(f != null) {
						if(!f.delete()) {
							printMessage("Could not delete file "+f.getName()+" with fileUID "+e.getKey());
						}
					} else {
						printMessage("File "+e.getKey()+" has only remote locations.");
					}
					fileIt.remove();
				}

			} else {
				printMessage("File "+e.getKey()+" is read-only and will not be deleted.");
			}
		}
		
		return fileUIDs;
	}
	
	private void deleteFile(String fileUID) {
		FileDescriptor desc = files.remove(fileUID);
		if(desc != null) {
			desc.deleteFile();
		}
	}
	
	private void printMessage(String string) {
		fs.printMessage("[LocalFilesDB] "+string);
	}


	public void deleteAllFiles() {
		Iterator<Entry<String, FileDescriptor>> fileIt = files.entrySet().iterator();
		while(fileIt.hasNext()) {

			Entry<String, FileDescriptor> e = fileIt.next();
			FileDescriptor fd = e.getValue();

			if( ! fd.isReadOnly()) {

				printMessage("Deleting file "+e.getKey()+" ("+fd.getFile().getName()+").");
				File f = fd.getFile();
				if(f != null) {
					if(!f.delete()) {
						printMessage("[FileDB] Could not delete file "+f.getName()+" with fileUID "+e.getKey());
					}
				} else {
					printMessage("[FileDB] File "+e.getKey()+" has only remote locations.");
				}
				fileIt.remove();

			} else {
				printMessage("[FileDB] File "+e.getKey()+" is read-only and will not be deleted.");
			}

		}
		
		files.clear();
	}

	public boolean fileExists(String fileUID) {

		FileDescriptor fd = files.get(fileUID);
		return fd != null;

	}

	public FileDescriptor getFileDescriptor(String fileUID) {
		return files.get(fileUID);
	}

	public void addFile(String fileUID, File localFile, FileMode fileMode) throws FileDBException {
		if(files.containsKey(fileUID))
			throw new FileDBException("File already present");
		
		files.put(fileUID, new FileDescriptor(localFile, fileMode));
	}
	
	public FileDescriptor removeFile(String fileUID) {
		return files.remove(fileUID);
	}


	public void removeFiles(LinkedList<String> fileUIDs) {
		Iterator<String> idIt = fileUIDs.iterator();
		while(idIt.hasNext()) {
			String fileUID = idIt.next();
			deleteFile(fileUID);
		}
	}

}
