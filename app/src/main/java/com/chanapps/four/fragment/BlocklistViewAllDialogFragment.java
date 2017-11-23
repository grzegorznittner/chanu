package com.chanapps.four.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Pair;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.chanapps.four.activity.BoardActivity;
import com.chanapps.four.activity.R;
import com.chanapps.four.component.EnhancedListView;
import com.chanapps.four.data.ChanBlocklist;
import com.chanapps.four.viewer.ThreadViewer;

import java.util.*;

/**
* Created with IntelliJ IDEA.
* User: arley
* Date: 12/14/12
* Time: 12:44 PM
* To change this template use File | Settings | File Templates.
*/
public class BlocklistViewAllDialogFragment
        extends DialogFragment
{

    public static final String TAG = BlocklistViewAllDialogFragment.class.getSimpleName();

    protected List<Pair<String, ChanBlocklist.BlockType>> blocks;
    protected EnhancedListAdapter adapter;
    protected EnhancedListView listView;
    protected AlertDialog dialog;
    protected Dialog.OnDismissListener onDismissListener;

    public BlocklistViewAllDialogFragment() {}

    public BlocklistViewAllDialogFragment(List<Pair<String, ChanBlocklist.BlockType>> blocks,
                                          Dialog.OnDismissListener onDismissListener)
    {
        super();
        this.blocks = blocks;
        this.onDismissListener = onDismissListener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getActivity();
        if (context == null)
            return null;
        //boolean useFriendlyIds = PreferenceManager
        //        .getDefaultSharedPreferences(context)
        //        .getBoolean(SettingsActivity.PREF_USE_FRIENDLY_IDS, true);
        blocks = ChanBlocklist.getSorted(context);
        return createFilledListDialog();
    }

    protected static final int UNDO_HIDE_DELAY_MS = 2500;

    protected Dialog createFilledListDialog() {
        setStyle(STYLE_NO_TITLE, 0);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View layout = inflater.inflate(R.layout.blocklist_items_dialog_fragment, null);
        View empty = layout.findViewById(R.id.empty);
        listView = (EnhancedListView)layout.findViewById(R.id.items);
        listView.setOnItemClickListener(itemClickListener);
        listView.setDismissCallback(dismissCallback);
        listView.enableSwipeToDismiss();
        listView.setRequireTouchBeforeDismiss(false);
        listView.setUndoHideDelay(UNDO_HIDE_DELAY_MS);
        listView.setEmptyView(empty);
        TextView titleView = (TextView)layout.findViewById(R.id.title);
        titleView.setText(R.string.blocklist_title);

        adapter = new EnhancedListAdapter();
        adapter.setItems(blocks);
        listView.setAdapter(adapter);
        dialog = (new AlertDialog.Builder(getActivity()).setView(layout))
                .setNegativeButton(R.string.dismiss, onCloseListener)
                .setPositiveButton(R.string.dialog_add, onAddListener)
                .create();
        dialog.setOnShowListener(onShowListener);
        return dialog;
    }

    protected ListView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            if (adapter != null && position >= 0)
                adapter.showEditTextDialog(position);
        }
    };

    protected DialogInterface.OnShowListener onShowListener = new DialogInterface.OnShowListener() {
        @Override
        public void onShow(DialogInterface d) {
            Button add = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button close = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            add.setOnClickListener(onAddButtonListener);
            close.setOnClickListener(onCloseButtonListener);
        }
    };

    protected DialogInterface.OnClickListener onAddListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
                    // we do this in the onShowListener
        }
    };

    protected View.OnClickListener onAddButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            adapter.insert(adapter.getCount(), new Pair<String, ChanBlocklist.BlockType>("", ChanBlocklist.BlockType.TEXT));
            ThreadViewer.jumpToBottom(listView, new Handler());
        }
    };

    protected View.OnClickListener onCloseButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (onDismissListener != null)
                onDismissListener.onDismiss(dialog);
            dismiss();
        }
    };

    protected DialogInterface.OnClickListener onCloseListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            // we do this in button click
        }
    };

    protected void save() {
        if (listView != null)
            listView.discardUndo();
        ChanBlocklist.save(getActivity(), blocks);
    }

    protected EnhancedListView.OnDismissCallback dismissCallback = new EnhancedListView.OnDismissCallback() {
        @Override
        public EnhancedListView.Undoable onDismiss(final EnhancedListView listView, final int position) {
            final Pair<String, ChanBlocklist.BlockType> item = (Pair<String, ChanBlocklist.BlockType>)adapter.getItem(position);
            adapter.remove(position);
            return new EnhancedListView.Undoable() {
                @Override
                public void undo() {
                    adapter.insert(position, item);
                }
            };
        }
    };

    private class EnhancedListAdapter extends BaseAdapter {

        private List<Pair<String, ChanBlocklist.BlockType>> mItems = new ArrayList<Pair<String, ChanBlocklist.BlockType>>();

        void setItems(List<Pair<String, ChanBlocklist.BlockType>> items) {
            mItems.clear();
            mItems = items;
            notifyDataSetChanged();
        }

        public void remove(int position) {
            mItems.remove(position);
            notifyDataSetChanged();
        }

        public void insert(int position, Pair<String, ChanBlocklist.BlockType> item) {
            mItems.add(position, item);
            notifyDataSetChanged();
        }

        /**
         * How many items are in the data set represented by this Adapter.
         *
         * @return Count of items.
         */
        @Override
        public int getCount() {
            return mItems.size();
        }

        /**
         * Get the data item associated with the specified position in the data set.
         *
         * @param position Position of the item whose data we want within the adapter's
         *                 data set.
         * @return The data at the specified position.
         */
        @Override
        public Object getItem(int position) {
            return mItems.get(position);
        }

        /**
         * Get the row id associated with the specified position in the list.
         *
         * @param position The position of the item within the adapter's data set whose row id we want.
         * @return The id of the item at the specified position.
         */
        @Override
        public long getItemId(int position) {
            return position;
        }

        /**
         * Get a View that displays the data at the specified position in the data set. You can either
         * create a View manually or inflate it from an XML layout file. When the View is inflated, the
         * parent View (GridView, ListView...) will apply default layout parameters unless you use
         * {@link android.view.LayoutInflater#inflate(int, android.view.ViewGroup, boolean)}
         * to specify a root view and to prevent attachment to the root.
         *
         * @param position    The position of the item within the adapter's data set of the item whose view
         *                    we want.
         * @param convertView The old view to reuse, if possible. Note: You should check that this view
         *                    is non-null and of an appropriate type before using. If it is not possible to convert
         *                    this view to display the correct data, this method can create a new view.
         *                    Heterogeneous lists can specify their number of view types, so that this View is
         *                    always of the right type (see {@link #getViewTypeCount()} and
         *                    {@link #getItemViewType(int)}).
         * @param parent      The parent that this view will eventually be attached to
         * @return A View corresponding to the data at the specified position.
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder holder;
            if(convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.blocklist_items_dialog_item, parent, false);
                holder = new ViewHolder();
                assert convertView != null;
                holder.mText1 = (TextView) convertView.findViewById(R.id.text1);
                holder.mText2 = (TextView) convertView.findViewById(R.id.text2);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.position = position;
            Pair<String, ChanBlocklist.BlockType> item = mItems.get(position);
            holder.mText1.setText(item.first);
            ChanBlocklist.BlockType blockType = ChanBlocklist.BlockType.valueOf(item.second.toString().toUpperCase());
            int pos = blockType.ordinal();
            String displayType = getResources().getStringArray(R.array.block_types)[pos];
            holder.mText2.setText(displayType);

            return convertView;
        }

        public void showEditTextDialog(final int pos) {
            if (pos < 0)
                return;
            if (blocks == null)
                return;
            Pair<String, ChanBlocklist.BlockType> b = blocks.get(pos);
            if (b == null)
                return;
            View layout = getActivity().getLayoutInflater().inflate(R.layout.blocklist_items_single_dialog_item, null);
            final EditText input = (EditText)layout.findViewById(R.id.text1);
            input.setText(b.first);
            final Spinner spinner = (Spinner)layout.findViewById(R.id.spinner);
            ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(
                    getActivity(),
                    R.array.block_types,
                    android.R.layout.simple_spinner_item);
            typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(typeAdapter);
            spinner.setSelection(b.second.ordinal());

            Dialog dialog = (new AlertDialog.Builder(getActivity()))
                    .setTitle(R.string.blocklist_title)
                    .setView(layout)
                    .setNeutralButton(R.string.done, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (listView == null)
                                return;
                            if (blocks == null)
                                return;
                            Pair<String, ChanBlocklist.BlockType> b = blocks.get(pos);
                            if (b == null)
                                return;
                            int sel = spinner.getSelectedItemPosition();
                            int spinnerPos = sel == AdapterView.INVALID_POSITION ? 0 : sel;
                            ChanBlocklist.BlockType newType = ChanBlocklist.BlockType.values()[spinnerPos];
                            Pair<String, ChanBlocklist.BlockType> newBlock =
                                    new Pair<String, ChanBlocklist.BlockType>(input.getText().toString(), newType);
                            blocks.set(pos, newBlock);
                            notifyDataSetChanged();
                            closeKeyboard();
                        }
                    })
                    .create();
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    input.requestFocus();
                }
            });
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    closeKeyboard();
                }
            });
            dialog.show();
        }

        private AdapterView.OnItemSelectedListener spinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ChanBlocklist.BlockType newType = ChanBlocklist.BlockType.values()[position];
                int pos = listView.getPositionForView(view);
                if (pos < 0)
                    return;
                Pair<String, ChanBlocklist.BlockType> b = blocks.get(pos);
                if (b == null)
                    return;
                Pair<String, ChanBlocklist.BlockType> newBlock = new Pair<String, ChanBlocklist.BlockType>(b.first, newType);
                blocks.set(pos, newBlock);
                notifyDataSetChanged();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };

        private class ViewHolder {
            TextView mText1;
            TextView mText2;
            int position;
        }

    }

    @Override
    public void onStop() {
        if(listView != null) {
            listView.discardUndo();
        }
        super.onStop();
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        closeKeyboard();
        save();
        BoardActivity.updateBoard(getActivity());
        dismiss();
    }

    private void closeKeyboard() {
        IBinder windowToken = getActivity().getCurrentFocus() != null ? getActivity().getCurrentFocus().getWindowToken() : null;
        if (windowToken != null) { // close the keyboard
            InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(windowToken, 0);
        }
    }

}
