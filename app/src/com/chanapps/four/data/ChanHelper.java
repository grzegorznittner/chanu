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
	public static final int VERBOSE = 0;
	public static final int DEBUG = 1;
	public static final int INFO = 2;
	public static final int WARNING = 3;
	public static final int ERROR = 4;
	
	public static final String BOARD_TYPE = "boardType";
    public static final String BOARD_CODE = "boardCode";
    public static final String BOARD_CATALOG = "boardCatalog";
    public static final String PAGE = "pageNo";
    public static final String POPUP_TYPE = "popupType";
    public static final String THREAD_NO = "threadNo";
    public static final String POST_NO = "postNo";
    public static final String BACKGROUND_LOAD = "backgroundLoad";
    public static final String TIM = "tim";
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
    public static final String CLEAR_FETCH_QUEUE = "clearFetchQueue";
    public static final String THREAD_FETCH_TIME = "threadFetchTime";
    public static final String USER_POSTS = "userPosts";

    public static final String PREF_WIDGET_BOARDS = "prefWidgetBoards";
    public static final String PREF_BLOCKLIST_TRIPCODE = "prefBlocklistTripcode";
    public static final String PREF_BLOCKLIST_NAME = "prefBlocklistName";
    public static final String PREF_BLOCKLIST_EMAIL = "prefBlocklistEmail";
    public static final String PREF_BLOCKLIST_ID = "prefBlocklistId";

    public static final String TITLE_SEPARATOR = " - ";

    public static String join(List<String> list, String delimiter) {
        String text = "";
        boolean first = true;
        for (String item : list) {
            if (first) {
                text += item;
                first = false;
                continue;
            }
            text += delimiter + item;
        }
        return text;
    }

    public static int countLines(String s) {
        if (s == null || s.isEmpty())
            return 0;
        int i = 1;
        int idx = -1;
        while ((idx = s.indexOf('\n', idx + 1)) != -1) {
            i++;
        }
        return i;
    }

    public enum Orientation {
        PORTRAIT,
        LANDSCAPE
    }

    public enum LastActivity {
        BOARD_SELECTOR_ACTIVITY,
        BOARD_ACTIVITY,
        THREAD_ACTIVITY,
        FULL_SCREEN_IMAGE_ACTIVITY,
        POST_REPLY_ACTIVITY,
        SETTINGS_ACTIVITY,
        ABOUT_ACTIVITY
    }

    public static final Orientation getOrientation(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        if (metrics.widthPixels > metrics.heightPixels) {
            return Orientation.LANDSCAPE;
        }
        else {
            return Orientation.PORTRAIT;
        }
    }

    public static ObjectMapper getJsonMapper() {
        JacksonNonBlockingObjectMapperFactory factory = new JacksonNonBlockingObjectMapperFactory();
        ObjectMapper mapper = factory.createObjectMapper();
    	mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    	// "Jan 15, 2013 10:16:20 AM"
    	mapper.setDateFormat(new SimpleDateFormat("MMM d, yyyy h:mm:ss aaa"));
    	return mapper;
    }
    
	public static void configureJsonParser(JsonParser jp) throws IOException, JsonParseException {
		jp.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
		jp.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
		jp.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);
		jp.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
		jp.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
		jp.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
		jp.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
	}

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

    public static void safeClearImageView(ImageView v) {
        /*
        Drawable d = v.getDrawable();
        if (d != null && d instanceof BitmapDrawable) {
            BitmapDrawable bd = (BitmapDrawable)d;
            Bitmap b = bd.getBitmap();
            if (b != null)
                b.recycle();
        }
        */
        v.setImageBitmap(null);
    }

    public static void clearBigImageView(final ImageView v) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Drawable d = v.getDrawable();
                if (d != null && d instanceof BitmapDrawable) {
                    BitmapDrawable bd = (BitmapDrawable)d;
                    Bitmap b = bd.getBitmap();
                    if (b != null)
                        b.recycle();
                }
            }
        }).start();
        v.setImageBitmap(null);
    }

}
