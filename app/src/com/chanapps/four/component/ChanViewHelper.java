package com.chanapps.four.component;

import android.app.ActionBar;
import android.app.Activity;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.SettingsActivity;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 11/23/12
 * Time: 3:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChanViewHelper {

    private static final String TAG = ChanViewHelper.class.getSimpleName();

    private Activity activity;
    private DisplayImageOptions options;
    private ImageLoader imageLoader = null;

    private boolean hideAllText = false;
    private boolean hideTextOnlyPosts = false;

    private enum ViewType {
        LIST,
        GRID
    }

    public ChanViewHelper(Activity activity) {
        this.activity = activity;
        imageLoader = ImageLoader.getInstance();
        imageLoader.init(ImageLoaderConfiguration.createDefault(activity));
        options = new DisplayImageOptions.Builder()
			.showImageForEmptyUri(R.drawable.stub_image)
			.cacheOnDisc()
			.imageScaleType(ImageScaleType.EXACT)
			.build();
    }

    public void refreshPrefs() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
        hideAllText = sharedPref.getBoolean(SettingsActivity.PREF_HIDE_ALL_TEXT, false);
        hideTextOnlyPosts = sharedPref.getBoolean(SettingsActivity.PREF_HIDE_TEXT_ONLY_POSTS, false);
    }

    public boolean setGridViewValue(View view, Cursor cursor, int columnIndex) {
        return setViewValue(view, cursor, columnIndex, ViewType.GRID);
    }

    public boolean setListViewValue(View view, Cursor cursor, int columnIndex) {
        return setViewValue(view, cursor, columnIndex, ViewType.LIST);
    }

    public boolean setViewValue(View view, Cursor cursor, int columnIndex, ViewType viewType) {
        String rawText = cursor.getString(cursor.getColumnIndex("text"));
        String rawImageUrl = cursor.getString(cursor.getColumnIndex("image_url"));
        String text = rawText == null ? "" : rawText;
        String imageUrl = rawImageUrl == null ? "" : rawImageUrl;
        if (view instanceof TextView) {
            //todo - @john - if the text is hidden then the image should take the full available space. Also we should not run ChanText replacements
            TextView tv = (TextView) view;
            if (hideAllText) {
                tv.setVisibility(TextView.INVISIBLE);
            } else if (hideTextOnlyPosts && imageUrl.isEmpty()) {
                tv.setVisibility(TextView.INVISIBLE);
            } else {
                Log.e(TAG, "setting text: " + text);
                switch (viewType) {
                    case GRID:
                        setGridViewText(tv, text, cursor);
                        break;
                    case LIST:
                        setListViewText(tv, text, cursor);
                        break;
                    default:
                        throw new RuntimeException("Unknown view type: " + viewType);
                }
            }
            return true;
        } else if (view instanceof ImageView) {
            ImageView iv = (ImageView) view;
            if (hideTextOnlyPosts && imageUrl.isEmpty()) {
                iv.setVisibility(ImageView.INVISIBLE);
            } else if (hideAllText && imageUrl.isEmpty()) {
                iv.setVisibility(ImageView.INVISIBLE);
            } else {
                setViewImage(iv, imageUrl, cursor);
            }
            return true;
        } else {
            return false;
        }
    }

    public void setListViewText(TextView textView, String text, Cursor cursor) {
        if (cursor == null) {
            Log.w(TAG, "setViewText - Why is cursor null?");
            return;
        }
        int tn_w = cursor.getInt(cursor.getColumnIndex("tn_w"));
        int tn_h = cursor.getInt(cursor.getColumnIndex("tn_h"));
        //Log.i(TAG, "tn_w=" + tn_w + ", tn_h=" + tn_h);
        Point imageDimensions = new Point(tn_w, tn_h);
        if (imageDimensions != null && imageDimensions.x > 0 && imageDimensions.y > 0) {
            text = text == null ? "" : text;
            FlowTextHelper.tryFlowText(text, imageDimensions, textView);
        } else {
            textView.setText(text);
        }
    }

    public void setGridViewText(TextView textView, String text, Cursor cursor) {
        if (cursor == null) {
            Log.w(TAG, "setViewText - Why is cursor null?");
            return;
        }
        text = text.substring(0, Math.min(text.length(), 22));
        textView.setText(text);
    }

    public void setViewImage(ImageView imageView, final String thumbnailImageUrl, Cursor cursor) {
        try {
            this.imageLoader.displayImage(thumbnailImageUrl, imageView, options);
        } catch (NumberFormatException nfe) {
            imageView.setImageURI(Uri.parse(thumbnailImageUrl));
        }
    }

    public void setBoardMenu(String code) {
        ActionBar a = activity.getActionBar();
        if (a != null) {
            a.setTitle("/" + code + " " + activity.getString(R.string.board_activity));
            a.setDisplayHomeAsUpEnabled(true);
        }
    }

}
