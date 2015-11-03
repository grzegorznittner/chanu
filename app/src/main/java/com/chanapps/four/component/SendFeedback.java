package com.chanapps.four.component;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import com.chanapps.four.activity.R;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 10/7/13
 * Time: 3:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class SendFeedback {
    
    public static boolean email(final Activity activity) {
        String email = activity.getString(R.string.pref_about_contact_email);
        Uri uri = Uri.fromParts("mailto", email, null);
        Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
        intent.putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.app_name));
        activity.startActivity(
                Intent.createChooser(intent, activity.getString(R.string.pref_about_send_email)));
        return true;
    }
}
