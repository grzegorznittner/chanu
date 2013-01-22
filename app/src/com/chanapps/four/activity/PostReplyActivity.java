package com.chanapps.four.activity;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.chanapps.four.component.ChanGridSizer;
import com.chanapps.four.component.DispatcherHelper;
import com.chanapps.four.component.RawResourceDialog;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.data.ChanHelper.LastActivity;
import com.chanapps.four.fragment.PostingReplyDialogFragment;
import com.chanapps.four.task.LoadCaptchaTask;
import com.chanapps.four.task.PostReplyTask;

import java.io.*;
import java.text.DecimalFormat;
import java.util.Random;

public class PostReplyActivity extends FragmentActivity implements ChanIdentifiedActivity {

    public static final String TAG = PostReplyActivity.class.getSimpleName();

    public static final int PASSWORD_MAX = 100000000;

    private static final boolean DEBUG = true;

    private static final int IMAGE_CAPTURE = 0x10;
    private static final int IMAGE_GALLERY = 0x11;

    private ImageButton cameraButton;
    private ImageButton pictureButton;
    private ImageButton rotateLeftButton;
    private ImageButton rotateRightButton;

    private Context ctx;
    private Resources res;

    private ImageButton recaptchaButton;
    private LoadCaptchaTask loadCaptchaTask;

    private EditText messageText;
    private EditText recaptchaText;
    private EditText nameText;
    private EditText emailText;
    private EditText subjectText;
    TextView.OnEditorActionListener fastSend;

    private ImageView imagePreview;
    private int angle = 0;

    public String imagePath;
    public String contentType;
    public String orientation;
    public Uri imageUri;
    public Uri cameraImageUri;
    public String boardCode = null;
    public long threadNo = 0;
    public long postNo = 0;
    public long tim = 0;

    private Random randomGenerator = new Random();
    private DecimalFormat eightDigits = new DecimalFormat("00000000");

    private SharedPreferences prefs = null;

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

