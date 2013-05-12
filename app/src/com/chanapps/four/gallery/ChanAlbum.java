/**
 * 
 */
package com.chanapps.four.gallery;

import java.util.ArrayList;
import java.util.List;

import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.data.ChanThread;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class ChanAlbum extends MediaSet {
	private GalleryApp application;
	private String name;
	private String board;
	private long threadNo;
	private List<ChanPost> posts = new ArrayList<ChanPost>();
	
	public ChanAlbum(Path path, GalleryApp application, ChanThread thread) {
		super(path, nextVersionNumber());
        if (thread == null) { // in case something went wrong
            this.board = ChanBoard.DEFAULT_BOARD_CODE;
            ChanBoard board = ChanBoard.getBoardByCode(application.getAndroidContext(), this.board);
            this.name = (board == null ? "" : board.name + " ")
                    + "/" + this.board + "/";
            this.threadNo = 0;
            return;
        }
		this.application = application;
		this.board = thread.board;
		this.name = "/" + board + "/" + thread.no;
		this.threadNo = thread.no;
		for (ChanPost post : thread.posts) {
			if (post.tim != 0) {
				posts.add(post);
			}
		}
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
    public ArrayList<MediaItem> getMediaItem(int start, int count) {
		ArrayList<MediaItem> result = new ArrayList<MediaItem>();
		for (int i = 0; i < count; i++) {
			if (i + start < posts.size()) {
				ChanPost post = posts.get(i + start);
				Path path = Path.fromString("/chan/" + post.board + "/" + threadNo + "/" + post.no);
				if (path.getObject() == null) {
					result.add(new ChanImage(application, path, post));
				} else {
					result.add((MediaItem)path.getObject());
				}
			}
		}
		return result;
	}
	
	@Override
    public int getMediaItemCount() {
		return posts.size();
	}
	
	@Override
    public boolean isLeafAlbum() {
        return true;
    }

	@Override
	public long reload() {
        if (application == null)
            return mDataVersion;
		ChanThread thread = ChanFileStorage.loadThreadData(application.getAndroidContext(), board, threadNo);
		int prevSize = posts.size();
		posts.clear();
		for (ChanPost post : thread.posts) {
			if (post.tim != 0) {
				posts.add(post);
			}
		}
		if (prevSize != posts.size()) {
			return nextVersionNumber();
		} else {
			return mDataVersion;
		}
	}
}
