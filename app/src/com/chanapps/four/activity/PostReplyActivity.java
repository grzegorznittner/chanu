package com.chanapps.four.activity;

import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.os.*;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.*;
import com.chanapps.four.component.ActivityDispatcher;
import com.chanapps.four.component.CameraComponent;
import com.chanapps.four.component.ChanGridSizer;
import com.chanapps.four.data.*;
import com.chanapps.four.data.LastActivity;
import com.chanapps.four.fragment.*;
import com.chanapps.four.loader.ChanImageLoader;
import com.chanapps.four.multipartmime.*;
import com.chanapps.four.service.FetchChanDataService;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.service.profile.NetworkProfile;
import com.chanapps.four.task.LoadCaptchaTask;
import com.chanapps.four.task.LogoutPassTask;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import java.io.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class PostReplyActivity extends FragmentActivity implements ChanIdentifiedActivity {

    public static final String TAG = PostReplyActivity.class.getSimpleName();

    public static final String BOARD_CODE = "postReplyBoardCode";
    public static final String THREAD_NO = "postReplyThreadNo";
    public static final String POST_NO = "postReplyPostNo";
    public static final String TEXT = "text";
    public static final String POST_REPLY_IMAGE_URL = "postReplyImageUrl";
    public static final String SUBJECT = "postReplySubject";
    public static final String POST_REPLY_TEXT = "postReplyText";
    public static final String SPOILER = "postReplySpoiler";
    public static final String IMAGE_PATH = "postReplyImagePath";
    public static final String CONTENT_TYPE = "postReplyContentType";
    public static final String ORIENTATION = "postReplyOrientation";

    protected static final String[] PREFS = {
            BOARD_CODE,
            THREAD_NO,
            POST_NO,
            SUBJECT,
            POST_REPLY_TEXT,
            SPOILER,
            POST_REPLY_IMAGE_URL,
            IMAGE_PATH,
            CONTENT_TYPE,
            ORIENTATION
    };

    public static final int POST_FINISHED = 0x01;

    private static final boolean DEBUG = true;

    public static final int PASSWORD_MAX = 100000000;
    private static final Random randomGenerator = new Random();
    private static final DecimalFormat eightDigits = new DecimalFormat("00000000");

    protected boolean exitingOnSuccess = false;

    private static final int IMAGE_CAPTURE = CameraComponent.CAMERA_RESULT;
    private static final int IMAGE_GALLERY = 0x11;
    //private static final String ANDROID_IMAGE_CAPTURE = "android.media.action.IMAGE_CAPTURE";

    private LinearLayout wrapperLayout;

    private ImageButton cameraButton;
    private ImageButton pictureButton;
    private ImageButton webButton;
    private ImageButton deleteButton;
    private ImageButton bumpButton;
    private ImageButton sageButton;
    private ImageButton passEnableButton;
    private ImageButton passDisableButton;
    private Handler handler;

    private FrameLayout recaptchaFrame;
    private ImageButton recaptchaButton;
    private ImageView recaptchaLoading;
    private EditText recaptchaText;
    private LoadCaptchaTask loadCaptchaTask;

    private EditText messageText;
    private TextView passStatusText;
    private EditText nameText;
    private EditText emailText;
    private EditText subjectText;
    private EditText passwordText;
    private CheckBox spoilerCheckbox;
    private RelativeLayout previewFrame;
    private ImageView imagePreview;
    private ProgressBar previewProgress;
    private TextView.OnEditorActionListener fastSend;
    private CameraComponent camera;

    protected Uri imageUri;
    protected String boardCode = null;
    protected long threadNo = 0;
    protected long postNo = 0;
    protected String imagePath = null;
    protected String contentType = null;
    protected String orientation = null;

    public static void startActivity(Context context, String boardCode, long threadNo, long postNo, String replyText) {
        Intent intent = createIntent(context, boardCode, threadNo, postNo, replyText);
        context.startActivity(intent);
    }

    public static Intent createIntent(Context context, String boardCode, long threadNo, long postNo, String replyText) {
        Intent replyIntent = new Intent(context, PostReplyActivity.class);
        replyIntent.putExtra(ChanBoard.BOARD_CODE, boardCode);
        replyIntent.putExtra(ChanThread.THREAD_NO, threadNo);
        replyIntent.putExtra(ChanPost.POST_NO, postNo);
        replyIntent.putExtra(TEXT, replyText);
        return replyIntent;
    }

    @Override
    protected void onCreate(Bundle bundle){
        super.onCreate(bundle);
        if (DEBUG) Log.i(TAG, "onCreate bundle=" + bundle);
        exitingOnSuccess = false;
        setContentView(R.layout.post_reply_layout);
        createViews();
        if (bundle != null)
            onRestoreInstanceState(bundle);
        else
            setFromIntent(getIntent());
        camera = new CameraComponent(getApplicationContext(), imageUri);
        if (boardCode == null || boardCode.isEmpty())
            boardCode = ChanBoard.DEFAULT_BOARD_CODE;
        setViews();
    }

    protected void createViews() {
        wrapperLayout = (LinearLayout)findViewById(R.id.post_reply_wrapper);
        previewFrame = (RelativeLayout)findViewById(R.id.post_reply_preview_frame);
        imagePreview = (ImageView)findViewById(R.id.post_reply_preview_image);
        previewProgress = (ProgressBar)findViewById(R.id.post_reply_preview_progress_bar);
        cameraButton = (ImageButton)findViewById(R.id.post_reply_camera_button);
        pictureButton = (ImageButton)findViewById(R.id.post_reply_picture_button);
        webButton = (ImageButton)findViewById(R.id.post_reply_web_button);
        deleteButton = (ImageButton)findViewById(R.id.post_reply_delete_button);
        bumpButton = (ImageButton)findViewById(R.id.post_reply_bump_button);
        sageButton = (ImageButton)findViewById(R.id.post_reply_sage_button);
        passEnableButton = (ImageButton)findViewById(R.id.post_reply_pass_enable_button);
        passDisableButton = (ImageButton)findViewById(R.id.post_reply_pass_disable_button);
        messageText = (EditText)findViewById(R.id.post_reply_text);
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
        webButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                (new WebImageDialogFragment(boardCode, threadNo))
                        .show(getSupportFragmentManager(), WebImageDialogFragment.TAG);
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
    /*
    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        setFromIntent(intent);
    }
    */
    protected void setFromIntent(Intent intent) {
        boardCode = intent.getStringExtra(ChanBoard.BOARD_CODE);
        threadNo = intent.getLongExtra(ChanThread.THREAD_NO, 0);
        postNo = intent.getLongExtra(ChanPost.POST_NO, 0);
        if (intent.hasExtra(SUBJECT))
            subjectText.setText(intent.getStringExtra(SUBJECT));
        if (intent.hasExtra(TEXT))
            setMessage(intent.getStringExtra(TEXT));
        if (intent.hasExtra(SPOILER))
            spoilerCheckbox.setChecked(intent.getBooleanExtra(SPOILER, false));
        // these are just reset
        imageUri = intent.hasExtra(POST_REPLY_IMAGE_URL)
                ? Uri.parse(intent.getStringExtra(POST_REPLY_IMAGE_URL))
                : null;
        imagePath = null;
        contentType = null;
        orientation = null;
        if (DEBUG) Log.i(TAG, "setIntent() intent has /" + boardCode + "/" + threadNo + ":" + postNo + " imageUri=" + imageUri);
        Bundle bundle = loadBundleFromPrefs();
        if (bundle != null
                && boardCode != null
                && boardCode.equals(bundle.getString(ChanBoard.BOARD_CODE))
                && threadNo == bundle.getLong(ChanThread.THREAD_NO)
                && postNo == bundle.getLong(ChanPost.POST_NO)
                ) {
            if (DEBUG) Log.i(TAG, "setIntent() found saved bundle for same thread, restoring");
            onRestoreInstanceState(bundle);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        if (bundle == null)
            return;
        boardCode = bundle.getString(ChanBoard.BOARD_CODE);
        threadNo = bundle.getLong(ChanThread.THREAD_NO, 0);
        postNo = bundle.getLong(ChanPost.POST_NO, 0);
        if (bundle.containsKey(SUBJECT))
            subjectText.setText(bundle.getString(SUBJECT));
        if (bundle.containsKey(TEXT))
            setMessage(bundle.getString(TEXT));
        if (bundle.containsKey(SPOILER))
            spoilerCheckbox.setChecked(bundle.getBoolean(SPOILER, false));
        if (bundle.containsKey(POST_REPLY_IMAGE_URL))
            imageUri = bundle.getString(POST_REPLY_IMAGE_URL) != null
                    ? Uri.parse(bundle.getString(POST_REPLY_IMAGE_URL))
                    : null;
        imagePath = bundle.getString(IMAGE_PATH);
        contentType = bundle.getString(CONTENT_TYPE);
        orientation = bundle.getString(ORIENTATION);
        if (DEBUG) Log.i(TAG, "onRestoreInstanceState() bundle=" + bundle);
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        saveStateToBundle(bundle);
        if (DEBUG) Log.i(TAG, "onSaveInstanceState bundle=" + bundle);
        saveUserFieldsToPrefs();
        saveBundleToPrefs(bundle);
        ActivityDispatcher.store(this);
    }

    protected Bundle saveStateToBundle(Bundle bundle) {
        bundle.putString(ChanBoard.BOARD_CODE, boardCode);
        bundle.putLong(ChanThread.THREAD_NO, threadNo);
        bundle.putLong(ChanPost.POST_NO, postNo);
        bundle.putString(SUBJECT, subjectText.getText().toString());
        bundle.putString(TEXT, messageText.getText().toString());
        bundle.putBoolean(SPOILER, spoilerCheckbox.isChecked());
        if (imageUri != null)
            bundle.putString(POST_REPLY_IMAGE_URL, imageUri.toString());
        bundle.putString(IMAGE_PATH, imagePath);
        bundle.putString(CONTENT_TYPE, contentType);
        bundle.putString(ORIENTATION, orientation);
        return bundle;
    }

    protected void saveBundleToPrefs(Bundle bundle) {
        SharedPreferences.Editor editor = ensurePrefs().edit();
        editor.putString(BOARD_CODE, boardCode);
        editor.putLong(THREAD_NO, threadNo);
        editor.putLong(POST_NO, postNo);
        editor.putString(SUBJECT, subjectText.getText().toString());
        editor.putString(POST_REPLY_TEXT, messageText.getText().toString());
        editor.putBoolean(SPOILER, spoilerCheckbox.isChecked());
        editor.putString(POST_REPLY_IMAGE_URL, imageUri == null ? null : imageUri.toString());
        editor.putString(IMAGE_PATH, imagePath);
        editor.putString(CONTENT_TYPE, contentType);
        editor.putString(ORIENTATION, orientation);
        editor.commit();
        if (DEBUG) Log.i(TAG, "saveBundleToPrefs bundle=" + bundle);
    }

    protected Bundle loadBundleFromPrefs() {
        SharedPreferences prefs = ensurePrefs();
        Bundle bundle = new Bundle();
        bundle.putString(ChanBoard.BOARD_CODE, prefs.getString(BOARD_CODE, null));
        bundle.putLong(ChanThread.THREAD_NO, prefs.getLong(THREAD_NO, 0));
        bundle.putLong(ChanPost.POST_NO, prefs.getLong(POST_NO, 0));
        bundle.putString(SUBJECT, prefs.getString(SUBJECT, null));
        bundle.putString(TEXT, prefs.getString(POST_REPLY_TEXT, null));
        bundle.putBoolean(SPOILER, prefs.getBoolean(SPOILER, false));
        bundle.putString(POST_REPLY_IMAGE_URL, prefs.getString(POST_REPLY_IMAGE_URL, null));
        bundle.putString(IMAGE_PATH, prefs.getString(IMAGE_PATH, null));
        bundle.putString(CONTENT_TYPE, prefs.getString(CONTENT_TYPE, null));
        bundle.putString(ORIENTATION, prefs.getString(ORIENTATION, null));
        return bundle;
    }

    protected void clearPrefs() {
        SharedPreferences.Editor editor = ensurePrefs().edit();
        for (String pref : PREFS) {
            editor.remove(pref);
        }
        editor.commit();
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
                    imageUri = camera.startCamera(PostReplyActivity.this);
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
        return PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    }

    public Handler getHandler() {
        return ensureHandler();
    }

    protected synchronized Handler ensureHandler() {
        if (handler == null && ActivityDispatcher.onUIThread())
            handler = new PostReplyHandler();
        return handler;
    }

    public void onRestart() {
        super.onRestart();
        if (DEBUG) Log.i(TAG, "onStart");
    }

    @Override
    public void onStart() {
        super.onStart();
        if (DEBUG) Log.i(TAG, "onStart");
        setImagePreview();
        //if (threadNo <= 0)
        //    NetworkProfileManager.instance().getUserStatistics().featureUsed(UserStatistics.ChanFeature.ADD_THREAD);
        //else
        //    NetworkProfileManager.instance().getUserStatistics().featureUsed(UserStatistics.ChanFeature.POST);
        if ((!isPassEnabled() || !isPassAvailable()) && recaptchaButton.getDrawable() == null)
            refresh();
    }

    protected void setViews() {
        if (messageText.getText().length() == 0 && postNo != 0)
            setMessage(">>" + postNo + "\n");
        updateBump();
        adjustFieldVisibility();
        defaultEmptyUserFieldsFromPrefs();
        setImagePreview();
        setActionBarTitle();
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
        handler = null;
        if (!exitingOnSuccess)
            saveBundleToPrefs(saveStateToBundle(new Bundle()));
    }

    protected void saveUserFieldsToPrefs() {
        SharedPreferences.Editor ed = ensurePrefs().edit();
        String name = nameText.getText().toString();
        String email = emailText.getText().toString();
        String password = passwordText.getText().toString();
        if (!name.isEmpty())
            ed.putString(SettingsActivity.PREF_USER_NAME, name);
        if (!email.isEmpty() && !email.equalsIgnoreCase("sage")) // no sagebombing
            ed.putString(SettingsActivity.PREF_USER_EMAIL, email);
        if (!password.isEmpty())
            ed.putString(SettingsActivity.PREF_USER_PASSWORD, password);
    }

    public void refresh() {
        boolean passEnabled = isPassEnabled();
        if (!passEnabled || !isPassAvailable())
            reloadCaptcha();
        updatePassRecaptchaViews(passEnabled);
    }

    public void setImageUri(String imageUrl) {
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

    public void setImageUri(Uri uri) {
        if (uri != null) {
            imageUri = uri;
            setImagePreview();
            if (DEBUG) Log.i(TAG, "loaded uri = " + uri);
        }
        else {
            imageUri = null;
            imagePreview.setVisibility(View.GONE);
            if (DEBUG) Log.i(TAG, "image uri passed was null");
        }
    }

    protected void defaultEmptyUserFieldsFromPrefs() {
        ensurePrefs();
        CharSequence existingName = nameText.getText();
        CharSequence existingEmail = emailText.getText();
        CharSequence existingPassword = passwordText.getText();
        if (existingName == null || existingName.length() == 0) {
            String name = ensurePrefs().getString(SettingsActivity.PREF_USER_NAME, "");
            if (!name.isEmpty())
                nameText.setText(name);
        }
        if (existingEmail == null || existingEmail.length() == 0) {
            String email = ensurePrefs().getString(SettingsActivity.PREF_USER_EMAIL, "");
            if (!email.isEmpty())
                emailText.setText(email);
        }
        if (existingPassword == null || existingPassword.length() == 0) {
            String password = ensurePrefs().getString(SettingsActivity.PREF_USER_PASSWORD, "");
            if (password.isEmpty())
                password = generatePassword();
            passwordText.setText(password);
        }
    }

    protected void adjustFieldVisibility() {
        if (threadNo == 0) // new thread
            sageButton.setVisibility(View.GONE);

        if (ChanBoard.hasName(boardCode))
            nameText.setVisibility(View.VISIBLE);
        else
            nameText.setVisibility(View.GONE);

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

        if (ChanBoard.hasSubject(boardCode)) {
            subjectText.setVisibility(View.VISIBLE);
        }
        else {
            subjectText.setVisibility(View.GONE);
        }

    }

    public boolean hasSpoiler() {
        return spoilerCheckbox.isChecked();
    }

    public void reloadCaptcha() {
        recaptchaText.setText("");
        recaptchaText.setHint(R.string.post_reply_recaptcha_hint);
        loadCaptchaTask = new LoadCaptchaTask(getApplicationContext(), recaptchaButton, recaptchaLoading);
        loadCaptchaTask.execute(getString(R.string.post_reply_recaptcha_url_root));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        int fragmentIndex = (requestCode >>> 16);
        if (fragmentIndex != 0) { // fragment bug
            super.onActivityResult(requestCode, resultCode, intent);
            return;
        }
        if (DEBUG) Log.i(TAG, "onActivityResult for requestCode=" + requestCode
                + " intent=" + intent
                + " intent.getData()=" + (intent == null ? "null" : intent.getData())
        );
        if (resultCode != RESULT_OK) {
            Log.e(TAG, "onActivityResult error resultCode=" + resultCode + " intent=" + intent);
            int errId = requestCode == IMAGE_CAPTURE
                    ? R.string.post_reply_no_load_camera_image
                    : R.string.post_reply_no_load_gallery_image;
            Toast.makeText(this, errId, Toast.LENGTH_SHORT).show();
            return;
        }
        switch (requestCode) {
            case IMAGE_CAPTURE:
                //imageUri = intent.hasExtra()
                //imageUri = Uri.parse(intent.getStringExtra(CameraActivity.CAMERA_IMAGE_URL));
                if (DEBUG) Log.i(TAG, "Got camera result for activity url=" + imageUri);
                if (imageUri == null) {
                    Log.e(TAG, "null image uri for camera image");
                    Toast.makeText(this, R.string.post_reply_no_load_camera_image, Toast.LENGTH_SHORT).show();
                    return;
                }
                camera.handleResult();
                break;
            case IMAGE_GALLERY:
                if (DEBUG) Log.i(TAG, "Got gallery result for activity imageUri=" + imageUri);
                if (intent == null || intent.getData() == null) {
                    Log.e(TAG, "null image uri for gallery image");
                    Toast.makeText(this, R.string.post_reply_no_load_gallery_image, Toast.LENGTH_SHORT).show();
                }
                imageUri = intent.getData();
                break;
            default:
                Log.e(TAG, "invalid request code for image");
                Toast.makeText(this, R.string.post_reply_no_load_image, Toast.LENGTH_SHORT).show();
        }
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
            String[] filePathColumn = {
                    MediaStore.Images.ImageColumns.DATA,
                    MediaStore.Images.ImageColumns.MIME_TYPE,
                    MediaStore.Images.ImageColumns.ORIENTATION };
            Cursor cursor = getContentResolver().query(imageUri, filePathColumn, null, null, null); // query so image shows up
            if (cursor != null) {
                cursor.moveToFirst();
                imagePath = cursor.getString(cursor.getColumnIndexOrThrow(filePathColumn[0]));
                contentType = cursor.getString(cursor.getColumnIndexOrThrow(filePathColumn[1]));
                orientation = cursor.getString(cursor.getColumnIndexOrThrow(filePathColumn[2]));
                cursor.close();
            }
            if (DEBUG) {
                if (cursor == null) {
                    Log.i(TAG, "null cursor on media resolver for imageUri=" + imageUri);
                }
                else {
                    Log.i(TAG, "media resolver for imageUri=" + imageUri
                            + " has path=" + imagePath + " type=" + contentType + " orientation=" + orientation);
                }
            }
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
    }

    protected void setImagePreview() {
        try {
            if (imageUri == null) {
                imagePreview.setVisibility(View.GONE);
                if (DEBUG) Log.i(TAG, "No image uri found, not setting image");
                return;
            }
            imagePreview.setVisibility(View.VISIBLE);
            DisplayImageOptions options = (new DisplayImageOptions.Builder())
                    .cacheOnDisc()
                    .showStubImage(R.drawable.stub_image_background)
                    .resetViewBeforeLoading()
                    .fullSizeImageLocation(imageUri.toString())
                    .imageSize(new ImageSize(300, 300))
                    .build();
            ChanImageLoader.getInstance(this).displayImage(imageUri.toString(), imagePreview, options, previewListener);
            /*
            Bitmap b = getImagePreviewBitmap();
            if (b != null) {
                resetImagePreview();
                imagePreview.setImageBitmap(b);
                if (DEBUG) Log.i(TAG, "setImagePreview with bitmap imageUri=" + imageUri.toString() + " dimensions: " + b.getWidth() + "x" + b.getHeight());
            }
            else {
                //Toast.makeText(getApplicationContext(), R.string.post_reply_no_image, Toast.LENGTH_SHORT).show();
                imagePreview.setVisibility(View.GONE);
                Log.e(TAG, "setImagePreview null bitmap with imageUri=" + imageUri.toString());
            }
            */
        }
        catch (Exception e) {
            Toast.makeText(getApplicationContext(), R.string.post_reply_no_image, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "setImagePreview exception while loading bitmap", e);
        }
    }

    protected ImageLoadingListener previewListener = new ImageLoadingListener() {
        @Override
        public void onLoadingStarted(String imageUri, View view) {
            previewProgress.setVisibility(View.VISIBLE);
            previewFrame.setVisibility(View.VISIBLE);
        }
        @Override
        public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
            previewProgress.setVisibility(View.GONE);
            //Toast.makeText(view.getContext(), R.string.web_image_download_failed, Toast.LENGTH_SHORT).show();
        }
        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            previewProgress.setVisibility(View.GONE);
        }
        @Override
        public void onLoadingCancelled(String imageUri, View view) {
            previewProgress.setVisibility(View.GONE);
            //Toast.makeText(view.getContext(), R.string.web_image_download_failed, Toast.LENGTH_SHORT).show();
        }
    };

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
            setMessage("bump");
    }

    private void sage() {
        emailText.setText("sage"); // 4chan way to post without bumping
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
            Toast.makeText(getApplicationContext(), validMsg, Toast.LENGTH_SHORT).show();
        }
        else {
            closeKeyboard();
            PostReplyTask postReplyTask = new PostReplyTask();
            PostingReplyDialogFragment dialogFragment = new PostingReplyDialogFragment(postReplyTask, threadNo);
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
            case android.R.id.home:
                navigateUp();
                return true;
            case R.id.post_reply_exit_menu:
                finish();
                return true;
            case R.id.post_reply_send_menu:
                validateAndSendReply();
                return true;
            case R.id.settings_menu:
                if (DEBUG) Log.i(TAG, "Starting settings activity");
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
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
                return getString(R.string.post_reply_add_image);
            if (ChanBoard.requiresThreadSubject(boardCode) && !hasSubject)
                return getString(R.string.post_reply_board_requires_subject);
        }
        if (!hasImage && !hasMessage)
            return getString(R.string.post_reply_add_text_or_image);

        if (!isPassEnabled() || !isPassAvailable()) {
            String recaptchaChallenge = loadCaptchaTask.getRecaptchaChallenge();
            if (recaptchaChallenge == null || recaptchaChallenge.trim().isEmpty()) {
                return getString(R.string.post_reply_captcha_error);
            }
            String recaptcha = recaptchaText.getText().toString();
            if (recaptcha == null || recaptcha.trim().isEmpty()) {
                return getString(R.string.post_reply_enter_captcha);
            }
        }

        return null;
    }

    public void navigateUp() {
        Intent intent = ThreadActivity.createIntent(this, boardCode, threadNo, postNo, "");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.post_reply_menu, menu);
        return true;
    }

    protected void setActionBarTitle() {
        String replyTitle = threadNo > 0
                ? getString(R.string.post_reply_title)
                : getString(R.string.post_reply_thread_title);
        String title = "/" + boardCode + "/ " + replyTitle;
        getActionBar().setTitle(title);
        getActionBar().setDisplayShowHomeEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
	public ChanActivityId getChanActivityId() {
		return new ChanActivityId(LastActivity.POST_REPLY_ACTIVITY, boardCode, threadNo, postNo,
                messageText != null ? messageText.getText().toString() : "");
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
                        //FetchChanDataService.scheduleThreadFetchAfterPost(PostReplyActivity.this, boardCode, threadNo);
                        //finish();
                        navigateUp();
                }
            }
            catch (Exception e) {
                Log.e(TAG, "Couldn't handle message " + msg, e);
            }
        }
    }

    @Override
    public void closeSearch() {}

    @Override
    public void setProgress(boolean on) {}

    public class PostReplyTask extends AsyncTask<PostingReplyDialogFragment, Void, Integer> {

        public final String TAG = PostReplyTask.class.getSimpleName();
        public static final String POST_URL_ROOT = "https://sys.4chan.org/";
        public static final String MAX_FILE_SIZE = "3145728";
        public static final boolean DEBUG = false;

        //private PostReplyActivity activity = null;
        private boolean usePass = false;
        private String password = null;
        private Context context = null;
        private PostingReplyDialogFragment dialogFragment = null;
        private String charset = null;

        public PostReplyTask() {
            this.usePass = usePass();
            this.context = getApplicationContext();
            this.charset = ChanBoard.isAsciiOnlyBoard(boardCode) ? PartBase.ASCII_CHARSET : PartBase.UTF8_CHARSET;
        }
        @Override
        protected Integer doInBackground(PostingReplyDialogFragment... params) { // dialog is for callback
            dialogFragment = params[0];
            try {
                password = getPassword(); // save for later use

                MultipartEntity entity = buildMultipartEntity();
                if (entity == null) {
                    Log.e(TAG, "Null entity returned building post");
                    return R.string.post_reply_error;
                }

                String response = executePostReply(entity);
                if (response == null || response.isEmpty()) {
                    Log.e(TAG, "Null response posting");
                    return R.string.post_reply_error;
                }

                ChanPostResponse chanPostResponse = new ChanPostResponse(context, response);
                chanPostResponse.processResponse();

                if (!postSuccessful(chanPostResponse))
                    return R.string.post_reply_error;

                return updateThreadsAndWatchlist(chanPostResponse);
            }
            catch (Exception e) {
                Log.e(TAG, "Error posting", e);
                return R.string.post_reply_error;
            }
        }

        protected MultipartEntity buildMultipartEntity() {
            List<Part> partsList = new ArrayList<Part>();
            partsList.add(new StringPart("MAX-FILE-SIZE", MAX_FILE_SIZE, charset));
            partsList.add(new StringPart("mode", "regist", charset));
            partsList.add(new StringPart("resto", Long.toString(threadNo), charset));
            partsList.add(new StringPart("name", getName(), charset));
            partsList.add(new StringPart("email", getEmail(), charset));
            partsList.add(new StringPart("sub", getSubject(), charset));
            partsList.add(new StringPart("com", getMessage(), charset));
            partsList.add(new StringPart("pwd", password, charset));
            if (!usePass) {
                partsList.add(new StringPart("recaptcha_challenge_field", getRecaptchaChallenge(), charset));
                partsList.add(new StringPart("recaptcha_response_field", getRecaptchaResponse(), charset));
            }
            if (hasSpoiler()) {
                partsList.add(new StringPart("spoiler", "on", charset));
            }
            if (!addImage(partsList))
                return null;

            Part[] parts = partsList.toArray(new Part[partsList.size()]);

            if (DEBUG)
                dumpPartsList(partsList);

            MultipartEntity entity = new MultipartEntity(parts);
            return entity;
        }

        protected void dumpPartsList(List<Part> partsList) {
            if (DEBUG) Log.i(TAG, "Dumping mime parts list:");
            for (Part p : partsList) {
                if (!(p instanceof StringPart))
                    continue;
                StringPart s = (StringPart)p;
                String line = s.getName() + ": " + s.getValue() + ", ";
                if (DEBUG) Log.i(TAG, line);
            }
        }

        protected boolean addImage(List<Part> partsList) {
            String imageUrl = imageUri == null ? null : imageUri.toString();
            if (imageUrl == null && threadNo == 0 && !ChanBoard.requiresThreadImage(boardCode)) {
                partsList.add(new StringPart("textonly", "on", charset));
            }
            if (imageUrl != null) {
                if (DEBUG) Log.i(TAG, "Trying to load image for imageUrl=" + imageUrl + " imagePath="+imagePath+" contentType="+contentType);
                File file = null;
                if (imagePath == null || imagePath.startsWith("http")) { // non-local path, load to tmp
                    InputStream in = null;
                    try {
                        String prefix = UUID.randomUUID().toString();
                        String suffix = MimeTypeMap.getFileExtensionFromUrl(imageUrl);
                        String fileName = prefix + "." + suffix;
                        file = new File(getExternalCacheDir(), fileName);
                        in = getContentResolver().openInputStream(imageUri);
                        FileUtils.copyInputStreamToFile(in, file);

                    }
                    catch (Exception e) {
                        Log.e(TAG, "Couldn't get file from uri=" + imageUri, e);
                        return false;
                    }
                    finally {
                        if (in != null) {
                            try {
                                in.close();
                            }
                            catch (Exception e) {
                                Log.e(TAG, "Couldn't close input stream:" + in, e);
                            }
                        }
                    }
                }
                else { // local file, load it directly
                    file = new File(imagePath);
                }

                try {
                    ExifInterface exif = new ExifInterface(file.getAbsolutePath());
                    if (DEBUG) Log.i(TAG, "Found exif orientation: " + exif.getAttribute(ExifInterface.TAG_ORIENTATION));
                }
                catch (Exception e) {
                    Log.e(TAG, "Couldn't read exif interface for file:" + file.getAbsolutePath(), e);
                }

                try {
                    if (file != null) {
                        FilePart filePart = new FilePart("upfile", file.getName(), file, contentType, charset);
                        partsList.add(filePart);
                    }
                }
                catch (Exception e) {
                    Log.e(TAG, "Couldn't add file to entity, file=" + file.getAbsolutePath(), e);
                    return false;
                }
            }
            return true;
        }

        protected String executePostReply(MultipartEntity entity) {
            String url = POST_URL_ROOT + boardCode + "/post";
            AndroidHttpClient client = AndroidHttpClient.newInstance("Android");
            HttpParams params = client.getParams();
            if (params != null)
                HttpClientParams.setRedirecting(params, true); // handle 302s from 4chan
            try {
                // setup post
                HttpPost request = new HttpPost(url);
                entity.setContentEncoding(charset);
                request.setEntity(entity);
                if (DEBUG)
                    dumpRequestContent(request.getEntity().getContent());
                if (DEBUG) Log.i(TAG, "Calling URL: " + request.getURI());

                // make call
                HttpResponse httpResponse;
                if (usePass) {
                    if (DEBUG) Log.i(TAG, "Using 4chan pass, attaching cookies to request");
                    PersistentCookieStore cookieStore = new PersistentCookieStore(context);
                    HttpContext localContext = new BasicHttpContext();
                    localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
                    httpResponse = client.execute(request, localContext);
                    if (DEBUG) Log.i(TAG, "Cookies: " + cookieStore.dump());
                }
                else {
                    if (DEBUG) Log.i(TAG, "Not using 4chan pass, executing with captcha");
                    httpResponse = client.execute(request);
                }
                if (DEBUG) Log.i(TAG, "Response Headers: " + (httpResponse == null ? "null" : Arrays.toString(httpResponse.getAllHeaders())));
                if (DEBUG) Log.i(TAG, "Response Body: " + (httpResponse == null ? "null" : "length: " + httpResponse.toString().length()));
                // check if response
                if (httpResponse == null) {
                    Log.e(TAG, context.getString(R.string.post_reply_no_response));
                    return null;
                }
                int statusCode = httpResponse.getStatusLine().getStatusCode();
                if (DEBUG) Log.i(TAG, "Response statusCode=" + statusCode);
                // process response
                BufferedReader r = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
                StringBuilder s = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) {
                    if (DEBUG) Log.i(TAG, "Response Line:" + line);
                    s.append(line);
                }
                String response = s.toString();
                return response;
            }
            catch (Exception e) {
                Log.e(TAG, "Exception while posting to url=" + url, e);
                return null;
            }
            finally {
                if (client != null) {
                    client.close();
                }
            }
        }

        protected void dumpRequestContent(InputStream is) {
            if (DEBUG) Log.i(TAG, "Request Message Body:");
            try {
                BufferedReader r = new BufferedReader(new InputStreamReader(is));
                String l;
                while ((l = r.readLine()) != null)
                    if (DEBUG) Log.i(TAG, l);
            }
            catch (IOException e) {
                if (DEBUG) Log.i(TAG, "Exception reading message for logging", e);
            }
        }

        protected String errorMessage = null;

        protected boolean postSuccessful(ChanPostResponse chanPostResponse) {
            errorMessage = chanPostResponse.getError(getApplicationContext());
            if (errorMessage != null && !errorMessage.isEmpty()) {
                return false;
            }

            if (DEBUG) Log.i(TAG, "isPosted:" + chanPostResponse.isPosted());
            if (!chanPostResponse.isPosted()) {
                Log.e(TAG, "Unable to post response=" + chanPostResponse.getResponse());
                return false;
            }

            return true;
        }

        protected int updateThreadsAndWatchlist(ChanPostResponse chanPostResponse) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            long postThreadNo = chanPostResponse.getThreadNo(); // direct from 4chan post response parsing
            final long newThreadNo = postThreadNo != 0 ? postThreadNo : threadNo; // fallback
            long postNo = chanPostResponse.getPostNo();
            if (DEBUG) Log.i(TAG, "posted /" + boardCode + "/" + newThreadNo + ":" + postNo);

            // forcing thread/board refresh
            ChanActivityId activityId = NetworkProfileManager.instance().getActivityId();
            if (activityId != null) {
                if (activityId.activity == LastActivity.THREAD_ACTIVITY) {
                    ChanFileStorage.resetLastFetched(activityId.boardCode, activityId.threadNo);
                    FetchChanDataService.scheduleThreadFetch(PostReplyActivity.this, boardCode, newThreadNo, true, false);
                } else if (activityId.activity == LastActivity.BOARD_ACTIVITY) {
                    ChanFileStorage.resetLastFetched(activityId.boardCode);
                    FetchChanDataService.scheduleBoardFetch(PostReplyActivity.this, boardCode, true, false);
                }
            }

            // auto-add to watchlist
            new Thread(new Runnable() {
                @Override
                public void run() {
                    ChanThread thread = new ChanThread();
                    thread.no = newThreadNo;
                    thread.board = boardCode;
                    thread.tn_w = 250;
                    thread.tn_h = 250;
                    thread.sub = getSubject().trim();
                    thread.com = getMessage().trim();
                    try {
                        ChanFileStorage.addWatchedThread(context, thread);
                        BoardActivity.refreshWatchlist();
                    }
                    catch (IOException e) {
                        Log.e(TAG, "Couldn't add thread /" + thread.board + "/" + thread.no + " to watchlist", e);
                    }
                    //ChanPostlist.addPost(context, boardCode, newThreadNo, postNo, password);
                }
            }).start();

            imageUri = null; // now we've processed so don't use it again
            return 0;
        }

        @Override
        protected void onCancelled() {
            Log.e(TAG, "Post cancelled");
            Toast.makeText(context, R.string.post_reply_cancelled, Toast.LENGTH_SHORT).show();
            reloadCaptcha(); // always need to do this after each request
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result != 0) {
                String error = context.getString(result) + (errorMessage == null ? "" : ": " + errorMessage);
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
                reloadCaptcha();
                dialogFragment.dismiss();
                return;
            }

            if (threadNo == 0) {
                Toast.makeText(context, R.string.post_reply_posted_thread, Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(context, R.string.post_reply_posted_reply, Toast.LENGTH_SHORT).show();
            }
            dialogFragment.dismiss();
            clearPrefs();
            exitingOnSuccess = true;
            Message.obtain(getHandler(), POST_FINISHED).sendToTarget();
        }

    }

}
