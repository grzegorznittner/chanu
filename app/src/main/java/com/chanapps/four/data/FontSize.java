package com.chanapps.four.data;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.widget.TextView;
import com.chanapps.four.activity.R;
import com.chanapps.four.activity.SettingsActivity;

public enum FontSize {

    TINY(R.string.font_size_tiny, R.dimen.FontSizeTiny),
    SMALL(R.string.font_size_small, R.dimen.FontSizeSmall),
    MEDIUM(R.string.font_size_medium, R.dimen.FontSizeMedium),
    LARGE(R.string.font_size_large, R.dimen.FontSizeLarge),
    HUGE(R.string.font_size_huge, R.dimen.FontSizeHuge);

    private final int displayStringId;
    private final int dimensionId;

    FontSize(int displayStringId, int dimensionId) {
        this.displayStringId = displayStringId;
        this.dimensionId = dimensionId;
    }

    public static final FontSize valueOfDisplayString(Context context, String displayString) {
        for (FontSize fontSize : FontSize.values())
            if (context.getString(fontSize.displayStringId).equals(displayString))
                return fontSize;
        return MEDIUM;
    }

    public static void sizeTextView(TextView tv) {
        FontSize fontSize = loadFromPrefs(tv.getContext());
        sizeTextView(tv, fontSize.dimensionId);
    }

    public static void sizeTextView(TextView tv, int dimension) {
        float textSize = tv.getResources().getDimensionPixelSize(dimension);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
    }

    public int displayStringId() {
        return displayStringId;
    }

    public static FontSize loadFromPrefs(Context context) {
        return FontSize.valueOfDisplayString(context, PreferenceManager
                .getDefaultSharedPreferences(context)
                .getString(SettingsActivity.PREF_FONT_SIZE,
                        context.getString(R.string.font_size_medium)));
    }

    public static void saveToPrefs(Context context, FontSize boardSortType) {
        PreferenceManager
                .getDefaultSharedPreferences(context)
                .edit()
                .putString(SettingsActivity.PREF_FONT_SIZE,
                        context.getString(boardSortType.displayStringId()))
                .commit();
    }

}
