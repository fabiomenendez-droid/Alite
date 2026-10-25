package de.phbouillon.android.framework.impl;

/* Alite - Discover the Universe on your Favorite Android Device
 * Copyright (C) 2015 Philipp Bouillon
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful and
 * fun, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see
 * http://http://www.gnu.org/licenses/gpl-3.0.txt.
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import de.phbouillon.android.framework.FileIO;
import de.phbouillon.android.games.alite.AliteConfig;
import de.phbouillon.android.games.alite.io.ObbExpansionsManager;

public class AndroidFileIO implements FileIO {
	private final AssetManager assets;
	private final File storageRoot;

	public AndroidFileIO(Context context) {
		assets = context.getAssets();
		storageRoot = context.getFilesDir();
		// Previous versions already used their own external files directory.
		// Copy once, never delete the original, and never overwrite newer data.
		File marker = new File(storageRoot, ".storage-v2");
		if (!marker.exists()) {
			try {
				File legacy = context.getExternalFilesDir(null);
				if (legacy != null && legacy.isDirectory()) {
					migrateDirectory(legacy, storageRoot);
				}
				// Old internal-storage fallback flattened commander paths.
				File[] flat = storageRoot.listFiles((dir, name) -> name.endsWith(".cmdr"));
				if (flat != null) {
					for (File commander : flat) {
						File target = getFile("commanders/" + commander.getName());
						if (!target.exists()) {
							copyLegacyFile(commander, target);
						}
					}
				}
				marker.createNewFile();
			} catch (IOException | SecurityException e) {
				android.util.Log.w("AliteStorage", "Legacy migration incomplete; will retry", e);
			}
		}
	}

	private static void migrateDirectory(File source, File target) throws IOException {
		if (!target.isDirectory() && !target.mkdirs()) {
			throw new IOException("Cannot create " + target);
		}
		File[] children = source.listFiles();
		if (children == null) throw new IOException("Cannot read " + source);
		for (File child : children) {
			File destination = new File(target, child.getName());
			if (child.isDirectory()) {
				migrateDirectory(child, destination);
			} else if (!destination.exists()) {
				copyLegacyFile(child, destination);
			}
		}
	}

	private static void copyLegacyFile(File source, File target) throws IOException {
		File parent = target.getParentFile();
		if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("Cannot create " + parent);
		File temporary = File.createTempFile("migration-", ".tmp", parent);
		try {
			try (InputStream in = new FileInputStream(source); OutputStream out = new FileOutputStream(temporary)) {
				byte[] buffer = new byte[8192];
				int count;
				while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
			}
			if (!temporary.renameTo(target)) throw new IOException("Cannot migrate " + target);
		} finally {
			temporary.delete();
		}
	}

	@Override
	public InputStream readFile(String fileName) throws IOException {
		return new FileInputStream(getFile(fileName));
	}

	@Override
	public OutputStream writeFile(String fileName) throws IOException {
		File file = getFile(fileName);
		File parent = file.getParentFile();
		if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("Cannot create " + parent);
		return new FileOutputStream(file);
	}

	@Override
	public OutputStream appendFile(String fileName) throws IOException {
		File file = getFile(fileName);
		if (!file.exists()) return writeFile(fileName);
		return new FileOutputStream(file, true);
	}

	@Override
	public boolean exists(String fileName) throws IOException {
		return getFile(fileName).exists();
	}

	@Override
	public long fileLastModifiedDate(String fileName) throws IOException {
		return getFile(fileName).lastModified();
	}

	@Override
	public boolean mkDir(String fileName) throws IOException {
		File directory = getFile(fileName);
		return directory.isDirectory() || directory.mkdirs();
	}

	public File getFile(String fileName) throws IOException {
		File file = new File(storageRoot, fileName).getCanonicalFile();
		String root = storageRoot.getCanonicalPath();
		if (!file.getPath().equals(root) && !file.getPath().startsWith(root + File.separator)) {
			throw new IOException("Path outside app storage: " + fileName);
		}
		return file;
	}

	@Override
	public byte[] readFileContents(String fileName) throws IOException {
		return readFileContents(fileName, 0);
	}

	@Override
	public byte[] readPartialFileContents(String fileName, int length) throws IOException {
		return readPartialFileContents(fileName, 0, length);
	}

	@Override
	public byte[] readPartialFileContents(String fileName, int fromOffset, int length) throws IOException {
		if (fromOffset < 0 || length < 0) throw new IOException("Negative read range");
		try (RandomAccessFile file = new RandomAccessFile(getFile(fileName), "r")) {
			file.seek(fromOffset);
			byte[] contents = new byte[length];
			file.readFully(contents);
			return contents;
		}
	}

	@Override
	public byte[] readFileContents(String fileName, int fromOffset) throws IOException {
		long length = getFile(fileName).length() - fromOffset;
		if (length < 0 || length > Integer.MAX_VALUE) throw new IOException("Invalid read range");
		return readPartialFileContents(fileName, fromOffset, (int) length);
	}

	@Override
	public File[] getFiles(final String directory, final String fileNamePattern) throws IOException {
		File[] files = getFile(directory).listFiles((dir, name) -> name.matches(fileNamePattern));
		return files == null ? new File[0] : files;
	}

	@Override
	public boolean deleteFile(String fileName) throws IOException {
		return getFile(fileName).delete();
	}

	@Override
	public boolean copyFile(String srcFileName, String dstFileName) throws IOException {		
		BufferedOutputStream bos = new BufferedOutputStream(writeFile(dstFileName));
		BufferedInputStream bis = new BufferedInputStream(readFile(srcFileName));
		byte [] buffer = new byte[2048];
		int length;
		do {
			length = bis.read(buffer);
			if (length != -1) {
				bos.write(buffer, 0, length);
			}
		} while (length != -1);
		bis.close();
		bos.close();
		return true;
	}
	
	@Override
	public void zip(String zipName, String ...fileNames) throws IOException {
		BufferedInputStream origin = null; 		
		File f = getFile(zipName);
	    FileOutputStream dest = new FileOutputStream(f); 	 
	    ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest)); 
	 
	    byte data[] = new byte[65536];
	    for (String fileName: fileNames) {
	    	FileInputStream fi = new FileInputStream(getFile(fileName)); 
	    	origin = new BufferedInputStream(fi, 65536); 
	    	ZipEntry entry = new ZipEntry(fileName.substring(fileName.lastIndexOf("/") + 1)); 
	    	out.putNextEntry(entry); 
	    	int count; 
	    	while ((count = origin.read(data, 0, 65536)) != -1) { 
	    		out.write(data, 0, count); 
	    	} 
	    	origin.close(); 	 	    	
	    }
	    out.close();
	}
	
	@Override
	public void unzip(File zipFile, File targetDirectory) throws IOException {
	    ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));
	    try {
	        ZipEntry ze;
	        int count;
	        byte [] buffer = new byte[8192];
	        while ((ze = zis.getNextEntry()) != null) {
	            File file = new File(targetDirectory, ze.getName()).getCanonicalFile();
	            if (!file.getPath().startsWith(targetDirectory.getCanonicalPath() + File.separator)) {
	                throw new IOException("Archive entry outside destination");
	            }
	            File dir = ze.isDirectory() ? file : file.getParentFile();
	            if (!dir.isDirectory() && !dir.mkdirs())
	                throw new FileNotFoundException("Failed to ensure directory: " +
	                        dir.getAbsolutePath());
	            if (ze.isDirectory())
	                continue;
	            FileOutputStream fout = new FileOutputStream(file);
	            try {
	                while ((count = zis.read(buffer)) != -1)
	                    fout.write(buffer, 0, count);
	            } finally {
	                fout.close();
	            }
	        }
	    } finally {
	        zis.close();
	    }
	}

	@Override
	public boolean existsPrivateFile(String fileName) throws IOException {
		if (AliteConfig.HAS_EXTENSION_APK) {
			return new File(ObbExpansionsManager.getInstance().getMainRoot() + "assets/" + fileName).exists();
		}
		try {
			InputStream open = assets.open(fileName);
			open.close();
			return true;
		} catch (IOException e) {
			return false;
		}
	}
	
	@Override
	public InputStream readPrivateFile(String fileName) throws IOException {
		if (AliteConfig.HAS_EXTENSION_APK) {
			return new FileInputStream(getPrivatePath(fileName));
		}
		return assets.open(fileName);
	}
	
	public AssetFileDescriptor getFileDescriptor(String fileName) throws IOException {
		if (AliteConfig.HAS_EXTENSION_APK) {
			throw new IOException("Cannot access extension assets via AssetDescriptor. Use private path instead.");
		}
		return assets.openFd(fileName);
	}

	@Override
	public String getPrivatePath(String fileName) throws IOException {
		if (AliteConfig.HAS_EXTENSION_APK) {
			String path; 		
			if (ObbExpansionsManager.getInstance() == null) {
				throw new IOException("Obb not loaded. Please try all-in-one solution from http://alite.mobi.");
			}
			if (fileName.indexOf("de.phbouillon.android.games.alite") == -1) {
				path = ObbExpansionsManager.getInstance().getMainRoot() + "assets/" + fileName;
			} else {
				int i = fileName.indexOf("assets/");
				path = ObbExpansionsManager.getInstance().getMainRoot() + fileName.substring(i) + fileName;
			}
			if (!(new File(path).exists())) {
				throw new FileNotFoundException(path + " not found.");
			}
			return path;
		} 
		throw new IOException("Cannot access local assets via path. Use AssetDescriptor instead.");
	}
}
