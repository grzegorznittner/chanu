package com.chanapps.four.component;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.chanapps.four.activity.*;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.fragment.BlocklistAddDialogFragment;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

/**
* Created with IntelliJ IDEA.
* User: johnarleyburns
* Date: 2/2/13
* Time: 11:31 AM
* To change this template use File | Settings | File Templates.
*/
public class BoardThreadPopup implements Dismissable {

    private static final int POPUP_BUTTON_HEIGHT_DP = 48;

    protected RefreshableActivity activity;
    protected LayoutInflater layoutInflater;
    protected ImageLoader imageLoader;
    protected DisplayImageOptions displayImageOptions;

    protected View popupView;
    protected ScrollView popupScrollView;
    //protected ImageView countryFlag;
    //protected TextView popupHeader;
    //protected TextView popupText;
    protected PopupWindow popupWindow;
    protected TextView deadThreadTextView;
    protected TextView closedThreadTextView;
    protected TextView spoilerTextView;
    protected TextView exifTextView;

    protected View spoilerButtonLine;
    protected Button spoilerButton;
    protected View exifButtonLine;
    protected Button exifButton;
    protected View blockButtonLine;
    protected Button blockButton;
    protected View replyButtonLine;
    protected Button replyButton;
    protected View highlightRepliesButtonLine;
    protected Button highlightRepliesButton;
    protected View highlightIdButtonLine;
    protected Button highlightIdButton;
    protected View showImageButtonLine;
    protected Button showImageButton;
    protected View goToThreadButtonLine;
    protected Button goToThreadButton;
    protected Button closeButton;

