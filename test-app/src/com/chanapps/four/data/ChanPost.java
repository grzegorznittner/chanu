package com.chanapps.four.data;

import java.util.Date;

public class ChanPost {
	public String board;
	
	public int no;
	public String now;
	public Date created;
	public long time;
	public String name;
	public String sub;
	public String com;
	public String tim;
	public String filename;
	public String ext;
	public int w, h;
	public int tn_w, tn_h;
	public String md5;
	public int fsize;
	public int resto;
	
	public String getThumbnailUrl() {
		if (tim != null) {
			return "http://0.thumbs.4chan.org/" + board + "/thumb/" + tim + "s.jpg";
		}
		return null;
	}
	
	public String getImageUrl() {
		if (tim != null) {
			return "http://images.4chan.org/" + board + "/src/" + tim + ext;
		}
		return null;
	}
	
	public String toString() {
		return "Post " + no + " " + com + ", filename: " + filename;
	}
}
