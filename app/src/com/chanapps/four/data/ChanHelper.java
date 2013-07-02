package com.chanapps.four.data;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.ObjectMapper;

import android.content.Context;
import android.util.DisplayMetrics;

import com.chanapps.four.activity.R;

public class ChanHelper {
	public static final int DEBUG = 1;

    public static final String BOARD_CODE = "boardCode";
    public static final String BOARD_CATALOG = "boardCatalog";
    public static final String PAGE = "pageNo";
    public static final String THREAD_NO = "threadNo";
    public static final String POST_NO = "postNo";
    public static final String BACKGROUND_LOAD = "backgroundLoad";
    public static final String TEXT = "text";
    public static final String NAME = "name";
    public static final String IMAGE_URL = "imageUrl";
    public static final String CAMERA_IMAGE_URL = "cameraImageUrl";
    public static final String IMAGE_PATH = "imagePath";
    public static final String CONTENT_TYPE = "contentType";
    public static final String ORIENTATION = "orientation";
    public static final String LAST_ACTIVITY = "lastActivity";
    public static final String IGNORE_DISPATCH = "ignoreDispatch";
    public static final String PRIORITY_MESSAGE = "priorityFetch";
    public static final String SECONDARY_THREAD_NO = "secondaryThreadNo";
    public static final String CLEAR_FETCH_QUEUE = "clearFetchQueue";
    public static final String THREAD_FETCH_TIME = "threadFetchTime";
    public static final String USER_POSTS = "userPosts";

    public static final String PREF_WIDGET_BOARDS = "prefWidgetBoards";
    public static final String PREF_BLOCKLIST_TRIPCODE = "prefBlocklistTripcode";
    public static final String PREF_BLOCKLIST_NAME = "prefBlocklistName";
    public static final String PREF_BLOCKLIST_EMAIL = "prefBlocklistEmail";
    public static final String PREF_BLOCKLIST_ID = "prefBlocklistId";

    public static final String TITLE_SEPARATOR = " - ";

    public static void launchUrlInBrowser(Context context, String url) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        context.startActivity(i);
    }

    public static void exitApplication(Context context) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static boolean onUIThread() {
        return Looper.getMainLooper().equals(Looper.myLooper());
    }

}
