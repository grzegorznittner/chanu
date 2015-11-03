/**
 * 
 */
package com.chanapps.four.component;

import java.io.File;

import com.chanapps.four.activity.R;
import com.chanapps.four.data.ChanFileStorage;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 * 
 */
public class CacheSizePreference extends Preference implements OnSeekBarChangeListener {
	private static final String TAG = "CacheSizePreference";
	private static final boolean DEBUG = false;
    public static final int MIN_VALUE = 32;
    public static final int MAX_VALUE = 1024;
    public static final int DEFAULT_VALUE = 128;
    public static final float MAX_CACHE_PERCENT = 0.1f;

	private static final String ANDROIDNS = "http://schemas.android.com/apk/res/android";
	private static final String CHANAPPS = "http://chanapps.com";

	private int maxValue = MAX_VALUE;
	private int minValue = MIN_VALUE;
	private int cacheSize = 1;
	private int currentValue = DEFAULT_VALUE;
	private String unitsLeft = "";
	private String unitsRight = "";
	private SeekBar seekBar;

	private TextView statusText;

	public CacheSizePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		initPreference(context, attrs);
	}

	public CacheSizePreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initPreference(context, attrs);
	}

	private void initPreference(Context context, AttributeSet attrs) {
		setValuesFromXml(attrs);
		seekBar = new SeekBar(context, attrs);
		seekBar.setMax(maxValue - minValue);
		seekBar.setOnSeekBarChangeListener(this);
	}

	private void setValuesFromXml(AttributeSet attrs) {
		maxValue = attrs.getAttributeIntValue(ANDROIDNS, "max", 1024);
		minValue = attrs.getAttributeIntValue(CHANAPPS, "min", DEFAULT_VALUE);
		if (DEBUG) Log.i(TAG, "Cache loaded from xml, min: " + minValue + " max: " + maxValue);
		try {
			File cacheFolder = ChanFileStorage.getCacheDirectory(getContext());
			long totalSpace = cacheFolder.getTotalSpace() / (1024*1024);
            long maxCache = Math.round((float)totalSpace * MAX_CACHE_PERCENT);
			maxValue = (int)maxCache;
			if (maxValue < minValue) {
				minValue = maxValue / 2 < DEFAULT_VALUE ? DEFAULT_VALUE : maxValue / 2;
			}
		} catch (Exception e) {
			Log.e(TAG, "Error while getting cache size", e);
		}

		unitsLeft = getAttributeStringValue(attrs, CHANAPPS, "unitsLeft", "");
		String units = getAttributeStringValue(attrs, CHANAPPS, "units", "");
		unitsRight = getAttributeStringValue(attrs, CHANAPPS, "unitsRight", units);

		try {
			String newInterval = attrs.getAttributeValue(CHANAPPS, "cacheSize");
			if (newInterval != null) {
				cacheSize = Integer.parseInt(newInterval);
			}
			if (cacheSize <= minValue) {
				cacheSize = DEFAULT_VALUE;
			}
		} catch (Exception e) {
			Log.e(TAG, "Invalid cacheSize value", e);
		}

		if (DEBUG) Log.i(TAG, "Cache value set to " + cacheSize);
	}

	private String getAttributeStringValue(AttributeSet attrs, String namespace, String name, String defaultValue) {
		String value = attrs.getAttributeValue(namespace, name);
		if (value == null) {
			value = defaultValue;
		}

		return value;
	}

	@Override
	protected View onCreateView(ViewGroup parent) {
		RelativeLayout layout = null;
		try {
			LayoutInflater mInflater = (LayoutInflater) getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			layout = (RelativeLayout) mInflater.inflate(
					R.layout.cache_size_preference, parent, false);
		} catch (Exception e) {
			Log.e(TAG, "Error creating seek bar preference", e);
		}

		return layout;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onBindView(View view) {
		super.onBindView(view);

		try {
			// move our seekbar to the new view we've been given
			ViewParent oldContainer = seekBar.getParent();
			ViewGroup newContainer = (ViewGroup) view
					.findViewById(R.id.seekBarPrefBarContainer);

			if (oldContainer != newContainer) {
				// remove the seekbar from the old view
				if (oldContainer != null) {
					((ViewGroup) oldContainer).removeView(seekBar);
				}
				// remove the existing seekbar (there may not be one) and add
				// ours
				newContainer.removeAllViews();
				newContainer.addView(seekBar,
						ViewGroup.LayoutParams.FILL_PARENT,
						ViewGroup.LayoutParams.WRAP_CONTENT);
			}
		} catch (Exception ex) {
			Log.e(TAG, "Error binding view: " + ex.toString(), ex);
		}

		updateView(view);
	}

	/**
	 * Update a SeekBarPreference view with our current state
	 * 
	 * @param view
	 */
	protected void updateView(View view) {

		try {
			RelativeLayout layout = (RelativeLayout) view;

			statusText = (TextView) layout.findViewById(R.id.seekBarPrefValue);
			statusText.setText(String.valueOf(currentValue));
			statusText.setMinimumWidth(30);

			seekBar.setProgress(currentValue - minValue);

			TextView unitsRightText = (TextView) layout.findViewById(R.id.seekBarPrefUnitsRight);
			unitsRightText.setText(unitsRight);

			TextView unitsLeftText = (TextView) layout.findViewById(R.id.seekBarPrefUnitsLeft);
			unitsLeftText.setText(unitsLeft);
		} catch (Exception e) {
			Log.e(TAG, "Error updating seek bar preference", e);
		}

	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		int newValue = progress + minValue;

		if (newValue > maxValue)
			newValue = maxValue;
		else if (newValue < minValue)
			newValue = minValue;

		// change rejected, revert to the previous value
		if (!callChangeListener(newValue)) {
			seekBar.setProgress(currentValue - minValue);
			return;
		}

		// change accepted, store it
		currentValue = newValue;
		statusText.setText(String.valueOf(newValue));
		persistInt(newValue);

	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		notifyChanged();
	}

	@Override
	protected Object onGetDefaultValue(TypedArray ta, int index) {
		int defaultValue = ta.getInt(index, DEFAULT_VALUE);
		return defaultValue;
	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		if (restoreValue) {
			currentValue = getPersistedInt(currentValue);
		} else {
			int temp = DEFAULT_VALUE;
			persistInt(temp);
			currentValue = temp;
		}
	}
}
