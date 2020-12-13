package com.damn.anotherglass.extensions.notifications;

import com.damn.anotherglass.core.GlassService;
import com.damn.anotherglass.shared.RPCMessage;
import com.damn.anotherglass.shared.notifications.NotificationData;
import com.damn.anotherglass.shared.notifications.NotificationsAPI;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class NotificationExtension {

    private final GlassService service;

    public NotificationExtension(final GlassService service) {
        this.service = service;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(NotificationEvent event) {
        NotificationData notificationData = Converter.convert(service, event.action, event.notification);
        service.send(new RPCMessage(NotificationsAPI.ID, notificationData));
    };

    public void start() {
        if(!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
    }

    public void stop() {
        if(EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
    }
}
