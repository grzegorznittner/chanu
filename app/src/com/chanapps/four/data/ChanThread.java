package com.chanapps.four.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChanThread extends ChanPost {
	public long lastFetched;

	public ChanPost posts[] = new ChanPost[0];

    public void mergePosts(List<ChanPost> posts) {
        List<ChanPost> mergedPosts = new ArrayList<ChanPost>(Arrays.asList(this.posts));
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
}
