/**
 * 
 */
package com.chanapps.four.activity;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import android.content.Context;
import android.content.Intent;
import android.util.Base64;
import android.util.Log;

import com.chanapps.four.data.LastActivity;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class ChanActivityId implements Serializable {

    public static final String TAG = ChanActivityId.class.getSimpleName();
    public static final boolean DEBUG = false;

    public LastActivity activity;
    public String boardCode = null;
    public int pageNo = -1;
    public long threadNo = 0;
    public long secondaryThreadNo = 0;
    public long postNo = 0;
    public int position = 0;
    public GalleryViewActivity.ViewType viewType = null;
    public String text = "";
    public String quoteText = "";
    public String threadUpdateMessage = null;
    public boolean priority = false;

    public ChanActivityId (String boardCode, int pageNo, boolean priority) {
		this.activity = null;
		this.boardCode = boardCode;
		this.pageNo = pageNo;
		this.priority = priority;
	}
	public ChanActivityId (String boardCode, long threadNo, boolean priority) {
		this.activity = null;
		this.boardCode = boardCode;
		this.threadNo = threadNo;
		this.priority = priority;
	}
	public ChanActivityId (LastActivity activity) {
		this.activity = activity;
	}
	public ChanActivityId (LastActivity activity, String boardCode) {
		this.activity = activity;
		this.boardCode = boardCode;
	}
	public ChanActivityId (LastActivity activity, String boardCode, String text) {
		this.activity = activity;
		this.boardCode = boardCode;
        this.text = text;
	}
	public ChanActivityId (LastActivity activity, String boardCode, long threadNo) {
		this.activity = activity;
		this.boardCode = boardCode;
		this.threadNo = threadNo;
	}
	public ChanActivityId (LastActivity activity, String boardCode, long threadNo, long postNo) {
		this.activity = activity;
		this.boardCode = boardCode;
		this.threadNo = threadNo;
        this.postNo = postNo;
	}
	public ChanActivityId (LastActivity activity, String boardCode, long threadNo, long postNo,
                           GalleryViewActivity.ViewType viewType) {
		this.activity = activity;
		this.boardCode = boardCode;
		this.threadNo = threadNo;
		this.postNo = postNo;
        this.viewType = viewType;
        if (DEBUG) Log.i(TAG, "set viewType=" + viewType);
	}

    public ChanActivityId (LastActivity activity, String boardCode, long threadNo, long postNo, String text) {
        this.activity = activity;
        this.boardCode = boardCode;
        this.threadNo = threadNo;
        this.postNo = postNo;
        this.text = text;
    }

	@Override
	public boolean equals(Object o) {
		if (o instanceof ChanActivityId) {
			ChanActivityId obj = (ChanActivityId)o;
			if (obj.activity == activity) {
				if (boardCode == null) {
					if (obj.boardCode != null) {
						return false;
					}
				} else if (!boardCode.equals(obj.boardCode)) {
					return false;
				} else if (pageNo != obj.pageNo) {
					return false;
				}

				if (obj.threadNo != threadNo) {
					return false;
				}
				if (obj.postNo != postNo) {
					return false;
				}
                if (obj.viewType != viewType) {
                    return false;
                }

				return true;
			}
		}
		return false;
	}
	
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		if (activity != null)
			buffer.append(activity);
		else
            buffer.append("Service for ");
        if (boardCode != null)
            buffer.append("/").append(boardCode);
        if (threadNo != 0)
            buffer.append("/").append(threadNo);
        if (postNo != 0)
            buffer.append("#").append(postNo);
        if (viewType != null)
            buffer.append(" viewType=" + viewType);
        if (text != null && !text.isEmpty())
            buffer.append(" text=" + text);
        return buffer.toString();
	}

    public Intent createIntent(Context context) {
        switch (activity) {
            case ABOUT_ACTIVITY:
                return AboutActivity.createIntent(context);
            case SETTINGS_ACTIVITY:
                return SettingsActivity.createIntent(context);
            case GALLERY_ACTIVITY:
                return GalleryViewActivity.createIntent(
                        context,
                        boardCode,
                        threadNo,
                        postNo,
                        viewType);
            case POST_REPLY_ACTIVITY:
                return PostReplyActivity.createIntent(
                        context,
                        boardCode,
                        threadNo,
                        postNo,
                        text,
                        text);
            case THREAD_ACTIVITY:
                return ThreadActivity.createIntent(
                        context,
                        boardCode,
                        threadNo,
                        postNo,
                        text);
            case BOARD_ACTIVITY:
            default:
                return BoardActivity.createIntent(
                        context,
                        boardCode,
                        text);
        }
    }

    public String serialize() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(baos);
            oos.writeObject(this);
            oos.close();
            byte[] bytes = baos.toByteArray();
            byte[] encodedBytes = Base64.encode(bytes, Base64.DEFAULT);
            return new String(encodedBytes);
        }
        catch (IOException e) {
            if (DEBUG) Log.e(TAG, "serialize() io exception " + this, e);
            return null;
        }
        finally {
            try {
                if (oos != null)
                    oos.close();
            }
            catch (IOException e) {
                if (DEBUG) Log.e(TAG, "serialize() close io exception " + this);
            }
        }
    }

    public static ChanActivityId deserialize(String s) {
        byte[] data = Base64.decode(s, Base64.DEFAULT);
        InputStream bais = new BufferedInputStream(new ByteArrayInputStream(data));
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(bais);
            Object o = ois.readObject();
            ois.close();
            if (!(o instanceof ChanActivityId)) {
                if (DEBUG) Log.e(TAG, "deserialize() wrong class " + o.getClass());
                return null;
            }
            return (ChanActivityId)o;
        }
        catch (IOException e) {
            if (DEBUG) Log.e(TAG, "deseriliaze() io exception " + s, e);
            return null;
        }
        catch (ClassNotFoundException e) {
            if (DEBUG) Log.e(TAG, "deseriliaze() class not found exception " + s, e);
            return null;
        }
        finally {
            try {
                if (ois != null)
                    ois.close();
            }
            catch (IOException e) {
                if (DEBUG) Log.e(TAG, "deserialize() close io exception " + s);
            }
        }
    }

}
