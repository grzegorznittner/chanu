package com.chanapps.four.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.WindowManager;
import com.chanapps.four.activity.R;
import com.chanapps.four.data.FontSize;

/**
* Created with IntelliJ IDEA.
* User: arley
* Date: 12/14/12
* Time: 12:44 PM
* To change this template use File | Settings | File Templates.
*/
public class FontSizeDialogFragment extends DialogFragment {

    public interface NotifyFontSizeListener {
        void onFontSizeChanged(FontSize fontSize);
    }

    public static final String TAG = FontSizeDialogFragment.class.getSimpleName();

    private FontSize fontSize;
    private CharSequence[] array;
    private NotifyFontSizeListener notifyFontSizeListener;

    public FontSizeDialogFragment(){}

    public FontSizeDialogFragment(FontSize fontSize) {
        super();
        this.fontSize = fontSize;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        array = getResources().getTextArray(R.array.font_sizes);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder
                .setTitle(R.string.font_size_menu)
                .setSingleChoiceItems(array, fontSize.ordinal(), selectFontSizeListener)
        ;
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }


    public FontSizeDialogFragment setNotifySortOrderListener(NotifyFontSizeListener notifySortOrderListener) {
        this.notifyFontSizeListener = notifySortOrderListener;
        return this;
    }


    private DialogInterface.OnClickListener selectFontSizeListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            CharSequence item = array[which];
            fontSize = FontSize.valueOfDisplayString(getActivity(), item.toString());
            if (notifyFontSizeListener != null)
                notifyFontSizeListener.onFontSizeChanged(fontSize);
            dismiss();
        }
    };

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

}
