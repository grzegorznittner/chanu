/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.chanapps.four.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.chanapps.four.data.ChanHelper;

/**
 * IntentService is a base class for {@link Service}s that handle asynchronous
 * requests (expressed as {@link Intent}s) on demand.  Clients send requests
 * through {@link android.content.Context#startService(Intent)} calls; the
 * service is started as needed, handles each Intent in turn using a worker
 * thread, and stops itself when it runs out of work.
 *
 * <p>This "work queue processor" pattern is commonly used to offload tasks
 * from an application's main thread.  The IntentService class exists to
 * simplify this pattern and take care of the mechanics.  To use it, extend
 * IntentService and implement {@link #onHandleIntent(Intent)}.  IntentService
 * will receive the Intents, launch a worker thread, and stop the service as
 * appropriate.
 *
 * <p>All requests are handled on a single worker thread -- they may take as
 * long as necessary (and will not block the application's main loop), but
 * only one request will be processed at a time.
 *
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>For a detailed discussion about_dialog how to create services, read the
 * <a href="{@docRoot}guide/topics/fundamentals/services.html">Services</a> developer guide.</p>
 * </div>
 *
 * 
 * 
 * Greg comments:
 * * data parsing should be separated - after parsing we should notify current activity so it will refresh cursor
 * * every screen should register here, so we'll know which activity is currently running (if not possible to do that in other way)
 * * 4 threads - on 2G we'll operate only using one thread
 * * class should use network status to determine its behaviour:
 *   * on 2G latency is high and bandwith low - incoming intent should stop current operation and be executed immediatelly
 *   * on 3G/4G - additional fetches performed with the intent (eg. fetch all boards), so we'll use mobile radio properly
 *   * on WiFi - aggressive cache, boards refreshed periodically, watched/favourites should be also updated often
 *   * 2G/3G/4G - mobile radio time window - fetch operations trigged in batches, at lest 1 min pause between operations (unless manual refresh)
 * * need to verify average response time/fetch time/parse time on 2G, 3G and WiFi
 * 
 * * we can also prepare paid service which will allow users for unlimited uploads, uploads will be done via our server
 */
public abstract class BaseChanService extends Service {
	private static final String TAG = BaseChanService.class.getSimpleName();
	
    protected static int NON_PRIORITY_MESSAGE = 99;
    protected static int PRIORITY_MESSAGE = 100;
    
    protected static final int MAX_NON_PRIORITY_MESSAGES = 20;
    protected static final int MAX_PRIORITY_MESSAGES = 2;
    
    private static int nonPriorityMessageCounter = 0;
    private static int priorityMessageCounter = 0;

    protected volatile Looper mServiceLooper;
    protected volatile ServiceHandler mServiceHandler;
    private String mName;
    private boolean mRedelivery;

    protected void toastUI(final int stringId) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(), stringId, Toast.LENGTH_SHORT).show();
            }
        });
    }

    protected final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            onHandleIntent((Intent)msg.obj);
            stopSelf(msg.arg1);
        }        
    }

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public BaseChanService(String name) {
        super();
        mName = name;
    }

    /**
     * Sets intent redelivery preferences.  Usually called from the constructor
     * with your preferred semantics.
     *
     * <p>If enabled is true,
     * {@link #onStartCommand(Intent, int, int)} will return
     * {@link Service#START_REDELIVER_INTENT}, so if this process dies before
     * {@link #onHandleIntent(Intent)} returns, the process will be restarted
     * and the intent redelivered.  If multiple Intents have been sent, only
     * the most recent one is guaranteed to be redelivered.
     *
     * <p>If enabled is false (the default),
     * {@link #onStartCommand(Intent, int, int)} will return
     * {@link Service#START_NOT_STICKY}, and if the process dies, the Intent
     * dies along with it.
     */
    public void setIntentRedelivery(boolean enabled) {
        mRedelivery = enabled;
    }

    @Override
    public void onCreate() {
        // TODO: It would be nice to have an option to hold a partial wakelock
        // during processing, and to have a static startService(Context, Intent)
        // method that would launch the service & hand off a wakelock.

        super.onCreate();
        HandlerThread thread = new HandlerThread("ChanService[" + mName + "]");
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	if (nonPriorityMessageCounter > MAX_NON_PRIORITY_MESSAGES) {
    		Log.i(TAG, "Clearing chan fetch service message queue from non priority messages (" + nonPriorityMessageCounter + ")");
        	mServiceHandler.removeMessages(NON_PRIORITY_MESSAGE);
        	nonPriorityMessageCounter = 0;
    	}
    	if (priorityMessageCounter > MAX_PRIORITY_MESSAGES) {
    		Log.i(TAG, "Clearing chan fetch service message queue from priority messages (" + priorityMessageCounter + ")");
        	mServiceHandler.removeMessages(PRIORITY_MESSAGE);
        	priorityMessageCounter = 0;
    	}
        if (intent != null && intent.getIntExtra(ChanHelper.CLEAR_FETCH_QUEUE, 0) == 1) {
        	Log.i(TAG, "Clearing chan fetch service message queue");
        	mServiceHandler.removeMessages(NON_PRIORITY_MESSAGE);
        	mServiceHandler.removeMessages(PRIORITY_MESSAGE);
        	return START_NOT_STICKY;
        }
        
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        if (intent != null && intent.getIntExtra(ChanHelper.PRIORITY_MESSAGE, 0) == 1) {
        	msg.what = PRIORITY_MESSAGE;
        	mServiceHandler.sendMessageAtFrontOfQueue(msg);
        	priorityMessageCounter++;
        } else {
        	msg.what = NON_PRIORITY_MESSAGE;
        	mServiceHandler.sendMessage(msg);
        	nonPriorityMessageCounter++;
        }
        
        return START_NOT_STICKY;
    }
	
    /*
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        mServiceHandler.sendMessage(msg);
        return mRedelivery ? START_REDELIVER_INTENT : START_NOT_STICKY;
    }
    */

    @Override
    public void onDestroy() {
        mServiceLooper.quit();
    }

    /**
     * Unless you provide binding for your service, you don't need to implement this
     * method, because the default implementation returns null. 
     * @see android.app.Service#onBind
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * This method is invoked on the worker thread with a request to process.
     * Only one Intent is processed at a time, but the processing happens on a
     * worker thread that runs independently from other application logic.
     * So, if this code takes a long time, it will hold up other requests to
     * the same IntentService, but it will not hold up anything else.
     * When all requests have been handled, the IntentService stops itself,
     * so you should not call {@link #stopSelf}.
     *
     * @param intent The value passed to {@link
     *               android.content.Context#startService(Intent)}.
     */
    protected abstract void onHandleIntent(Intent intent);
}
