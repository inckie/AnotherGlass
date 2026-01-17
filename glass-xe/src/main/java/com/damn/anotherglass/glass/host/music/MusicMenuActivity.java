package com.damn.anotherglass.glass.host.music;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.damn.anotherglass.glass.host.R;

public class MusicMenuActivity extends Activity {

    public static final String ACTION_BROADCAST = "com.damn.anotherglass.glass.host.music.ACTION_CONTROL";
    public static final String EXTRA_ACTION = "action"; // "play", "pause", "next", "prev"
    public static final String EXTRA_IS_PLAYING = "is_playing";

    private boolean isPlaying;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isPlaying = getIntent().getBooleanExtra(EXTRA_IS_PLAYING, false);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        openOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.music_menu, menu);
        MenuItem playPause = menu.findItem(R.id.action_play_pause);
        if (playPause != null) {
            playPause.setTitle(isPlaying ? "Pause" : "Play");
            playPause.setIcon(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = new Intent(ACTION_BROADCAST);
        int id = item.getItemId();
        if (id == R.id.action_play_pause) {
            intent.putExtra(EXTRA_ACTION, isPlaying ? "Pause" : "Play");
        } else if (id == R.id.action_next) {
            intent.putExtra(EXTRA_ACTION, "Next");
        } else if (id == R.id.action_prev) {
            intent.putExtra(EXTRA_ACTION, "Previous");
        } else {
            return super.onOptionsItemSelected(item);
        }
        sendBroadcast(intent);
        return true;
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        super.onOptionsMenuClosed(menu);
        finish();
    }
}
