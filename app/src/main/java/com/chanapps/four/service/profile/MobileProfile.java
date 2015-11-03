package com.chanapps.four.service.profile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.chanapps.four.activity.*;
import com.chanapps.four.component.GlobalAlarmReceiver;
import com.chanapps.four.data.*;
import com.chanapps.four.data.LastActivity;
import com.chanapps.four.service.FetchChanDataService;
import com.chanapps.four.service.FetchPopularThreadsService;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.widget.WidgetProviderUtils;

public class MobileProfile extends AbstractNetworkProfile {
    private static final String TAG = MobileProfile.class.getSimpleName();
    private static final boolean DEBUG = false;

    private String networkType = "3G";

    private static final Map<Health, FetchParams> REFRESH_TIME = new HashMap<Health, FetchParams>();

    static {
        /* Mapping between connection health and fetch params
           *               HEALTH  ----->   REFRESH_DELAY, FORCE_REFRESH_DELAY, READ_TIMEOUT, CONNECT_TIMEOUT
           */
        REFRESH_TIME.put(Health.BAD, new FetchParams(600L, 3L, 20, 15, 0, 0));
        REFRESH_TIME.put(Health.VERY_SLOW, new FetchParams(600L, 3L, 20, 15, 50000, 5));
        REFRESH_TIME.put(Health.SLOW, new FetchParams(600L, 3L, 20, 10, 100000, 10));
        REFRESH_TIME.put(Health.GOOD, new FetchParams(600L, 3L, 12, 8, 250000, 15));
        REFRESH_TIME.put(Health.PERFECT, new FetchParams(600L, 3L, 8, 4, 500000, 20));
    }

    @Override
    public Type getConnectionType() {
        return Type.MOBILE;
    }

    @Override
    public FetchParams getFetchParams() {
        return REFRESH_TIME.get(getConnectionHealth());
    }

    public void setNetworkType(String networkType) {
        this.networkType = networkType;
    }

    @Override
    public Health getDefaultConnectionHealth() {
        if ("2G".equalsIgnoreCase(networkType)) {
            return Health.VERY_SLOW;
        } else {
            return Health.SLOW;
        }
    }

    @Override
    public void onProfileActivated(Context context) {
        super.onProfileActivated(context);

        Health health = getConnectionHealth();
        if (health == Health.NO_CONNECTION)
            return;

        ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
        ChanActivityId activityId = NetworkProfileManager.instance().getActivityId();
        if (activityId == null)
            return;
        /*
        switch (activityId.activity) {
            case THREAD_ACTIVITY:
                ChanThread thread = ChanFileStorage.loadThreadData(context, activityId.boardCode, activityId.threadNo);
                if (thread == null || thread.posts == null || thread.posts.length < 2) {
                    makeToast(R.string.mobile_profile_loading_thread);
                    FetchChanDataService.scheduleThreadFetchWithPriority(context, activityId.boardCode, activityId.threadNo);
                }
                break;
            case BOARD_ACTIVITY:
                ChanBoard board = ChanFileStorage.loadBoardData(context, activityId.boardCode);
                if (board == null || board.threads == null || board.threads.length == 0) {
                    makeToast(R.string.mobile_profile_loading_board);
                    FetchChanDataService.scheduleBoardFetch(context, activityId.boardCode, true, false);
                }
                break;
            case GALLERY_ACTIVITY:
                Handler handler = activity.getChanHandler();
                if (handler != null) {
                    makeToast(R.string.mobile_profile_loading_image);
                    handler.sendEmptyMessageDelayed(GalleryViewActivity.START_DOWNLOAD_MSG, 100);
                }
                break;
            case BOARD_SELECTOR_ACTIVITY:
                onBoardSelectorSelected(context, activityId.boardCode);
                break;
            default:
        }
        */

    }

