package com.chanapps.four.data;

import android.util.Log;

import java.util.*;

public class ChanThread extends ChanPost {
	public long lastFetched;

	public ChanPost posts[] = new ChanPost[0];
	
	public String toString() {
		return "Thread " + no + " " + com + ", posts: " + posts.length 
				+ ", thumb: " + getThumbnailUrl() + " tn_w: " + tn_w + " tn_h: " + tn_h;
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
        Log.i(TAG, "getPrevPostsRef postNo=" + sourcePostNo);
        List<Long> prevPosts = new ArrayList<Long>();
        ChanPost sourcePost = getPost(sourcePostNo);
        Log.i(TAG, "Source post=" + sourcePost + " for postNo=" + sourcePostNo);
        if (sourcePost != null)
            for (ChanPost post : posts)
                if (sourcePost.refersTo(post.no))
                    prevPosts.add(post.no);
        long[] prevPostsArr = new long[prevPosts.size()];
        for (int i = 0; i < prevPostsArr.length; i++)
            prevPostsArr[i] = prevPosts.get(i);
        Log.i(TAG, "postNo=" + sourcePostNo + " found prevPosts=" + Arrays.toString(prevPostsArr));
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

}
