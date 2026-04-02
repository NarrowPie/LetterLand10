package com.example.letterland;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;

public class SoundManager {
    private static SoundManager instance;
    private SoundPool soundPool;

    private int clickSoundId;
    private int shutterSoundId;

    // ✏️ NEW: Pencil scratch variables
    private int scratchSoundId;
    private int scratchStreamId = 0;

    private MediaPlayer backgroundMusicPlayer;
    private boolean isSoundOn = true;

    private SoundManager(Context context) {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(audioAttributes)
                .build();

        clickSoundId = soundPool.load(context, R.raw.button_pop, 1);
        shutterSoundId = soundPool.load(context, R.raw.shutter, 1);

        // ✏️ NEW: Load the scratch sound
        try {
            int scratchResId = context.getResources().getIdentifier("scratch", "raw", context.getPackageName());
            if (scratchResId != 0) {
                scratchSoundId = soundPool.load(context, scratchResId, 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 🎵 Load the background music
        try {
            int musicResId = context.getResources().getIdentifier("game_music", "raw", context.getPackageName());
            if(musicResId == 0) {
                musicResId = R.raw.menu_music;
            }

            backgroundMusicPlayer = MediaPlayer.create(context.getApplicationContext(), musicResId);
            if (backgroundMusicPlayer != null) {
                backgroundMusicPlayer.setLooping(true);
                // 🎵 LOWERED background music volume to 0.2f so the scratch sounds much louder!
                backgroundMusicPlayer.setVolume(0.2f, 0.2f);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static SoundManager getInstance(Context context) {
        if (instance == null) {
            instance = new SoundManager(context.getApplicationContext());
        }
        return instance;
    }

    public void playClick() {
        if (isSoundOn) {
            soundPool.play(clickSoundId, 1.0f, 1.0f, 0, 0, 1.0f);
        }
    }

    public void playShutter() {
        if (isSoundOn) {
            soundPool.play(shutterSoundId, 1.0f, 1.0f, 0, 0, 1.0f);
        }
    }

    // ✏️ NEW: Start looping the scratch sound at MAX volume
    public void startScratchSound() {
        if (isSoundOn && scratchSoundId != 0 && scratchStreamId == 0) {
            // Volume is 1.0f (Max). Loop is -1 (Loop forever until told to stop)
            scratchStreamId = soundPool.play(scratchSoundId, 1.0f, 1.0f, 0, -1, 1.0f);
        }
    }

    // ✏️ NEW: Stop the scratch sound immediately when finger is lifted
    public void stopScratchSound() {
        if (scratchStreamId != 0) {
            soundPool.stop(scratchStreamId);
            scratchStreamId = 0;
        }
    }

    public void startBackgroundMusic() {
        if (isSoundOn && backgroundMusicPlayer != null && !backgroundMusicPlayer.isPlaying()) {
            backgroundMusicPlayer.start();
        }
    }

    public void pauseBackgroundMusic() {
        if (backgroundMusicPlayer != null && backgroundMusicPlayer.isPlaying()) {
            backgroundMusicPlayer.pause();
        }
    }

    public void toggleSound(boolean soundOn) {
        isSoundOn = soundOn;
        if (!isSoundOn) {
            pauseBackgroundMusic();
            stopScratchSound();
        }
    }
}