package com.chanapps.four.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
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
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.data.ChanHelper.LastActivity;
import com.chanapps.four.fragment.*;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.service.profile.NetworkProfile;
import com.chanapps.four.task.AuthorizePassTask;
import com.chanapps.four.task.LoadCaptchaTask;
import com.chanapps.four.task.LogoutPassTask;
import com.chanapps.four.task.PostReplyTask;

import java.io.*;
import java.text.DecimalFormat;
import java.util.Random;

public class PostReplyActivity extends FragmentActivity implements ChanIdentifiedActivity, RefreshableActivity {

    public static final String TAG = PostReplyActivity.class.getSimpleName();

    public static final int POST_FINISHED = 0x01;

    private static final boolean DEBUG = true;

    public static final int PASSWORD_MAX = 100000000;
    private static final Random randomGenerator = new Random();
    private static final DecimalFormat eightDigits = new DecimalFormat("00000000");

    private static final int IMAGE_CAPTURE = 0x10;
    private static final int IMAGE_GALLERY = 0x11;

    private LinearLayout wrapperLayout;

    private ImageButton cameraButton;
    private ImageButton pictureButton;
    private ImageButton deleteButton;
    private ImageButton bumpButton;
    private ImageButton sageButton;
    private ImageButton passEnableButton;
    private ImageButton passDisableButton;

    private Context ctx;
    private Resources res;
    private Handler handler;

    private FrameLayout recaptchaFrame;
    private ImageButton recaptchaButton;
    private ImageView recaptchaLoading;
    private EditText recaptchaText;
    private LoadCaptchaTask loadCaptchaTask;

    private TextView messageText;
    private TextView passStatusText;
    private EditText nameText;
    private EditText emailText;
    private EditText subjectText;
    private EditText passwordText;
    private CheckBox spoilerCheckbox;

    TextView.OnEditorActionListener fastSend;

    private ImageView imagePreview;
    private int angle = 0;

    public String imagePath;
    public String contentType;
    public String orientation;
    public Uri imageUri;
    public String boardCode = null;
    public long threadNo = 0;
    public long postNo = 0;
    public long tim = 0;

    private SharedPreferences prefs = null;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        res = getResources();
        ctx = getApplicationContext();

        setContentView(R.layout.post_reply_layout);

        wrapperLayout = (LinearLayout)findViewById(R.id.post_reply_wrapper);

        imagePreview = (ImageView)findViewById(R.id.post_reply_image_preview);

        cameraButton = (ImageButton)findViewById(R.id.post_reply_camera_button);
        pictureButton = (ImageButton)findViewById(R.id.post_reply_picture_button);
        deleteButton = (ImageButton)findViewById(R.id.post_reply_delete_button);
        bumpButton = (ImageButton)findViewById(R.id.post_reply_bump_button);
        sageButton = (ImageButton)findViewById(R.id.post_reply_sage_button);
        passEnableButton = (ImageButton)findViewById(R.id.post_reply_pass_enable_button);
        passDisableButton = (ImageButton)findViewById(R.id.post_reply_pass_disable_button);

