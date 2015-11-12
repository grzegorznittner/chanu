/**
 * 
 */
package com.chanapps.four.activity;

import android.app.*;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.widget.TextView;
import com.chanapps.four.service.ThreadImageDownloadService;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

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
        (new CancelDownloadDialogFragment(notificationId)).show(getFragmentManager(), TAG);
	}

    protected class CancelDownloadDialogFragment extends DialogFragment {

        private int notificationId;

        public CancelDownloadDialogFragment() {
            super();
        }

        public CancelDownloadDialogFragment(int notificationId) {
            super();
            this.notificationId = notificationId;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View layout = inflater.inflate(R.layout.message_dialog_fragment, null);
            TextView title = (TextView)layout.findViewById(R.id.title);
            TextView message = (TextView)layout.findViewById(R.id.message);
            title.setText(R.string.cancel_download_title);
            message.setText(R.string.cancel_download_message);
            setStyle(STYLE_NO_TITLE, 0);
            return (new AlertDialog.Builder(getActivity()))
                    .setView(layout)
                    .setPositiveButton(android.R.string.yes,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (notificationId != 0) {
                                        ThreadImageDownloadService.cancelDownload(getBaseContext(), notificationId);
                                    }
                                    CancelDownloadActivity.this.finish();
                                }
                            })
                    .setNegativeButton(R.string.dismiss,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    CancelDownloadActivity.this.finish();
                                }
                            })
                    .create();
        }

    }

}
