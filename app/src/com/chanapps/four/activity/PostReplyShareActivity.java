package com.chanapps.four.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import com.chanapps.four.component.ActivityDispatcher;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.fragment.PickShareBoardDialogFragment;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 2/21/13
 * Time: 11:52 AM
 * To change this template use File | Settings | File Templates.
 */
public class PostReplyShareActivity extends PostReplyActivity implements ChanIdentifiedActivity {

    public static final boolean DEBUG = false;
    public static final int PICK_BOARD = 0x011;
    public static final int POST_CANCELLED = 0x12;

    protected Handler handler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ensureHandler();
        Intent intent = getIntent();
        String type = intent.getType();
        Uri imageUri = (Uri)intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (Intent.ACTION_SEND.equals(intent.getAction())
                && type != null
                && type.startsWith("image/")
                && !"".equals(imageUri))
        {
            handleSendImage(imageUri);
        }
        else {
            Toast.makeText(this, R.string.post_reply_share_error, Toast.LENGTH_SHORT).show();
        }
    }

    protected void handleSendImage(Uri imageUri) {
        this.imageUri = imageUri;
        new PickShareBoardDialogFragment(handler).show(getFragmentManager(), PickShareBoardDialogFragment.TAG);
    }

    @Override
    protected synchronized Handler ensureHandler() {
        if (handler == null && ActivityDispatcher.onUIThread())
            handler = new ShareHandler();
        return handler;
    }

    public class ShareHandler extends Handler {

        public ShareHandler() {}

        @Override
        public void handleMessage(Message msg) {
            try {
                super.handleMessage(msg);
                switch (msg.what) {
                    case PICK_BOARD:
                        Bundle b = msg.getData();
                        if (b == null) {
                            Toast.makeText(PostReplyShareActivity.this, R.string.post_reply_share_error, Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                        boardCode = b.getString(ChanBoard.BOARD_CODE);
                        if ("".equals(boardCode)) {
                            Toast.makeText(PostReplyShareActivity.this, R.string.post_reply_share_error, Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                        setViews();
                        break;
                    case POST_FINISHED:
                        // go to board to see our new post
                        finish();
                        if (!"".equals(boardCode))
                            BoardActivity.startActivity(PostReplyShareActivity.this, boardCode, "");
                    case POST_CANCELLED:
                    default:
                        finish();
                }
            }
            catch (Exception e) {
                Log.e(TAG, "Couldn't handle message " + msg, e);
            }
        }
    }

    @Override
    public void closeSearch() {}

    @Override
    public void setProgress(boolean on) {}

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

}
