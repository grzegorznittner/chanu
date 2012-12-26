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

}