    private void prefetchDefaultBoards(Context context) {
        //FetchPopularThreadsService.schedulePopularFetchService(context);
        //FetchChanDataService.scheduleBoardFetch(context, "a", false, true);
        /*
            FetchChanDataService.scheduleBoardFetch(context, "b");
            FetchChanDataService.scheduleBoardFetch(context, "v");
            FetchChanDataService.scheduleBoardFetch(context, "vg");
            FetchChanDataService.scheduleBoardFetch(context, "s");
        */
    }

    @Override
    public void onProfileDeactivated(Context context) {
        super.onProfileDeactivated(context);
    }

    @Override
    public void onApplicationStart(Context context) {
        super.onApplicationStart(context);
    }

    @Override
    public void onBoardSelectorSelected(Context context, String boardCode) {
        super.onBoardSelectorSelected(context, boardCode);
        // prefetch popular in the background if we haven't loaded it yet
        Health health = getConnectionHealth();
        if (health == Health.NO_CONNECTION) {
            //makeHealthStatusToast(context, health);
            return;
        }
        ChanBoard board = ChanFileStorage.loadBoardData(context, ChanBoard.POPULAR_BOARD_CODE);
        if (board != null && !board.defData && board.threads != null && board.threads.length > 1) {
            if (DEBUG) Log.i(TAG, "skipping fetch board selector board=" + ChanBoard.POPULAR_BOARD_CODE
                    + " as already have " + board.threads.length + " threads");
        } else {
            if (DEBUG) Log.i(TAG, "fetching board selector " + boardCode + " as no data is present");
            FetchPopularThreadsService.schedulePopularFetchService(context, true, false);
        }
    }


    @Override
    public void onBoardSelectorRefreshed(Context context, Handler handler, String boardCode) {
        super.onBoardSelectorRefreshed(context, handler, boardCode);
        Health health = getConnectionHealth();
        if (health == Health.NO_CONNECTION) {
            makeHealthStatusToast(context, health);
            return;
        }
        if (ChanBoard.POPULAR_BOARD_CODE.equals(boardCode)
                || ChanBoard.LATEST_BOARD_CODE.equals(boardCode)
                || ChanBoard.LATEST_IMAGES_BOARD_CODE.equals(boardCode)) {
            if (DEBUG) Log.i(TAG, "Manual refresh board=" + boardCode);
            boolean canFetch = FetchPopularThreadsService.schedulePopularFetchService(context, true, false);
            if (!canFetch)
                postStopMessage(handler, 0);
        }
    }

