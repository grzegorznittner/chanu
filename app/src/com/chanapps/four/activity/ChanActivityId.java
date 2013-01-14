/**
 * 
 */
package com.chanapps.four.activity;

import com.chanapps.four.data.ChanHelper;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 *
 */
public class ChanActivityId {
	public ChanActivityId (String boardCode, int pageNo, boolean priority) {
		this.activity = null;
		this.boardCode = boardCode;
		this.pageNo = pageNo;
		this.priority = priority;
	}
	public ChanActivityId (String boardCode, long threadNo, boolean priority) {
		this.activity = null;
		this.boardCode = boardCode;
		this.threadNo = threadNo;
		this.priority = priority;
	}
	public ChanActivityId (ChanHelper.LastActivity activity) {
		this.activity = activity;
	}
	public ChanActivityId (ChanHelper.LastActivity activity, String boardCode) {
		this.activity = activity;
		this.boardCode = boardCode;
	}
	public ChanActivityId (ChanHelper.LastActivity activity, String boardCode, long threadNo) {
		this.activity = activity;
		this.boardCode = boardCode;
		this.threadNo = threadNo;
	}
	public ChanActivityId (ChanHelper.LastActivity activity, String boardCode, long threadNo, long postNo) {
		this.activity = activity;
		this.boardCode = boardCode;
		this.threadNo = threadNo;
		this.postNo = postNo;
	}
	
	public ChanHelper.LastActivity activity;
	public String boardCode = null;
	public int pageNo = -1;
    public long threadNo = 0;
    public long postNo = 0;
    public boolean priority = false;
    
	@Override
	public boolean equals(Object o) {
		if (o instanceof ChanActivityId) {
			ChanActivityId obj = (ChanActivityId)o;
			if (obj.activity == activity) {
				if (boardCode == null) {
					if (obj.boardCode != null) {
						return false;
					}
				} else if (!boardCode.equals(obj.boardCode)) {
					return false;
				} else if (pageNo != obj.pageNo) {
					return false;
				}
				
				if (obj.threadNo != threadNo) {
					return false;
				}
				if (obj.postNo != postNo) {
					return false;
				}
				return true;
			}
		}
		return false;
	}
	
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		if (activity != null) {
			buffer.append(activity);
		} else {
			buffer.append("Service for ");
		}
		if (boardCode != null) {
			buffer.append(" ").append(boardCode);
			if (threadNo != 0) {
				buffer.append("/").append(threadNo);
				if (postNo != 0) {
					buffer.append(" post: ").append(postNo);
				}
			}
		}
		return buffer.toString();
	}
}
