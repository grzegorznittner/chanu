package com.chanapps.four.data;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import android.database.MatrixCursor;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ChanThread {
	public static final String TAG = "ChanThread";
	
	public MatrixCursor cursor = null;
	
	public String board;
	public boolean loaded = false;
	
	public int no;
	public int sticky;
	public int closed;
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
	
	public ArrayList<ChanPost> posts = new ArrayList<ChanPost>();
	
	private void copy(ChanThread t) {
		no = t.no;
		sticky = t.sticky;
		closed = t.closed;
		now = t.now;
		time = t.time;
		name = t.name;
		sub = t.sub;
		com = t.com;
		tim = t.tim;
		filename = t.filename;
		ext = t.ext;
		w = t.w;
		h = t.h;
		tn_w = t.tn_w;
		tn_h = t.tn_h;
		md5 = t.md5;
		fsize = t.fsize;
		resto = t.resto;
		
		if (cursor != null) {
            cursor.addRow(new Object[] {no, getThumbnailUrl(), sub});
		}
	}
	
	private void addPost(ChanPost post) {
		posts.add(post);
		if (cursor != null) {
            cursor.addRow(new Object[] {post.no, post.getThumbnailUrl(), post.com});
		}
	}
	
	public void loadChanThread(Handler handler, String board, int number) {
		this.board = board;
		BufferedReader in = null;
		try {
			URL chanApi = new URL("http://api.4chan.org/" + board + "/res/" + number + ".json");
	        URLConnection tc = chanApi.openConnection();
	        in = new BufferedReader(new InputStreamReader(tc.getInputStream()));
	        Gson gson = new GsonBuilder().create();
	        
	        boolean threadRead = false;
			JsonReader reader = new JsonReader(in);
			reader.setLenient(true);
			reader.beginObject();
			String name = reader.nextName();
			reader.beginArray();
			while (reader.hasNext()) {
				if (!threadRead) {
					ChanThread thread = gson.fromJson(reader, ChanThread.class);
					copy(thread);
					Log.i(TAG, this.toString());
					threadRead = true;
				} else {
					ChanPost post = gson.fromJson(reader, ChanPost.class);
					post.board = board;
					Log.i(TAG, post.toString());
					addPost(post);
				}
		        handler.sendEmptyMessage(posts.size());
		        Thread.sleep(500);
			}
			
			
		} catch (Exception e) {
			Log.e(TAG, "Error parsing Chan thread json. " + e.getMessage(), e);
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (Exception e) {
				Log.e(TAG, "Error closing reader", e);
			}
		}
	}
	
	public String toString() {
		return "Thread " + no + " " + sub + ", thumb: " + getThumbnailUrl();
	}
}
