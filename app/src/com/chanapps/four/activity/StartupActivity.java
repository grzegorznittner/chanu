package com.chanapps.four.activity;

import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.service.NetworkProfileManager;

public class StartupActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Uri data = getIntent() != null ? getIntent().getData() : null;
        if (data != null) {
            dispatchUri(data);
        }
        else if (needStartApp()) {
            NetworkProfileManager.instance().startLastActivity(this);
        }

        finish();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // this prevents StartupActivity recreation on Configuration changes
        // (device orientation changes or hardware keyboard open/close).
        // just do nothing on these changes:
        super.onConfigurationChanged(null);
    }

    private boolean needStartApp() {
        final ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final List<RunningTaskInfo> tasksInfo = am.getRunningTasks(1024);

        if (!tasksInfo.isEmpty()) {
            final String ourAppPackageName = getPackageName();
            RunningTaskInfo taskInfo;
            final int size = tasksInfo.size();
            for (int i = 0; i < size; i++) {
                taskInfo = tasksInfo.get(i);
                if (ourAppPackageName.equals(taskInfo.baseActivity.getPackageName())) {
                    // continue application start only if there is the only Activity in the task
                    // (BTW in this case this is the StartupActivity)
                    return taskInfo.numActivities == 1;
                }
            }
        }

        return true;
    }


    // 0 seg "" - home
    // 1 seg "" - home
    // 1 seg "[a-z0-9]+" - board
    // 3 seg "[a-z0-9]+", "res", "([0-9]+).*" - thread
    private void dispatchUri(Uri data) {
        List<String> params = data.getPathSegments();
        if (params == null || params.size() == 0 || params.get(0) == null || params.get(0).trim().isEmpty()) {
            dispatchBoardSelector();
        }
        else if (params.size() == 1 && params.get(0).trim().matches("[a-z0-9]+")) {
            dispatchBoard(params.get(0).trim());
        }
        else if (params.size() == 3
                && params.get(0).trim().matches("[a-z0-9]+")
                && params.get(1).trim().matches("res")
                && params.get(2).trim().matches("[0-9]+.*")
                ) {
            dispatchThread(params.get(0).trim(), params.get(2).trim());
        }
        else {
            dispatchBoardSelector();
        }
    }

    private void dispatchBoardSelector() {
        startActivity(BoardSelectorActivity.createIntent(getBaseContext(), ChanBoard.defaultBoardCode(this), ""));
    }

    private void dispatchBoard(String boardCode) {
        startActivity(BoardActivity.createIntent(getBaseContext(), boardCode, ""));
    }

    private void dispatchThread(String boardCode, String threadStr) {
        startActivity(ThreadActivity.createIntent(getBaseContext(), boardCode, Long.valueOf(threadStr), ""));

    }

}