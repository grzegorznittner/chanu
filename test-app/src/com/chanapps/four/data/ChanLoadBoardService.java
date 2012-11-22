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
import com.chanapps.four.test.R;
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
public class ChanLoadBoardService extends BaseChanService {
	private static final String TAG = ChanLoadBoardService.class.getName();

    protected static ChanDatabaseHelper chanDatabaseHelper;
    protected static InsertHelper insertHelper;

	public ChanLoadBoardService() {
		super("Thread");
	}
	
	protected ChanLoadBoardService(String name) {
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
		String boardName = intent.getStringExtra(ChanHelper.BOARD_CODE);
		int boardPage = intent.getIntExtra(ChanHelper.PAGE, 0);
		Log.i(TAG, "Handling board=" + boardName + " page=" + boardPage);

        initDbHelpers();

        BufferedReader in = null;
		try {
			Set<Integer> ids = getListOfIds(boardName);
			prepareColumnIndexes();
			
			URL chanApi = new URL("http://api.4chan.org/" + boardName + "/" + boardPage + ".json");
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
                while (reader.hasNext()) { // first object is the thread post, spin over rest
                    ChanThread thread = gson.fromJson(reader, ChanThread.class);
                    thread.board = boardName;
                    boolean postExists = !ids.contains(thread.no);
                	Log.d(TAG, thread.toString() + ", existed = " + postExists);
                	addPost(thread, postExists);
                }
                reader.endArray();
                reader.endObject();
            }
            reader.endArray();
            reader.endObject();
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

        // getNumThreads();

        cleanupDbHelpers();
	}

    protected void getNumThreads() {
        if (chanDatabaseHelper == null) {
            return;
        }
        synchronized (chanDatabaseHelper) {
    		try {
    			String query = "SELECT count(*) 'num_threads'"
    					+ " FROM " + ChanDatabaseHelper.POST_TABLE
    					+ " WHERE " + ChanDatabaseHelper.POST_BOARD_NAME + "='" + boardName + "' AND "
    						+ ChanDatabaseHelper.POST_RESTO + "=0";
    			Cursor c = chanDatabaseHelper.getWritableDatabase().rawQuery(query, null);
    			int numIdx = c.getColumnIndex("num_threads");
    			for (boolean hasItem = c.moveToFirst(); hasItem; hasItem = c.moveToNext()) {
    			    Log.i(TAG, "Number of threads: " + c.getString(numIdx));
    			}
    			c.close();
    		} catch (Exception e) {
                toastUI(R.string.board_service_couldnt_read_db);
    			Log.e(TAG, "Error querying chan DB. " + e.getMessage(), e);
    		}
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

	protected void addPost(ChanThread thread, boolean postExists) throws SQLiteConstraintException {
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

            insertHelper.bind(id, thread.no);
            insertHelper.bind(boardName, thread.board);
            insertHelper.bind(resto, thread.resto);
            insertHelper.bind(time, thread.time);
            if (thread.now != null) {
                insertHelper.bind(now, thread.now);
            }
            if (thread.name != null) {
                insertHelper.bind(name, thread.name);
            }
            if (thread.com != null) {
                insertHelper.bind(com, thread.com);
            }
            if (thread.sub != null) {
                insertHelper.bind(sub, thread.sub);
            }
            if (thread.tim != null) {
                insertHelper.bind(tim, thread.tim);
            }
            if (thread.filename != null) {
                insertHelper.bind(filename, thread.filename);
            }
            if (thread.ext != null) {
                insertHelper.bind(ext, thread.ext);
            }
            if (thread.w != -1) {
                insertHelper.bind(w, thread.w);
            }
            if (thread.h != -1) {
                insertHelper.bind(h, thread.h);
            }
            if (thread.tn_w != -1) {
                insertHelper.bind(tn_w, thread.tn_w);
            }
            if (thread.tn_h != -1) {
                insertHelper.bind(tn_h, thread.tn_h);
            }
            if (thread.fsize != -1) {
                insertHelper.bind(fsize, thread.fsize);
            }
            insertHelper.bind(lastUpdate, new Date().getTime());
            insertHelper.bind(text, ChanText.getText(thread.sub, thread.com));

            insertHelper.execute();
        }
	}

}
