package com.chanapps.four.fragment;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.chanapps.four.activity.R;
import com.chanapps.four.data.ChanFileStorage;

/**
* Created with IntelliJ IDEA.
* User: arley
* Date: 12/14/12
* Time: 12:44 PM
* To change this template use File | Settings | File Templates.
*/
public class SetWallpaperDialogFragment extends DialogFragment {

    public static final String TAG = SetWallpaperDialogFragment.class.getSimpleName();

    private static final boolean DEBUG = false;

    // overcoming android broken wallpaper algorithms
    private static final int WALLPAPER_PADDING_PORTRAIT_PX = 100;
    private static final int WALLPAPER_PADDING_LANDSCAPE_PX = 600;

    private String localImageUri;
    private Context context;
    private WallpaperManager wallpaperManager;
    private Dimensions screenDimensions;

    public SetWallpaperDialogFragment(String localImageUri) {
        super();
        this.localImageUri = localImageUri;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        context = getActivity();
        wallpaperManager = WallpaperManager.getInstance(context);
        setScreenDimensions(getActivity());
        return (new AlertDialog.Builder(context))
                .setMessage(R.string.dialog_set_wallpaper)
                .setPositiveButton(R.string.dialog_set,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                setWallpaper();
                            }
                        })
                .setNegativeButton(R.string.dialog_cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // ignore
                            }
                        })
                .create();
    }

    private void setWallpaper() {
        Toast.makeText(this.getActivity(), R.string.full_screen_setting_wallpaper, Toast.LENGTH_SHORT).show();
        SetImageAsWallpaperTask wallpaperTask = new SetImageAsWallpaperTask(context);
        wallpaperTask.execute(localImageUri);

    }

    private class SetImageAsWallpaperTask extends AsyncTask<String, Void, Integer> {
        private String url;
        private Context context;

        public SetImageAsWallpaperTask(Context context) {
            this.context = context;
        }

        @Override
        protected Integer doInBackground(String... params) {
            return setImageAsWallpaper();
        }

        @Override
        protected void onPostExecute(Integer result) {
            Toast.makeText(context, result, Toast.LENGTH_SHORT).show();
        }
    }

    private class Dimensions {
        public int width = 0;
        public int height = 0;
        public Dimensions() {}
        public Dimensions(int x, int y) {
            width = x;
            height = y;
        }
    }

    private void setScreenDimensions(Activity activity) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int orientation = getScreenOrientation(activity);
        int screenWidth = orientation == Configuration.ORIENTATION_PORTRAIT
                ? displayMetrics.widthPixels
                : displayMetrics.heightPixels;
        int screenHeight  = orientation == Configuration.ORIENTATION_PORTRAIT
                ? displayMetrics.heightPixels
                : displayMetrics.widthPixels;
        screenDimensions = new Dimensions(screenWidth, screenHeight);
    }

    private int getScreenOrientation(Activity activity) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int orientation;
        if (displayMetrics.widthPixels == displayMetrics.heightPixels) {
            orientation = Configuration.ORIENTATION_SQUARE;
        }
        else {
            if(displayMetrics.widthPixels < displayMetrics.heightPixels) {
                orientation = Configuration.ORIENTATION_PORTRAIT;
            }
            else {
                orientation = Configuration.ORIENTATION_LANDSCAPE;
            }
        }
        return orientation;
    }

    private Dimensions getWallpaperDimensions() {
        int screenWidth = wallpaperManager.getDesiredMinimumWidth();
        int screenHeight = wallpaperManager.getDesiredMinimumHeight();
        if (screenWidth <= 0 || screenHeight <= 0) { // we are not guaranteed results
            screenWidth = screenDimensions.width;
            screenHeight = screenDimensions.height;
        }
        return new Dimensions(screenWidth, screenHeight);
    }

    private Dimensions getSourceBitmapDimensions(File f) {
        String path = f.getAbsolutePath();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        Bitmap throwaway = BitmapFactory.decodeFile(path, options);
        Dimensions d = new Dimensions(options.outWidth, options.outHeight);
        if (throwaway != null)
            throwaway.recycle();
        return d;
    }

    private Bitmap createScaledBitmap(Bitmap sourceBitmap, Dimensions wallpaperDimensions) {
        int width = sourceBitmap.getWidth();
        int height = sourceBitmap.getHeight();
        int padding = width <= height ? WALLPAPER_PADDING_PORTRAIT_PX : WALLPAPER_PADDING_LANDSCAPE_PX;
        int maxWidth = wallpaperDimensions.width - padding;
        int maxHeight = wallpaperDimensions.height - padding;
        if (width < maxWidth && height < maxHeight)
            return sourceBitmap;
        float scaleX = (float)maxWidth / width;
        float scaleY = (float)maxHeight / height;
        float scale = Math.min(scaleX, scaleY);
        if (DEBUG) Log.i(TAG, "picked scale factor " + scale);
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale); // preserve aspect ratio
        return Bitmap.createBitmap(sourceBitmap, 0, 0, width, height, matrix, true);
    }

    private Bitmap createPaddedBitmap(Bitmap scaledBitmap, Dimensions wallpaperDimensions) {
        int width = scaledBitmap.getWidth();
        int height = scaledBitmap.getHeight();
        int maxWidth = wallpaperDimensions.width;
        int maxHeight = wallpaperDimensions.height;
        if (width > wallpaperDimensions.width || height > wallpaperDimensions.height)
            return scaledBitmap;
        int xPadding = Math.max(0, maxWidth - width) / 2;
        int yPadding = Math.max(0, maxHeight - height) / 2;
        Bitmap paddedBitmap = Bitmap.createBitmap(maxWidth, maxHeight, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[width * height]; // possible crash
        scaledBitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        paddedBitmap.setPixels(pixels, 0, width, xPadding, yPadding, width, height);
        return paddedBitmap;
    }

    private int computeOptimumLoadingScale(Dimensions source, Dimensions target) {
        float overscaleX = (float)source.width / (float)target.width;
        float overscaleY = (float)source.height / (float)target.height;
        int scaleX = (int)Math.round(Math.floor(overscaleX));
        int scaleY = (int)Math.round(Math.floor(overscaleY));
        int scale = Math.max(scaleX, scaleY);
        return scale;
    }

    private Integer setImageAsWallpaper() {
        Integer result = R.string.full_screen_wallpaper_error;
        try {
            File sourceBitmapFile = new File(URI.create(this.localImageUri));
            Dimensions wallpaperDimensions = getWallpaperDimensions();
            if (DEBUG) Log.i(TAG, "Got wallpaper dimensions " + wallpaperDimensions.width + "x" + wallpaperDimensions.height);
            Dimensions sourceBitmapDimensions = getSourceBitmapDimensions(sourceBitmapFile);
            if (DEBUG) Log.i(TAG, "Got source bitmap dimensions " + sourceBitmapDimensions.width + "x" + sourceBitmapDimensions.height);

            // so the point of this is to not load in a bunch more pixels than we need for huge images
            int scale = computeOptimumLoadingScale(sourceBitmapDimensions, wallpaperDimensions);
            if (DEBUG) Log.i(TAG, "Picked loading scale = " + scale);

            if (DEBUG) Log.i(TAG, "Loading source bitmap from file " + sourceBitmapFile.getAbsolutePath());
            BitmapFactory.Options options=new BitmapFactory.Options();
            options.inSampleSize = scale;
            Bitmap sourceBitmap = BitmapFactory.decodeFile(sourceBitmapFile.getAbsolutePath(), options);
            if (DEBUG) Log.i(TAG, "Loaded bitmap dimensions at " + sourceBitmap.getWidth() + "x" + sourceBitmap.getHeight());

            Bitmap scaledBitmap = createScaledBitmap(sourceBitmap, wallpaperDimensions);
            if (DEBUG) Log.i(TAG, "Scaled bitmap dimensions to " + scaledBitmap.getWidth() + "x" + scaledBitmap.getHeight());

            Bitmap paddedBitmap = createPaddedBitmap(scaledBitmap, wallpaperDimensions);
            if (DEBUG) Log.i(TAG, "Padded bitmap dimensions are " + paddedBitmap.getWidth() + "x" + paddedBitmap.getHeight());
            File file = ChanFileStorage.createWallpaperFile(context);
            if (DEBUG) Log.i(TAG, "Writing wallpaper image to file " + file.getAbsolutePath());
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file);
                paddedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            }
            finally {
                if (fos != null)
                    fos.close();
                if (sourceBitmap != null)
                    sourceBitmap.recycle();
                if (scaledBitmap != null)
                    scaledBitmap.recycle();
                if (paddedBitmap != null)
                    paddedBitmap.recycle();
            }

            FileInputStream fis = new FileInputStream(file);
            InputStream is = new BufferedInputStream(fis);
            if (DEBUG) Log.i(TAG, "Loading wallpaper file to wallpaper manager");
            wallpaperManager.setStream(is);

            if (DEBUG) Log.i(TAG, "Successfully set wallpaper from file=" + file.getAbsolutePath());
            result = R.string.full_screen_wallpaper_set;
        }
        catch (Exception e) {
            Log.e(TAG, "Exception while setting wallpaper", e);
        }
        return result;
    }

}