    public BoardThreadPopup(RefreshableActivity activity, LayoutInflater layoutInflater, ImageLoader imageLoader, DisplayImageOptions displayImageOptions) {
        this.activity = activity;
        this.layoutInflater = layoutInflater;
        this.imageLoader = imageLoader;
        this.displayImageOptions = displayImageOptions;

        popupView = layoutInflater.inflate(R.layout.popup_full_text_layout, null);
        popupWindow = new PopupWindow (popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setBackgroundDrawable(new BitmapDrawable(activity.getBaseContext().getResources())); // magic so back button dismiss works
        popupWindow.setFocusable(true);
/* blows up on galaxy note
        popupWindow.setOutsideTouchable(true); // magic so click outside window dismisses
        popupWindow.setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    popupWindow.dismiss();
                    return true;
                }
                else {
                    return false;
                }
            }
        });
*/
        popupScrollView = (ScrollView)popupView.findViewById(R.id.popup_full_text_scroll_view);
        //countryFlag = (ImageView)popupView.findViewById(R.id.popup_country_flag);
        //popupHeader = (TextView)popupView.findViewById(R.id.popup_header);
        //popupText = (TextView)popupView.findViewById(R.id.popup_full_text);
        deadThreadTextView = (TextView)popupView.findViewById(R.id.popup_dead_thread_text_view);
        closedThreadTextView = (TextView)popupView.findViewById(R.id.popup_closed_thread_text_view);

        spoilerTextView = (TextView)popupView.findViewById(R.id.popup_spoiler_text);
        spoilerButtonLine = (View)popupView.findViewById(R.id.popup_spoiler_button_line);
        spoilerButton = (Button)popupView.findViewById(R.id.popup_spoiler_button);
        spoilerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                spoilerTextView.setVisibility(View.VISIBLE);
                spoilerButtonLine.setVisibility(View.GONE);
                spoilerButton.setVisibility(View.GONE);
            }
        });

        exifTextView = (TextView)popupView.findViewById(R.id.popup_exif_text);
        exifButtonLine = (View)popupView.findViewById(R.id.popup_exif_button_line);
        exifButton = (Button)popupView.findViewById(R.id.popup_exif_button);
        exifButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exifTextView.setVisibility(View.VISIBLE);
                exifButtonLine.setVisibility(View.GONE);
                exifButton.setVisibility(View.GONE);
            }
        });

        blockButtonLine = (View)popupView.findViewById(R.id.popup_block_button_line);
        blockButton = (Button)popupView.findViewById(R.id.popup_block_button);
        replyButtonLine = (View)popupView.findViewById(R.id.popup_reply_button_line);
        replyButton = (Button)popupView.findViewById(R.id.popup_reply_button);
        highlightRepliesButtonLine = (View)popupView.findViewById(R.id.popup_highlight_replies_button_line);
        highlightRepliesButton = (Button)popupView.findViewById(R.id.popup_highlight_replies_button);
        highlightIdButtonLine = (View)popupView.findViewById(R.id.popup_highlight_id_button_line);
        highlightIdButton = (Button)popupView.findViewById(R.id.popup_highlight_id_button);
        showImageButtonLine = (View)popupView.findViewById(R.id.popup_show_image_button_line);
        showImageButton = (Button)popupView.findViewById(R.id.popup_show_image_button);
        goToThreadButtonLine = (View)popupView.findViewById(R.id.popup_go_to_thread_button_line);
        goToThreadButton = (Button)popupView.findViewById(R.id.popup_go_to_thread_button);
        closeButton = (Button)popupView.findViewById(R.id.popup_close_button);
    }

    public void showFromCursor(AdapterView<?> adapterView, final View view, int position, long id) {
        Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
        final String countryFlagUrl = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_COUNTRY_URL));
        final String headerText = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_HEADER_TEXT));
        final String rawText = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_TEXT));
        final String text = rawText == null ? "" : rawText;
        final int isDeadInt = cursor.getInt(cursor.getColumnIndex(ChanHelper.POST_IS_DEAD));
        final int isClosedInt = cursor.getInt(cursor.getColumnIndex(ChanHelper.POST_CLOSED));
        final boolean isDead = isDeadInt == 0 ? false : true;
        final boolean isClosed = isClosedInt == 0 ? false : true;
        final long resto = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_RESTO));
        final String clickedBoardCode = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_BOARD_NAME));
        final long postId = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_ID));
        final String userId = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_USER_ID));
        final String tripcode = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_TRIPCODE));
        final String name = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_NAME));
        final String email = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_EMAIL));
        final long tim = cursor.getLong(cursor.getColumnIndex(ChanHelper.POST_TIM));
        final String spoilerText = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_SPOILER_TEXT));
        final String exifText = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_EXIF_TEXT));
        final long clickedThreadNo = resto == 0 ? postId : resto;
        final long clickedPostNo = resto == 0 || postId == resto ? 0 : postId;
        if (BoardActivity.DEBUG) Log.i(BoardActivity.TAG, "Calling popup with id=" + id + " isDead=" + isDead + " postNo=" + postId + " resto=" + resto + " text=" + text);
        //popupHeader.setText(headerText);
        //popupText.setText(text);

        //setCountryFlag(countryFlagUrl);
        setSpoilerButton(spoilerText);
        setExifButton(exifText);
        setBlockButton(userId);
        setReplyButtons(isDead, isClosed, clickedBoardCode, clickedThreadNo, clickedPostNo, tim, text);
        setShowImageButton(adapterView, view, position, id);
        setGoToThreadButton(adapterView, view, position, id);
        displayHighlightRepliesButton(clickedBoardCode, clickedThreadNo, clickedPostNo);
        displayHighlightIdButton(clickedBoardCode, clickedThreadNo, clickedPostNo, userId, tripcode, name, email);
        setCloseButton();
        setScrollViewMargin();

        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                view.setBackgroundColor(activity.getBaseContext().getResources().getColor(R.color.PaletteBlack));
            }
        });
        popupWindow.showAtLocation(adapterView, Gravity.CENTER, 0, 0);
    }

    protected void setShowImageButton(final AdapterView<?> adapterView, final View view, final int position, final long id) {
        Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
        final String imageUrl = cursor.getString(cursor.getColumnIndex(ChanHelper.POST_IMAGE_URL));
        if (imageUrl != null && !imageUrl.isEmpty()) {
            showImageButtonLine.setVisibility(View.VISIBLE);
            showImageButton.setVisibility(View.VISIBLE);
            showImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FullScreenImageActivity.startActivity((Activity)activity, adapterView, view, position, id);                }
            });
        }
        else {
            showImageButtonLine.setVisibility(View.GONE);
            showImageButton.setVisibility(View.GONE);
        }
    }

    protected void setGoToThreadButton(final AdapterView<?> adapterView, final View view, final int position, final long id) {
        goToThreadButtonLine.setVisibility(View.VISIBLE);
        goToThreadButton.setVisibility(View.VISIBLE);
        goToThreadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ThreadActivity.startActivity((Activity)activity, adapterView, view, position, id, true);
            }
        });
    }

    /*
    protected void setCountryFlag(String countryFlagUrl) {
        if (BoardActivity.DEBUG) Log.v(BoardActivity.TAG, "Country flag url=" + countryFlagUrl);
        if (countryFlagUrl != null && !countryFlagUrl.isEmpty()) {
            try {
                if (BoardActivity.DEBUG) Log.v(BoardActivity.TAG, "calling imageloader for country flag" + countryFlagUrl);
                countryFlag.setVisibility(View.VISIBLE);
                this.imageLoader.displayImage(countryFlagUrl, countryFlag, displayImageOptions);
            }
            catch (Exception e) {
                Log.e(BoardActivity.TAG, "Couldn't set country flag image with url=" + countryFlagUrl, e);
                countryFlag.setVisibility(View.GONE);
                countryFlag.setImageBitmap(null);
            }
        }
        else {
            countryFlag.setVisibility(View.GONE);
            countryFlag.setImageBitmap(null);
        }
    }
    */

    protected void setSpoilerButton(String spoilerText) {
        spoilerTextView.setVisibility(View.GONE);
        if (spoilerText != null && !spoilerText.isEmpty()) {
            spoilerButtonLine.setVisibility(View.VISIBLE);
            spoilerButton.setVisibility(View.VISIBLE);
            spoilerTextView.setText(spoilerText);
        }
        else {
            spoilerButtonLine.setVisibility(View.GONE);
            spoilerButton.setVisibility(View.GONE);
            spoilerTextView.setText("");
        }
    }

    protected void setExifButton(String exifText) {
        exifTextView.setVisibility(View.GONE);
        if (exifText != null && !exifText.isEmpty()) {
            exifButtonLine.setVisibility(View.VISIBLE);
            exifButton.setVisibility(View.VISIBLE);
            exifTextView.setText(exifText);
        }
        else {
            exifButtonLine.setVisibility(View.GONE);
            exifButton.setVisibility(View.GONE);
            exifTextView.setText("");
        }
    }

    protected void setBlockButton(final String userId) {
        if (userId != null && !userId.isEmpty()) {
            blockButtonLine.setVisibility(View.VISIBLE);
            blockButton.setVisibility(View.VISIBLE);
            blockButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    (new BlocklistAddDialogFragment(BoardThreadPopup.this, activity, userId)).show(activity.getSupportFragmentManager(), BoardActivity.TAG);
                }
            });
        }
        else {
            blockButtonLine.setVisibility(View.GONE);
            blockButton.setVisibility(View.GONE);
        }
    }

    public void dismiss() {
        popupWindow.dismiss();
    }

    protected void setReplyButtons(boolean isDead, boolean isClosed,
                                   final String clickedBoardCode, final long clickedThreadNo, final long clickedPostNo,
                                   final long tim, final String text)
    {
        if (isDead) {
            replyButtonLine.setVisibility(View.GONE);
            replyButton.setVisibility(View.GONE);
            deadThreadTextView.setVisibility(View.VISIBLE);
            closedThreadTextView.setVisibility(View.GONE);
        }
        else if (isClosed) {
            replyButtonLine.setVisibility(View.GONE);
            replyButton.setVisibility(View.GONE);
            deadThreadTextView.setVisibility(View.GONE);
            closedThreadTextView.setVisibility(View.VISIBLE);
        }
        else {
            deadThreadTextView.setVisibility(View.GONE);
            closedThreadTextView.setVisibility(View.GONE);
            replyButtonLine.setVisibility(View.VISIBLE);
            replyButton.setVisibility(View.VISIBLE);
            replyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent replyIntent = new Intent(activity.getBaseContext(), PostReplyActivity.class);
                    replyIntent.putExtra(ChanHelper.BOARD_CODE, clickedBoardCode);
                    replyIntent.putExtra(ChanHelper.THREAD_NO, clickedThreadNo);
                    replyIntent.putExtra(ChanHelper.POST_NO, clickedPostNo);
                    replyIntent.putExtra(ChanHelper.TIM, tim);
                    replyIntent.putExtra(ChanHelper.TEXT, "");
                    popupWindow.dismiss();
                    ((Activity)activity).startActivity(replyIntent);
                }
            });
        }
    }

    protected void displayHighlightRepliesButton(final String boardCode, final long threadNo, final long postNo) { // board-level doesn't highlight, only thread-level does
        highlightRepliesButtonLine.setVisibility(View.GONE);
        highlightRepliesButton.setVisibility(View.GONE);
    }

    protected void displayHighlightIdButton(final String boardCode, final long threadNo, final long postNo,
                                            final String userId, final String tripcode, final String name, final String email)
    { // board-level doesn't highlight, only thread-level does
        highlightIdButtonLine.setVisibility(View.GONE);
        highlightIdButton.setVisibility(View.GONE);
    }

    protected void setCloseButton() {
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
            }
        });
    }

    protected void setScrollViewMargin() {
        ScrollView.LayoutParams params = (ScrollView.LayoutParams)popupScrollView.getLayoutParams();
        if (params == null)
            params = new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        int numVisibleButtons = 0;
        if (spoilerButton.getVisibility() == View.VISIBLE)
            numVisibleButtons++;
        if (exifButton.getVisibility() == View.VISIBLE)
            numVisibleButtons++;
        if (blockButton.getVisibility() == View.VISIBLE)
            numVisibleButtons++;
        if (replyButton.getVisibility() == View.VISIBLE)
            numVisibleButtons++;
        if (highlightRepliesButton.getVisibility() == View.VISIBLE)
            numVisibleButtons++;
        if (highlightIdButton.getVisibility() == View.VISIBLE)
            numVisibleButtons++;
        if (showImageButton.getVisibility() == View.VISIBLE)
            numVisibleButtons++;
        if (goToThreadButton.getVisibility() == View.VISIBLE)
            numVisibleButtons++;
        if (closeButton.getVisibility() == View.VISIBLE)
            numVisibleButtons++;
        int bottomMarginDp = numVisibleButtons * POPUP_BUTTON_HEIGHT_DP;
        int bottomMarginPx = ChanGridSizer.dpToPx(activity.getBaseContext().getResources().getDisplayMetrics(), bottomMarginDp);
        params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, bottomMarginPx);
        popupScrollView.setLayoutParams(params);
    }

}
