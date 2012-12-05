package com.chanapps.four.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;
import com.chanapps.four.activity.R;

import java.util.*;

public class ChanPost {
	public static final String TAG = ChanPost.class.getSimpleName();

	public String toString() {
		return "Thread " + no + " " + com + ", thumb: " + getThumbnailUrl() + " tn_w: " + tn_w + " tn_h: " + tn_h;
	}

    public String board;

   	public long no = -1;
  	public int sticky = 0;
   	public String now;
   	public Date created;
   	public long time = -1;
   	public String name;
   	public String sub;
   	public String com;
   	public String tim;
   	public String filename;
   	public String ext;
   	public int w = 0;
    public int h = 0;
   	public int tn_w = 0;
    public int tn_h = 0;
  	public int fsize = -1;
  	public int resto = -1;
  	public int replies = -1;
  	public int images = -1;
  	public int omitted_posts = -1;
  	public int omitted_images = -1;
  	public int bumplimit = 0;
  	public int imagelimit = 0;

   	public String getThumbnailUrl() {
   		if (tim != null) {
   			return "http://0.thumbs.4chan.org/" + board + "/thumb/" + tim + "s.jpg";
   		}
   		return null;
   	}
   /*
   	public String getImageUrl() {
   		if (tim != null) {
   			return "http://images.4chan.org/" + board + "/src/" + tim + ext;
   		}
   		return null;
   	}
   */

    public String getText() {
        return ChanText.getText(sub, com);
    }

}