    @Override
    public void onBoardSelected(Context context, String boardCode) {
        super.onBoardSelected(context, boardCode);
        if (DEBUG) Log.i(TAG, "onBoardSelected");
        ChanBoard board = ChanFileStorage.loadBoardData(context, boardCode);
        if (board != null && board.isVirtualBoard() && !board.isPopularBoard()) {
            if (DEBUG) Log.i(TAG, "skipping non-popular virtual board /" + boardCode + "/");
        }
        else if (!ChanBoard.boardNeedsRefresh(context, boardCode, false)) {
            if (DEBUG) Log.i(TAG, "skipping board /" + boardCode + "/ doesnt need refresh");
            final ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
            ChanActivityId aid = activity == null ? null : activity.getChanActivityId();
            Handler handler = activity == null ? null : activity.getChanHandler();
            if (activity != null && activity instanceof BoardActivity
                    && aid != null && aid.boardCode != null && aid.boardCode.equals(boardCode)
                    && handler != null)
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        activity.setProgress(false);
                    }
                });
        }
        else {
            NetworkProfileManager.NetworkBroadcastReceiver.checkNetwork(context);
            Health health = getConnectionHealth();
            if (health == Health.NO_CONNECTION) {
                makeHealthStatusToast(context, health);
                if (DEBUG) Log.i(TAG, "skipping preload board as there is no network connection");
            }
            else if (!ChanBoard.boardHasData(context, boardCode)) {
                if (DEBUG) Log.i(TAG, "no board data, thus priority fetching /" + boardCode + "/");
                if (FetchChanDataService.scheduleBoardFetch(context, boardCode, true, false))
                    startProgress(NetworkProfileManager.instance().getActivity().getChanHandler());
            }
            else {
                if (DEBUG) Log.i(TAG, "board needs update, non-priority fetching /" + boardCode + "/");
                if (FetchChanDataService.scheduleBoardFetch(context, boardCode, false, false))
                    startProgress(NetworkProfileManager.instance().getActivity().getChanHandler());
            }
        }
    }

    @Override
    public void onBoardRefreshed(final Context context, Handler handler, String boardCode) {
        super.onBoardRefreshed(context, handler, boardCode);
        Health health = getConnectionHealth();
        ChanBoard board = ChanFileStorage.loadBoardData(context, boardCode);
        if (board != null && board.hasNewBoardData()) {
            board.swapLoadedThreads();
        }
        if (health == Health.NO_CONNECTION) {
            makeHealthStatusToast(context, health);
            return;
        } else if (ChanBoard.FAVORITES_BOARD_CODE.equals(boardCode)) {
            if (DEBUG) Log.i(TAG, "Manual refresh favorites");
            GlobalAlarmReceiver.fetchFavoriteBoards(context);
            postStopMessage(handler, 0);
        } else if (ChanBoard.WATCHLIST_BOARD_CODE.equals(boardCode)) {
            if (DEBUG) Log.i(TAG, "Manual refresh watchlist");
            GlobalAlarmReceiver.fetchWatchlistThreads(context);
            postStopMessage(handler, 0);
        } else if (ChanBoard.isPopularBoard(boardCode)) {
            if (DEBUG) Log.i(TAG, "Manual refresh popular board=" + boardCode);
            boolean canFetch = FetchPopularThreadsService.schedulePopularFetchService(context, true, false);
            if (!canFetch)
                postStopMessage(handler, 0);
        } else if (ChanBoard.isVirtualBoard(boardCode)) {
            if (DEBUG) Log.i(TAG, "Manual refresh non-popular virtual board=" + boardCode + ", skipping");
            postStopMessage(handler, 0);
        } else {
            boolean canFetch = FetchChanDataService.scheduleBoardFetch(context, boardCode, true, false);
            if (!canFetch)
                postStopMessage(handler, 0);
        }
        if (DEBUG) {
            UserStatistics userStats = NetworkProfileManager.instance().getUserStatistics();
            if (userStats != null) {
                int i = 1;
                for (ChanBoardStat stat : userStats.topBoards()) {
                    Log.i(TAG, "Top boards: " + i++ + ". " + stat);
                }
                i = 1;
                for (ChanThreadStat stat : userStats.topThreads()) {
                    Log.i(TAG, "Top threads: " + i++ + ". " + stat);
                }
            }
        }
    }

    @Override
    public void onUpdateViewData(Context baseContext, Handler handler, String boardCode) {
        super.onUpdateViewData(baseContext, handler, boardCode);
        if (DEBUG) Log.i(TAG, "onUpdateViewData /" + boardCode + "/");

        final ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
        final ChanActivityId currentActivityId = NetworkProfileManager.instance().getActivityId();

        String refreshText = null;
        ChanBoard board = ChanFileStorage.loadBoardData(baseContext, boardCode);
        if (board != null && board.hasNewBoardData()) {
            refreshText = board.refreshMessage();
            board.swapLoadedThreads();
        }
        final String refreshMessage = refreshText;

        boolean boardActivity = currentActivityId != null
                && currentActivityId.boardCode != null
                && currentActivityId.boardCode.equals(boardCode);

        if (boardActivity && currentActivityId.activity == LastActivity.BOARD_ACTIVITY
                && currentActivityId.threadNo == 0 && handler != null) {
            if (DEBUG) Log.i(TAG, "onUpdateViewData /" + boardCode + "/ refreshing activity");
            handler.post(new Runnable() {
                @Override
                public void run() {
                    ((BoardActivity)activity).refresh();
                }
            });
        }
    }

    @Override
    public void onThreadSelected(Context context, String boardCode, long threadNo) {
        if (DEBUG) Log.d(TAG, "onThreadSelected /" + boardCode + "/" + threadNo);
        super.onThreadSelected(context, boardCode, threadNo);
        boolean threadScheduled = false;

        if (!ChanBoard.boardNeedsRefresh(context, boardCode, false)) {
            if (DEBUG) Log.i(TAG, "board already loaded for thread, skipping load");
        }
        else {
            NetworkProfileManager.NetworkBroadcastReceiver.checkNetwork(context);
            Health health = getConnectionHealth();
            if (health == Health.NO_CONNECTION) {
                //makeHealthStatusToast(context, health);
                return;
            }
            else if (!ChanBoard.boardHasData(context, boardCode)) {
                if (DEBUG) Log.i(TAG, "onThreadSelected no board data priority fetch /" + boardCode + "/");
                if (FetchChanDataService.scheduleBoardFetch(context, boardCode, true, false, threadNo)) {
                    startProgress(NetworkProfileManager.instance().getActivity().getChanHandler());
                    threadScheduled = true;
                }
            }
            else if (health == Health.BAD) {
                //makeHealthStatusToast(context, health);
                return;
            }
            else {
                if (DEBUG) Log.i(TAG, "onThreadSelected board needs fetch /" + boardCode + "/" + threadNo);
                if (FetchChanDataService.scheduleBoardFetch(context, boardCode, true, false, threadNo)) {
                    startProgress(NetworkProfileManager.instance().getActivity().getChanHandler());
                    threadScheduled = true;
                }
            }
        }

        if (threadScheduled) {
            Log.i(TAG, "onThreadSelected thread update scheduled after board fetch, exiting");
            return;
        }

        if (!ChanThread.threadNeedsRefresh(context, boardCode, threadNo, false)) {
            if (DEBUG) Log.i(TAG, "thread already loaded, skipping load");
        }
        else {
            NetworkProfileManager.NetworkBroadcastReceiver.checkNetwork(context);
            Health health = getConnectionHealth();
            if (health == Health.NO_CONNECTION) {
                //makeHealthStatusToast(context, health);
                return;
            } else {
                if (DEBUG) Log.i(TAG, "scheduling thread fetch with priority for /" + boardCode + "/" + threadNo);
                if (FetchChanDataService.scheduleThreadFetch(context, boardCode, threadNo, true, false))
                    startProgress(NetworkProfileManager.instance().getActivity().getChanHandler());
            }
        }
    }

    @Override
    public void onThreadRefreshed(Context context, Handler handler, String boardCode, long threadNo) {
        super.onThreadRefreshed(context, handler, boardCode, threadNo);
        Health health = getConnectionHealth();
        if (health == Health.NO_CONNECTION) {
            makeHealthStatusToast(context, health);
            return;
        }

        boolean threadScheduled = false;
        /*
        if (!ChanBoard.boardNeedsRefresh(context, boardCode, false)) {
            if (DEBUG) Log.i(TAG, "onThreadRefreshed board already loaded for thread, skipping load");
        }
        else
        */
        if (!ChanBoard.boardHasData(context, boardCode)) {
            if (DEBUG) Log.i(TAG, "onThreadRefreshed no board data priority fetch /" + boardCode + "/");
            if (FetchChanDataService.scheduleBoardFetch(context, boardCode, true, false, threadNo)) {
                startProgress(NetworkProfileManager.instance().getActivity().getChanHandler());
                threadScheduled = true;
            }
        }
        /*
        else {
            if (DEBUG) Log.i(TAG, "onThreadRefreshed normal fetch /" + boardCode + "/");
            FetchChanDataService.scheduleBoardFetch(context, boardCode, false, false);
        }
        */
        if (threadScheduled) {
            Log.i(TAG, "onThreadRefreshed thread update scheduled after board fetch, exiting");
            return;
        }

        boolean canFetch = FetchChanDataService.scheduleThreadFetch(context, boardCode, threadNo, true, false);
        if (DEBUG) Log.i(TAG, "onThreadRefreshed canFetch=" + canFetch + " handler=" + handler);
        if (!canFetch) {
            ChanThread thread = ChanFileStorage.loadThreadData(context, boardCode, threadNo);
            int msgId;
            if (thread != null && thread.isDead)
                msgId = R.string.thread_dead;
            else if (thread != null && thread.closed > 0)
                msgId = R.string.thread_closed;
            else
                msgId = 0;
            if (DEBUG) Log.i(TAG, "onThreadRefreshed skipping refresh reason=" + msgId);
            postStopMessage(handler, null);
        }
    }

    @Override
    public void onDataFetchSuccess(ChanIdentifiedService service, int time, int size) {
        // default behaviour is to parse properly loaded item
        super.onDataFetchSuccess(service, time, size);
    }

    private void handleBoardSelectorParseSuccess(ChanIdentifiedService service) {
        final ChanActivityId data = service.getChanActivityId();
        final ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
        final ChanActivityId currentActivityId = NetworkProfileManager.instance().getActivityId();
        if (DEBUG) Log.i(TAG, "handleBoardSelectorParseSuccess board=" + data.boardCode + " priority=" + data.priority);
        // check if board data corrupted, we need to reload it
        if (ChanBoard.isPopularBoard(data.boardCode))
            handlePopularParseSuccess(service, data, activity, currentActivityId);
        else if (ChanBoard.WATCHLIST_BOARD_CODE.equals(data.boardCode))
            handleWatchlistParseSuccess(service, data, activity, currentActivityId);
    }

    private void handlePopularParseSuccess(final ChanIdentifiedService service,
                                           final ChanActivityId data,
                                           final ChanIdentifiedActivity activity,
                                           final ChanActivityId currentActivityId) {
        ChanBoard board = ChanFileStorage.loadBoardData(service.getApplicationContext(), data.boardCode);
        if ((board == null || board.defData)) {
            if (DEBUG) Log.w(TAG, "Board " + data.boardCode + " is corrupted");
            NetworkProfileManager.instance().getCurrentProfile().onDataParseFailure(service, Failure.CORRUPT_DATA);
            FetchPopularThreadsService.schedulePopularFetchService(service.getApplicationContext(), true, false);
            return;
        }
        // user is on the same page and it's a manual refresh, reload it
        Handler handler = activity == null ? null : activity.getChanHandler();
        boolean hasHandler = handler != null;
        boolean isPriority = (currentActivityId != null && currentActivityId.priority) || data.priority;
        boolean sameActivity = currentActivityId != null
                && currentActivityId.activity == LastActivity.BOARD_ACTIVITY
                && board.isVirtualBoard();
        if (hasHandler && isPriority && sameActivity)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (DEBUG) Log.i(TAG, "Refreshing boardCode=" + currentActivityId.boardCode
                            + " data boardcode=" + data.boardCode);
                    activity.refresh();
                }
            });
        // tell it to refresh widgets for board if any are configured
        if (DEBUG) Log.i(TAG, "Calling widget provider update for boardCode=" + data.boardCode);
        WidgetProviderUtils.updateAll(activity.getBaseContext(), data.boardCode);
    }

    private void handleWatchlistParseSuccess(final ChanIdentifiedService service,
                                             final ChanActivityId data,
                                             final ChanIdentifiedActivity activity,
                                             final ChanActivityId currentActivityId) {
        ChanBoard board = ChanFileStorage.loadBoardData(service.getApplicationContext(), data.boardCode);
        if ((board == null || board.defData)) {
            if (DEBUG) Log.w(TAG, "Board " + data.boardCode + " is corrupted");
            NetworkProfileManager.instance().getCurrentProfile().onDataParseFailure(service, Failure.CORRUPT_DATA);
            return;
        }
        // user is on the same page and it's a manual refresh, reload it
        Handler handler = activity.getChanHandler();
        final boolean isPriority = currentActivityId.priority || data.priority;
        final boolean sameActivity = currentActivityId.activity == LastActivity.BOARD_ACTIVITY
                && ChanBoard.WATCHLIST_BOARD_CODE.equals(currentActivityId.boardCode);
        if (handler != null)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (DEBUG) Log.i(TAG, "Refreshing boardCode=" + currentActivityId.boardCode
                            + " data boardcode=" + data.boardCode);
                    if (isPriority && sameActivity)
                        activity.refresh();
                    else
                        activity.setProgress(false);
                }
            });
        // tell it to refresh widgets for board if any are configured
        if (DEBUG) Log.i(TAG, "Calling widget provider update for boardCode=" + data.boardCode);
        WidgetProviderUtils.updateAll(activity.getBaseContext(), data.boardCode);
    }

    private void handleBoardParseSuccess(ChanIdentifiedService service) {
        ChanActivityId data = service.getChanActivityId();

        //String refreshText = null;
        ChanBoard board = ChanFileStorage.loadBoardData(service.getApplicationContext(), data.boardCode);
        //if (data.priority && board != null && board.hasNewBoardData()) {
        if (board != null && board.hasNewBoardData()) {
            if (DEBUG) Log.i(TAG, "handleBoardParseSuccess /" + data.boardCode + "/ swapping threads");
            //refreshText = board.refreshMessage();
            board.swapLoadedThreads();
            try {
                ChanFileStorage.storeBoardData(service.getApplicationContext(), board);
            }
            catch (IOException e) {
                Log.e(TAG, "exception storing board /" + board.link + "/", e);
            }
        }
        //final String refreshMessage = refreshText;
        if (DEBUG) Log.i(TAG, "handleBoardParseSuccess /" + data.boardCode + "/"
                + " priority=" + data.priority
                + (board != null ? ""
                + " threads=" + board.threads.length
                + " loadedThreads=" + board.loadedThreads.length
                + " defData=" + board.defData
                : ""));

        if (board == null || board.defData) {
            // board data corrupted, we need to reload it
            if (DEBUG) Log.w(TAG, "Board " + data.boardCode + " is corrupted");
            NetworkProfileManager.instance().getCurrentProfile().onDataParseFailure(service, Failure.CORRUPT_DATA);
            //FetchChanDataService.scheduleBoardFetch(service.getApplicationContext(), data.boardCode);
            return;
        }

        final ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
        Handler handler = activity == null ? null : activity.getChanHandler();
        if (DEBUG) Log.i(TAG, "Board /" + data.boardCode + "/ loaded, activity=" + activity + " lastActivity=" + data.activity);
        if (handler != null && activity instanceof ThreadActivity && data.secondaryThreadNo <= 0) { // refresh view pager
            handler.post(new Runnable() {
                @Override
                public void run() {
                    ((ThreadActivity)activity).notifyBoardChanged();
                }
            });
        }

        if (handler != null && data.secondaryThreadNo > 0 && activity instanceof ThreadActivity) {
            if (DEBUG) Log.i(TAG, "Board /" + data.boardCode + "/ loaded, notifying thread, fetching secondary threadNo=" + data.secondaryThreadNo);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    ((ThreadActivity)activity).notifyBoardChanged();
                }
            });
            FetchChanDataService.scheduleThreadFetch(service.getApplicationContext(), data.boardCode,
                    data.secondaryThreadNo, true, false);
        }
        else {
            ChanActivityId aid = activity == null ? null : activity.getChanActivityId();
            final boolean sameBoard = aid != null
                    && aid.boardCode != null
                    && aid.boardCode.equals(data.boardCode);
            final boolean refresh = data.priority || board.defData || !board.isCurrent()
                    || board.shouldSwapThreads() || board.isVirtualBoard();
                // user is on the board page, we need to be reloaded it
            if (handler != null && activity instanceof BoardActivity && sameBoard) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        BoardActivity ba = (BoardActivity)activity;
                        if (refresh)
                            ba.refresh();
                        else
                            ba.setProgress(false);
                    }
                });
            }
        }

        // tell it to refresh widgets for board if any are configured
        if (DEBUG) Log.i(TAG, "Calling widget provider update for boardCode=" + data.boardCode);
        WidgetProviderUtils.updateAll(service.getApplicationContext(), data.boardCode);
    }

    private void handleThreadParseSuccess(ChanIdentifiedService service) {
        final ChanActivityId data = service.getChanActivityId();
        final ChanIdentifiedActivity activity = NetworkProfileManager.instance().getActivity();
        final ChanActivityId currentActivityId = NetworkProfileManager.instance().getActivityId();
        final Handler handler = activity == null ? null : activity.getChanHandler();

        ChanThread thread = ChanFileStorage.loadThreadData(service.getApplicationContext(), data.boardCode, data.threadNo);
        if (DEBUG) Log.i(TAG, "Loaded thread " + thread.board + "/" + thread.no + " " + thread.posts.length
                + " posts priority=" + data.priority + " activity=" + activity + " handler=" + handler);
        if ((thread == null || thread.defData)) {
            // thread file is corrupted, and user stays on thread page (or loads image), we need to refetch thread
            if (DEBUG) Log.w(TAG, "Thread " + data.boardCode + "/" + data.threadNo + " is corrupted");
            //FetchChanDataService.scheduleThreadFetch(service.getApplicationContext(), data.boardCode, data.threadNo);
            NetworkProfileManager.instance().getCurrentProfile().onDataParseFailure(service, Failure.CORRUPT_DATA);
            return;
        }

        /* this happens somewhere???
        // store updated status into board thread record
        ChanBoard board = ChanFileStorage.loadBoardData(service.getApplicationContext(), thread.board);
        if (board != null && board.threads != null) {
            for (int i = 0; i < board.threads.length; i++) {
                if (board.threads[i] != null && board.threads[i].no == thread.no) {
                    ChanThread.mergeboard.threads[i].isDead = true;
                    ChanFileStorage.storeBoardData(service.getApplicationContext(), board);
                    break;
                }
            }
        }

        // store updated status into watchlist thread record
        ChanBoard watchlist = ChanFileStorage.loadBoardData(service.getApplicationContext(), ChanBoard.WATCHLIST_BOARD_CODE);
        if (board != null && board.threads != null) {
            for (int i = 0; i < board.threads.length; i++) {
                if (board.threads[i] != null && board.threads[i].no == thread.no) {
                    board.threads[i].isDead = thread.isDead;
                    ChanFileStorage.storeBoardData(service.getApplicationContext(), board);
                    break;
                }
            }
        }
       */

        // user is on the thread page, we need to reloaded it
        if (activity instanceof ThreadActivity && data.priority) {
            final String boardCode = thread.board;
            final long threadNo = thread.no;
            if (DEBUG) Log.i(TAG, "asking thread activity to reload fragment /" + boardCode + "/" + threadNo
                    + " secondaryThreadNo=" + data.secondaryThreadNo);
            ((ThreadActivity)activity).refreshFragment(boardCode, threadNo, data.threadUpdateMessage); // dispatches on separate thread
        }
        else {
            if (DEBUG) Log.i(TAG, "skipping thread pager fragment reload");
        }

    }

    @Override
    public void onDataParseSuccess(ChanIdentifiedService service) {
        super.onDataParseSuccess(service);
        ChanActivityId data = service.getChanActivityId();
        if (ChanBoard.isVirtualBoard(data.boardCode))
            handleBoardSelectorParseSuccess(service);
        else if (data.threadNo == 0)
            handleBoardParseSuccess(service);
        else if (data.postNo == 0)
            handleThreadParseSuccess(service);
        // otherwise image fetching, ignore
    }

    @Override
    public void onDataFetchFailure(final ChanIdentifiedService service, Failure failure) {
        super.onDataFetchFailure(service, failure);
    }

    @Override
    public void onDataParseFailure(final ChanIdentifiedService service, Failure failure) {
        super.onDataFetchFailure(service, failure);
    }
}
