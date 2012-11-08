package com.chanapps.four.test;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
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
import com.chanapps.four.data.LoadCaptchaTask;
import com.chanapps.four.data.PostReplyTask;

import java.io.*;

public class PostReplyActivity extends Activity {

    public static final String TAG = PostReplyActivity.class.getSimpleName();

    public static final String RECAPTCHA_NOSCRIPT_URL = "http://www.google.com/recaptcha/api/noscript?k=";
    public static final String RECAPTCHA_PUBLIC_KEY = "6Ldp2bsSAAAAAAJ5uyx_lx34lJeEpTLVkP5k04qc";
    public static final String RECAPTCHA_URL = RECAPTCHA_NOSCRIPT_URL + RECAPTCHA_PUBLIC_KEY;

    private static final int IMAGE_CAPTURE = 0;
    private static final int IMAGE_GALLERY = 1;

    private ImageButton cameraButton;
    private ImageButton pictureButton;
    private ImageButton rotateLeftButton;
    private ImageButton rotateRightButton;
    private ImageButton refreshCaptchaButton;

    private WebView recaptchaView;
    private LoadCaptchaTask loadCaptchaTask;

    private EditText messageText;
    private EditText recaptchaText;

    private ImageView imagePreview;
    private int angle = 0;

    private String fileName;
    private Uri imageUri;
    public String boardCode = null;
    public int threadNo = 0;

    private void randomizeThread() {
        setBoardCode("diy");
        threadNo = 325344;
        /*
        double d = Math.random();
        if (d >= 0.75) {
            setBoardCode("trv");
            threadNo = 609350;
        }
        else if (d >= 0.5) {
            setBoardCode("diy");
            threadNo = 100304;
        }
        else if (d >= 0.25) {
            setBoardCode("fit");
            threadNo = 4820056;
        }
        else {
            setBoardCode("po");
            threadNo = 430177;
        }
        */
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent.hasExtra("threadNo")) {
            setBoardCode(intent.getStringExtra("boardCode"));
            threadNo = intent.getIntExtra("threadNo", 0);
        } else {
            randomizeThread();
        }

        setContentView(R.layout.post_reply_activity_layout);

        imagePreview = (ImageView)findViewById(R.id.post_reply_image_preview);

        cameraButton = (ImageButton)findViewById(R.id.post_reply_camera_button);
        pictureButton = (ImageButton)findViewById(R.id.post_reply_picture_button);
        rotateLeftButton = (ImageButton)findViewById(R.id.post_reply_rotate_left_button);
        rotateRightButton = (ImageButton)findViewById(R.id.post_reply_rotate_right_button);
        refreshCaptchaButton = (ImageButton)findViewById(R.id.post_reply_reload_captcha);

        messageText = (EditText)findViewById(R.id.post_reply_text);
        recaptchaText = (EditText)findViewById(R.id.post_reply_recaptcha_response);

        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startCamera();
            }
        });
        pictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startGallery();
            }
        });
        rotateLeftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rotateLeft();
            }
        });
        rotateRightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rotateRight();
            }
        });
        refreshCaptchaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reloadCaptcha();
            }
        });

        recaptchaView = (WebView) findViewById(R.id.post_reply_recaptcha_webview);
        recaptchaView.getSettings().setAllowFileAccess(true);

        reloadCaptcha();
    }


    public void reloadCaptcha() {
        loadCaptchaTask = new LoadCaptchaTask(getApplicationContext(), recaptchaView);
        loadCaptchaTask.execute(RECAPTCHA_URL);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String msg = "Couldn't load activity image";
        try {
        if (requestCode == IMAGE_CAPTURE) {
            if (resultCode == RESULT_OK && imageUri != null) {
                msg = "Added image to post";
                setImagePreview();
            }
            else {
                Log.e(TAG, "Couldn't load camera image");
            }
        }
        else if (requestCode == IMAGE_GALLERY) {
            if (resultCode == RESULT_OK) {
                msg = "Added image to post";
                imageUri = data.getData();
                setImagePreview();
            }
            else {
                Log.e(TAG, "Couldn't load gallery image");
            }
        }
        else {
            msg = "Unknown activity";
        }
        }
        catch (Exception e) {
            Log.e(TAG, msg, e);
        }
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private Bitmap getImagePreviewBitmap() throws Exception {
        return getImagePreviewBitmap(true);
    }

    private Bitmap getImagePreviewBitmap(boolean useWidth) throws Exception {
        if (imageUri == null) {
            throw new Exception("Null image URI for preview");
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
                Log.e(TAG, "Image: " + imageUri.toString() + " dimensions: " + b.getWidth() + "x" + b.getHeight());
            }
            else {
                Log.e(TAG, "Image: " + imageUri+toString() + " null bitmap");
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Couldn't load image bitmap", e);
        }
    }

    private void rotateLeft() {
        rotateImagePreview(-90);
    }

    private void rotateRight() {
        rotateImagePreview(90);
    }

    private void rotateImagePreview(int theta) {
        Log.e(TAG, "rotate right...");
        try {
            Bitmap b = getImagePreviewBitmap(false);
            if (b != null) {
                Log.e(TAG, "ready to rotate right...");
                Matrix matrix = new Matrix();
                angle += theta;
                matrix.postRotate(angle);
                Bitmap rotatedBitmap = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);
                imagePreview.setImageBitmap(rotatedBitmap);
            }
            else {
                Log.e(TAG, "couldn't get image bitmap for rotation");
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Couldn't rotate image bitmap", e);
        }
    }

    private void startCamera() {
        Log.d(TAG, "starting camera...");
        fileName = java.util.UUID.randomUUID().toString() + ".jpg";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, fileName);
        values.put(MediaStore.Images.Media.DESCRIPTION,
                "Image capture by camera");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        imageUri = getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (imageUri != null) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(intent, IMAGE_CAPTURE);
        }
        else {
            Toast.makeText(getApplicationContext(), "Unable to load camera, try again", Toast.LENGTH_SHORT).show();
        }
    }

    private void startGallery() {
        Log.d(TAG, "starting gallery...");
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, IMAGE_GALLERY);
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
                    Toast.makeText(getApplicationContext(), validMsg, Toast.LENGTH_SHORT).show();
                    return true;
                }
                Toast.makeText(getApplicationContext(), "Posting reply...", Toast.LENGTH_SHORT).show();
                PostReplyTask postReplyTask = new PostReplyTask(this);
                postReplyTask.execute();
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

    public String getContentType() {
        return imageUri != null ? getContentResolver().getType(imageUri) : null;
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
            return "Can't post without captca, try later";
        }
        String recaptcha = recaptchaText.getText().toString();
        if (recaptcha == null || recaptcha.trim().isEmpty()) {
            return "Enter captcha to post";
        }
        String message = messageText.getText().toString();
        String image = imageUri != null ? imageUri.getPath() : null;
        boolean hasMessage = message != null && !message.trim().isEmpty();
        boolean hasImage = image != null && !image.trim().isEmpty();
        if (!hasMessage && !hasImage) {
            return "Enter text or image to post";
        }
        return null;
    }

    public void navigateUp() {
        Intent upIntent = new Intent(this, ThreadListActivity.class);
        upIntent.putExtra("boardCode", boardCode);
        upIntent.putExtra("threadNo", threadNo);
        NavUtils.navigateUpTo(this, upIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "onCreateOptionsMenu called");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.post_relpy_menu, menu);
        return true;
    }

    private void setBoardCode(String code) {
        boardCode = code;
        if (getActionBar() != null) {
            getActionBar().setTitle("/" + boardCode + " " + getString(R.string.post_reply_activity));
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

}
