package com.damn.anotherglass.glass.host.ui;

import android.support.annotation.NonNull;

import com.google.android.glass.timeline.LiveCard;

public class ICardViewProvider {

    protected final LiveCard mLiveCard;

    public ICardViewProvider(@NonNull LiveCard card) {
        mLiveCard = card;
    }
    public void onRemoved(){}
}
