package com.chanapps.four.data;

import java.io.IOException;
import java.text.SimpleDateFormat;

import android.content.Intent;
import android.net.Uri;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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
	
	//public static final String PREF_NAME = "4Channer";

	public static final String BOARD_TYPE = "boardType";
    public static final String BOARD_CODE = "boardCode";
    public static final String BOARD_CATALOG = "boardCatalog";
    public static final String PAGE = "pageNo";
    public static final String FROM_PARENT = "fromParent";
    public static final String THREAD_NO = "threadNo";
    public static final String FORCE_REFRESH = "forceRefresh";
    public static final String POST_NO = "postNo";
    public static final String TIM = "tim";
    public static final String TEXT = "text";
    //public static final String QUOTE_TEXT = "quoteText";
    public static final String NAME = "name";
    public static final String EMAIL = "email";
    public static final String SUBJECT = "subject";
    public static final String PASSWORD = "password";
    public static final String SPOILER = "spoiler";
    public static final String IMAGE_URL = "imageUrl";
    public static final String POST_REPLY_IMAGE_URL = "postReplyImageUrl";
    public static final String CAMERA_IMAGE_URL = "cameraImageUrl";
    public static final String IMAGE_WIDTH = "imageWidth";
    public static final String IMAGE_HEIGHT = "imageHeight";
    public static final String IMAGE_PATH = "imagePath";
    public static final String CONTENT_TYPE = "contentType";
    public static final String ORIENTATION = "orientation";
    public static final String THREAD_WATCHLIST = "threadWatchlist";
    public static final String POST_ID = "_id";
    public static final String POST_BOARD_NAME = "board_name";
    public static final String POST_NOW = "now";
    public static final String POST_TIME = "time";
    public static final String POST_NAME = "name";
    public static final String POST_EMAIL = "email";
    public static final String POST_SUB = "sub";
    public static final String POST_COM = "com";
    public static final String POST_COUNTRY = "country";
    public static final String POST_TIM = "tim";
    public static final String POST_FILENAME = "filename";
    public static final String POST_EXT = "ext";
    public static final String POST_W = "w";
    public static final String POST_H = "h";
    public static final String POST_TN_W = "tn_w";
    public static final String POST_TN_H = "tn_h";
    public static final String POST_FSIZE = "fsize";
    public static final String POST_RESTO = "resto";
    public static final String POST_LAST_UPDATE = "last_update";
    public static final String POST_STICKY = "sticky";
    public static final String POST_CLOSED = "closed";
    public static final String POST_OMITTED_POSTS = "omitted_posts";
    public static final String POST_OMITTED_IMAGES = "omitted_images";
    public static final String POST_DATE_TEXT = "headerText"; // we construct and filter this // NOT USED
    public static final String POST_SHORT_TEXT = "shortText"; // we construct and filter this
    public static final String POST_TEXT = "text"; // we construct and filter this
    public static final String POST_IMAGE_URL = "image_url"; // we construct this from board and tim
    public static final String POST_COUNTRY_URL = "country_url"; // we construct this from the country code
    public static final String POST_SPOILER = "spoiler";
    public static final String POST_SPOILER_TEXT = "spoilerText";
    public static final String POST_EXIF_TEXT = "exifText";
    public static final String POST_USER_ID = "id";
    public static final String POST_TRIPCODE = "trip";
    public static final String POST_IS_DEAD = "isDead";
    public static final String POST_IMAGE_DIMENSIONS = "imageDim";
    public static final String POST_EXPAND_BUTTON = "expandButton";
    public static final String AD_ITEM = "adItem";

    public static final String LAST_BOARD_POSITION = "lastBoardPosition";
    public static final String LAST_THREAD_POSITION = "lastThreadPosition";
    public static final String LAST_ACTIVITY = "lastActivity";
    public static final String IGNORE_DISPATCH = "ignoreDispatch";
    public static final String PRIORITY_MESSAGE = "priorityFetch";
    public static final String CLEAR_FETCH_QUEUE = "clearFetchQueue";
    public static final String THREAD_FETCH_TIME = "threadFetchTime";
    public static final String FIRST_TIME_INIT = "firstTimeInit";
    public static final String TRIGGER_BOARD_REFRESH = "triggerBoardRefresh";

    public static final String USER_POSTS = "userPosts";

    public static final String[] POST_COLUMNS = {
            POST_ID,
            POST_BOARD_NAME,
            POST_RESTO,
            POST_IMAGE_URL,
            POST_COUNTRY_URL,
            POST_SHORT_TEXT,
            POST_DATE_TEXT,
            POST_TEXT,
            POST_TN_W,
            POST_TN_H,
            POST_W,
            POST_H,
            POST_TIM,
            POST_SPOILER,
            POST_SPOILER_TEXT,
            POST_EXIF_TEXT,
            POST_USER_ID,
            POST_TRIPCODE,
            POST_NAME,
            POST_EMAIL,
            POST_IMAGE_DIMENSIONS,
            POST_IS_DEAD,
            POST_CLOSED,
            POST_EXPAND_BUTTON,
            AD_ITEM
    };

    public static final String BOARD_ID = "_id";
    public static final String BOARD_IMAGE_RESOURCE_ID = "boardImageResourceId";
    public static final String BOARD_NAME = "boardName";
    public static final String[] SELECTOR_COLUMNS = {
            BOARD_ID,
            BOARD_CODE,
            BOARD_IMAGE_RESOURCE_ID,
            BOARD_NAME
    };

    public static final String PREF_WIDGET_BOARDS = "prefWidgetBoards";
    public static final String PREF_BLOCKLIST = "prefBlocklist";

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
        SETTINGS_ACTIVITY
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

    public static void fadeout(Context context, View view) {
        Animation animation = AnimationUtils.loadAnimation(context, R.animator.fadeout);
        view.startAnimation(animation);
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

}
