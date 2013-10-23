/**
 * 
 */
package com.chanapps.four.activity;

import com.chanapps.four.service.ThreadImageDownloadService;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

/**
 * @author grzegorznittner
 *
 */
public class CancelDownloadActivity extends Activity {
	public static final String TAG = CancelDownloadActivity.class.getSimpleName();
	
	private static final String NOTIFICATION_ID = "NotificationId";
	
	final Context context = this;
	
    public static Intent createIntent(Context context, final int notificationId, final String boardCode, final long threadNo) {
		Intent intent = new Intent(context, CancelDownloadActivity.class);
		intent.putExtra(NOTIFICATION_ID, notificationId);
		intent.putExtra(ThreadActivity.BOARD_CODE, boardCode);
        intent.putExtra(ThreadActivity.THREAD_NO, threadNo);
		intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		return intent;
	}
 
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		final int notificationId = getIntent().getIntExtra(NOTIFICATION_ID, 0);
		if (notificationId == 0) {
			finish();
			return;
		}
		
		setContentView(R.layout.cancel_download_dialog);
		Button closeBtn = (Button)findViewById(R.id.cancel_download_close_btn);
		closeBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.w(TAG, "Close action called");
				CancelDownloadActivity.this.finish();
			}
		});

		Button stopBtn = (Button)findViewById(R.id.cancel_download_stop_btn);
		stopBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.w(TAG, "Stop download action called");
				ThreadImageDownloadService.cancelDownload(getBaseContext(), notificationId);
				CancelDownloadActivity.this.finish();
			}
		});
	}
}
