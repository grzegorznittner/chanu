/**
 * 
 */
package com.chanapps.four.data;

import java.io.File;
import java.util.Date;

import android.util.Log;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class FileDesc {
	private static final String TAG = "FileDesc";
    private static final boolean DEBUG = false;
    
	public FileDesc() {
		size = 0;
		lastModified = 0;
	}
	public FileDesc(File file) {
		try {
			path = file.getAbsolutePath();
			size = file.length();
			lastModified = file.lastModified();
		} catch (Exception e) {
			Log.e(TAG, "Error while getting file info", e);
		}
	}
	public String path;
	public long size;
	public long lastModified;
	
	public String toString() {
		return path + " " + size + "b " + new Date(lastModified) + " (" + lastModified + ")";
	}
}