        fastSend = new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                validateAndSendReply();
                return true;
            }
        };
        messageText = (EditText)findViewById(R.id.post_reply_text);
        nameText = (EditText)findViewById(R.id.post_reply_name);
        emailText = (EditText)findViewById(R.id.post_reply_email);
        subjectText = (EditText)findViewById(R.id.post_reply_subject);
        recaptchaText = (EditText)findViewById(R.id.post_reply_recaptcha_response);
        recaptchaText.setOnEditorActionListener(fastSend);

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
        recaptchaButton = (ImageButton) findViewById(R.id.post_reply_recaptcha_imgview);
        recaptchaButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                reloadCaptcha();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (DEBUG) Log.i(TAG, "onStart");
    }

    public SharedPreferences ensurePrefs() {
        if (prefs == null)
            prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return prefs;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG) Log.i(TAG, "onResume");
        restoreInstanceState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (DEBUG) Log.i(TAG, "onPause");
        saveInstanceState();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (DEBUG) Log.i(TAG, "onStop");
    }

    private void restoreInstanceState() {
        if (getIntent().hasExtra(ChanHelper.BOARD_CODE))
            loadFromIntent(getIntent());
        else
            loadFromPrefs();

        loadImageFromPrefs();
        reloadCaptcha();
    }

    protected void setMessageText(String text, String quoteText) {
        messageText.setText("");
        if (text != null && !text.isEmpty()) {
            messageText.append(text);
        }
        else {
            if (postNo != 0) {
                messageText.append(">>" + postNo + "\n");
            }
            if (quoteText != null && !quoteText.isEmpty())
                messageText.append(quoteText);
        }
    }

    protected void setImageUri(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty())
            try {
                imageUri = Uri.parse(imageUrl);
                setImagePreview();
                if (DEBUG) Log.i(TAG, "successfully parsed imageUri=" + imageUri);
            }
            catch (Exception e) {
                Log.e(TAG, "Couldn't parse image uri=" + imageUri, e);
                imageUri = null;
                imagePreview.setVisibility(View.GONE);
            }
        else {
            imageUri = null;
            imagePreview.setVisibility(View.GONE);
            if (DEBUG) Log.i(TAG, "imageUrl passed was null or empty");
        }
    }

    protected void loadFromIntent(Intent intent) {
        boardCode = intent.getStringExtra(ChanHelper.BOARD_CODE);
        threadNo = intent.getLongExtra(ChanHelper.THREAD_NO, 0);
        postNo = intent.getLongExtra(ChanHelper.POST_NO, 0);
        tim = intent.getLongExtra(ChanHelper.TIM, 0);
        imagePath = null;
        contentType = null;
        orientation = null;

        String text = intent.getStringExtra(ChanHelper.TEXT);
        String quoteText = ChanPost.quoteText(intent.getStringExtra(ChanHelper.QUOTE_TEXT));
        setMessageText(text, quoteText);
        adjustSubjectHint();

        String imageUrl = intent.getStringExtra(ChanHelper.POST_REPLY_IMAGE_URL);
        if (imageUrl != null && !imageUrl.isEmpty()) {
            setImageUri(imageUrl);
            setImagePreview();
        }
        else {
            imagePreview.setVisibility(View.GONE);
        }
        cameraImageUri = null;

        setActionBarTitle();

        if (DEBUG) Log.i(TAG, "loaded from intent " + boardCode + "/" + threadNo + ":" + postNo + " tim=" + tim + " imageUrl=" + imageUrl + " text=" + " quoteText=" + quoteText);
    }

    protected void loadFromPrefs() {
        ensurePrefs();
        boardCode = prefs.getString(ChanHelper.BOARD_CODE, null);
        threadNo = prefs.getLong(ChanHelper.THREAD_NO, 0);
        postNo = prefs.getLong(ChanHelper.POST_NO, 0);
        tim = prefs.getLong(ChanHelper.TIM, 0);
        imagePath = prefs.getString(ChanHelper.IMAGE_PATH, null);
        contentType = prefs.getString(ChanHelper.CONTENT_TYPE, null);
        orientation = prefs.getString(ChanHelper.ORIENTATION, null);

        String text = prefs.getString(ChanHelper.TEXT, "");
        String quoteText = ChanPost.quoteText(prefs.getString(ChanHelper.QUOTE_TEXT, ""));
        setMessageText(text, quoteText);
        adjustSubjectHint();

        setActionBarTitle();
        if (DEBUG) Log.i(TAG, "loaded from prefs " + boardCode + "/" + threadNo + ":" + postNo + " tim=" + tim + " text=" + " quoteText=" + quoteText);
    }

    protected void adjustSubjectHint() {
        if (boardCode.equals("q") && threadNo == 0) {
            messageText.setHint(R.string.post_reply_text_hint_board_q);
            subjectText.setHint(R.string.post_reply_subject_hint_board_q);
        }
        else {
            messageText.setHint(R.string.post_reply_text_hint);
            subjectText.setHint(R.string.post_reply_subject_hint);
        }
    }

    protected void loadImageFromPrefs() {
        String imageUrl = ensurePrefs().getString(ChanHelper.POST_REPLY_IMAGE_URL, null);
        if (DEBUG) Log.i(TAG, "Found pref value for imageUrl=" + imageUrl);
        if (imageUrl != null && !imageUrl.isEmpty()) {
            setImageUri(imageUrl);
            setImagePreview();
        }
        else {
            imageUri = null;
        }
        // we have to do this because of a bug in android activity result intent for the camera which returns null instead of uri
        cameraImageUri = null;
    }

    protected void saveInstanceState() {
        SharedPreferences.Editor ed = ensurePrefs().edit();
        ed.putString(ChanHelper.BOARD_CODE, boardCode);
        ed.putLong(ChanHelper.THREAD_NO, threadNo);
        ed.putLong(ChanHelper.POST_NO, postNo);
        ed.putString(ChanHelper.TEXT, messageText.getText().toString());
        ed.putString(ChanHelper.QUOTE_TEXT, null);
        ed.putLong(ChanHelper.TIM, tim);
        ed.putString(ChanHelper.CAMERA_IMAGE_URL, cameraImageUri == null ? null : cameraImageUri.toString());
        ed.putString(ChanHelper.POST_REPLY_IMAGE_URL, imageUri == null ? null : imageUri.toString());
        ed.putString(ChanHelper.IMAGE_PATH, imagePath);
        ed.putString(ChanHelper.CONTENT_TYPE, contentType);
        ed.putString(ChanHelper.ORIENTATION, orientation);
        ed.commit();
        if (DEBUG) Log.i(TAG, "Saved to prefs " + boardCode + "/" + threadNo + ":" + postNo + " tim=" + tim
                + " imageUrl=" + (imageUri == null ? "" : imageUri.toString())
                + " cameraImageUrl=" + (cameraImageUri == null ? "" : cameraImageUri.toString()));
        DispatcherHelper.saveActivityToPrefs(this);
    }

    public void reloadCaptcha() {
        recaptchaText.setText("");
        recaptchaText.setHint(R.string.post_reply_recaptcha_hint);
        loadCaptchaTask = new LoadCaptchaTask(ctx, recaptchaButton);
        loadCaptchaTask.execute(res.getString(R.string.post_reply_recaptcha_url_root));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (DEBUG) Log.i(TAG, "onActivityResult for code=" + requestCode + " data=" + intent);
        String msg;
        try {
            if (requestCode == IMAGE_CAPTURE) {
                if (resultCode == RESULT_OK) {
                    msg = res.getString(R.string.post_reply_added_image);
                    cameraImageUri = Uri.parse(ensurePrefs().getString(ChanHelper.CAMERA_IMAGE_URL, null));
                    imageUri = cameraImageUri;
                    ensurePrefs().edit().putString(ChanHelper.POST_REPLY_IMAGE_URL, imageUri.toString()).commit();
                    if (DEBUG) Log.i(TAG, "Got camera result for activity url=" + imageUri);
                }
                else {
                    msg = res.getString(R.string.post_reply_no_load_camera_image);
                    if (DEBUG) Log.i(TAG, msg);
                }
            }
            else if (requestCode == IMAGE_GALLERY) {
                if (resultCode == RESULT_OK && intent != null && intent.getData() != null) {
                    msg = res.getString(R.string.post_reply_added_image);
                    imageUri = intent.getData();
                    ensurePrefs().edit().putString(ChanHelper.POST_REPLY_IMAGE_URL, imageUri.toString()).commit();
                    if (DEBUG) Log.i(TAG, "Got gallery result for activity imageUri=" + imageUri);
                }
                else {
                    msg = res.getString(R.string.post_reply_no_load_gallery_image);
                    if (DEBUG) Log.i(TAG, msg);
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

    private enum MaxAxis {
        WIDTH,
        HEIGHT
    }

    private Bitmap getImagePreviewBitmap() throws Exception {
        if (DEBUG) Log.i(TAG, "getImagePreviewBitmap with imageUri=" + imageUri);
        if (imageUri == null) {
            return null;
        }

        imagePath = null;
        contentType = null;
        orientation = null;
        try {
            String[] filePathColumn = { MediaStore.Images.ImageColumns.DATA, MediaStore.Images.ImageColumns.MIME_TYPE, MediaStore.Images.ImageColumns.ORIENTATION };
            Cursor cursor = getContentResolver().query(imageUri, filePathColumn, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
                imagePath = cursor.getString(cursor.getColumnIndexOrThrow(filePathColumn[0]));
                contentType = cursor.getString(cursor.getColumnIndexOrThrow(filePathColumn[1]));
                orientation = cursor.getString(cursor.getColumnIndexOrThrow(filePathColumn[2]));
                cursor.close();
            }
            if (DEBUG) Log.i(TAG, "Got media settings for imageUri=" + imageUri + " path=" + imagePath + " contentType=" + contentType + " orientation=" + orientation);
        }
        catch (Exception e) {
            Log.e(TAG, "Couldn't get media information for imageUri=" + imageUri, e);
        }

        //return MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

        // just find the image bounds first pass
        InputStream in = getContentResolver().openInputStream(imageUri);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        Bitmap previewBitmap = BitmapFactory.decodeStream(in, null, options); // this just sets options, returns null
        MaxAxis axis = (options.outWidth >= options.outHeight) ? MaxAxis.WIDTH : MaxAxis.HEIGHT;
        if (DEBUG) Log.i(TAG, "Initial size: " + options.outWidth + "x" + options.outHeight);
        int scale = 1;
        int maxPx = ChanGridSizer.dpToPx(getWindowManager().getDefaultDisplay(), 250); // limit to match thread preview size
        if (DEBUG) Log.i(TAG, "Max px:" + maxPx);
        switch (axis) {
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
        InputStream inScale = getContentResolver().openInputStream(imageUri);
        BitmapFactory.Options scaleOptions = new BitmapFactory.Options();
        scaleOptions.inSampleSize = scale;
        Bitmap b = BitmapFactory.decodeStream(inScale, null, scaleOptions);
        if (DEBUG) Log.i(TAG, "Final size: " + b.getWidth() + "x" + b.getHeight());
        return b;
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
            if (imageUri == null) {
                imagePreview.setVisibility(View.GONE);
                if (DEBUG) Log.i(TAG, "No image uri found, not setting image");
                return;
            }
            Bitmap b = getImagePreviewBitmap();
            if (b != null) {
                resetImagePreview();
                imagePreview.setImageBitmap(b);
                if (DEBUG) Log.i(TAG, "setImagePreview with bitmap imageUri=" + imageUri.toString() + " dimensions: " + b.getWidth() + "x" + b.getHeight());
            }
            else {
                //Toast.makeText(ctx, R.string.post_reply_no_image, Toast.LENGTH_SHORT).show();
                imagePreview.setVisibility(View.GONE);
                Log.e(TAG, "setImagePreview null bitmap with imageUri=" + imageUri.toString());
            }
        }
        catch (Exception e) {
            Toast.makeText(ctx, R.string.post_reply_no_image, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "setImagePreview exception while loading bitmap", e);
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
            Bitmap b = getImagePreviewBitmap();
            if (b != null) {
                Matrix matrix = new Matrix();
                angle += theta;
                matrix.postRotate(angle);
                Bitmap rotatedBitmap = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);
                imagePreview.setImageBitmap(rotatedBitmap);
            }
            else {
                Toast.makeText(ctx, R.string.post_reply_no_image_rotate, Toast.LENGTH_SHORT).show();
                Log.e(TAG, res.getString(R.string.post_reply_no_image_rotate));
            }
        }
        catch (Exception e) {
            Toast.makeText(ctx, R.string.post_reply_no_image_rotate, Toast.LENGTH_SHORT).show();
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
        Uri reservedCameraUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (reservedCameraUri != null) {
            cameraImageUri = reservedCameraUri;
            if (DEBUG) Log.i(TAG, "Starting camera with imageUri=" + cameraImageUri);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            intent.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            startActivityForResult(intent, IMAGE_CAPTURE);
        }
        else {
            Toast.makeText(ctx, R.string.post_reply_no_camera, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Couldn't get camera image");
        }
    }

    private void startGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, IMAGE_GALLERY);
    }

    protected void validateAndSendReply() {
        String validMsg = validatePost();
        if (validMsg != null) {
            Toast.makeText(ctx, validMsg, Toast.LENGTH_SHORT).show();
        }
        else {
            //Toast.makeText(ctx, R.string.post_reply_posting, Toast.LENGTH_LONG).show();
            IBinder windowToken = getCurrentFocus() != null ? getCurrentFocus().getWindowToken() : null;
            if (windowToken != null) { // close the keyboard
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(windowToken, 0);
            }
            PostReplyTask postReplyTask = new PostReplyTask(this);
            PostingReplyDialogFragment dialogFragment = new PostingReplyDialogFragment(postReplyTask);
            dialogFragment.show(getSupportFragmentManager(), PostingReplyDialogFragment.TAG);
            if (!postReplyTask.isCancelled()) {
                postReplyTask.execute(dialogFragment);
            }
        }
    }

    public void navigateUp() {
        Intent intent = ThreadActivity.createIntentForActivity(
                this,
                boardCode,
                threadNo,
                null,
                null,
                0,
                0,
                tim,
                false,
                0,
                true
        );
        NavUtils.navigateUpTo(this, intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
/*
            case android.R.id.home:
                navigateUp();
                return true;
*/
            case R.id.post_reply_send_menu:
                validateAndSendReply();
                return true;
            case R.id.settings_menu:
                if (DEBUG) Log.i(TAG, "Starting settings activity");
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.help_menu:
                RawResourceDialog rawResourceDialog = new RawResourceDialog(this, R.layout.about_dialog, R.raw.help_header, R.raw.help_post_reply);
                rawResourceDialog.show();
                return true;
            case R.id.about_menu:
                RawResourceDialog aboutDialog = new RawResourceDialog(this, R.layout.about_dialog, R.raw.about_header, R.raw.about_detail);
                aboutDialog.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public String getMessage() {
        String s = messageText.getText().toString();
        return (s != null) ? s : "";
    }

    public String getName() {
        String s = nameText.getText().toString();
        return (s != null) ? s : "";
    }

    public String getEmail() {
        String s = emailText.getText().toString();
        return (s != null) ? s : "";
    }

    public String getSubject() {
        String s = subjectText.getText().toString();
        return (s != null) ? s : "";
    }

    public String getRecaptchaChallenge() {
        return loadCaptchaTask.getRecaptchaChallenge();
    }

    public String getRecaptchaResponse() {
        return recaptchaText.getText().toString();
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
        String subject = subjectText.getText().toString();
        String image = imageUri != null ? imageUri.getPath() : null;
        boolean hasMessage = message != null && !message.trim().isEmpty();
        boolean hasImage = image != null && !image.trim().isEmpty();
        if (threadNo == 0 && !hasImage)
            return res.getString(R.string.post_reply_add_image);
        if (threadNo != 0 && !hasMessage && !hasImage)
            return res.getString(R.string.post_reply_add_text_or_image);
        if (threadNo == 0
                && boardCode.equals("q")
                && !(subject != null && !subject.isEmpty())
                && !(message != null && !message.isEmpty()))
            return res.getString(R.string.post_reply_board_q_special_needs);
        return null;
    }

    /*
    public void navigateUp() {
        Intent upIntent;
        if (threadNo != 0 || !fromBoard) {
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
    */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.post_relpy_menu, menu);
        return true;
    }

    private void setActionBarTitle() {
        if (getActionBar() != null) {
            if (threadNo == 0) {
                getActionBar().setTitle("/" + boardCode + " " + getString(R.string.post_reply_thread_title));
            }
            else {
                getActionBar().setTitle("/" + boardCode + " " + getString(R.string.post_reply_title));
            }
            getActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    public String generatePwd() {
        return eightDigits.format(randomGenerator.nextInt(PASSWORD_MAX));
    }

    @Override
	public ChanActivityId getChanActivityId() {
		return new ChanActivityId(LastActivity.POST_REPLY_ACTIVITY);
	}

	@Override
	public Handler getChanHandler() {
		return null;
	}
}
