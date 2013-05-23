package com.chanapps.four.data;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.annotate.JsonDeserialize;

import android.content.Context;
import android.database.MatrixCursor;

import com.chanapps.four.activity.R;

public class ChanThread extends ChanPost {
    private static final boolean DEBUG = false;

    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingLongDeserializer.class)
    public long lastFetched = 0;

    @JsonDeserialize(using=JacksonNonBlockingObjectMapperFactory.NonBlockingBooleanDeserializer.class)
    public boolean loadedFromBoard = false;

	public ChanPost posts[] = new ChanPost[0];

    public static final String THREAD_COMPOSITE_ID = "_id";
    public static final String THREAD_BOARD_CODE = "threadBoardCode";
    public static final String THREAD_NO = "threadNo";
    public static final String THREAD_TITLE = "threadTitle";
    public static final String THREAD_SUBJECT = "threadSub";
    public static final String THREAD_HEADLINE = "threadHeadline";
    public static final String THREAD_TEXT = "threadText";
    public static final String THREAD_THUMBNAIL_URL = "threadThumb";
    public static final String THREAD_COUNTRY_FLAG_URL = "threadFlag";
    public static final String THREAD_CLICK_URL = "threadClick";
    public static final String THREAD_NUM_REPLIES = "threadNumReplies";
    public static final String THREAD_NUM_IMAGES = "threadNumImages";
    public static final String THREAD_FLAGS = "threadFlags";

    public static final int THREAD_FLAG_DEAD = 0x01;
    public static final int THREAD_FLAG_CLOSED = 0x02;
    public static final int THREAD_FLAG_STICKY = 0x04;
    public static final int THREAD_FLAG_AD = 0x08;
    public static final int THREAD_FLAG_BOARD = 0x10;
    public static final int THREAD_FLAG_TITLE = 0x20;

    public static final String[] THREAD_COLUMNS = {
            THREAD_COMPOSITE_ID,
            THREAD_BOARD_CODE,
            THREAD_NO,
            THREAD_TITLE,
            THREAD_SUBJECT,
            THREAD_HEADLINE,
            THREAD_TEXT,
            THREAD_THUMBNAIL_URL,
            THREAD_COUNTRY_FLAG_URL,
            THREAD_CLICK_URL,
            THREAD_NUM_REPLIES,
            THREAD_NUM_IMAGES,
            THREAD_FLAGS
    };

    public static MatrixCursor buildMatrixCursor() {
        return new MatrixCursor(THREAD_COLUMNS);
    }

    private static int threadFlags(ChanPost post) {
        int flags = 0;
        if (post.isDead)
            flags |= THREAD_FLAG_DEAD;
        if (post.closed > 0)
            flags |= THREAD_FLAG_CLOSED;
        if (post.sticky > 0)
            flags |= THREAD_FLAG_STICKY;
        return flags;
    }

    public static Object[] makeRow(Context context, ChanPost post, String query) {
        String id = post.board + "/" + post.no;
        String[] textComponents = post.textComponents(query);
        return new Object[] {
                id.hashCode(),
                post.board,
                post.no,
                "",
                textComponents[0],
                post.headline(query, true, null),
                textComponents[1],
                post.thumbnailUrl(),
                post.countryFlagUrl(),
                "",
                post.replies,
                post.images,
                threadFlags(post)
        };
    }

    public static Object[] makeBoardRow(Context context, String boardCode, String boardName, int boardImageResourceId) {
        return new Object[] {
                boardCode.hashCode(),
                boardCode,
                0,
                "",
                boardName,
                ChanBoard.getDescription(context, boardCode),
                "",
                "drawable://" + boardImageResourceId,
                "",
                "",
                0,
                0,
                THREAD_FLAG_BOARD
        };
    }


    public static Object[] makeBoardTypeRow(Context context, BoardType boardType) {
        return makeTitleRow("", context.getString(boardType.displayStringId()));
    }

    public static Object[] makeTitleRow(String boardCode, String title) {
        return new Object[] {
                title.hashCode(),
                boardCode,
                0,
                title,
                "",
                "",
                "",
                "",
                "",
                "",
                0,
                0,
                THREAD_FLAG_TITLE
        };
    }

    public static final String AD_DELIMITER = "\t";

    public static Object[] makeAdRow(Context context, String boardCode, ChanAd ad) {
        return new Object[] {
                ad.hashCode(),
                boardCode,
                0,
                "",
                context.getResources().getString(R.string.board_advert_full),
                "",
                context.getResources().getString(R.string.board_advert_info),
                ad.imageUrl() + AD_DELIMITER + ad.bannerImageUrl(),
                "",
                ad.clickUrl() + AD_DELIMITER + ad.bannerClickUrl(),
                0,
                0,
                THREAD_FLAG_AD
        };
    }

    public String toString() {
		return "Thread " + no + ", com: " + com + ", sub:" + sub + ", posts: " + posts.length
				+ (posts.length > 0 ? ", posts[0].no: " + posts[0].no : "")
				+ ", thumb: " + thumbnailUrl() + " tn_w: " + tn_w + " tn_h: " + tn_h;
	}
	
    public void mergePosts(List<ChanPost> newPosts) {
        Map<Long,ChanPost> postMap = new HashMap<Long,ChanPost>(this.posts.length);
        for (ChanPost post : this.posts)
            postMap.put(post.no, post);
        for (ChanPost newPost: newPosts)
            postMap.put(newPost.no, newPost); // overwrite any existing posts
        ChanPost[] postArray = postMap.values().toArray(new ChanPost[0]);
        Arrays.sort(postArray, new Comparator<ChanPost>() {
            @Override
            public int compare(ChanPost lhs, ChanPost rhs) {
                if (lhs.no == rhs.no)
                    return 0;
                else if (lhs.no < rhs.no)
                    return -1;
                else
                    return 1;
            }
        });
        this.posts = postArray; // swap
    }

    public Map<Long, HashSet<Long>> backlinksMap() {
        Map<Long, HashSet<Long>> backlinksMap = new HashMap<Long, HashSet<Long>>();
        for (ChanPost post : posts) {
            HashSet<Long> backlinks = post.backlinks();
            if (backlinks != null && !backlinks.isEmpty())
                backlinksMap.put(post.no, backlinks);
        }
        return backlinksMap;
    }

    public Map<Long, HashSet<Long>> repliesMap(Map<Long, HashSet<Long>> backlinksMap) {
        Map<Long, HashSet<Long>> repliesMap = new HashMap<Long, HashSet<Long>>();
        for (Long laterPostNo : backlinksMap.keySet()) {
            for (Long originalPostNo : backlinksMap.get(laterPostNo)) {
                HashSet<Long> replies = repliesMap.get(originalPostNo);
                if (replies == null) {
                    replies = new HashSet<Long>();
                    repliesMap.put(originalPostNo, replies);
                }
                replies.add(laterPostNo);
            }
        }
        return repliesMap;
    }

    public Map<String, HashSet<Long>> sameIdsMap() {
        Map<String, HashSet<Long>> sameIdsMap = new HashMap<String, HashSet<Long>>();
        for (ChanPost post : posts) {
            if (post.id == null || post.id.isEmpty() || post.id.equals(ChanPost.SAGE_POST_ID))
                continue;
            HashSet<Long> sameIds = sameIdsMap.get(post.id);
            if (sameIds == null) {
                sameIds = new HashSet<Long>();
                sameIdsMap.put(post.id, sameIds);
            }
            sameIds.add(post.no);
        }
        return sameIdsMap;
    }
    
    public ChanThread cloneForWatchlist() {
    	ChanThread t = new ChanThread();
    	t.no = no;
    	t.board = board;
    	t.closed = closed;
    	t.created = created;
    	t.omitted_images = omitted_images;
    	t.omitted_posts = omitted_posts;
    	t.resto = resto;
    	t.defData = false;

    	if (posts.length > 0 && posts[0] != null) {
        	t.replies = posts[0].replies;
        	t.images = posts[0].images;
	    	t.bumplimit = posts[0].bumplimit;
	    	t.capcode = posts[0].capcode;
	    	t.com = posts[0].com;
	    	t.country = posts[0].country;
	    	t.country_name = posts[0].country_name;
	    	t.email = posts[0].email;
	    	t.ext = posts[0].ext;
	    	t.filedeleted = posts[0].filedeleted;
	    	t.filename = posts[0].filename;
	    	t.fsize = posts[0].fsize;
	    	t.h = posts[0].h;
	    	t.hideAllText = posts[0].hideAllText;
	    	t.hidePostNumbers = posts[0].hidePostNumbers;
	    	t.id = posts[0].id;
	    	t.now = posts[0].now;
	    	t.spoiler = posts[0].spoiler;
	    	t.sticky = posts[0].sticky;
	    	t.sub = posts[0].sub;
	    	t.tim = posts[0].tim;
	    	t.tn_h = posts[0].tn_h;
	    	t.tn_w = posts[0].tn_w;
	    	t.trip = posts[0].trip;
	    	t.useFriendlyIds = posts[0].useFriendlyIds;
	    	t.w = posts[0].w;
    	}
    	
    	return t;
    }

}
