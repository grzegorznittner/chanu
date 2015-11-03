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
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.chanapps.four.data.ChanFileStorage;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class ChanOffLineAlbumSet extends MediaSet {
	private static final String TAG = "ChanOffLineAlbumSet";
	
	private GalleryApp application;
	private String name;
	private List<ChanOffLineAlbum> boards = new ArrayList<ChanOffLineAlbum>();
	
	public ChanOffLineAlbumSet(Path path, GalleryApp application) {
		super(path, nextVersionNumber());
		this.application = application;
		this.name = "Cached images";
		
		loadData();
	}

	@Override
	public String getName() {
		return name;
	}
	
	@Override
    public MediaSet getSubMediaSet(int index) {
        return boards.get(index);
    }

    @Override
    public int getSubMediaSetCount() {
        return boards.size();
    }

	@Override
	public long reload() {
		int prevSize = boards.size();
		boards.clear();
		
		loadData();
        
		if (prevSize != boards.size()) {
			return nextVersionNumber();
		} else {
			return mDataVersion;
		}
	}

	private void loadData() {
		Context context = application.getAndroidContext();
        File cacheFolder = ChanFileStorage.getCacheDirectory(context);
        File[] dirs = cacheFolder.listFiles(new FilenameFilter() {
            public boolean accept(File directory, String fileName) {
                return new File(directory, fileName).isDirectory();
            }
        });
        
        for (File dir : dirs) {
        	if (!folderContainsImages(dir)) {
        		continue;
        	}
        	Log.i(TAG, "Creating album object for folder " + dir.getAbsolutePath());
        	Path path = Path.fromString("/" + ChanOffLineSource.SOURCE_PREFIX + "/" + dir.getName());
        	if (path.getObject() == null) {
        		boards.add(new ChanOffLineAlbum(path, application, dir));
        	} else {
        		boards.add((ChanOffLineAlbum)path.getObject());
        	}
        }
	}
	
	private boolean folderContainsImages(File dir) {
		File[] files = dir.listFiles(new FilenameFilter() {
            public boolean accept(File directory, String fileName) {
                return !fileName.endsWith(".txt");
            }
        });
		return files.length > 0;
	}
}
