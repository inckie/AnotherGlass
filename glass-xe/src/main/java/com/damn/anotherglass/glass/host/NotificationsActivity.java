package com.damn.anotherglass.glass.host;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.damn.anotherglass.glass.host.notifications.NotificationViewBuilder;
import com.damn.anotherglass.glass.host.notifications.NotificationsRepo;
import com.damn.anotherglass.shared.notifications.NotificationData;
import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import java.util.Collections;
import java.util.List;

public class NotificationsActivity extends Activity {

    private CardScrollView mCardScroller;

    private List<NotificationData> mNotifications;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        mNotifications = NotificationsRepo.get().getDismissibleNotifications();

        mCardScroller = new CardScrollView(this);
        if(mNotifications.isEmpty()) {
            // allow to create mCardScroller for empty list, to avoid checks later
            finish();
            return;
        }
        // order should be already correct, but just in case
        Collections.sort(mNotifications, (lhs, rhs) -> Long.compare(rhs.postedTime, lhs.postedTime));

        mCardScroller.setAdapter(new CardScrollAdapter() {
            @Override
            public int getCount() {
                return mNotifications.size();
            }

            @Override
            public Object getItem(int position) {
                return mNotifications.get(position);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return buildView(mNotifications.get(position));
            }

            @Override
            public int getPosition(Object item) {
                // if not found returns -1, save value as AdapterView.INVALID_POSITION
                if(item instanceof NotificationData)
                    return mNotifications.indexOf(item);
                return AdapterView.INVALID_POSITION;
            }
        });

        // todo: handle controls: allow to dismiss single/all
        mCardScroller.setOnItemClickListener((parent, view, position, id) -> {
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            am.playSoundEffect(Sounds.DISALLOWED);
        });
        setContentView(mCardScroller);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCardScroller.activate();
    }

    @Override
    protected void onPause() {
        mCardScroller.deactivate();
        super.onPause();
    }

    private View buildView(NotificationData notification) {
        return NotificationViewBuilder.buildView(this, notification).getView();
    }

}
