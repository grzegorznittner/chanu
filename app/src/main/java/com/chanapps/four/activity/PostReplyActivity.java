package com.chanapps.four.activity;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.ExifInterface;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.chanapps.four.component.ActivityDispatcher;
import com.chanapps.four.component.CaptchaView;
import com.chanapps.four.component.ChanGridSizer;
import com.chanapps.four.component.ThemeSelector;
import com.chanapps.four.component.URLFormatComponent;
import com.chanapps.four.component.CaptchaView.CaptchaCallback;
import com.chanapps.four.data.Captcha;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanFileStorage;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.data.ChanPostResponse;
import com.chanapps.four.data.ChanThread;
import com.chanapps.four.data.LastActivity;
import com.chanapps.four.data.PersistentCookieStore;
import com.chanapps.four.fragment.LogoutPassDialogFragment;
import com.chanapps.four.fragment.PassSettingsFragment;
import com.chanapps.four.fragment.PasswordInfoDialogFragment;
import com.chanapps.four.fragment.PostingReplyDialogFragment;
import com.chanapps.four.fragment.WebImageDialogFragment;
import com.chanapps.four.loader.ChanImageLoader;
import com.chanapps.four.multipartmime.FilePart;
import com.chanapps.four.multipartmime.MultipartEntity;
import com.chanapps.four.multipartmime.Part;
import com.chanapps.four.multipartmime.PartBase;
import com.chanapps.four.multipartmime.StringPart;
import com.chanapps.four.service.FetchChanDataService;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.service.profile.NetworkProfile;
import com.chanapps.four.task.LogoutPassTask;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;

public class PostReplyActivity extends FragmentActivity implements ChanIdentifiedActivity, ThemeSelector.ThemeActivity {

    public static final String TAG = PostReplyActivity.class.getSimpleName();
    private static final boolean DEBUG = false;

