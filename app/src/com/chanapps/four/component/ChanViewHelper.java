package com.chanapps.four.component;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.SettingsActivity;
import com.chanapps.four.activity.ThreadListActivity;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanLoadBoardService;
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
            //todo - @john - if the text is hidden then the image should take the full available space.
            TextView tv = (TextView) view;
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
            return true;
        } else if (view instanceof ImageView) {
            ImageView iv = (ImageView) view;
            setViewImage(iv, imageUrl, cursor);
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

    private void setBoardMenu(String code) {
        ActionBar a = activity.getActionBar();
        if (a != null) {
            a.setTitle("/" + code + " " + activity.getString(R.string.board_activity));
            a.setDisplayHomeAsUpEnabled(true);
        }
    }

    public String loadBoard() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        String oldBoardCode = prefs.getString(ChanHelper.BOARD_CODE, "s");
        String newBoardCode = "s";
        Intent intent = activity.getIntent();
        if (intent.hasExtra(ChanHelper.BOARD_CODE)) {
            newBoardCode = intent.getStringExtra(ChanHelper.BOARD_CODE);
            Log.i(TAG, "Board code read from intent: " + newBoardCode);
        }
        if (!intent.hasExtra(ChanHelper.BOARD_CODE) || !ChanBoard.isValidBoardCode(newBoardCode)) {
            newBoardCode = prefs.getString(ChanHelper.BOARD_CODE, "s");
            Log.i(TAG, "Board code loaded from prefs: " + newBoardCode);
        }
        setBoardMenu(newBoardCode);

        if (!oldBoardCode.equals(newBoardCode)) {
            SharedPreferences.Editor ed = prefs.edit();
            ed.putString(ChanHelper.BOARD_CODE, newBoardCode);
            ed.commit();
        }

        Log.i(TAG, "Starting ChanLoadBoardService");
        Intent threadIntent = new Intent(activity, ChanLoadBoardService.class);
        threadIntent.putExtra(ChanHelper.BOARD_CODE, newBoardCode);
        activity.startService(threadIntent);

        return newBoardCode;
    }

    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id, String boardCode) {
        Log.i(TAG, "onItemClick id=" + id + ", position=" + position);
        Intent intent = new Intent(activity, ThreadListActivity.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        intent.putExtra(ChanHelper.THREAD_NO, id);
        activity.startActivity(intent);
    }

}
