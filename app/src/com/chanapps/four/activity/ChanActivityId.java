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
    public long threadNo = 0;
    public long postNo = 0;
    
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
}
