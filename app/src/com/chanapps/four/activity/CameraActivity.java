package com.chanapps.four.activity;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.chanapps.four.component.ToastRunnable;
import com.chanapps.four.data.ChanHelper;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 1/26/13
 * Time: 7:19 PM
 * To change this template use File | Settings | File Templates.
 */

public class CameraActivity extends Activity {

    public static final String TAG = CameraActivity.class.getSimpleName();

    private static final boolean DEBUG = false;

    private Camera mCamera;
    private CameraPreview mPreview;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setResult(RESULT_CANCELED); // only OK when we've taken a picture

        setContentView(R.layout.camera_layout);

        // Create an instance of Camera
        mCamera = getCameraInstance();
        if (mCamera == null) {
            Log.e(TAG, "Couldn't open camera");
            Toast.makeText(this, R.string.camera_open_error, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        RelativeLayout preview = (RelativeLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview, 0);

        // Add a listener to the Capture button
        ImageButton captureButton = (ImageButton) findViewById(R.id.button_capture);
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // get an image from the camera
                        mCamera.takePicture(mShutter, null, mPicture);
                    }
                }
        );
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            Log.e(TAG, "Couldn't get camera", e);
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    private static final int MEDIA_TYPE_IMAGE = 0x11;
    private static final int MEDIA_TYPE_VIDEO = 0x12;

    private Camera.ShutterCallback mShutter = new Camera.ShutterCallback() {
        @Override
        public void onShutter() {
            if (DEBUG) Log.i(TAG, "Shutter clicked");
            shootSound();
        }
    };

    private MediaPlayer shootMP = null;

    public void shootSound() {
        AudioManager meng = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int volume = meng.getStreamVolume( AudioManager.STREAM_NOTIFICATION);

        if (volume != 0) {
            if (shootMP == null)
                shootMP = MediaPlayer.create(this, Uri.parse("file:///system/media/audio/ui/camera_click.ogg"));
            if (shootMP != null)
                shootMP.start();
        }
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            boolean success = false;

            try {
                File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
                if (pictureFile == null){
                    if (DEBUG) Log.i(TAG, "Error creating media file, check storage permissions");
                }
                else {
                    if (DEBUG) Log.i(TAG, "Took picture:" + pictureFile.getAbsolutePath());

                    Bitmap b = createPortraitBitmap(data);
                    FileOutputStream fos = new FileOutputStream(pictureFile);
                    b.compress(Bitmap.CompressFormat.JPEG, 85, fos);
                    fos.close();
                    b.recycle();

                    String url = Uri.fromFile(pictureFile).toString();
                    Intent intent = new Intent();
                    intent.putExtra(ChanHelper.CAMERA_IMAGE_URL, url);
                    setResult(Activity.RESULT_OK, intent);
                    if (DEBUG) Log.i(TAG, "Returning camera file:" + url);
                    success = true;
                }
            } catch (FileNotFoundException e) {
                if (DEBUG) Log.i(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                if (DEBUG) Log.i(TAG, "Error accessing file: " + e.getMessage());
            }

            if (!success) {
                runOnUiThread(new ToastRunnable(CameraActivity.this, R.string.camera_open_error));
            }
            finish();
        }

    };

    private enum MaxAxis {
        WIDTH,
        HEIGHT
    }

    private Bitmap createPortraitBitmap(byte[] imageData) throws FileNotFoundException {

        // measure the scale factor limiting to 1000px max size
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        Bitmap throwaway = BitmapFactory.decodeByteArray(imageData, 0, imageData.length, options); // this just sets options, returns null
        MaxAxis maxAxis = (options.outWidth >= options.outHeight) ? MaxAxis.WIDTH : MaxAxis.HEIGHT;
        if (DEBUG) Log.i(TAG, "Initial size: " + options.outWidth + "x" + options.outHeight);
        int scale = 1;
        int maxPx = 1000;
        if (DEBUG) Log.i(TAG, "Max px:" + maxPx);
        switch (maxAxis) {
            case WIDTH:
                while (options.outWidth / scale > maxPx)
                    scale *= 2;
                break;
            case HEIGHT:
                while (options.outHeight / scale > maxPx)
                    scale *= 2;
                break;
        }

        // second pass actually scale the preview image
        BitmapFactory.Options scaleOptions = new BitmapFactory.Options();
        scaleOptions.inSampleSize = scale;
        Bitmap image = BitmapFactory.decodeByteArray(imageData, 0, imageData.length, scaleOptions);

        if (maxAxis == MaxAxis.WIDTH) { // shot in landscape
            if (DEBUG) Log.i(TAG, "Pre-rotate size: " + image.getWidth() + "x" + image.getHeight());
            int w = image.getWidth();
            int h = image.getHeight();
            Matrix m = new Matrix();
            m.postRotate(90);
            Bitmap rotatedBMP = Bitmap.createBitmap(image, 0, 0, w, h, m, true);
            image.recycle();
            image = rotatedBMP;
        }

        if (DEBUG) Log.i(TAG, "Final size: " + image.getWidth() + "x" + image.getHeight());
        return image;
    }

    /** Create a File for saving an image or video */
    private File getOutputMediaFile(int type){
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = timeStamp + ".jpg";
        String contentType = "image/jpeg";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, fileName);
        values.put(MediaStore.Images.Media.DESCRIPTION, getString(R.string.post_reply_camera_capture));
        values.put(MediaStore.Images.Media.MIME_TYPE, contentType);
        values.put(MediaStore.Images.Media.ORIENTATION, "0");
        Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        String[] filePathColumn = { MediaStore.Images.ImageColumns.DATA };
        Cursor cursor = getContentResolver().query(imageUri, filePathColumn, null, null, null);
        if (cursor == null) {
            Log.e(TAG, "Unable to obtain cursor for camera uri=" + imageUri);
            return null;
        }

        cursor.moveToFirst();
        String imagePath = cursor.getString(cursor.getColumnIndexOrThrow(filePathColumn[0]));
        cursor.close();

        File mediaFile = new File(imagePath);

        return mediaFile;
    }

    public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
        private SurfaceHolder mHolder;
        private Camera mCamera;

        public CameraPreview(Context context, Camera camera) {
            super(context);
            mCamera = camera;

            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            mHolder = getHolder();
            mHolder.addCallback(this);
        }

        public void surfaceCreated(SurfaceHolder holder) {
            // The Surface has been created, now tell the camera where to draw the preview.
            try {
                mCamera.setDisplayOrientation(90);
                Camera.Parameters p = mCamera.getParameters();
                p.set("jpeg-quality", 85);
                //p.setRotation(90);
                p.setPictureFormat(ImageFormat.JPEG);
///                p.setPreviewSize(h, w);
                mCamera.setParameters(p);mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (IOException e) {
                if (DEBUG) Log.i(TAG, "Error setting camera preview: " + e.getMessage());
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            // empty. Take care of releasing the Camera preview in your activity.
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.

            if (mHolder.getSurface() == null){
                // preview surface does not exist
                return;
            }

            // stop preview before making changes
            try {
                mCamera.stopPreview();
            } catch (Exception e){
                // ignore: tried to stop a non-existent preview
            }

            // set preview size and make any resize, rotate or
            // reformatting changes here

            // start preview with new settings
            try {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();

            } catch (Exception e){
                if (DEBUG) Log.i(TAG, "Error starting camera preview: " + e.getMessage());
            }
        }
    }

}
