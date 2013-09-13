/**
 * 
 */
package com.chanapps.four.gallery;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;

import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.chanapps.four.data.ChanFileStorage;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class ChanOffLineAlbum extends MediaSet {
	private static final String TAG = "ChanOffLineAlbum";
	private static final boolean DEBUG = false;
	
	private GalleryApp application;
	private String name;
	private File dir;
	private List<ChanOffLineImage> images = new ArrayList<ChanOffLineImage>();
	
	public ChanOffLineAlbum(Path path, GalleryApp application, String dir) {
		super(path, nextVersionNumber());
		Context context = application.getAndroidContext();
        File cacheFolder = ChanFileStorage.getCacheDirectory(context);
        this.dir = new File(cacheFolder, dir);
        this.application = application;
		this.name = "Cached /" + dir;
		
		loadData();
	}
	
	public ChanOffLineAlbum(Path path, GalleryApp application, File dir) {
		super(path, nextVersionNumber());
		this.application = application;
		this.dir = dir;
		this.name = "Cached /" + dir.getName();
		
		loadData();
	}

	@Override
	public String getName() {
		return name;
	}
	
	public String getDirName() {
		return dir != null ? dir.getName() : "null";
	}

	@Override
    public ArrayList<MediaItem> getMediaItem(int start, int count) {
		ArrayList<MediaItem> result = new ArrayList<MediaItem>();
		for (int i = 0; i < count; i++) {
			if (i + start < images.size()) {
				ChanOffLineImage post = images.get(i + start);
				result.add(post);
			}
		}
		return result;
	}
	
	@Override
    public int getMediaItemCount() {
		return images.size();
	}
	
	@Override
    public boolean isLeafAlbum() {
        return true;
    }

	@Override
	public long reload() {
		int prevSize = images.size();
		images.clear();
		
        loadData();
        
		if (prevSize != images.size()) {
			return nextVersionNumber();
		} else {
			return mDataVersion;
		}
	}

	private void loadData() {
        if (dir == null) {
            Log.e(TAG, "loadData() null directory, exiting");
            return;
        }
		Log.i(TAG, "Loading data from " + dir.getAbsolutePath());
		File[] files = dir.listFiles(new FilenameFilter() {
            public boolean accept(File directory, String fileName) {
            	if (DEBUG) Log.d(TAG, "Checking file " + directory.getAbsolutePath() + "/" + fileName);
                return !fileName.endsWith(".txt");
            }
        });
        if (files == null || files.length == 0) {
            Log.e(TAG, "loadData() exiting, no gallery images found in dir=" + dir);
            return;
        }
        String dirName = dir.getName();
        for (File file : files) {
        	Path path = Path.fromString("/" + ChanOffLineSource.SOURCE_PREFIX + "/" + dir.getName() + "/" + file.getName());
        	if (path.getObject() == null) {
        		images.add(new ChanOffLineImage(application, path, dirName, file));
        	} else {
        		images.add((ChanOffLineImage)path.getObject());
        	}
        }
	}
}
