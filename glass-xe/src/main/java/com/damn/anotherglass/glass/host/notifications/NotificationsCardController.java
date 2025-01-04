package com.damn.anotherglass.glass.host.notifications;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import com.damn.anotherglass.glass.host.BroadcastingStopMenuActivity;
import com.damn.anotherglass.glass.host.HostService;
import com.damn.anotherglass.glass.host.NotificationsActivity;
import com.damn.anotherglass.shared.notifications.NotificationData;
import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.widget.CardBuilder;

import java.util.HashMap;
import java.util.Map;

public class NotificationsCardController extends BroadcastReceiver {

    private static final String NOTIFICATIONS_DISMISS = "com.damn.anotherglass.glass.host.notifications.dismiss";

    private static final String TAG = "NotificationsCardController";

    // Broadcast intent key for card ID
    private static final String KEY_CARD = "card";

    // Reserved card Id for stack
    private static final String CARD_ID_STACK = "stack";

    private static final String STACK_LIVE_CARD_TAG = "NotificationsStack";

    private final HostService service;

    // each ongoing notification gets its own card
    private final Map<NotificationId, LiveCard> mOngoingCards = new HashMap<>();

    // fake 'stack' of dismissible notifications
    private LiveCard mStackedCard;

    public NotificationsCardController(HostService service) {
        this.service = service;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(NOTIFICATIONS_DISMISS);
        service.registerReceiver(this, intentFilter);
    }

    public void onNotificationUpdate(NotificationData data) {
        NotificationsRepo.get().update(data);
        NotificationId id = new NotificationId(data);
        if (data.action == NotificationData.Action.Posted) {
            if (data.isOngoing) {
                showOngoing(id, data);
            } else {
                removeOngoing(id); // dismissible notification can replace ongoing with the same id
                showDismissible(data);
            }
        } else if (data.action == NotificationData.Action.Removed) {
            removeOngoing(id); // don't bother to check if it was ongoing there, just try to remove
            // do not remove dismissible ones for now
        }
    }

    private void showOngoing(NotificationId id, NotificationData data) {
        LiveCard liveCard = mOngoingCards.get(id);
        if (null != liveCard) {
            liveCard.setViews(getViews(data, false));
            // do not scroll to ongoing notifications, they can update a lot and block UI
        } else {
            liveCard = new LiveCard(service, STACK_LIVE_CARD_TAG);
            PendingIntent intent = getCardDismissPendingIntent(id.toString());
            liveCard.setAction(intent);
            liveCard.publish(LiveCard.PublishMode.REVEAL);
            mOngoingCards.put(id, liveCard);
        }
    }

    private void showDismissible(NotificationData data) {
        if (mStackedCard == null) {
            mStackedCard = new LiveCard(service, STACK_LIVE_CARD_TAG);
        }
        boolean hasMore = NotificationsRepo.get().getDismissibleNotifications().size() > 1;
        mStackedCard.setViews(getViews(data, hasMore));

        // Update intent
        // todo: logic there is a bit flawed, since NotificationsActivity can't clear stack or remove card right now
        PendingIntent pendingIntent =
                hasMore
                        ? getNotificationsPendingListIntent()
                        : getCardDismissPendingIntent(CARD_ID_STACK);
        mStackedCard.setAction(pendingIntent);

        if (!mStackedCard.isPublished())
            mStackedCard.publish(LiveCard.PublishMode.REVEAL);
        else
            mStackedCard.navigate();
    }

    private PendingIntent getNotificationsPendingListIntent() {
        Intent menuIntent = new Intent(service, NotificationsActivity.class);
        return PendingIntent.getActivity(service, (int) System.currentTimeMillis(), menuIntent, 0);
    }

    private void removeOngoing(NotificationId notificationId) {
        LiveCard liveCard = mOngoingCards.remove(notificationId);
        if (null == liveCard)
            return;
        liveCard.unpublish();
    }

    private PendingIntent getCardDismissPendingIntent(String cardId) {
        Intent dismissIntent = new Intent(NOTIFICATIONS_DISMISS);
        dismissIntent.putExtra(KEY_CARD, cardId);
        return BroadcastingStopMenuActivity.createIntent(service, dismissIntent);
    }

    public void remove() {
        service.unregisterReceiver(this);
        removeStack();
        for (LiveCard value : mOngoingCards.values()) {
            if (value.isPublished()) // should always be true
                value.unpublish();
        }
        mOngoingCards.clear();
    }

    private void removeStack() {
        if (null == mStackedCard) {
            return;
        }
        if (mStackedCard.isPublished()) {
            mStackedCard.unpublish();
        }
        mStackedCard = null;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String cardId = intent.getStringExtra(KEY_CARD);
        if(TextUtils.isEmpty(cardId)) {
            Log.e(TAG, "Received broadcast with empty KEY_CARD");
            return;
        }
        if (CARD_ID_STACK.equals(cardId)) {
            removeStack();
        } else {
            for (Map.Entry<NotificationId, LiveCard> lc : mOngoingCards.entrySet()) {
                NotificationId key = lc.getKey();
                if (!key.toString().equals(cardId))
                    continue;
                mOngoingCards
                        .remove(key)
                        .unpublish();
                break;
            }
        }
    }

    private RemoteViews getViews(NotificationData data, boolean showStack) {
        CardBuilder builder = NotificationViewBuilder.buildView(service.getApplicationContext(), data);
        builder.showStackIndicator(showStack);
        return builder.getRemoteViews();
    }

}
