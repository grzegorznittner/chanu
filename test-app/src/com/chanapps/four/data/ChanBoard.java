package com.chanapps.four.data;

import android.database.MatrixCursor;
import android.os.Handler;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ChanBoard {
	public static final String TAG = ChanBoard.class.getSimpleName();
	
	public MatrixCursor cursor = null;
	
	public String board;
	public boolean loaded = false;
	
	public int no;

	public List<ChanThread> threads = Collections.synchronizedList(new ArrayList<ChanThread>());
	
	private void addThread(ChanThread thread) {
		threads.add(thread);
		if (cursor != null) {
            cursor.addRow(new Object[] {thread.no, thread.getThumbnailUrl(), ChanText.sanitizeText(thread.com)});
		}
	}
	
	public void loadChanBoard(Handler handler, String board, int number) {
		this.board = board;
		BufferedReader in = null;
		try {
			URL chanApi = new URL("http://api.4chan.org/" + board + "/" + number + ".json");
	        URLConnection tc = chanApi.openConnection();
            Log.i(TAG, "Calling API " + tc.getURL() + " response length=" + tc.getContentLength());
	        in = new BufferedReader(new InputStreamReader(tc.getInputStream()));
	        Gson gson = new GsonBuilder().create();
	        
			JsonReader reader = new JsonReader(in);
			reader.setLenient(true);
			reader.beginObject(); // has "threads" as single property
			reader.nextName(); // "threads"
			reader.beginArray();
			while (reader.hasNext()) { // iterate over threads
                reader.beginObject(); // has "posts" as single property
                reader.nextName(); // "posts"
                reader.beginArray();
                boolean threadRead = false;
                while (reader.hasNext()) { // first object is the thread post, spin over rest
                    if (!threadRead) {
                        ChanThread thread = gson.fromJson(reader, ChanThread.class);
                        thread.board = board;
                        Log.i(TAG, thread.toString());
                        addThread(thread);
                        threadRead = true;
                        handler.sendEmptyMessage(threads.size());
                        Thread.sleep(100);
                    } else { // we ignore but need to do the parser step
                        ChanPost post = gson.fromJson(reader, ChanPost.class);
                        post.board = board;
                        Log.i(TAG, post.toString());
                    }
                }
                reader.endArray();
                reader.endObject();
            }
            reader.endArray();
            reader.endObject();
		} catch (Exception e) {
			Log.e(TAG, "Error parsing Chan board json. " + e.getMessage(), e);
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
        return "Board " + board + " page " + no;
	}
}
