package com.chanapps.four.activity;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.*;
import android.webkit.WebView;
import android.widget.*;
import com.chanapps.four.component.RawResourceDialog;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanText;
import com.chanapps.four.task.LoadCaptchaTask;
import com.chanapps.four.task.PostReplyTask;

import java.io.*;
import java.text.DecimalFormat;
import java.util.Random;

public class PostReplyActivity extends Activity {

    public static final String TAG = PostReplyActivity.class.getSimpleName();

    public static final int PASSWORD_MAX = 100000000;

    private static final int IMAGE_CAPTURE = 0;
    private static final int IMAGE_GALLERY = 1;

    private ImageButton cameraButton;
    private ImageButton pictureButton;
    private ImageButton rotateLeftButton;
    private ImageButton rotateRightButton;
    private ImageButton refreshCaptchaButton;

    private Context ctx;
    private Resources res;

    private WebView recaptchaView;
    private LoadCaptchaTask loadCaptchaTask;

    private EditText messageText;
    private EditText recaptchaText;

    private ImageView imagePreview;
    private int angle = 0;

    public String imagePath;
    public String contentType;
    private String orientation;
    private Uri imageUri;
    public String boardCode = null;
    public long threadNo = 0;

    private Random randomGenerator = new Random();
    private DecimalFormat eightDigits = new DecimalFormat("00000000");

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        res = getResources();
        ctx = getApplicationContext();

        setContentView(R.layout.post_reply_layout);

        imagePreview = (ImageView)findViewById(R.id.post_reply_image_preview);

        cameraButton = (ImageButton)findViewById(R.id.post_reply_camera_button);
        pictureButton = (ImageButton)findViewById(R.id.post_reply_picture_button);
        rotateLeftButton = (ImageButton)findViewById(R.id.post_reply_rotate_left_button);
        rotateRightButton = (ImageButton)findViewById(R.id.post_reply_rotate_right_button);
        refreshCaptchaButton = (ImageButton)findViewById(R.id.post_reply_reload_captcha);

        messageText = (EditText)findViewById(R.id.post_reply_text);
        recaptchaText = (EditText)findViewById(R.id.post_reply_recaptcha_response);

        cameraButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                startCamera();
            }
        });
        pictureButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                startGallery();
            }
        });
        rotateLeftButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                rotateLeft();
            }
        });
        rotateRightButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                rotateRight();
            }
        });
        refreshCaptchaButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                reloadCaptcha();
            }
        });

        recaptchaView = (WebView) findViewById(R.id.post_reply_recaptcha_webview);
        recaptchaView.getSettings().setAllowFileAccess(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        Intent intent = getIntent();
        if (intent.hasExtra(ChanHelper.BOARD_CODE)) {
            boardCode = intent.getStringExtra(ChanHelper.BOARD_CODE);
            threadNo = intent.getLongExtra(ChanHelper.THREAD_NO, 0);
            String initialText = intent.getStringExtra(ChanHelper.TEXT);
            if (initialText != null && !initialText.isEmpty()) {
                String quotedText = ChanText.quoteText(initialText);
                messageText.setText(quotedText);
            }
            setBoardCode(boardCode);
        }
        else {
            Toast.makeText(ctx, R.string.post_reply_no_board, Toast.LENGTH_SHORT);
            navigateUp();
        }

        reloadCaptcha();
    }


    public void reloadCaptcha() {
        loadCaptchaTask = new LoadCaptchaTask(ctx, recaptchaView);
        loadCaptchaTask.execute(res.getString(R.string.post_reply_recaptcha_url_root));
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String msg;
        try {
            if (requestCode == IMAGE_CAPTURE) {
                if (resultCode == RESULT_OK && data != null) {
                    msg = res.getString(R.string.post_reply_added_image);
                    processImage(data);
                }
                else {
                    msg = res.getString(R.string.post_reply_no_load_camera_image);
                    Log.e(TAG, msg);
                }
            }
            else if (requestCode == IMAGE_GALLERY) {
                if (resultCode == RESULT_OK && data != null) {
                    msg = res.getString(R.string.post_reply_added_image);
                    processImage(data);
                }
                else {
                    msg = res.getString(R.string.post_reply_no_load_gallery_image);
                    Log.e(TAG, msg);
                }
            }
            else {
                msg = res.getString(R.string.post_reply_no_load_image);
                Log.e(TAG, msg);
            }
        }
        catch (Exception e) {
            msg = res.getString(R.string.post_reply_no_load_image);
            Log.e(TAG, msg, e);
        }
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
    }

    private Bitmap getImagePreviewBitmap() throws Exception {
        return getImagePreviewBitmap(true);
    }

    private Bitmap getImagePreviewBitmap(boolean useWidth) throws Exception {
        if (imageUri == null) {
            throw new Exception(res.getString(R.string.post_reply_no_image));
        }

        InputStream in = getContentResolver().openInputStream(imageUri);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        Bitmap previewBitmap = BitmapFactory.decodeStream(in, null, options);
        int requiredWidth = imagePreview.getWidth();
        int scale = 1;
        if (useWidth) {
            while (options.outWidth / scale / 2>= requiredWidth) {
                scale *= 2;
            }
        }
        else {
            while (options.outHeight / scale / 2>= requiredWidth) {
                scale *= 2;
            }
        }

        InputStream inScale = getContentResolver().openInputStream(imageUri);
        BitmapFactory.Options scaleOptions = new BitmapFactory.Options();
        scaleOptions.inSampleSize = scale;
        return BitmapFactory.decodeStream(inScale, null, scaleOptions);
    }

    private void resetImagePreview() {
        rotateLeftButton.setVisibility(View.VISIBLE);
        rotateRightButton.setVisibility(View.VISIBLE);
        imagePreview.setVisibility(View.VISIBLE);
        imagePreview.setPadding(0, 0, 0, 16);
        angle = 0;
    }

    private void setImagePreview() {
        try {
            Bitmap b = getImagePreviewBitmap();
            if (b != null) {
                resetImagePreview();
                imagePreview.setImageBitmap(b);
                Log.d(TAG, "Image: " + imageUri.toString() + " dimensions: " + b.getWidth() + "x" + b.getHeight());
            }
            else {
                Toast.makeText(ctx, R.string.post_reply_no_image, Toast.LENGTH_SHORT);
                Log.e(TAG, "Image: " + imageUri + toString() + " couldn't get bitmap");
            }
        }
        catch (Exception e) {
            Toast.makeText(ctx, R.string.post_reply_no_image, Toast.LENGTH_SHORT);
            Log.e(TAG, res.getString(R.string.post_reply_no_image), e);
        }
    }

    private void rotateLeft() {
        rotateImagePreview(-90);
    }

    private void rotateRight() {
        rotateImagePreview(90);
    }

    private void rotateImagePreview(int theta) {
        try {
            Bitmap b = getImagePreviewBitmap(false);
            if (b != null) {
                Matrix matrix = new Matrix();
                angle += theta;
                matrix.postRotate(angle);
                Bitmap rotatedBitmap = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);
                imagePreview.setImageBitmap(rotatedBitmap);
            }
            else {
                Toast.makeText(ctx, R.string.post_reply_no_image_rotate, Toast.LENGTH_SHORT);
                Log.e(TAG, res.getString(R.string.post_reply_no_image_rotate));
            }
        }
        catch (Exception e) {
            Toast.makeText(ctx, R.string.post_reply_no_image_rotate, Toast.LENGTH_SHORT);
            Log.e(TAG, res.getString(R.string.post_reply_no_image_rotate), e);
        }
    }

    private void startCamera() {
        String fileName = java.util.UUID.randomUUID().toString() + ".jpg";
        String contentType = "image/jpeg";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, fileName);
        values.put(MediaStore.Images.Media.DESCRIPTION, res.getString(R.string.post_reply_camera_capture));
        values.put(MediaStore.Images.Media.MIME_TYPE, contentType);
        values.put(MediaStore.Images.Media.ORIENTATION, "0");
        Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (imageUri != null) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(intent, IMAGE_CAPTURE);
        }
        else {
            Toast.makeText(ctx, R.string.post_reply_no_camera, Toast.LENGTH_SHORT).show();
            Log.e(TAG, res.getString(R.string.post_reply_no_camera));
        }
    }

    private void startGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, IMAGE_GALLERY);
    }

    private void processImage(Intent data) {
        imageUri = data.getData();
        String[] filePathColumn = { MediaStore.Images.ImageColumns.DATA, MediaStore.Images.ImageColumns.MIME_TYPE, MediaStore.Images.ImageColumns.ORIENTATION };
        Cursor cursor = getContentResolver().query(imageUri, filePathColumn, null, null, null);
        cursor.moveToFirst();
        imagePath = cursor.getString(cursor.getColumnIndexOrThrow(filePathColumn[0]));
        contentType = cursor.getString(cursor.getColumnIndexOrThrow(filePathColumn[1]));
        orientation = cursor.getString(cursor.getColumnIndexOrThrow(filePathColumn[2]));

        cursor.close();
        setImagePreview();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                navigateUp();
                return true;
            case R.id.post_reply_send_menu:
                String validMsg = validatePost();
                if (validMsg != null) {
                    Toast.makeText(ctx, validMsg, Toast.LENGTH_SHORT).show();
                    return true;
                }
                Toast.makeText(ctx, R.string.post_reply_posting, Toast.LENGTH_SHORT).show();
                PostReplyTask postReplyTask = new PostReplyTask(this);
                postReplyTask.execute();
                return true;
            case R.id.settings_menu:
                Log.i(TAG, "Starting settings activity");
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.help_menu:
                RawResourceDialog rawResourceDialog = new RawResourceDialog(this, R.raw.help_header, R.raw.help_post_reply);
                rawResourceDialog.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public String getMessage() {
        return messageText.getText().toString();
    }

    public String getImageUrl() {
        return imageUri != null ? imageUri.toString() : null;
    }

    public String getRecaptchaChallenge() {
        return loadCaptchaTask.getRecaptchaChallenge();
    }

    public String getRecaptchaResponse() {
        return  recaptchaText.getText().toString();
    }

    private String validatePost() {
        String recaptchaChallenge = loadCaptchaTask.getRecaptchaChallenge();
        if (recaptchaChallenge == null || recaptchaChallenge.trim().isEmpty()) {
            return res.getString(R.string.post_reply_captcha_error);
        }
        String recaptcha = recaptchaText.getText().toString();
        if (recaptcha == null || recaptcha.trim().isEmpty()) {
            return res.getString(R.string.post_reply_enter_captcha);
        }
        String message = messageText.getText().toString();
        String image = imageUri != null ? imageUri.getPath() : null;
        boolean hasMessage = message != null && !message.trim().isEmpty();
        boolean hasImage = image != null && !image.trim().isEmpty();
        if (threadNo == 0 && !hasImage) {
            return res.getString(R.string.post_reply_add_image);
        }
        if (threadNo != 0 && !hasMessage && !hasImage) {
            return res.getString(R.string.post_reply_add_text_or_image);
        }
        return null;
    }

    public void navigateUp() {
        Intent upIntent;
        if (threadNo != 0) {
            upIntent = new Intent(this, ThreadActivity.class);
            upIntent.putExtra(ChanHelper.BOARD_CODE, boardCode);
            upIntent.putExtra(ChanHelper.THREAD_NO, threadNo);
            upIntent.putExtra(ChanHelper.LAST_BOARD_POSITION, getIntent().getIntExtra(ChanHelper.LAST_BOARD_POSITION, 0));
            upIntent.putExtra(ChanHelper.LAST_THREAD_POSITION, getIntent().getIntExtra(ChanHelper.LAST_THREAD_POSITION, 0));
        }
        else {
            upIntent = new Intent(this, BoardActivity.class);
            upIntent.putExtra(ChanHelper.BOARD_CODE, boardCode);
            upIntent.putExtra(ChanHelper.LAST_BOARD_POSITION, getIntent().getIntExtra(ChanHelper.LAST_BOARD_POSITION, 0));
        }
        NavUtils.navigateUpTo(this, upIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.post_relpy_menu, menu);
        return true;
    }

    private void setBoardCode(String code) {
        boardCode = code;
        if (getActionBar() != null) {
            if (threadNo == 0) {
                getActionBar().setTitle("/" + boardCode + " " + getString(R.string.post_reply_thread_title));
            }
            else {
                getActionBar().setTitle("/" + boardCode + " " + getString(R.string.post_reply_title));
            }
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    public String generatePwd() {
        return eightDigits.format(randomGenerator.nextInt(PASSWORD_MAX));
    }

}
