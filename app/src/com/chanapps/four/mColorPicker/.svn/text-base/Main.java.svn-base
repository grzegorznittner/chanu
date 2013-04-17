/*
 * Copyright (C) 2010 Daniel Nilsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package afzkl.development.mColorPicker;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Main extends PreferenceActivity implements
		Preference.OnPreferenceClickListener {

	private final static int ACTIVITY_COLOR_PICKER_REQUEST_CODE = 1000;

	private Preference mDialogPreference;
	private Preference mActivityPreference;
	private Preference mGetSourceCodePreference;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().setFormat(PixelFormat.RGBA_8888);

		addPreferencesFromResource(R.xml.main);

		setUp();

	}

	private void setUp() {

		mDialogPreference = findPreference("dialog");
		mActivityPreference = findPreference("activity");
		mGetSourceCodePreference = findPreference("source_code");

		mDialogPreference.setOnPreferenceClickListener(this);
		mActivityPreference.setOnPreferenceClickListener(this);
		mGetSourceCodePreference.setOnPreferenceClickListener(this);

	}

	@Override
	public boolean onPreferenceClick(Preference preference) {

		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(Main.this);
		String key = preference.getKey();

		if (key.equals("dialog")) {

			final ColorPickerDialog d = new ColorPickerDialog(this, prefs
					.getInt("dialog", 0xffffffff));
			d.setAlphaSliderVisible(true);

			d.setButton("Ok", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {

					SharedPreferences.Editor editor = prefs.edit();
					editor.putInt("dialog", d.getColor());
					editor.commit();

				}
			});

			d.setButton2("Cancel", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {

				}
			});

			d.show();

			return true;
		} else if (key.equals("activity")) {

			Intent i = new Intent(this, ColorPickerActivity.class);
			i.putExtra(ColorPickerActivity.INTENT_DATA_INITIAL_COLOR, prefs
					.getInt("activity", 0xff000000));
			startActivityForResult(i, ACTIVITY_COLOR_PICKER_REQUEST_CODE);

			return true;
		}
		else if(key.equals("source_code")){
			Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("http://code.google.com/p/color-picker-view/"));
			startActivity(i);
		}

		return false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == ACTIVITY_COLOR_PICKER_REQUEST_CODE
				&& resultCode == Activity.RESULT_OK) {

			SharedPreferences customSharedPreference = PreferenceManager
					.getDefaultSharedPreferences(Main.this);
			SharedPreferences.Editor editor = customSharedPreference.edit();
			editor.putInt("activity", data.getIntExtra(
					ColorPickerActivity.RESULT_COLOR, 0xff000000));
			editor.commit();

		}

	}

}