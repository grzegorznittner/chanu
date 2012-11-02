package com.chanapps.four.data;

import android.database.MatrixCursor;
import android.graphics.Point;
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
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class ChanBoard {
	public static final String TAG = ChanBoard.class.getSimpleName();

    public ChanBoard() {

    }

	private ChanBoard(Type type, String name, String link, int iconId,
			boolean workSafe, boolean classic, boolean textOnly) {
		this.type = type;
		this.name = name;
		this.link = link;
		this.iconId = iconId;
		this.workSafe = workSafe;
		this.classic = classic;
		this.textOnly = textOnly;
	}
	
	public enum Type {JAPANESE_CULTURE, INTERESTS, CREATIVE, ADULT, OTHER, MISC};
	
	public MatrixCursor cursor = null;
	
	public String board;
	public boolean loaded = false;
	public String name;
    public String link;
    public int iconId;
	public int no;

	public List<ChanThread> threads = Collections.synchronizedList(new ArrayList<ChanThread>());

    public Map<String, Point> thumbnailToPointMap = new HashMap<String, Point>();

	public Type type;
	public boolean workSafe;
	public boolean classic;
	public boolean textOnly;

	private void addThread(ChanThread thread) {
		threads.add(thread);
		if (cursor != null) {
            cursor.addRow(new Object[] {thread.no, thread.getThumbnailUrl(), ChanText.sanitizeText(thread.getText())});
		}
        thumbnailToPointMap.put(thread.getThumbnailUrl(), new Point(thread.tn_w, thread.tn_h));
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

	private static List<ChanBoard> boards = null;
	private static Map<Type, List<ChanBoard>> boardsByType = null;

	public static List<ChanBoard> getBoards() {
		if (boards == null) {
			initBoards();
		}
		return boards;
	}
	
	public static boolean isValidBoardCode(String boardCode) {
		for (ChanBoard board : getBoards()) {
			if (board.link.equals(boardCode)) {
				return true;
			}
		}
		return false;
	}
	
	public static List<ChanBoard> getBoardsByType(Type type) {
		if (boards == null) {
			initBoards();
		}
		return boardsByType.get(type);
	}

	private static void initBoards() {
		boards = new ArrayList<ChanBoard>();
		boardsByType = new HashMap<Type, List<ChanBoard>>();
		
		List<ChanBoard> boardType = new ArrayList<ChanBoard>();
		boardType.add(new ChanBoard(Type.JAPANESE_CULTURE, "Anime & Manga", "a", 0, true, true, false));
		boardType.add(new ChanBoard(Type.JAPANESE_CULTURE, "Anime/Cute", "c", 0, true, true, false));
		boardType.add(new ChanBoard(Type.JAPANESE_CULTURE, "Anime/Wallpapers", "w", 0, true, true, false));
		boardType.add(new ChanBoard(Type.JAPANESE_CULTURE, "Mecha", "m", 0, true, true, false));
		boardType.add(new ChanBoard(Type.JAPANESE_CULTURE, "Cosplay & EGL", "cgl", 0, true, true, false));
		boardType.add(new ChanBoard(Type.JAPANESE_CULTURE, "Cute/Male", "cm", 0, true, true, false));
		boardType.add(new ChanBoard(Type.JAPANESE_CULTURE, "Transportation", "n", 0, true, true, false));
		boardType.add(new ChanBoard(Type.JAPANESE_CULTURE, "Otaku Culture", "jp", 0, true, true, false));
		boardType.add(new ChanBoard(Type.JAPANESE_CULTURE, "Pokemon", "vp", 0, true, true, false));
		boardsByType.put(Type.JAPANESE_CULTURE, boardType);
		boards.addAll(boardType);
		
		boardType = new ArrayList<ChanBoard>();
		boardType.add(new ChanBoard(Type.INTERESTS, "Video Games", "v", 0, true, true, false));
		boardType.add(new ChanBoard(Type.INTERESTS, "Video Game Generals", "vg", 0, true, true, false));
		boardType.add(new ChanBoard(Type.INTERESTS, "Comics & Cartoons", "co", 0, true, true, false));
		boardType.add(new ChanBoard(Type.INTERESTS, "Technology", "g", 0, true, true, false));
		boardType.add(new ChanBoard(Type.INTERESTS, "Television & Film", "tv", 0, true, true, false));
		boardType.add(new ChanBoard(Type.INTERESTS, "Weapons", "k", 0, true, true, false));
		boardType.add(new ChanBoard(Type.INTERESTS, "Auto", "o", 0, true, true, false));
		boardType.add(new ChanBoard(Type.INTERESTS, "Animals & Nature", "an", 0, true, true, false));
		boardType.add(new ChanBoard(Type.INTERESTS, "Traditional Games", "tg", 0, true, true, false));
		boardType.add(new ChanBoard(Type.INTERESTS, "Sports", "sp", 0, true, true, false));
		boardType.add(new ChanBoard(Type.INTERESTS, "Science & Math", "sci", 0, true, true, false));
		boardType.add(new ChanBoard(Type.INTERESTS, "International", "int", 0, true, true, false));
		boardsByType.put(Type.INTERESTS, boardType);
		boards.addAll(boardType);
		
		boardType = new ArrayList<ChanBoard>();
		boardType.add(new ChanBoard(Type.CREATIVE, "Oekaki", "i", 0, true, true, false));
		boardType.add(new ChanBoard(Type.CREATIVE, "Papercraft & Origami", "po", 0, true, true, false));
		boardType.add(new ChanBoard(Type.CREATIVE, "Photography", "p", 0, true, true, false));
		boardType.add(new ChanBoard(Type.CREATIVE, "Food & Cooking", "ck", 0, true, true, false));
		boardType.add(new ChanBoard(Type.CREATIVE, "Artwork/Critique", "ic", 0, true, true, false));
		boardType.add(new ChanBoard(Type.CREATIVE, "Wallpapers/General", "wg", 0, true, true, false));
		boardType.add(new ChanBoard(Type.CREATIVE, "Music", "mu", 0, true, true, false));
		boardType.add(new ChanBoard(Type.CREATIVE, "Fashion", "fa", 0, true, true, false));
		boardType.add(new ChanBoard(Type.CREATIVE, "Toys", "toy", 0, true, true, false));
		boardType.add(new ChanBoard(Type.CREATIVE, "3DCG", "3", 0, true, true, false));
		boardType.add(new ChanBoard(Type.CREATIVE, "Do-It-Yourself", "diy", 0, true, true, false));
		boardType.add(new ChanBoard(Type.CREATIVE, "Worksafe GIF", "wsg", 0, true, true, false));
		boardsByType.put(Type.CREATIVE, boardType);
		boards.addAll(boardType);
		
		boardType = new ArrayList<ChanBoard>();
		boardType.add(new ChanBoard(Type.ADULT, "Sexy Beautiful Women", "s", 0, false, true, false));
		boardType.add(new ChanBoard(Type.ADULT, "Hardcore", "hc", 0, false, true, false));
		boardType.add(new ChanBoard(Type.ADULT, "Handsome Men", "hm", 0, false, true, false));
		boardType.add(new ChanBoard(Type.ADULT, "Hentai", "h", 0, false, true, false));
		boardType.add(new ChanBoard(Type.ADULT, "Ecchi", "e", 0, false, true, false));
		boardType.add(new ChanBoard(Type.ADULT, "Yuri", "u", 0, false, true, false));
		boardType.add(new ChanBoard(Type.ADULT, "Hentai/Alternative", "d", 0, false, true, false));
		boardType.add(new ChanBoard(Type.ADULT, "Yaoi", "y", 0, false, true, false));
		boardType.add(new ChanBoard(Type.ADULT, "Torrents", "t", 0, false, true, false));
		boardType.add(new ChanBoard(Type.ADULT, "High Resolution", "hr", 0, false, true, false));
		boardType.add(new ChanBoard(Type.ADULT, "Animated GIF", "gif", 0, false, true, false));
		boardsByType.put(Type.ADULT, boardType);
		boards.addAll(boardType);
		
		boardType = new ArrayList<ChanBoard>();
		boardType.add(new ChanBoard(Type.OTHER, "4chan Discussion", "q", 0, false, true, false));
		boardType.add(new ChanBoard(Type.OTHER, "Travel", "trv", 0, false, true, false));
		boardType.add(new ChanBoard(Type.OTHER, "Health & Fitness", "fit", 0, false, true, false));
		boardType.add(new ChanBoard(Type.OTHER, "Paranormal", "x", 0, false, true, false));
		boardType.add(new ChanBoard(Type.OTHER, "Literature", "lit", 0, false, true, false));
		boardType.add(new ChanBoard(Type.OTHER, "Advice", "adv", 0, false, true, false));
		boardType.add(new ChanBoard(Type.OTHER, "Pony", "mlp", 0, false, true, false));
		boardsByType.put(Type.OTHER, boardType);
		boards.addAll(boardType);
		
		boardType = new ArrayList<ChanBoard>();
		boardType.add(new ChanBoard(Type.MISC, "Random", "b", 0, false, true, false));
		boardType.add(new ChanBoard(Type.MISC, "Request", "r", 0, false, true, false));
		boardType.add(new ChanBoard(Type.MISC, "ROBOT9001", "r9k", 0, false, true, false));
		boardType.add(new ChanBoard(Type.MISC, "Politically Incorrect", "pol", 0, false, true, false));
		boardType.add(new ChanBoard(Type.MISC, "Social", "soc", 0, false, true, false));
		boardsByType.put(Type.MISC, boardType);
		boards.addAll(boardType);
	}
}
