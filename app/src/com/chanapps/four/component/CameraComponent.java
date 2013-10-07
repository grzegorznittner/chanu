package com.chanapps.four.component;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;
import com.chanapps.four.activity.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 8/5/13
 * Time: 4:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class CameraComponent {

    public static final int CAMERA_RESULT = 0x19;

    private static final String TAG = CameraComponent.class.getSimpleName();
    private static final boolean DEBUG = true;
    private static final String PICTURES_DIR = "Pictures";
    private static final String ALBUM_NAME = "Chanu";
    private static final String JPEG_FILE_PREFIX = "Photo_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";
    private static final int MAX_PICTURE_SIZE_PX = 1000;

    private Context context;
    private Uri imageUri;

    public CameraComponent(Context context, Uri existingImageUri) {
        this.context = context;
        this.imageUri = existingImageUri;
    }

    public Uri startCamera(Activity activity) {
        //Intent intent = new Intent(this, CameraActivity.class);
        //startActivityForResult(intent, IMAGE_CAPTURE);
        //File filePhoto = getOutputMediaFile();
        //try {
        //File filePhoto = getTempPictureFile();
        Uri mediaUri = createMediaUri();
        File filePhoto = getMediaFile(mediaUri);
        imageUri = Uri.fromFile(filePhoto);
        if (DEBUG) Log.i(TAG, "startCamera() got mediaUri=" + mediaUri + " set imageUri=" + imageUri);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        intent.putExtra("return-data", true);
        activity.startActivityForResult(intent, CAMERA_RESULT);
        //}
        //catch (IOException e) {
        //    Log.e(TAG, "Couldn't create camera photo temp file", e);
        //    Toast.makeText(context, R.string.post_reply_no_camera, Toast.LENGTH_SHORT).show();
        //}
        return imageUri;
    }

    public void handleResult() {
        try {
            resizeIfNecessary();
            addImageToGallery();
        } catch (FileNotFoundException e) {
            if (DEBUG) Log.i(TAG, "File not found: " + e.getMessage());
            Toast.makeText(context, R.string.camera_capture_error, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            if (DEBUG) Log.i(TAG, "Error accessing file: " + e.getMessage());
            Toast.makeText(context, R.string.camera_capture_error, Toast.LENGTH_SHORT).show();
        }
    }
    /*
    private File getAlbumDir() {
        return new File(Environment.getExternalStorageDirectory()
                + "/"
                + PICTURES_DIR
                + "/"
                + ALBUM_NAME);
    }

    private File getTempPictureFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
        File image = File.createTempFile(
                imageFileName,
                JPEG_FILE_SUFFIX,
                getAlbumDir()
        );
        return image;
    }
    */
    private File getMediaFile(Uri mediaUri) {
        String[] filePathColumn = { MediaStore.Images.ImageColumns.DATA };
        Cursor cursor = context.getContentResolver().query(mediaUri, filePathColumn, null, null, null);
        if (cursor == null) {
            Log.e(TAG, "getOutputMediaFile() unable to obtain cursor for camera mediaUri=" + mediaUri);
            return null;
        }
        cursor.moveToFirst();
        String imagePath = cursor.getString(cursor.getColumnIndexOrThrow(filePathColumn[0]));
        cursor.close();
        File mediaFile = new File(imagePath);
        return mediaFile;
    }

    private File getImageFile() {
        return new File(imageUri.getPath());
    }

    private Uri createMediaUri() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = timeStamp + ".jpg";
        String contentType = "image/jpeg";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, fileName);
        values.put(MediaStore.Images.Media.DESCRIPTION, context.getString(R.string.post_reply_camera_capture));
        values.put(MediaStore.Images.Media.MIME_TYPE, contentType);
        values.put(MediaStore.Images.Media.ORIENTATION, "0");
        Uri mediaUri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        return mediaUri;
    }

    private void addImageToGallery() {
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(imageUri);
        context.sendBroadcast(intent);
    }

    private void resizeIfNecessary() throws FileNotFoundException, IOException {
        File pictureFile = getImageFile();
        if (DEBUG) Log.i(TAG, "Took picture:" + pictureFile.getAbsolutePath());

        ExifInterface exif = new ExifInterface(pictureFile.getAbsolutePath());
        String orientation = "" + exif.getAttribute(ExifInterface.TAG_ORIENTATION); // camera orientation
        if (DEBUG) Log.i(TAG, "post-take orientation=" + orientation);
        //exif.setAttribute(ExifInterface.TAG_ORIENTATION, "0"); // prevent post-upload rotation of image
        //exif.saveAttributes();

        Bitmap b = createBitmap(orientation);
        FileOutputStream fos = new FileOutputStream(pictureFile);
        b.compress(Bitmap.CompressFormat.JPEG, 85, fos);
        fos.close();
        b.recycle();

        exif = new ExifInterface(pictureFile.getAbsolutePath());
        orientation = "" + exif.getAttribute(ExifInterface.TAG_ORIENTATION); // camera orientation
        if (DEBUG) Log.i(TAG, "post-save orientation=" + orientation);
    }

    private enum MaxAxis {
        WIDTH,
        HEIGHT
    }

    private int rotationAngle(String orientString) {
        int orientation = orientString == null ? ExifInterface.ORIENTATION_NORMAL : Integer.parseInt(orientString);
        int rotationAngle;
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                rotationAngle = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                rotationAngle = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                rotationAngle = 270;
                break;
            default:
                rotationAngle = 0;
        }
        return rotationAngle;
    }

    private int inSampleSize(int maxSizePx) { // scale to prevent excessively large images
        int scale = 1;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        File imageFile = getImageFile();
        if (imageFile == null) {
            Log.e(TAG, "Couldn't get image file from imageUri=" + imageUri);
            return scale;
        }
        BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options); // this just sets options, returns null
        MaxAxis maxAxis = (options.outWidth >= options.outHeight) ? MaxAxis.WIDTH : MaxAxis.HEIGHT;
        if (DEBUG) Log.i(TAG, "inSampleSize() input size=" + options.outWidth + "x" + options.outHeight);
        switch (maxAxis) {
            case WIDTH:
                while (options.outWidth / scale > maxSizePx)
                    scale *= 2;
                break;
            case HEIGHT:
                while (options.outHeight / scale > maxSizePx)
                    scale *= 2;
                break;
        }
        if (DEBUG) Log.i(TAG, "inSampleSize() output scale=" + scale);
        return scale;
    }

    private int adjustRotation(Bitmap image, int inputAngle) { // overcome bug in android camera orientation
        return inputAngle;
    }

    private Bitmap rotateBitmap(Bitmap image, int rotationAngle) { // destructively rotate bitmap
        if (DEBUG) Log.i(TAG, "rotateBitmap() input size=" + image.getWidth() + "x" + image.getHeight()
                + " angle=" + rotationAngle);
        int w = image.getWidth();
        int h = image.getHeight();
        Matrix m = new Matrix();
        m.postRotate(rotationAngle);
        Bitmap rotatedBMP = Bitmap.createBitmap(image, 0, 0, w, h, m, true);
        image.recycle();
        image = rotatedBMP;
        if (DEBUG) Log.i(TAG, "rotateBitmap() output size=" + image.getWidth() + "x" + image.getHeight());
        return image;
    }

    private Bitmap createBitmap(String orientString) throws FileNotFoundException {
        if (DEBUG) Log.i(TAG, "createBitmap() input orientation=" + orientString);

        // scale the image
        int scale = inSampleSize(MAX_PICTURE_SIZE_PX);
        BitmapFactory.Options scaleOptions = new BitmapFactory.Options();
        scaleOptions.inSampleSize = scale;
        File imageFile = getImageFile();
        if (imageFile == null) {
            Log.e(TAG, "Couldn't get image file from imageUri=" + imageUri);
            return null;
        }
        Bitmap image = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), scaleOptions);
        if (DEBUG) Log.i(TAG, "createBitmap() scale=" + scale);

        // third pass rotate the image if necessary
        int inputAngle = rotationAngle(orientString);
        int rotationAngle = adjustRotation(image, inputAngle);
        if (rotationAngle != 0)
            image = rotateBitmap(image, rotationAngle);
        if (DEBUG) Log.i(TAG, "createBitmap() angle=" + rotationAngle);

        if (DEBUG) Log.i(TAG, "createBitmap() output size=" + image.getWidth() + "x" + image.getHeight());
        return image;
    }

}
