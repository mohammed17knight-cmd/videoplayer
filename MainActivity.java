package com.example.vplayer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

public class MainActivity extends AppCompatActivity {
    private static final int PICK = 1;
    private PlayerView playerView;
    private View openBtn;
    private ExoPlayer player;
    private Uri uri;
    private long pos = 0;
    private boolean playWhenReady = true;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        playerView = findViewById(R.id.player);
        openBtn = findViewById(R.id.open);
        openBtn.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT); // متاح من أندرويد 4.4
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("video/*");
            startActivityForResult(i, PICK);
        });
        playerView.setControllerVisibilityListener(
            (PlayerView.ControllerVisibilityListener) v -> openBtn.setVisibility(v == View.VISIBLE ? View.VISIBLE : View.GONE));
        if (b != null) { uri = (Uri) b.getParcelable("uri"); pos = b.getLong("pos"); }
        else if (getIntent().getData() != null) uri = getIntent().getData();
    }

    @Override protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        if (i.getData() != null) { uri = i.getData(); pos = 0; release(); init(); }
    }

    @Override protected void onActivityResult(int req, int res, Intent d) {
        super.onActivityResult(req, res, d);
        if (req == PICK && res == RESULT_OK && d != null && d.getData() != null) {
            uri = d.getData(); pos = 0; release(); init();
        }
    }

    private void init() {
        if (player != null || uri == null) return;
        // الاعتماد على فك التشفير بالهاردوير (MediaCodec) + fallback لو الديكودر الأول فشل
        DefaultRenderersFactory rf = new DefaultRenderersFactory(this)
            .setEnableDecoderFallback(true);
        player = new ExoPlayer.Builder(this, rf).build();
        playerView.setPlayer(player);
        player.setMediaItem(MediaItem.fromUri(uri));
        player.seekTo(pos);
        player.setPlayWhenReady(playWhenReady);
        player.prepare();
    }

    private void release() {
        if (player == null) return;
        pos = player.getCurrentPosition();
        playWhenReady = player.getPlayWhenReady();
        player.release();
        player = null;
    }

    @Override protected void onStart() { super.onStart(); init(); }
    @Override protected void onStop() { super.onStop(); release(); }

    @Override protected void onSaveInstanceState(Bundle o) {
        super.onSaveInstanceState(o);
        if (player != null) pos = player.getCurrentPosition();
        o.putParcelable("uri", uri); o.putLong("pos", pos);
    }

    @Override public void onWindowFocusChanged(boolean f) {
        super.onWindowFocusChanged(f);
        if (f) getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}