        // do this popup jazz because android doesn't really handle multiline edit text views very well
        messageText = (TextView)findViewById(R.id.post_reply_text);
        messageText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                (new EditMessageTextDialogFragment()).show(getSupportFragmentManager(), EditMessageTextDialogFragment.TAG);
            }
        });

        passStatusText = (TextView)findViewById(R.id.post_reply_pass_status);
        nameText = (EditText)findViewById(R.id.post_reply_name);
        emailText = (EditText)findViewById(R.id.post_reply_email);
        subjectText = (EditText)findViewById(R.id.post_reply_subject);
        passwordText = (EditText)findViewById(R.id.post_reply_password);
        passwordText.setText(generatePassword()); // always default random generate, then we store for later use
        spoilerCheckbox = (CheckBox)findViewById(R.id.post_reply_spoiler_checkbox);

        fastSend = new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                validateAndSendReply();
                return true;
            }
        };
        recaptchaFrame = (FrameLayout)findViewById(R.id.post_reply_recaptcha_frame);
        recaptchaText = (EditText)findViewById(R.id.post_reply_recaptcha_response);
        recaptchaText.setOnEditorActionListener(fastSend);

        setupCameraButton();
        pictureButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                startGallery();
            }
        });
        deleteButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                deleteImage();
            }
        });
        bumpButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                bump();
            }
        });
        sageButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                sage();
            }
        });
        passEnableButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (!isPassEnabled() && isPassAvailable())
                    showPassFragment();
            }
        });
        passDisableButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (isPassEnabled()) {
                    disablePass();
                }
            }
        });
        passStatusText.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (!isPassEnabled() && isPassAvailable())
                    showPassFragment();
            }
        });

        recaptchaButton = (ImageButton) findViewById(R.id.post_reply_recaptcha_imgview);
        recaptchaButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                reloadCaptcha();
            }
        });
        recaptchaLoading = (ImageView) findViewById(R.id.post_reply_recaptcha_loading);

        updatePassRecaptchaViews(isPassEnabled());
    }


    private void showPassFragment() {
        showPassFragment(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
                wrapperLayout.setVisibility(View.VISIBLE);
                boolean passEnabled = isPassEnabled();
                updatePassRecaptchaViews(passEnabled);
                if (passEnabled)
                    Toast.makeText(PostReplyActivity.this, R.string.post_reply_pass_enabled_text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showPassFragment(DialogInterface.OnDismissListener dismissListener) {
        closeKeyboard();
        PassSettingsFragment fragment = new PassSettingsFragment();
        fragment.setOnDismissListener(dismissListener);
        fragment.setRefreshableActivity(this);
        android.app.FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(android.R.id.content, fragment);
        ft.setTransition(android.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.addToBackStack(null);
        ft.commit();
        wrapperLayout.setVisibility(View.GONE);
    }

    private void setupCameraButton() {
        boolean hasCameraFeature = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
        int numCameras = Camera.getNumberOfCameras();
        boolean hasCamera = hasCameraFeature && numCameras > 0;
        if (DEBUG) Log.i(TAG, "has cameraFeature=" + hasCameraFeature + " numCameras=" + numCameras + " hasCamera=" + hasCamera);
        if (hasCamera) {
            cameraButton.setVisibility(View.VISIBLE);
            cameraButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    startCamera();
                }
            });
        }
        else {
            cameraButton.setVisibility(View.GONE);
        }
    }

    private boolean isPassEnabled() {
        return ensurePrefs().getBoolean(SettingsActivity.PREF_PASS_ENABLED, false);
    }

    private boolean isPassAvailable() {
        switch (NetworkProfileManager.instance().getCurrentProfile().getConnectionType()) {
            case WIFI:
                return true;
            case MOBILE:
            case NO_CONNECTION:
            default:
                return false;
        }
    }

    public boolean usePass() {
        return isPassAvailable() && isPassEnabled();
    }

    private void disablePass() {
        ensurePrefs().edit().putBoolean(SettingsActivity.PREF_PASS_ENABLED, false).commit();
        String passToken = ensurePrefs().getString(SettingsActivity.PREF_PASS_TOKEN, "");
        String passPIN = ensurePrefs().getString(SettingsActivity.PREF_PASS_PIN, "");
        LogoutPassTask logoutPassTask = new LogoutPassTask(this, passToken, passPIN);
        LogoutPassDialogFragment passDialogFragment = new LogoutPassDialogFragment(logoutPassTask);
        passDialogFragment.show(getFragmentManager(), LogoutPassDialogFragment.TAG);
        if (!logoutPassTask.isCancelled()) {
            logoutPassTask.execute(passDialogFragment);
        }
    }

    private void updatePassRecaptchaViews(boolean passEnabled) {
        switch (NetworkProfileManager.instance().getCurrentProfile().getConnectionType()) {
            case WIFI:
                if (passEnabled) {
                    passStatusText.setText(R.string.post_reply_pass_enabled_text);
                    setPassEnabled();
                    setRecaptchaDisabled();
                }
                else {
                    passStatusText.setText(R.string.post_reply_pass_enable_text);
                    setPassDisabled();
                    setRecaptchaEnabled();
                }
                break;
            case MOBILE:
                passStatusText.setText(R.string.post_reply_pass_mobile_text);
                setPassUnavailable();
                setRecaptchaEnabled();
                break;
            case NO_CONNECTION:
            default:
                passStatusText.setText(R.string.post_reply_pass_no_connection_text);
                setPassUnavailable();
                setRecaptchaDisabled();
                break;
        }
    }

    private void setPassUnavailable() {
        passEnableButton.setVisibility(View.GONE);
        passDisableButton.setVisibility(View.GONE);
    }

    private void setPassEnabled() {
        passEnableButton.setVisibility(View.GONE);
        passDisableButton.setVisibility(View.VISIBLE);
    }

    private void setPassDisabled() {
        passEnableButton.setVisibility(View.VISIBLE);
        passDisableButton.setVisibility(View.GONE);
    }

    private void setRecaptchaEnabled() {
        recaptchaFrame.setVisibility(View.VISIBLE);
        recaptchaText.setVisibility(View.VISIBLE);
    }

    private void setRecaptchaDisabled() {
        recaptchaFrame.setVisibility(View.GONE);
        recaptchaText.setVisibility(View.GONE);
    }

    public SharedPreferences ensurePrefs() {
        if (prefs == null)
            prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return prefs;
    }

    public Handler getHandler() {
        return ensureHandler();
    }

    protected synchronized Handler ensureHandler() {
        if (handler == null)
            handler = new PostReplyHandler();
        return handler;
    }

    private boolean restarted = false;

    public void onRestart() {
        super.onRestart();
        if (DEBUG) Log.i(TAG, "onStart");
        restoreOnRestart();
        restarted = true;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (DEBUG) Log.i(TAG, "onStart");
        if (restarted)
            restarted = false;
        else
            restoreInstanceState();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG) Log.i(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (DEBUG) Log.i(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (DEBUG) Log.i(TAG, "onStop");
        saveInstanceState();
        handler = null;
    }

    protected void restoreInstanceState() {
        if (getIntent().hasExtra(ChanHelper.BOARD_CODE))
            loadFromIntent(getIntent());
        else
            loadFromPrefs();
        refreshActivity();
    }

    private void restoreOnRestart() {
        loadFromPrefs();
    }

    public void refreshActivity() {
        boolean passEnabled = isPassEnabled();
        if (!passEnabled || !isPassAvailable())
            reloadCaptcha();
        updatePassRecaptchaViews(passEnabled);
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
        updateBump();
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
        loadFromIntentWithBoardCode(
                intent,
                intent.getStringExtra(ChanHelper.BOARD_CODE),
                intent.getStringExtra(ChanHelper.POST_REPLY_IMAGE_URL)
        );
    }

    protected void loadFromIntentWithBoardCode(Intent intent, String boardCode, String imageUrl) {
        this.boardCode = boardCode;
        threadNo = intent.getLongExtra(ChanHelper.THREAD_NO, 0);
        postNo = intent.getLongExtra(ChanHelper.POST_NO, 0);
        tim = intent.getLongExtra(ChanHelper.TIM, 0);
        imagePath = null;
        contentType = null;
        orientation = null;

        String text = intent.getStringExtra(ChanHelper.TEXT);
        Log.i(TAG, "Intent has text? " + intent.hasExtra(ChanHelper.TEXT));
        Log.i(TAG, "Intent has text=" + text);
        //String quoteText = ChanPost.quoteText(intent.getStringExtra(ChanHelper.QUOTE_TEXT));
        setMessageText(text, "");
        adjustFieldVisibility();
        loadUserPrefs();

        String subject = intent.getStringExtra(ChanHelper.SUBJECT);
        if (subject != null && !subject.isEmpty())
            subjectText.setText(subject);
        boolean spoilerChecked = intent.getBooleanExtra(ChanHelper.SPOILER, false);
            spoilerCheckbox.setChecked(spoilerChecked);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            setImageUri(imageUrl);
            setImagePreview();
        }
        else {
            imagePreview.setVisibility(View.GONE);
        }

        setActionBarTitle();

        if (DEBUG) Log.i(TAG, "loaded from intent " + boardCode + "/" + threadNo + ":" + postNo + " tim=" + tim + " imageUrl=" + imageUrl + " text=" + text);
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
        setMessageText(text, "");
        adjustFieldVisibility();
        loadUserPrefs();

        String subject = prefs.getString(ChanHelper.SUBJECT, "");
        if (!subject.isEmpty())
            subjectText.setText(subject);
        boolean spoilerChecked = prefs.getBoolean(ChanHelper.SPOILER, false);
            spoilerCheckbox.setChecked(spoilerChecked);

        loadImageFromPrefs();

        setActionBarTitle();
        if (DEBUG) Log.i(TAG, "loaded from prefs " + boardCode + "/" + threadNo + ":" + postNo + " tim=" + tim + " text=" + text);
    }


    protected void loadUserPrefs() {
        ensurePrefs();
        String name = prefs.getString(SettingsActivity.PREF_USER_NAME, "");
        if (!name.isEmpty())
            nameText.setText(name);
        String email = prefs.getString(SettingsActivity.PREF_USER_EMAIL, "");
        if (!email.isEmpty())
            emailText.setText(email);
        String password = prefs.getString(SettingsActivity.PREF_USER_PASSWORD, "");
        if (!password.isEmpty()) {
            passwordText.setText(password);
        }
        else {
            passwordText.setText(generatePassword());
        }
    }

    protected void adjustFieldVisibility() {
        if (threadNo == 0) // new thread
            sageButton.setVisibility(View.GONE);

        if (ChanBoard.hasName(boardCode))
            nameText.setVisibility(View.VISIBLE);
        else
            nameText.setVisibility(View.GONE);

        if (ChanBoard.hasSubject(boardCode))
            subjectText.setVisibility(View.VISIBLE);
        else
            subjectText.setVisibility(View.GONE);

        if (ChanBoard.hasSpoiler(boardCode))
            spoilerCheckbox.setVisibility(View.VISIBLE);
        else
            spoilerCheckbox.setVisibility(View.GONE);

        // subject hints
        if (threadNo == 0 && ChanBoard.requiresThreadSubject(boardCode)) {
            messageText.setHint(R.string.post_reply_text_hint_required);
            subjectText.setHint(R.string.post_reply_subject_hint_required);
        }
        else {
            messageText.setHint(R.string.post_reply_text_hint);
            subjectText.setHint(R.string.post_reply_subject_hint);
        }
    }

    public boolean hasSpoiler() {
        return spoilerCheckbox.isChecked();
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
    }

    protected void saveInstanceState() {
        SharedPreferences.Editor ed = ensurePrefs().edit();
        ed.putString(ChanHelper.BOARD_CODE, boardCode);
        ed.putLong(ChanHelper.THREAD_NO, threadNo);
        ed.putLong(ChanHelper.POST_NO, postNo);
        ed.putString(ChanHelper.TEXT, messageText.getText().toString());
        ed.putString(ChanHelper.SUBJECT, subjectText.getText().toString());
        ed.putBoolean(ChanHelper.SPOILER, spoilerCheckbox.isChecked());
        ed.putLong(ChanHelper.TIM, tim);
        ed.putString(ChanHelper.POST_REPLY_IMAGE_URL, imageUri == null ? null : imageUri.toString());
        ed.putString(ChanHelper.IMAGE_PATH, imagePath);
        ed.putString(ChanHelper.CONTENT_TYPE, contentType);
        ed.putString(ChanHelper.ORIENTATION, orientation);
        saveUserPrefs(ed);
        ed.commit();
        if (DEBUG) Log.i(TAG, "Saved to prefs " + boardCode + "/" + threadNo + ":" + postNo + " tim=" + tim
                + " imageUrl=" + (imageUri == null ? "" : imageUri.toString()));
        DispatcherHelper.saveActivityToPrefs(this);
    }

    protected void saveUserPrefs(SharedPreferences.Editor ed) {
        String email = emailText.getText().toString();
        if (!email.equalsIgnoreCase("sage")) // no sagebombing
            ed.putString(SettingsActivity.PREF_USER_EMAIL, email);
        ed.putString(SettingsActivity.PREF_USER_NAME, nameText.getText().toString());
        ed.putString(SettingsActivity.PREF_USER_PASSWORD, passwordText.getText().toString());
    }


    public void reloadCaptcha() {
        recaptchaText.setText("");
        recaptchaText.setHint(R.string.post_reply_recaptcha_hint);
        loadCaptchaTask = new LoadCaptchaTask(ctx, recaptchaButton, recaptchaLoading);
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
                    imageUri = Uri.parse(intent.getStringExtra(ChanHelper.CAMERA_IMAGE_URL));
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
        deleteButton.setVisibility(View.VISIBLE);
        imagePreview.setVisibility(View.VISIBLE);
        imagePreview.setPadding(0, 0, 0, 16);
        angle = 0;
    }

    protected void setImagePreview() {
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

    private void deleteImage() {
        imagePreview.setVisibility(View.GONE);
        deleteButton.setVisibility(View.GONE);
        imageUri = null;
        imagePath = null;
        contentType = null;
        orientation = null;
    }

    private void updateBump() {
        String s = messageText.getText().toString().trim();
        if (DEBUG) Log.i(TAG, "updateBump for s=" + s);
        if (threadNo == 0 || !ChanBoard.allowsBump(boardCode) || !s.isEmpty())
            bumpButton.setVisibility(View.GONE);
        else
            bumpButton.setVisibility(View.VISIBLE);
    }

    private void bump() {
        String s = messageText.getText().toString(); // defensive coding
        if (s == null || s.isEmpty())
            messageText.setText("bump");
    }

    private void sage() {
        emailText.setText("sage"); // 4chan way to post without bumping
    }

    private void startCamera() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivityForResult(intent, IMAGE_CAPTURE);
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
            closeKeyboard();
            PostReplyTask postReplyTask = new PostReplyTask(this);
            PostingReplyDialogFragment dialogFragment = new PostingReplyDialogFragment(postReplyTask);
            dialogFragment.show(getSupportFragmentManager(), PostingReplyDialogFragment.TAG);
            if (!postReplyTask.isCancelled()) {
                postReplyTask.execute(dialogFragment);
            }
        }
    }

    private void closeKeyboard() {
        IBinder windowToken = getCurrentFocus() != null ? getCurrentFocus().getWindowToken() : null;
        if (windowToken != null) { // close the keyboard
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(windowToken, 0);
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.post_reply_send_menu);
        if (item != null) {
            if (NetworkProfileManager.instance().getCurrentProfile().getConnectionType()
                    == NetworkProfile.Type.NO_CONNECTION)
            {
                item.setEnabled(false);
                Toast.makeText(this, R.string.post_reply_pass_no_connection_text, Toast.LENGTH_SHORT).show();
            }
            else
            {
                item.setEnabled(true);
            }
        }
        return true;
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

    public void setMessage(String text) {
        messageText.setText(text);
        updateBump();
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

    public String getPassword() {
        String s = passwordText.getText().toString();
        return (s != null) ? s : "";
    }

    private String generatePassword() {
        return eightDigits.format(randomGenerator.nextInt(PASSWORD_MAX));
    }

    public String getRecaptchaChallenge() {
        return loadCaptchaTask.getRecaptchaChallenge();
    }

    public String getRecaptchaResponse() {
        return recaptchaText.getText().toString();
    }

    private String validatePost() {
        String subject = subjectText.getText().toString();
        String message = messageText.getText().toString();
        String image = imageUri != null ? imageUri.getPath() : null;
        boolean hasSubject = subject != null && !subject.trim().isEmpty();
        boolean hasMessage = message != null && !message.trim().isEmpty();
        boolean hasImage = image != null && !image.trim().isEmpty();

        if (threadNo == 0) {
            if (ChanBoard.requiresThreadImage(boardCode) && !hasImage)
                return res.getString(R.string.post_reply_add_image);
            if (ChanBoard.requiresThreadSubject(boardCode) && !hasSubject)
                return res.getString(R.string.post_reply_board_requires_subject);
        }
        if (!hasImage && !hasMessage)
            return res.getString(R.string.post_reply_add_text_or_image);

        if (!isPassEnabled() || !isPassAvailable()) {
            String recaptchaChallenge = loadCaptchaTask.getRecaptchaChallenge();
            if (recaptchaChallenge == null || recaptchaChallenge.trim().isEmpty()) {
                return res.getString(R.string.post_reply_captcha_error);
            }
            String recaptcha = recaptchaText.getText().toString();
            if (recaptcha == null || recaptcha.trim().isEmpty()) {
                return res.getString(R.string.post_reply_enter_captcha);
            }
        }

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
        inflater.inflate(R.menu.post_reply_menu, menu);
        return true;
    }

    private void setActionBarTitle() {
        if (getActionBar() != null) {
            if (threadNo == 0) {
                getActionBar().setTitle("/" + boardCode + "/ " + getString(R.string.post_reply_thread_title));
            }
            else {
                getActionBar().setTitle("/" + boardCode + "/" + threadNo + " " + getString(R.string.post_reply_title));
            }
            getActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    @Override
	public ChanActivityId getChanActivityId() {
		return new ChanActivityId(LastActivity.POST_REPLY_ACTIVITY);
	}

	@Override
	public Handler getChanHandler() {
		return null;
	}

    public class PostReplyHandler extends Handler {

        public PostReplyHandler() {}

        @Override
        public void handleMessage(Message msg) {
            try {
                super.handleMessage(msg);
                switch (msg.what) {
                    case POST_FINISHED:
                    default:
                        finish();
                }
            }
            catch (Exception e) {
                Log.e(TAG, "Couldn't handle message " + msg, e);
            }
        }
    }

}
