/**
 * 
 */
package com.chanapps.four.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.chanapps.four.component.BaseChanService;
import com.chanapps.four.activity.R;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteConstraintException;
import android.util.Log;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class ChanLoadService extends BaseChanService {
	private static final String TAG = ChanLoadService.class.getName();

    protected static ChanDatabaseHelper chanDatabaseHelper;
    protected static InsertHelper insertHelper;

    public ChanLoadService() {
   		super("board");
   	}

    protected ChanLoadService(String name) {
   		super(name);
   	}
	
	private int id = -1, boardName, now, time, name, sub, com, tim, filename, ext, w, h, tn_w, tn_h, fsize, resto, lastUpdate, text;
	
	protected Set<Integer> getListOfIds(String boardName) {
		Set<Integer> ids = new HashSet<Integer>();
        synchronized (chanDatabaseHelper) {
            if (chanDatabaseHelper == null) {
                return ids;
            }
            Cursor c = null;
            try {
                String query = "SELECT " + ChanDatabaseHelper.POST_ID
                        + " FROM " + ChanDatabaseHelper.POST_TABLE
                        + " WHERE " + ChanDatabaseHelper.POST_BOARD_NAME + "='" + boardName + "'";
                c = chanDatabaseHelper.getWritableDatabase().rawQuery(query, null);
                int indexIdx = c.getColumnIndex(ChanDatabaseHelper.POST_ID);
                for (boolean hasItem = c.moveToFirst(); hasItem; hasItem = c.moveToNext()) {
                    ids.add(c.getInt(indexIdx));
                }
            } catch (Exception e) {
                toastUI(R.string.board_service_couldnt_fetch);
                Log.e(TAG, "Error while fetching list of thread ids for board " + boardName + ". " + e.getMessage(), e);
            } finally {
                if (c != null) {
                    c.close();
                }
    		}
        }
		return ids;
	}

    protected void initDbHelpers() {
        if (chanDatabaseHelper == null) {
            chanDatabaseHelper = new ChanDatabaseHelper(getBaseContext());
        }
        if (insertHelper == null) {
            insertHelper = new InsertHelper(chanDatabaseHelper.getWritableDatabase(), ChanDatabaseHelper.POST_TABLE);
        }
    }

    protected void cleanupDbHelpers() {
        if (chanDatabaseHelper != null) {
            synchronized (chanDatabaseHelper) {
		        chanDatabaseHelper.close();
            }
            chanDatabaseHelper = null;
        }
        if (insertHelper != null) {
            synchronized (insertHelper) {
		        insertHelper.close();
            }
            insertHelper = null;
        }
    }

	@Override
	protected void onHandleIntent(Intent intent) {
		String boardCode = intent.getStringExtra(ChanHelper.BOARD_CODE);
		int boardPage = intent.getIntExtra(ChanHelper.PAGE, 0);
        long threadNo = intent.getLongExtra(ChanHelper.THREAD_NO, 0);
		Log.i(TAG, "Handling board=" + boardCode + " page=" + boardPage);

        initDbHelpers();

        BufferedReader in = null;
		try {
			prepareColumnIndexes();
			
			URL chanApi = threadNo == 0
                ? new URL("http://api.4chan.org/" + boardCode + "/" + boardPage + ".json")
                : new URL("http://api.4chan.org/" + boardCode + "/res/" + threadNo + ".json");

            URLConnection tc = chanApi.openConnection();
            Log.i(TAG, "Calling API " + tc.getURL() + " response length=" + tc.getContentLength());
	        in = new BufferedReader(new InputStreamReader(tc.getInputStream()));
            if (threadNo == 0) {
                parseBoard(boardCode, in);
            }
            else {
                parseThread(boardCode, in);
            }
        } catch (IOException e) {
            toastUI(R.string.board_service_couldnt_read);
            Log.e(TAG, "IO Error reading Chan board json. " + e.getMessage(), e);
		} catch (Exception e) {
            toastUI(R.string.board_service_couldnt_load);
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
        cleanupDbHelpers();
	}

    protected void parseBoard(String boardCode, BufferedReader in) throws IOException {
        Set<Integer> ids = getListOfIds(boardCode);

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
            while (reader.hasNext()) { // first object is the thread post, spin over rest
                ChanPost post = gson.fromJson(reader, ChanPost.class);
                post.board = boardCode;
                boolean postExists = !ids.contains(post.no);
                Log.d(TAG, post.toString() + ", existed = " + postExists);
                addPost(post, postExists);
            }
            reader.endArray();
            reader.endObject();
        }
        //reader.endArray();
        //reader.endObject();
    }

    protected void parseThread(String boardCode, BufferedReader in) throws IOException {
        Set<Integer> ids = getListOfIds(boardCode);

        Gson gson = new GsonBuilder().create();

        JsonReader reader = new JsonReader(in);
        reader.setLenient(true);
        reader.beginObject(); // has "posts" as single property
        reader.nextName(); // "posts"
        reader.beginArray();

        while (reader.hasNext()) {
            ChanPost post = gson.fromJson(reader, ChanPost.class);
            post.board = boardCode;
            boolean postExists = !ids.contains(post.no);
            Log.i(TAG, post.toString() + ", existed = " + postExists);
            addPost(post, postExists);
        }
    }

	protected void prepareColumnIndexes() {
        if (id != -1) {
            return;
        }
        if (insertHelper == null) {
            return;
        }
        synchronized (insertHelper) {
            if (insertHelper == null) {
                return;
            }
            id = insertHelper.getColumnIndex(ChanDatabaseHelper.POST_ID);
            boardName = insertHelper.getColumnIndex(ChanDatabaseHelper.POST_BOARD_NAME);
            now = insertHelper.getColumnIndex(ChanDatabaseHelper.POST_NOW);
            time = insertHelper.getColumnIndex(ChanDatabaseHelper.POST_TIME);
            name = insertHelper.getColumnIndex(ChanDatabaseHelper.POST_NAME);
            sub = insertHelper.getColumnIndex(ChanDatabaseHelper.POST_SUB);
            com = insertHelper.getColumnIndex(ChanDatabaseHelper.POST_COM);
            tim = insertHelper.getColumnIndex(ChanDatabaseHelper.POST_TIM);
            filename = insertHelper.getColumnIndex(ChanDatabaseHelper.POST_FILENAME);
            ext = insertHelper.getColumnIndex(ChanDatabaseHelper.POST_EXT);
            w = insertHelper.getColumnIndex(ChanDatabaseHelper.POST_W);
            h = insertHelper.getColumnIndex(ChanDatabaseHelper.POST_H);
            tn_w = insertHelper.getColumnIndex(ChanDatabaseHelper.POST_TN_W);
            tn_h = insertHelper.getColumnIndex(ChanDatabaseHelper.POST_TN_H);
            fsize = insertHelper.getColumnIndex(ChanDatabaseHelper.POST_FSIZE);
            resto = insertHelper.getColumnIndex(ChanDatabaseHelper.POST_RESTO);
            lastUpdate = insertHelper.getColumnIndex(ChanDatabaseHelper.POST_LAST_UPDATE);
            // constructed fields
            text = insertHelper.getColumnIndex(ChanDatabaseHelper.POST_TEXT);
        }
    }

	protected void addPost(ChanPost post, boolean postExists) throws SQLiteConstraintException {
        if (insertHelper == null) {
            return;
        }
        synchronized (insertHelper) {
            if (insertHelper == null) {
                return;
            }
            if (postExists) {
                insertHelper.prepareForInsert();
            } else {
                insertHelper.prepareForReplace();
            }

            insertHelper.bind(id, post.no);
            insertHelper.bind(boardName, post.board);
            insertHelper.bind(resto, post.resto);
            insertHelper.bind(time, post.time);
            if (post.now != null) {
                insertHelper.bind(now, post.now);
            }
            if (post.name != null) {
                insertHelper.bind(name, post.name);
            }
            if (post.com != null) {
                insertHelper.bind(com, post.com);
            }
            if (post.sub != null) {
                insertHelper.bind(sub, post.sub);
            }
            if (post.tim != null) {
                insertHelper.bind(tim, post.tim);
            }
            if (post.filename != null) {
                insertHelper.bind(filename, post.filename);
            }
            if (post.ext != null) {
                insertHelper.bind(ext, post.ext);
            }
            if (post.w != -1) {
                insertHelper.bind(w, post.w);
            }
            if (post.h != -1) {
                insertHelper.bind(h, post.h);
            }
            if (post.tn_w != -1) {
                insertHelper.bind(tn_w, post.tn_w);
            }
            if (post.tn_h != -1) {
                insertHelper.bind(tn_h, post.tn_h);
            }
            if (post.fsize != -1) {
                insertHelper.bind(fsize, post.fsize);
            }
            insertHelper.bind(lastUpdate, new Date().getTime());
            insertHelper.bind(text, ChanText.getText(post.sub, post.com));

            insertHelper.execute();
        }
	}

}
