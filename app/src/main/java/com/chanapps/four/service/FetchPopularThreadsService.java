/**
 * 
 */
package com.chanapps.four.service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.chanapps.four.component.ActivityDispatcher;
import com.chanapps.four.component.URLFormatComponent;
import org.apache.commons.io.IOUtils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.chanapps.four.activity.ChanActivityId;
import com.chanapps.four.activity.ChanIdentifiedService;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.data.ChanThread;
import com.chanapps.four.data.FetchParams;
import com.chanapps.four.service.profile.NetworkProfile;
import com.chanapps.four.service.profile.NetworkProfile.Failure;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class FetchPopularThreadsService extends BaseChanService implements ChanIdentifiedService {
	private static final String TAG = FetchPopularThreadsService.class.getSimpleName();
	private static final boolean DEBUG = false;

    private boolean priority;
    private boolean backgroundLoad;

    public static boolean schedulePopularFetchService(Context context, boolean priority, boolean backgroundLoad) {
        if (!boardNeedsRefresh(context, priority)) {
            if (DEBUG) Log.i(TAG, "Skipping priority popular threads fetch service refresh unneeded");
            return false;
        }
        if (DEBUG) Log.i(TAG, "Start popular threads fetch service priority=" + priority + " background=" + backgroundLoad);
        Intent intent = new Intent(context, FetchPopularThreadsService.class);
        if (priority)
            intent.putExtra(PRIORITY_MESSAGE_FETCH, priority ? 1 : 0);
        if (backgroundLoad)
            intent.putExtra(BACKGROUND_LOAD, true);
        context.startService(intent);
        return true;
    }
    
    public static void clearServiceQueue(Context context) {
        if (DEBUG) Log.i(TAG, "Clearing chan fetch service queue");
        Intent intent = new Intent(context, FetchPopularThreadsService.class);
        intent.putExtra(CLEAR_FETCH_QUEUE, 1);
        context.startService(intent);
    }

	private static boolean boardNeedsRefresh(Context context, boolean forceRefresh) {
		FetchParams params = NetworkProfileManager.instance().getFetchParams();
        ChanBoard board = ChanFileStorage.loadBoardData(context, ChanBoard.POPULAR_BOARD_CODE);
        long now = new Date().getTime();
        if (board != null && !board.defData && board.lastFetched > 0) {
        	long refresh = forceRefresh ? params.forceRefreshDelay : params.refreshDelay;
        	if (now - board.lastFetched < refresh) {
        		if (DEBUG) Log.i(TAG, "Skiping board " + ChanBoard.POPULAR_BOARD_CODE + " fetch as it was fetched "
        				+ ((now - board.lastFetched) / 1000) + "s ago, refresh delay is " + (refresh / 1000) + "s" );
        		return false;
        	}
        }
        return true;
	}

    public FetchPopularThreadsService() {
   		super("chan_popular_threads_fetch");
   	}

    protected FetchPopularThreadsService(String name) {
   		super(name);
   	}
    
	@Override
	protected void onHandleIntent(Intent intent) {
        backgroundLoad = intent.getBooleanExtra(BACKGROUND_LOAD, false);
		if (!isChanForegroundActivity() && !backgroundLoad) {
            if (DEBUG)
                Log.i(TAG, "Not foreground activity, exiting");
			return;
		}

        NetworkProfileManager.NetworkBroadcastReceiver.checkNetwork(this.getBaseContext());
        NetworkProfile profile = NetworkProfileManager.instance().getCurrentProfile();
        if (profile.getConnectionType() == NetworkProfile.Type.NO_CONNECTION
                || profile.getConnectionHealth() == NetworkProfile.Health.NO_CONNECTION) {
            if (DEBUG) Log.i(TAG, "No network connection, exiting");
            return;
        }

		priority = intent.getIntExtra(PRIORITY_MESSAGE_FETCH, 0) > 0;
		if (DEBUG) Log.i(TAG, "Handling popular threads fetch");
		handlePopularThreadsFetch();
	}
	
	private boolean isChanForegroundActivity() {
        return ActivityDispatcher.safeGetIsChanForegroundActivity(this);
	}

    private void handlePopularThreadsFetch() {
        URL chanApi;
        try {
            String url = URLFormatComponent.getUrl(getApplicationContext(), URLFormatComponent.CHAN_FRONTPAGE_URL);
            chanApi = new URL(url);
            if (DEBUG) Log.i(TAG, "Fetching " + url);
        }
        catch (MalformedURLException e) {
            Log.e(TAG, "malformed url", e);
            return;
        }

        HttpURLConnection tc = null;
		try {

    		long startTime = Calendar.getInstance().getTimeInMillis();
            tc = (HttpURLConnection) chanApi.openConnection();
            FetchParams fetchParams = NetworkProfileManager.instance().getFetchParams();
            tc.setReadTimeout(fetchParams.readTimeout);
            tc.setConnectTimeout(fetchParams.connectTimeout);

			ChanBoard board = ChanFileStorage.loadBoardData(getBaseContext(), ChanBoard.POPULAR_BOARD_CODE);
            if (board == null || board.defData || board.threads == null || board.threads.length <= 1) {
                priority = true;
                if (DEBUG) Log.i(TAG, "Upping priority for first fetch");
            }
            if (board != null && board.lastFetched > 0 && !priority) {
            	if (DEBUG) Log.i(TAG, "IfModifiedSince set as last fetch happened "
        				+ ((startTime - board.lastFetched) / 1000) + "s ago");
                tc.setIfModifiedSince(board.lastFetched);
            }
            String contentType = tc.getContentType();
            if (DEBUG) Log.i(TAG, "Called API " + tc.getURL() + " response length=" + tc.getContentLength()
            		+ " code=" + tc.getResponseCode() + " type=" + contentType);
            if (tc.getResponseCode() == 304) {
            	if (DEBUG) Log.i(TAG, "Got 304 for " + chanApi + " so was not modified since " + board.lastFetched);
            	return;
            }

            if (tc.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                closeConnection(tc);
                if (DEBUG) Log.i(TAG, "Got 404 during popular threads fetch");
                board.lastFetched = new Date().getTime();
                ChanFileStorage.storeBoardData(getBaseContext(), board);
            } else if (contentType == null || !contentType.contains("text/html")) {
                // happens if 4chan is temporarily down or when access requires authentication to wifi router
                closeConnection(tc);
                if (DEBUG) Log.i(TAG, "Wrong content type returned board=" + board + " contentType='" + contentType + "' responseCode=" + tc.getResponseCode() + " content=" + tc.getContent().toString());
            } else {
            	// long fileSize = ChanFileStorage.storeBoardFile(getBaseContext(), boardCode, pageNo, new InputStreamReader(tc.getInputStream()));
                InputStream is = null;
                try {
                    is = new BufferedInputStream(tc.getInputStream());
                    String response = IOUtils.toString(is);
                    IOUtils.closeQuietly(is);
                    closeConnection(tc);
                    parseAndStore(board, response, startTime);
                }
                catch (IOException e) {
                    Log.e(TAG, "IO Error reading response url=" + chanApi, e);
                    NetworkProfileManager.instance().failedFetchingData(this, Failure.NETWORK);
                }
                finally {
                    IOUtils.closeQuietly(is);
                    closeConnection(tc);
                }
            }
        } catch (IOException e) {
            closeConnection(tc);
            Log.e(TAG, "IO Error fetching Chan web page url=" + chanApi, e);
            NetworkProfileManager.instance().failedFetchingData(this, Failure.NETWORK);
		} catch (Exception e) {
            closeConnection(tc);
            Log.e(TAG, "Exception fetching Chan web page url=" + chanApi, e);
            NetworkProfileManager.instance().failedFetchingData(this, Failure.WRONG_DATA);
		} catch (Error e) {
            closeConnection(tc);
            Log.e(TAG, "Error fetching Chan web page url=" + chanApi, e);
            NetworkProfileManager.instance().failedFetchingData(this, Failure.WRONG_DATA);
		} finally {
			closeConnection(tc);
		}
	}

    protected void parseAndStore(ChanBoard board, String response, long startTime) throws IOException {
        long lastFetched = new Date().getTime();
        int fetchTime = (int)(lastFetched - startTime);

        parsePopularThreads(board, response);
        board.lastFetched = lastFetched;
        ChanFileStorage.storeBoardData(getBaseContext(), board);

        ChanBoard latestBoard = ChanFileStorage.loadBoardData(getBaseContext(), ChanBoard.LATEST_BOARD_CODE);
        parseLatestPosts(latestBoard, response);
        latestBoard.lastFetched = lastFetched;
        ChanFileStorage.storeBoardData(getBaseContext(), latestBoard);

        ChanBoard imagesBoard = ChanFileStorage.loadBoardData(getBaseContext(), ChanBoard.LATEST_IMAGES_BOARD_CODE);
        parseLatestImages(imagesBoard, response);
        imagesBoard.lastFetched = lastFetched;
        ChanFileStorage.storeBoardData(getBaseContext(), imagesBoard);

        if (DEBUG) Log.w(TAG, "Fetched and stored /" + board.link + "/ in " + fetchTime + "ms, size " + response.length());
        NetworkProfileManager.instance().finishedFetchingData(this, fetchTime, (int)response.length());
        NetworkProfileManager.instance().finishedParsingData(this);
    }

	private static final String DIV_CLASS_BOX_OUTER_RIGHT_BOX = "class=\"box-outer right-box\"";
	private static final String ID_POPULAR_THREADS = "id=\"popular-threads\"";

	private void parseLatestImages(ChanBoard board, String response) {
		board.defData = false;
		
		int startIdx = response.indexOf("id=\"recent-images\"");
		int endIdx = response.indexOf(DIV_CLASS_BOX_OUTER_RIGHT_BOX, startIdx);
		
		List<ChanThread> threads = new ArrayList<ChanThread>();
		if (startIdx > 0 && endIdx > 0) {
			String popularThreadsStr = response.substring(startIdx, endIdx);
			String strings[] = popularThreadsStr.split("<li>");
			for (int i = 1; i < strings.length; i++) {
				try {
					threads.add(parseThread(strings[i]));
				} catch (Exception e) {
					Log.e(TAG, "Problem occured for: " + strings[i], e);
				}
			}
			board.threads = threads.toArray(new ChanThread[]{});
			for (ChanPost post : board.threads) {
				if (post.com == null || post.com.trim().length() == 0) {
					post.com = (post.ext != null ? post.ext.substring(1) : "") + " " + post.w + "x" + post.h;
				}
			}
		}
		if (DEBUG) Log.i(TAG, "board /" + board.link + "/ has " + board.threads.length + " threads\n\n");
	}

	private void parseLatestPosts(ChanBoard board, String response) {
		board.defData = false;
		
		int startIdx = response.indexOf("id=\"recent-threads\"");
		int endIdx = response.indexOf(DIV_CLASS_BOX_OUTER_RIGHT_BOX, startIdx);
		
		List<ChanThread> threads = new ArrayList<ChanThread>();
		if (startIdx > 0 && endIdx > 0) {
			String popularThreadsStr = response.substring(startIdx, endIdx);
			String strings[] = popularThreadsStr.split("<li>");
			for (int i = 1; i < strings.length; i++) {
				try {
					threads.add(parseThread(strings[i]));
				} catch (Exception e) {
					Log.e(TAG, "Problem occured for: " + strings[i], e);
				}
			}
			board.threads = threads.toArray(new ChanThread[]{});
		}
        if (DEBUG) Log.i(TAG, "board /" + board.link + "/ has " + board.threads.length + " threads\n\n");
    }

	
	private void parsePopularThreads(ChanBoard board, String response) {
		board.defData = false;
		
		int startIdx = response.indexOf(ID_POPULAR_THREADS);
		int endIdx = response.indexOf(DIV_CLASS_BOX_OUTER_RIGHT_BOX, startIdx);
		
		List<ChanThread> threads = new ArrayList<ChanThread>();
		if (startIdx > 0 && endIdx > 0) {
			String popularThreadsStr = response.substring(startIdx, endIdx);
			String strings[] = popularThreadsStr.split("<li>");
			for (int i = 1; i < strings.length; i++) {
				try {
                    if (DEBUG) Log.v(TAG, "paring line=" + strings[i]);
                    ChanThread thread = parseThread(strings[i]);
                    if (DEBUG) Log.v(TAG, "parsed thread /" + thread.board + "/" + thread.no
                            + " tn_w=" + thread.tn_w + " tn_h=" + thread.tn_h + " tim=" + thread.tim
                            + " thumbUrl=" + thread.thumbnailUrl(getApplicationContext()));
					threads.add(thread);
				} catch (Exception e) {
					Log.e(TAG, "Problem occured for: " + strings[i], e);
				}
			}
			board.threads = threads.toArray(new ChanThread[]{});
		}
        if (DEBUG) Log.i(TAG, "board /" + board.link + "/ has " + board.threads.length + " threads\n\n");
    }

    private ChanThread parseThread(String threadStr) {
		ChanThread thread = new ChanThread();
		ParsableString str = new ParsableString();
		str.str = threadStr;
		str.pos = threadStr.indexOf("href");
		
		thread.board = str.extract(".4chan.org/", "/");
		thread.no = Long.parseLong(str.extract("thread/", "#p"));
        thread.jumpToPostNo = Long.parseLong(str.extract("#p", "\""));
        //String image = str.extract("&lt;a href=&quot;#&quot;&gt;", "&lt;/a&gt;");
		
		if (str.moveTo(")&lt;")) {
			String imageDesc = str.extractBefore("(");
			String parts[] = imageDesc != null ? imageDesc.split(",") : new String[]{};
			if (parts.length == 3) {
				try {
					String sizeStr[] = parts[0].split(" ");
					thread.fsize = Integer.parseInt(sizeStr[0]);
					String power = sizeStr.length < 2 || sizeStr[1] == null ? "" : sizeStr[1].toLowerCase();
					if (power.contains("mb")) {
						thread.fsize *= 1024 * 1024;
					} else if (power.contains("kb")) {
						thread.fsize *= 1024;
					}
				} catch (Exception e) {
                    if (DEBUG) Log.i(TAG, "Exception parsing popular thread filesize", e);
                    thread.fsize = 0;
				}
				try {
					String dimStr[] = parts[1].trim().split("x");
					thread.w = Integer.parseInt(dimStr[0]);
					thread.h = Integer.parseInt(dimStr[1]);
                    double w = thread.w;
                    double h = thread.h;
                    double aspectRatio = w / h;
                    double biggestDimension = aspectRatio >= 1 ? w : h;
                    double scale = ChanThread.MAX_THUMBNAIL_PX / biggestDimension;
				    thread.tn_w = (int)(scale * (double)thread.w);
                    thread.tn_h = (int)(scale * (double)thread.h);

                } catch (Exception e) {
					if (DEBUG) Log.i(TAG, "Exception parsing popular thread dimensions", e);
				}
				try {
					String imgStr[] = parts[2].trim().split("\\.");
					thread.filename = imgStr[0];
					thread.ext = "." + imgStr[imgStr.length - 1];
				} catch (Exception e) {
                    if (DEBUG) Log.i(TAG, "Exception parsing popular thread filename", e);
                }
			}
		}
		String thumb = str.extract("thumbs.4chan.org/" + thread.board + "/thumb/", "&quot;");
		if (thumb != null) {
			int sIdx = thumb.indexOf("s");
			if (sIdx > 0) {
				thread.tim = Long.parseLong(thumb.substring(0, sIdx));
			}
		}
		if (str.moveTo("</a>")) {
			thread.sub = str.extractBefore(">");
		}
		str.pos = 0;
		thread.com = str.extract("<blockquote>", "</blockquote>");

        if (DEBUG) Log.i(TAG, "Board: " + thread.board + ", no: " + thread.no + ", tim: " + thread.tim
        		+ ", sub: " + thread.sub + ", com: " + thread.com
        		+ ", size: " + thread.fsize + ", wXh=" + thread.w + "x" + thread.h
        		+ ", tn_wXtn_h=" + thread.tn_w + "x" + thread.tn_h
				+ ", img: " + thread.imageUrl(getApplicationContext())
                + ", thumb: " + thread.thumbnailUrl(getApplicationContext())
				+ ", topic: " + thread.sub);
		return thread;
	}


	@Override
	public ChanActivityId getChanActivityId() {
		return new ChanActivityId(ChanBoard.POPULAR_BOARD_CODE, -1, priority);
	}

	static class ParsableString {
		String str;
		int pos;
		
		public String extract(String start, String end) {
			int startIdx = str.indexOf(start, pos);
			if (startIdx < 0) {
				return null;
			}
			int endIdx = str.indexOf(end, startIdx + start.length());
			if (endIdx > -1) {
				pos = endIdx;
				return str.substring(startIdx + start.length(), endIdx);
			}
			return null;
		}
		
		public String extractBefore(String start) {
			int startIdx = str.lastIndexOf(start, pos);
			if (startIdx > -1) {
				return str.substring(startIdx + start.length(), pos);
			} else {
				return null;
			}
		}
		
		public boolean moveTo(String text) {
			int moveIdx = str.indexOf(text, pos);
			if (moveIdx > -1) {
				pos = moveIdx;
				return true;
			} else {
				return false;
			}
		}
	}
}