    public static final String BOARD_CODE = "postReplyBoardCode";
    public static final String THREAD_NO = "postReplyThreadNo";
    public static final String POST_NO = "postReplyPostNo";
    public static final String TEXT = "text";
    public static final String REPLY_TEXT = "replyText";
    public static final String POST_REPLY_TEXT = "postReplyText";
    public static final String QUOTE_TEXT = "quoteText";
    public static final String POST_REPLY_QUOTE_TEXT = "postReplyQuoteText";
    public static final String POST_REPLY_IMAGE_URL = "postReplyImageUrl";
    public static final String SUBJECT = "postReplySubject";
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
            ORIENTATION,
            POST_REPLY_QUOTE_TEXT
    };

    public static final int POST_FINISHED = 0x01;

    public static final int PASSWORD_MAX = 100000000;
    private static final Random randomGenerator = new Random();
    private static final DecimalFormat eightDigits = new DecimalFormat("00000000");

    protected boolean exitingOnSuccess = false;

    private static final int IMAGE_GALLERY = 0x11;
    // private static final String ANDROID_IMAGE_CAPTURE =
    // "android.media.action.IMAGE_CAPTURE";

    private LinearLayout wrapperLayout;

    /*
     * private ImageView cameraButton; private ImageView pictureButton; private
     * ImageView webButton; private ImageView passEnableButton; private
     * ImageView passDisableButton; private ImageView bumpButton;
     */
    private ImageView deleteButton;
    private View deleteButtonBg;
    private ImageView webImageButton;
    private View webImageButtonBg;
    private View sageButton;
    private Handler handler;

    private CaptchaView recaptchaView;
    private View infoButton;
    // private Button doneButton;

    private EditText messageText;
    private TextView passStatusText;
    private EditText nameText;
    private EditText emailText;
    private EditText subjectText;
    private EditText passwordText;
    private CheckBox spoilerCheckbox;
    private ViewGroup previewFrame;
    private ImageView imagePreview;
    private ProgressBar previewProgress;
    private TextView.OnEditorActionListener fastSend;
    private View imageClickTarget;

    private Captcha captcha = new Captcha();

    protected Uri imageUri;
    protected String boardCode = null;
    protected long threadNo = 0;
    protected long postNo = 0;
    protected String imagePath = null;
    protected String contentType = null;
    protected String orientation = null;

    protected int themeId;
    protected ThemeSelector.ThemeReceiver broadcastThemeReceiver;

    protected boolean showPassEnable = true;
    protected boolean showPassDisable = false;

    protected String replyText = "";
    protected String quoteText = "";

    public static void startActivity(Context from, ChanActivityId aid) {
        startActivity(from, aid.boardCode, aid.threadNo, aid.postNo, aid.text, aid.quoteText);
    }

    public static void startActivity(Context context, String boardCode, long threadNo, long postNo, String replyText, String quoteText) {
        Intent intent = createIntent(context, boardCode, threadNo, postNo, replyText, quoteText);
        context.startActivity(intent);
    }

    public static Intent createIntent(Context context, String boardCode, long threadNo, long postNo, String replyText, String quoteText) {
        Intent replyIntent = new Intent(context, PostReplyActivity.class);
        replyIntent.putExtra(ChanBoard.BOARD_CODE, boardCode);
        replyIntent.putExtra(ChanThread.THREAD_NO, threadNo);
        replyIntent.putExtra(ChanPost.POST_NO, postNo);
        replyIntent.putExtra(TEXT, replyText);
        replyIntent.putExtra(REPLY_TEXT, replyText);
        replyIntent.putExtra(QUOTE_TEXT, quoteText);
        return replyIntent;
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (DEBUG) Log.i(TAG, "onCreate bundle=" + bundle);
        exitingOnSuccess = false;
        broadcastThemeReceiver = new ThemeSelector.ThemeReceiver(this);
        broadcastThemeReceiver.register();
        setContentView(R.layout.post_reply_layout);
        createViews();
        if (bundle != null) onRestoreInstanceState(bundle);
        else setFromIntent(getIntent());
        if (boardCode == null || boardCode.isEmpty()) boardCode = ChanBoard.DEFAULT_BOARD_CODE;
    }

    @Override
    public int getThemeId() {
        return themeId;
    }

    @Override
    public void setThemeId(int themeId) {
        this.themeId = themeId;
    }

    protected void createViews() {
        wrapperLayout = (LinearLayout) findViewById(R.id.post_reply_wrapper);
        previewFrame = (ViewGroup) findViewById(R.id.post_reply_preview_frame);
        imagePreview = (ImageView) findViewById(R.id.post_reply_preview_image);
        previewProgress = (ProgressBar) findViewById(R.id.post_reply_preview_progress_bar);
        /*
         * cameraButton =
         * (ImageView)findViewById(R.id.post_reply_camera_button); pictureButton
         * = (ImageView)findViewById(R.id.post_reply_picture_button); webButton
         * = (ImageView)findViewById(R.id.post_reply_web_button);
         * passEnableButton =
         * (ImageView)findViewById(R.id.post_reply_pass_enable_button);
         * passDisableButton =
         * (ImageView)findViewById(R.id.post_reply_pass_disable_button);
         * bumpButton = (ImageView)findViewById(R.id.post_reply_bump_button);
         */
        infoButton = findViewById(R.id.password_help_icon);
        // doneButton = (Button)findViewById(R.id.done);
        deleteButtonBg = findViewById(R.id.post_reply_delete_button_bg);
        deleteButton = (ImageView) findViewById(R.id.post_reply_delete_button);
        webImageButtonBg = findViewById(R.id.post_reply_web_image_button_bg);
        webImageButton = (ImageView) findViewById(R.id.post_reply_web_image_button);
        sageButton = findViewById(R.id.post_reply_sage);
        messageText = (EditText) findViewById(R.id.post_reply_text);
        passStatusText = (TextView) findViewById(R.id.post_reply_pass_status);
        nameText = (EditText) findViewById(R.id.post_reply_name);
        emailText = (EditText) findViewById(R.id.post_reply_email);
        subjectText = (EditText) findViewById(R.id.post_reply_subject);
        passwordText = (EditText) findViewById(R.id.post_reply_password);
        passwordText.setText(generatePassword()); // always default random
                                                  // generate, then we store for
                                                  // later use
        spoilerCheckbox = (CheckBox) findViewById(R.id.post_reply_spoiler_checkbox);
        recaptchaView = (CaptchaView) findViewById(R.id.post_reply_recaptcha_webview);

        fastSend = new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                validateAndSendReply();
                return true;
            }
        };
        recaptchaView.initCaptcha();

        /*
         * doneButton.setOnClickListener(new View.OnClickListener() {
         * 
         * @Override public void onClick(View v) { validateAndSendReply(); } });
         */
        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInfoFragment();
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                deleteImage();
            }
        });

        webImageButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                (new WebImageDialogFragment(boardCode, threadNo)).show(getSupportFragmentManager(), WebImageDialogFragment.TAG);
            }
        });
        /*
         * pictureButton.setOnClickListener(new View.OnClickListener() { public
         * void onClick(View view) { startGallery(); } });
         * webButton.setOnClickListener(new View.OnClickListener() { public void
         * onClick(View view) { (new WebImageDialogFragment(boardCode,
         * threadNo)) .show(getSupportFragmentManager(),
         * WebImageDialogFragment.TAG); } });
         * passEnableButton.setOnClickListener(new View.OnClickListener() {
         * public void onClick(View view) { if (!isPassEnabled() &&
         * isPassAvailable()) showPassFragment(); } });
         * passDisableButton.setOnClickListener(new View.OnClickListener() {
         * public void onClick(View view) { if (isPassEnabled()) {
         * disablePass(); } } }); bumpButton.setOnClickListener(new
         * View.OnClickListener() { public void onClick(View view) { bump(); }
         * });
         */
        sageButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                sage();
            }
        });

        passStatusText.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (isPassEnabled()) disablePass();
                else showPassFragment();
                /*
                 * if (!isPassEnabled() && isPassAvailable()) if
                 * (isPassAvailable()) showPassFragment();
                 */
            }
        });

        imageClickTarget = findViewById(R.id.post_reply_image_click_target);
        imageClickTarget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGallery();
            }
        });

        updatePassRecaptchaViews(isPassEnabled());
    }

    protected void setFromIntent(Intent intent) {
        boardCode = intent.getStringExtra(ChanBoard.BOARD_CODE);
        threadNo = intent.getLongExtra(ChanThread.THREAD_NO, 0);
        postNo = intent.getLongExtra(ChanPost.POST_NO, 0);
        if (intent.hasExtra(SUBJECT)) subjectText.setText(intent.getStringExtra(SUBJECT));
        if (intent.hasExtra(TEXT)) setMessage(intent.getStringExtra(TEXT));
        if (intent.hasExtra(REPLY_TEXT)) replyText = intent.getStringExtra(REPLY_TEXT);
        if (intent.hasExtra(QUOTE_TEXT)) quoteText = intent.getStringExtra(QUOTE_TEXT);
        if (intent.hasExtra(SPOILER)) spoilerCheckbox.setChecked(intent.getBooleanExtra(SPOILER, false));
        // these are just reset
        imageUri = intent.hasExtra(POST_REPLY_IMAGE_URL) ? Uri.parse(intent.getStringExtra(POST_REPLY_IMAGE_URL)) : null;
        imagePath = null;
        contentType = null;
        orientation = null;
        if (DEBUG) Log.i(TAG, "setIntent() intent has /" + boardCode + "/" + threadNo + ":" + postNo + " imageUri=" + imageUri + " combinedSubCom=" + intent.getStringExtra(SUBJECT) + " text=" + intent.getStringExtra(TEXT) + " replyText=" + intent.getStringExtra(REPLY_TEXT) + " quoteText=" + intent.getStringExtra(QUOTE_TEXT));
        Bundle bundle = loadBundleFromPrefs();
        if (bundle != null && boardCode != null && boardCode.equals(bundle.getString(ChanBoard.BOARD_CODE)) && threadNo == bundle.getLong(ChanThread.THREAD_NO)) {
            if (DEBUG) Log.i(TAG, "setIntent() found saved bundle for same thread, restoring");
            long savedPostNo = postNo;
            String savedReplyText = replyText;
            String savedQuoteText = quoteText;
            onRestoreInstanceState(bundle);
            Editable msg = messageText.getText();
            String s = msg.toString();
            if (!s.contains(savedReplyText)) msg.append((s.trim().isEmpty() ? "" : "\n\n") + savedReplyText);
            replyText = savedReplyText;
            quoteText = savedQuoteText;
            postNo = savedPostNo;
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        if (bundle == null) return;
        boardCode = bundle.getString(ChanBoard.BOARD_CODE);
        threadNo = bundle.getLong(ChanThread.THREAD_NO, 0);
        postNo = bundle.getLong(ChanPost.POST_NO, 0);
        if (bundle.containsKey(SUBJECT)) subjectText.setText(bundle.getString(SUBJECT));
        if (bundle.containsKey(TEXT)) setMessage(bundle.getString(TEXT));
        if (bundle.containsKey(REPLY_TEXT)) replyText = bundle.getString(REPLY_TEXT);
        if (bundle.containsKey(QUOTE_TEXT)) quoteText = bundle.getString(QUOTE_TEXT);
        if (bundle.containsKey(SPOILER)) spoilerCheckbox.setChecked(bundle.getBoolean(SPOILER, false));
        if (bundle.containsKey(POST_REPLY_IMAGE_URL)) imageUri = bundle.getString(POST_REPLY_IMAGE_URL) != null ? Uri.parse(bundle.getString(POST_REPLY_IMAGE_URL)) : null;
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
        if (DEBUG) Log.i(TAG, "saveStateToBundle()");
        bundle.putString(ChanBoard.BOARD_CODE, boardCode);
        bundle.putLong(ChanThread.THREAD_NO, threadNo);
        bundle.putLong(ChanPost.POST_NO, postNo);
        bundle.putString(SUBJECT, subjectText.getText().toString());
        bundle.putString(TEXT, messageText.getText().toString());
        bundle.putString(REPLY_TEXT, replyText);
        bundle.putString(QUOTE_TEXT, quoteText);
        bundle.putBoolean(SPOILER, spoilerCheckbox.isChecked());
        if (imageUri != null) bundle.putString(POST_REPLY_IMAGE_URL, imageUri.toString());
        bundle.putString(IMAGE_PATH, imagePath);
        bundle.putString(CONTENT_TYPE, contentType);
        bundle.putString(ORIENTATION, orientation);
        return bundle;
    }

    protected void saveBundleToPrefs(Bundle bundle) {
        if (DEBUG) Log.i(TAG, "saveBundleToPrefs()");
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
        editor.putString(POST_REPLY_QUOTE_TEXT, quoteText);
        editor.putString(REPLY_TEXT, replyText);
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
        bundle.putString(REPLY_TEXT, prefs.getString(REPLY_TEXT, null));
        bundle.putString(QUOTE_TEXT, prefs.getString(POST_REPLY_QUOTE_TEXT, null));
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
                if (passEnabled) Toast.makeText(PostReplyActivity.this, R.string.post_reply_pass_enabled_text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showInfoFragment() {
        (new PasswordInfoDialogFragment()).show(getSupportFragmentManager(), PasswordInfoDialogFragment.TAG);

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

    private boolean isPassEnabled() {
        return ensurePrefs().getBoolean(SettingsActivity.PREF_PASS_ENABLED, false);
    }

    private boolean isPassAvailable() {
        switch (NetworkProfileManager.instance().getCurrentProfile().getConnectionType()) {
        case WIFI:
        case MOBILE:
            return true;
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
        case MOBILE:
            if (passEnabled) {
                passStatusText.setText(R.string.post_reply_pass_enabled_text);
                setPassEnabled();
                setRecaptchaDisabled();
            } else {
                passStatusText.setText(R.string.post_reply_pass_enable_text);
                setPassDisabled();
                setRecaptchaEnabled();
            }
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
        showPassEnable = false;
        showPassDisable = false;
    }

    private void setPassEnabled() {
        showPassEnable = false;
        showPassDisable = true;
    }

    private void setPassDisabled() {
        showPassEnable = true;
        showPassDisable = false;
    }

    private void setRecaptchaEnabled() {
        recaptchaView.setVisibility(View.VISIBLE);
    }

    private void setRecaptchaDisabled() {
        recaptchaView.setVisibility(View.GONE);
    }

    public SharedPreferences ensurePrefs() {
        return PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    }

    public Handler getHandler() {
        return ensureHandler();
    }

    protected synchronized Handler ensureHandler() {
        if (handler == null && ActivityDispatcher.onUIThread()) handler = new PostReplyHandler();
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
        if ((!isPassEnabled() || !isPassAvailable())) refresh();
    }

    protected void setViews() {
        if (messageText.getText().length() == 0 && postNo != 0) setMessage(">>" + postNo + "\n");
        // updateBump();
        adjustFieldVisibility();
        defaultEmptyUserFieldsFromPrefs();
        setActionBarTitle();
        setImagePreview();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG) Log.i(TAG, "onResume");
        setViews();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveUserFieldsToPrefs();
        if (DEBUG) Log.i(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (DEBUG) Log.i(TAG, "onStop");
        handler = null;
        if (!exitingOnSuccess) saveBundleToPrefs(saveStateToBundle(new Bundle()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        broadcastThemeReceiver.unregister();
    }

    protected void saveUserFieldsToPrefs() {
        SharedPreferences.Editor ed = ensurePrefs().edit();
        String name = nameText.getText().toString();
        String email = emailText.getText().toString();
        String password = passwordText.getText().toString();
        // if (!name.isEmpty())
        ed.putString(SettingsActivity.PREF_USER_NAME, name);
        // if (!email.isEmpty() && !email.equalsIgnoreCase("sage")) // no
        // sagebombing
        ed.putString(SettingsActivity.PREF_USER_EMAIL, email);
        // if (!password.isEmpty())
        ed.putString(SettingsActivity.PREF_USER_PASSWORD, password);
        ed.apply();
        if (DEBUG) Log.i(TAG, "saved name=" + name);
    }

    public void refresh() {
        boolean passEnabled = isPassEnabled();
        updatePassRecaptchaViews(passEnabled);
    }

    public void setImageUri(Uri uri) {
        if (uri != null) {
            imageUri = uri;
            setImagePreview();
            if (DEBUG) Log.i(TAG, "loaded uri = " + uri);
        } else {
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
        if (DEBUG) Log.i(TAG, "existingName=" + existingName);
        if (existingName == null || existingName.length() == 0) {
            String name = ensurePrefs().getString(SettingsActivity.PREF_USER_NAME, "");
            if (DEBUG) Log.i(TAG, "prefName=" + name);
            if (!name.isEmpty()) nameText.setText(name);
        }
        if (existingEmail == null || existingEmail.length() == 0) {
            String email = ensurePrefs().getString(SettingsActivity.PREF_USER_EMAIL, "");
            if (!email.isEmpty()) emailText.setText(email);
        }
        if (existingPassword == null || existingPassword.length() == 0) {
            String password = ensurePrefs().getString(SettingsActivity.PREF_USER_PASSWORD, "");
            if (password.isEmpty()) password = generatePassword();
            passwordText.setText(password);
        }
    }

    protected void adjustFieldVisibility() {
        if (threadNo == 0) // new thread
        sageButton.setVisibility(View.GONE);

        if (ChanBoard.hasName(boardCode)) nameText.setVisibility(View.VISIBLE);
        else nameText.setVisibility(View.GONE);

        if (ChanBoard.hasSpoiler(boardCode)) spoilerCheckbox.setVisibility(View.VISIBLE);
        else spoilerCheckbox.setVisibility(View.GONE);

        // combinedSubCom hints
        if (threadNo == 0 && ChanBoard.requiresThreadSubject(boardCode)) {
            messageText.setHint(R.string.post_reply_text_hint_required);
            subjectText.setHint(R.string.post_reply_subject_hint_required);
        } else {
            messageText.setHint(R.string.post_reply_text_hint);
            subjectText.setHint(R.string.post_reply_subject_hint);
        }

        if (ChanBoard.hasSubject(boardCode)) {
            subjectText.setVisibility(View.VISIBLE);
        } else {
            subjectText.setVisibility(View.GONE);
        }

    }

    public boolean hasSpoiler() {
        return spoilerCheckbox.isChecked();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        int fragmentIndex = (requestCode >>> 16);
        if (fragmentIndex != 0) { // fragment bug
            super.onActivityResult(requestCode, resultCode, intent);
            return;
        }
        if (DEBUG) Log.i(TAG, "onActivityResult for requestCode=" + requestCode + " intent=" + intent + " intent.getData()=" + (intent == null ? "null" : intent.getData()));
        if (resultCode != RESULT_OK) {
            Log.e(TAG, "onActivityResult error resultCode=" + resultCode + " intent=" + intent);
            int errId = R.string.post_reply_no_load_gallery_image;
            Toast.makeText(this, errId, Toast.LENGTH_SHORT).show();
            return;
        }
        switch (requestCode) {
        case IMAGE_GALLERY:
            if (DEBUG) Log.i(TAG, "Got gallery result for activity imageUri=" + imageUri);
            if (intent == null || intent.getData() == null) {
                Log.e(TAG, "null image uri for gallery image");
                Toast.makeText(this, R.string.post_reply_no_load_gallery_image, Toast.LENGTH_SHORT).show();
            }
            imageUri = intent.getData();
            imageClickTarget.setVisibility(View.GONE);
            break;
        default:
            Log.e(TAG, "invalid request code for image");
            Toast.makeText(this, R.string.post_reply_no_image, Toast.LENGTH_SHORT).show();
        }
    }

    private enum MaxAxis {
        WIDTH, HEIGHT
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
                    MediaStore.Images.ImageColumns.ORIENTATION
            };
            // query so image shows up
            Cursor cursor = getContentResolver().query(imageUri, filePathColumn, null, null, null);
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
                } else {
                    Log.i(TAG, "media resolver for imageUri=" + imageUri + " has path=" + imagePath + " type=" + contentType + " orientation=" + orientation);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Couldn't get media information for imageUri=" + imageUri, e);
        }

        // just find the image bounds first pass
        InputStream in = new BufferedInputStream(getContentResolver().openInputStream(imageUri));
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(in, null, options); // this just sets
                                                       // options, returns null
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
        InputStream inScale = new BufferedInputStream(getContentResolver().openInputStream(imageUri));
        BitmapFactory.Options scaleOptions = new BitmapFactory.Options();
        scaleOptions.inSampleSize = scale;
        Bitmap b = BitmapFactory.decodeStream(inScale, null, scaleOptions);
        if (DEBUG) Log.i(TAG, "Final size: " + b.getWidth() + "x" + b.getHeight());
        return b;
    }

    protected void setImagePreview() {
        try {
            if (imageUri == null) {
                imagePreview.setVisibility(View.GONE);
                if (DEBUG) Log.i(TAG, "No image uri found, not setting image");
                return;
            }
            if (DEBUG) Log.i(TAG, "Setting preview image to uri=" + imageUri);
            DisplayImageOptions options = (new DisplayImageOptions.Builder()).cacheInMemory().cacheOnDisc().showStubImage(R.drawable.stub_image_background).resetViewBeforeLoading().displayer(new FadeInBitmapDisplayer(100)).fullSizeImageLocation(imageUri.toString()).imageSize(new ImageSize(300, 300)).build();
            ChanImageLoader.getInstance(this).displayImage(imageUri.toString(), imagePreview, options, previewListener);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), R.string.post_reply_no_image, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "setImagePreview exception while loading bitmap", e);
        }
    }

    protected ImageLoadingListener previewListener = new ImageLoadingListener() {
        @Override
        public void onLoadingStarted(String imageUri, View view) {
            imageClickTarget.setVisibility(View.GONE);
            previewProgress.setVisibility(View.VISIBLE);
        }

        @Override
        public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
            previewProgress.setVisibility(View.GONE);
            imageClickTarget.setVisibility(View.VISIBLE);
            deleteImage();
        }

        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            imagePreview.setVisibility(View.VISIBLE);
            deleteButtonBg.setVisibility(View.VISIBLE);
            deleteButton.setVisibility(View.VISIBLE);
            previewProgress.setVisibility(View.GONE);
            imageClickTarget.setVisibility(View.GONE);
        }

        @Override
        public void onLoadingCancelled(String imageUri, View view) {
            previewProgress.setVisibility(View.GONE);
            imageClickTarget.setVisibility(View.VISIBLE);
            deleteImage();
        }
    };

    private void deleteImage() {
        imagePreview.setVisibility(View.GONE);
        deleteButtonBg.setVisibility(View.GONE);
        deleteButton.setVisibility(View.GONE);
        imageClickTarget.setVisibility(View.VISIBLE);
        imageUri = null;
        imagePath = null;
        contentType = null;
        orientation = null;
    }

    private void sage() {
        emailText.setText("sage"); // 4chan way to post without bumping
    }

    private void startGallery() {
        if (DEBUG) Log.i(TAG, "startGallery()");
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        //intent.setType("image/*|video/webm");
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, IMAGE_GALLERY);
    }

    protected void validateAndSendReply() {
        String validMsg = validatePost();
        if (validMsg != null) {
            Toast.makeText(getApplicationContext(), validMsg, Toast.LENGTH_SHORT).show();
        } else {
            makePost();
        }
    }

    private void makePost() {
        closeKeyboard();
        PostReplyTask postReplyTask = new PostReplyTask();
        PostingReplyDialogFragment dialogFragment = new PostingReplyDialogFragment(postReplyTask, threadNo);
        dialogFragment.show(getSupportFragmentManager(), PostingReplyDialogFragment.TAG);
        if (!postReplyTask.isCancelled()) {
            postReplyTask.execute(dialogFragment);
        }
    }

    private void closeKeyboard() {
        IBinder windowToken = getCurrentFocus() != null ? getCurrentFocus().getWindowToken() : null;
        if (windowToken != null) { // close the keyboard
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(windowToken, 0);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.post_reply_send_menu);
        if (item != null) {
            boolean hasConnection = NetworkProfileManager.instance().getCurrentProfile().getConnectionType() != NetworkProfile.Type.NO_CONNECTION;
            if (!hasConnection) Toast.makeText(this, R.string.post_reply_pass_no_connection_text, Toast.LENGTH_SHORT).show();
            item.setVisible(hasConnection);
        }
        item = menu.findItem(R.id.post_reply_quote_menu);
        if (item != null) {
            boolean hasQuote = quoteText != null && !quoteText.trim().isEmpty();
            item.setVisible(hasQuote);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            navigateUp();
            return true;
        case R.id.post_reply_quote_menu:
            insertQuote();
            return true;
            /*
             * case R.id.post_reply_exit_menu: finish(); return true;
             */
        case R.id.post_reply_send_menu:
            validateAndSendReply();
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
        // updateBump();
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

    public Captcha getCaptcha() {
        return captcha;
    }

    public void setCaptcha(Captcha captcha) {
        this.captcha = captcha;
    }

    public String getPassword() {
        String s = passwordText.getText().toString();
        return (s != null) ? s : "";
    }

    private String generatePassword() {
        return eightDigits.format(randomGenerator.nextInt(PASSWORD_MAX));
    }

    private String validatePost() {
        String subject = subjectText.getText().toString();
        String message = messageText.getText().toString();
        String image = imageUri != null ? imageUri.getPath() : null;
        boolean hasSubject = subject != null && !subject.trim().isEmpty();
        boolean hasMessage = message != null && !message.trim().isEmpty();
        boolean hasImage = image != null && !image.trim().isEmpty();

        if (threadNo == 0) {
            if (ChanBoard.requiresThreadImage(boardCode) && !hasImage) return getString(R.string.post_reply_add_image);
            if (ChanBoard.requiresThreadSubject(boardCode) && !hasSubject) return getString(R.string.post_reply_board_requires_subject);
        }
        if (!hasImage && !hasMessage) return getString(R.string.post_reply_add_text_or_image);

        if (!isPassEnabled() || !isPassAvailable()) {
            String recaptcha = getRecaptchaResponse();
            if (StringUtils.isEmpty(recaptcha)) {
                recaptchaView.initCaptcha();
                return getString(R.string.post_reply_enter_captcha);
            }
        }

        return null;
    }

    public void navigateUp() {
        Pair<Integer, ActivityManager.RunningTaskInfo> p = ActivityDispatcher.safeGetRunningTasks(this);
        int numTasks = p.first;
        ActivityManager.RunningTaskInfo task = p.second;
        if (task != null) {
            if (DEBUG) Log.i(TAG, "navigateUp() top=" + task.topActivity + " base=" + task.baseActivity);
            if (task.baseActivity != null && !this.getClass().getName().equals(task.baseActivity.getClassName())) {
                if (DEBUG) Log.i(TAG, "navigateUp() using finish instead of intents with me=" + this.getClass().getName() + " base=" + task.baseActivity.getClassName());
                finish();
                return;
            }
        }

        if (DEBUG) Log.i(TAG, "navigateUp() launching intent /" + boardCode + "/" + threadNo + "#p" + postNo);
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
        String replyTitle = threadNo > 0 ? getString(R.string.post_reply_title) : getString(R.string.new_thread_menu);
        String title = "/" + boardCode + "/ " + replyTitle;
        getActionBar().setTitle(title);
        getActionBar().setDisplayShowHomeEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public ChanActivityId getChanActivityId() {
        ChanActivityId aid = new ChanActivityId(LastActivity.POST_REPLY_ACTIVITY, boardCode, threadNo, postNo, replyText);
        aid.quoteText = quoteText;
        return aid;
    }

    @Override
    public Handler getChanHandler() {
        return null;
    }

    public class PostReplyHandler extends Handler {

        public PostReplyHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                super.handleMessage(msg);
                switch (msg.what) {
                case POST_FINISHED:
                default:
                    navigateUp();
                }
            } catch (Exception e) {
                Log.e(TAG, "Couldn't handle message " + msg, e);
            }
        }
    }

    @Override
    public void closeSearch() {
    }

    @Override
    public void setProgress(boolean on) {
    }

    public class PostReplyTask extends AsyncTask<PostingReplyDialogFragment, Void, Integer> {

        public static final String MAX_FILE_SIZE = "3145728";

        // private PostReplyActivity activity = null;
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
        protected Integer doInBackground(PostingReplyDialogFragment... params) { // dialog
                                                                                 // is
                                                                                 // for
                                                                                 // callback
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

                if (!postSuccessful(chanPostResponse)) return R.string.post_reply_error;

                clearPrefs(); // clear on successful response

                return updateThreadsAndWatchlist(chanPostResponse);
            } catch (Exception e) {
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
                partsList.add(new StringPart("g-recaptcha-response", getRecaptchaResponse(), charset));
            }
            if (hasSpoiler()) {
                partsList.add(new StringPart("spoiler", "on", charset));
            }
            if (!addImage(partsList)) return null;

            Part[] parts = partsList.toArray(new Part[partsList.size()]);

            if (DEBUG) dumpPartsList(partsList);

            MultipartEntity entity = new MultipartEntity(parts);
            return entity;
        }

        protected void dumpPartsList(List<Part> partsList) {
            if (DEBUG) Log.i(TAG, "Dumping mime parts list:");
            for (Part p : partsList) {
                if (!(p instanceof StringPart)) continue;
                StringPart s = (StringPart) p;
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
                if (DEBUG) Log.i(TAG, "Trying to load image for imageUrl=" + imageUrl + " imagePath=" + imagePath + " contentType=" + contentType);
                File file = null;
                if (imagePath == null || imagePath.startsWith("http")) { // non-local path, load to tmp
                    InputStream in = null;
                    try {
                        String prefix = UUID.randomUUID().toString();
                        String suffix = MimeTypeMap.getFileExtensionFromUrl(imageUrl);
                        String fileName = prefix + "." + suffix;
                        file = new File(getExternalCacheDir(), fileName);
                        in = new BufferedInputStream(getContentResolver().openInputStream(imageUri));
                        FileUtils.copyInputStreamToFile(in, file);

                    } catch (Exception e) {
                        Log.e(TAG, "Couldn't get file from uri=" + imageUri, e);
                        return false;
                    } finally {
                        if (in != null) {
                            try {
                                in.close();
                            } catch (Exception e) {
                                Log.e(TAG, "Couldn't close input stream:" + in, e);
                            }
                        }
                    }
                } else { // local file, load it directly
                    file = new File(imagePath);
                }

                try {
                    ExifInterface exif = new ExifInterface(file.getAbsolutePath());
                    if (DEBUG) Log.i(TAG, "Found exif orientation: " + exif.getAttribute(ExifInterface.TAG_ORIENTATION));
                } catch (Exception e) {
                    Log.e(TAG, "Couldn't read exif interface for file:" + file.getAbsolutePath(), e);
                }

                try {
                    if (file != null) {
                        FilePart filePart = new FilePart("upfile", file.getName(), file, contentType, charset);
                        partsList.add(filePart);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Couldn't add file to entity, file=" + file.getAbsolutePath(), e);
                    return false;
                }
            }
            return true;
        }

        protected String executePostReply(MultipartEntity entity) {
            String url = String.format(URLFormatComponent.getUrl(context, URLFormatComponent.CHAN_POST_URL_FORMAT), boardCode);
            AndroidHttpClient client = AndroidHttpClient.newInstance("Android");
            HttpParams params = client.getParams();
            if (params != null) HttpClientParams.setRedirecting(params, true); // handle 302s from 4chan
            try {
                // setup post
                HttpPost request = new HttpPost(url);
                entity.setContentEncoding(charset);
                request.setEntity(entity);
                if (DEBUG) dumpRequestContent(request.getEntity().getContent());
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
                } else {
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
            } catch (Exception e) {
                Log.e(TAG, "Exception while posting to url=" + url, e);
                return null;
            } finally {
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
            } catch (IOException e) {
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
            if (PreferenceManager.getDefaultSharedPreferences(PostReplyActivity.this).getBoolean(SettingsActivity.PREF_AUTOMATICALLY_MANAGE_WATCHLIST, true)) new Thread(new Runnable() {
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
                        BoardActivity.refreshWatchlist(context);
                    } catch (IOException e) {
                        Log.e(TAG, "Couldn't add thread /" + thread.board + "/" + thread.no + " to watchlist", e);
                    }
                }
            }).start();

            imageUri = null; // now we've processed so don't use it again
            return 0;
        }

        @Override
        protected void onCancelled() {
            Log.e(TAG, "Post cancelled");
            Toast.makeText(context, R.string.post_reply_cancelled, Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result != 0) {
                String error = context.getString(result) + (errorMessage == null ? "" : ": " + errorMessage);
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
                safeDismiss();
                return;
            }

            if (threadNo == 0) {
                Toast.makeText(context, R.string.post_reply_posted_thread, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, R.string.post_reply_posted_reply, Toast.LENGTH_SHORT).show();
            }
            safeDismiss();
            clearPrefs();
            exitingOnSuccess = true;
            Message.obtain(getHandler(), POST_FINISHED).sendToTarget();
        }

        protected void safeDismiss() {
            if (dialogFragment != null) try {
                dialogFragment.dismiss();
            } catch (Exception e) {
                Log.e(TAG, "Exception while dismissing dialog", e);
            }
        }
    }

    protected PopupMenu.OnMenuItemClickListener popupListener = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
            case R.id.post_reply_quote_menu:
                insertQuote();
                return true;
            default:
                return false;
            }
        }
    };

    protected void insertQuote() {
        if (messageText == null) return;
        Editable t = messageText.getText();
        if (t == null) return;
        String s = t.subSequence(0, t.length()).toString();
        if (DEBUG) Log.i(TAG, "insertQuote text=" + s + " replyText=" + replyText + " quoteText=" + quoteText);
        int st;
        if (quoteText != null && !quoteText.isEmpty() && (st = s.indexOf(quoteText)) >= 0) {
            // quote is already there, so remove
            t.replace(st, st + replyText.length(), "");
        } else if (replyText != null && !replyText.isEmpty() && (st = s.indexOf(replyText)) >= 0) {
            t.replace(st, st + replyText.length(), quoteText);
        } else {
            t.insert(0, quoteText);
        }
    }

    @Override
    public void switchBoard(String boardCode, String query) {
    }

    public String getRecaptchaResponse() {
        return recaptchaView.getCaptchaResponse();
    }

    public void setRecaptchaResponse(String gRecaptchaResponse) {
        recaptchaView.setCaptchaResponse(gRecaptchaResponse);
    }

}
