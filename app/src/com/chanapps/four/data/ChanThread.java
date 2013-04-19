package com.chanapps.four.data;

import android.content.Context;
import android.database.MatrixCursor;
import android.util.Log;
import com.chanapps.four.activity.R;
import org.codehaus.jackson.map.annotate.JsonDeserialize;

import java.util.*;

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
    public static final String THREAD_SUBJECT = "threadSub";
    public static final String THREAD_INFO = "threadInfo";
    public static final String THREAD_THUMBNAIL_URL = "threadThumb";
    public static final String THREAD_COUNTRY_FLAG_URL = "threadFlag";
    public static final String THREAD_CLICK_URL = "threadClick";
    public static final String THREAD_NUM_REPLIES = "threadNumReplies";
    public static final String THREAD_NUM_IMAGES = "threadNumImages";

    public static final String[] THREAD_COLUMNS = {
            THREAD_COMPOSITE_ID,
            THREAD_BOARD_CODE,
            THREAD_NO,
            THREAD_SUBJECT,
            THREAD_INFO,
            THREAD_THUMBNAIL_URL,
            THREAD_COUNTRY_FLAG_URL,
            THREAD_CLICK_URL,
            THREAD_NUM_REPLIES,
            THREAD_NUM_IMAGES
    };

    public static MatrixCursor buildMatrixCursor() {
        return new MatrixCursor(THREAD_COLUMNS);
    }

    public static Object[] makeRow(Context context, ChanPost post) {
        String id = post.board + "/" + post.no;
        return new Object[] {
                id.hashCode(),
                post.board,
                post.no,
                post.threadSubject(context),
                post.threadInfoLine(),
                post.thumbnailUrl(),
                post.countryFlagUrl(),
                "",
                post.replies,
                post.images
        };
    }

    public static Object[] makeBoardTypeRow(Context context, BoardType boardType) {
        return new Object[] {
                0,
                "",
                0,
                context.getString(boardType.displayStringId()),
                "|||BOARD_TYPE|||",
                boardType.drawableId() > 0 ? "drawable://" + boardType.drawableId() : "",
                "",
                "",
                0,
                0
        };
    }

    public static Object[] makeBoardRow(String boardCode, String boardName, int boardImageResourceId) {
        return new Object[] {
                boardCode.hashCode(),
                boardCode,
                0,
                boardName,
                "",
                "drawable://" + boardImageResourceId,
                "",
                "",
                0,
                0
        };
    }
    public static Object[] makeAdRow(Context context, String imageUrl, String clickUrl) {
        return new Object[] {
                0,
                "",
                0,
                context.getResources().getString(R.string.board_advert_full),
                context.getResources().getString(R.string.board_advert_info),
                imageUrl,
                "",
                clickUrl,
                0,
                0
        };
    }

    public String toString() {
		return "Thread " + no + " " + com + ", posts: " + posts.length 
				+ ", thumb: " + thumbnailUrl() + " tn_w: " + tn_w + " tn_h: " + tn_h;
	}
	
    public void mergePosts(List<ChanPost> posts) {
        List<ChanPost> mergedPosts = new ArrayList<ChanPost>(Arrays.asList(this.posts));
        if (mergedPosts.size() > 0 && mergedPosts.get(0).defData) {
        	mergedPosts.remove(0);
        }
        for (ChanPost newPost : posts) {
            boolean exists = false;
            for (ChanPost p : this.posts) {
                if (p.no == newPost.no) {
                    exists = true;
                    p.copyThreadStatusFields(newPost);
                }
            }
            if (!exists) {
                mergedPosts.add(newPost);
            }
        }
        Collections.sort(mergedPosts, new Comparator<ChanPost>() {
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
        this.posts = mergedPosts.toArray(new ChanPost[0]);
    }

    public ChanPost[] getPrevNextPosts(long postNo) {
        ChanPost prevPost = null;
        ChanPost nextPost = null;
        if (posts != null) {
            for (int i = 0; i < posts.length; i++) {
                ChanPost post = posts[i];
                if (post.no == postNo) {
                    for (int j = i - 1; j >= 0; j--)
                        if (posts[j].w > 0) {
                            prevPost = posts[j];
                            break;
                        }
                    for (int j = i + 1; j < posts.length; j++)
                        if (posts[j].w > 0) {
                            nextPost = posts[j];
                            break;
                        }
                    break;
                }
            }
        }
        ChanPost[] posts = { prevPost, nextPost };
        return posts;
    }

    public ChanPost getPost(long sourcePostNo) {
        ChanPost sourcePost = null;
        if (posts != null)
            for (ChanPost post : posts)
                if (post.no == sourcePostNo) {
                    sourcePost = post;
                    break;
                }
        return sourcePost;
    }

    public long[] getPrevPostsReferenced(long sourcePostNo) { // what posts does this post refer to
        if (DEBUG) Log.i(TAG, "getPrevPostsRef postNo=" + sourcePostNo);
        List<Long> prevPosts = new ArrayList<Long>();
        ChanPost sourcePost = getPost(sourcePostNo);
        if (DEBUG) Log.i(TAG, "Source post=" + sourcePost + " for postNo=" + sourcePostNo);
        if (sourcePost != null)
            for (ChanPost post : posts)
                if (sourcePost.refersTo(post.no))
                    prevPosts.add(post.no);
        long[] prevPostsArr = new long[prevPosts.size()];
        for (int i = 0; i < prevPostsArr.length; i++)
            prevPostsArr[i] = prevPosts.get(i);
        if (DEBUG) Log.i(TAG, "postNo=" + sourcePostNo + " found prevPosts=" + Arrays.toString(prevPostsArr));
        return prevPostsArr;
    }

    public long[] getNextPostsReferredTo(long postNo) { // what other posts refer to this post
        List<Long> nextPosts = new ArrayList<Long>();
        for (ChanPost post : posts)
            if (post.refersTo(postNo))
                nextPosts.add(post.no);
        long[] nextPostsArr = new long[nextPosts.size()];
        for (int i = 0; i < nextPostsArr.length; i++)
            nextPostsArr[i] = nextPosts.get(i);
        return nextPostsArr;
    }

    public long[] getIdPosts(long postNo, String userId) { // what other posts refer to this post
        List<Long> idPosts = new ArrayList<Long>();
        if (userId == null || userId.isEmpty())
            return null;
        for (ChanPost post : posts)
            if (post.no != postNo && userId.equals(post.id))
                idPosts.add(post.no);
        long[] idPostsArr = new long[idPosts.size()];
        for (int i = 0; i < idPostsArr.length; i++)
            idPostsArr[i] = idPosts.get(i);
        return idPostsArr;
    }

    public long[] getTripcodePosts(long postNo, String tripcode) { // what other posts refer to this post
        List<Long> tripcodePosts = new ArrayList<Long>();
        if (tripcode == null || tripcode.isEmpty())
            return null;
        for (ChanPost post : posts)
            if (post.no != postNo && tripcode.equals(post.trip))
                tripcodePosts.add(post.no);
        long[] tripcodePostsArr = new long[tripcodePosts.size()];
        for (int i = 0; i < tripcodePostsArr.length; i++)
            tripcodePostsArr[i] = tripcodePosts.get(i);
        return tripcodePostsArr;
    }

    public long[] getNamePosts(long postNo, String name) { // what other posts refer to this post
        List<Long> namePosts = new ArrayList<Long>();
        if (name == null || name.isEmpty() || name.equals("Anonymous"))
            return null;
        for (ChanPost post : posts)
            if (post.no != postNo && name.equals(post.name))
                namePosts.add(post.no);
        long[] namePostsArr = new long[namePosts.size()];
        for (int i = 0; i < namePostsArr.length; i++)
            namePostsArr[i] = namePosts.get(i);
        return namePostsArr;
    }

    public long[] getEmailPosts(long postNo, String email) { // what other posts refer to this post
        List<Long> emailPosts = new ArrayList<Long>();
        if (email == null || email.isEmpty())
            return null;
        for (ChanPost post : posts)
            if (post.no != postNo && email.equals(post.email))
                emailPosts.add(post.no);
        long[] emailPostsArr = new long[emailPosts.size()];
        for (int i = 0; i < emailPostsArr.length; i++)
            emailPostsArr[i] = emailPosts.get(i);
        return emailPostsArr;
    }

}
