package com.damn.anotherglass.shared.music;

import java.io.Serializable;

public class MusicData implements Serializable {
    private static final long serialVersionUID = 1L;

    public String artist;
    public String track;
    public byte[] albumArt; // PNG or JPEG bytes
    public boolean isPlaying;

    public MusicData() {
    }

    public MusicData(String artist, String track, byte[] albumArt, boolean isPlaying) {
        this.artist = artist;
        this.track = track;
        this.albumArt = albumArt;
        this.isPlaying = isPlaying;
    }
}
