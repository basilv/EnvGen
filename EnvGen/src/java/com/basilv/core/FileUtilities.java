package com.basilv.core;


import java.io.File;
import java.io.IOException;

/**
 * Contains utility methods for working with Files.
 */
public class FileUtilities
{

	/**
	 * Return the canonical path, or the absolute path if an error occurs.
	 * @param file  Cannot be null.
	 * @return the full path of the file.
	 */
	public static String getCanonicalPath(File file) {
		Assert.notNull("file", file);
		try {
			return file.getCanonicalPath();
		} catch (IOException e) {
			return file.getAbsolutePath();
		}
	}
	
	
	/**
	 * Delete the specified directory and all its contents. 
	 * @param file  Cannot be null. If a file is specified, it is deleted.
	 */
	public static void deleteRecursively(File file) {
		Assert.notNull("file", file);
		
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			for (int i = 0; i < files.length; i++) {
				deleteRecursively(files[i]);
			}
		}
		deleteFile(file);
	}

	public static void deleteFile(File file) {
		Assert.notNull("file", file);
		
		if (!file.delete()) {
			// Perhaps we couldn't delete the file because it no longer exists.
			if (file.exists()) {
				throw new RuntimeException("Unable to delete file '" + FileUtilities.getCanonicalPath(file) + "'.");
			}
		}
		
	}
}
