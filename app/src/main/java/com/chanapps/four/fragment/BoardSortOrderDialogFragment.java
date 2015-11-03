package com.chanapps.four.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.WindowManager;
import com.chanapps.four.activity.R;
import com.chanapps.four.data.BoardSortType;

/**
* Created with IntelliJ IDEA.
* User: arley
* Date: 12/14/12
* Time: 12:44 PM
* To change this template use File | Settings | File Templates.
*/
public class BoardSortOrderDialogFragment extends DialogFragment {

    public interface NotifySortOrderListener {
        void onSortOrderChanged(BoardSortType boardSortType);
    }

    public static final String TAG = BoardSortOrderDialogFragment.class.getSimpleName();

    private BoardSortType sortType;
    private CharSequence[] array;
    private NotifySortOrderListener notifySortOrderListener;

    public BoardSortOrderDialogFragment(){}

    public BoardSortOrderDialogFragment(BoardSortType sortType) {
        super();
        this.sortType = sortType;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        array = getResources().getTextArray(R.array.sort_order_types);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder
                .setTitle(R.string.sort_order_menu)
                .setSingleChoiceItems(array, sortType.ordinal(), selectSortOrderListener)
        ;
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }


    public BoardSortOrderDialogFragment setNotifySortOrderListener(NotifySortOrderListener notifySortOrderListener) {
        this.notifySortOrderListener = notifySortOrderListener;
        return this;
    }


    private DialogInterface.OnClickListener selectSortOrderListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            CharSequence item = array[which];
            sortType = BoardSortType.valueOfDisplayString(getActivity(), item.toString());
            if (notifySortOrderListener != null)
                notifySortOrderListener.onSortOrderChanged(sortType);
            dismiss();
        }
    };

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

}
