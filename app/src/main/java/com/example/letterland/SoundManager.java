package com.example.letterland;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

public class SoundManager {
    private static SoundManager instance;
    private SoundPool soundPool;

    private int clickSoundId;
    private int shutterSoundId; // 📸 The new shutter sound variable!

    private boolean isSoundOn = true;

    // Replace the top part of your SoundManager constructor with this:

    private SoundManager(Context context) {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA) // 🚀 CHANGED TO MEDIA VOLUME
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(audioAttributes)
                .build();

        // Load both of your sounds into memory!
        clickSoundId = soundPool.load(context, R.raw.button_pop, 1);
        shutterSoundId = soundPool.load(context, R.raw.shutter, 1);
    }
    public static SoundManager getInstance(Context context) {
        if (instance == null) {
            instance = new SoundManager(context.getApplicationContext());
        }
        return instance;
    }

    // Plays the Button Pop
    public void playClick() {
        if (isSoundOn) {
            soundPool.play(clickSoundId, 1.0f, 1.0f, 0, 0, 1.0f);
        }
    }

    // 📸 Plays the Camera Shutter!
    public void playShutter() {
        if (isSoundOn) {
            soundPool.play(shutterSoundId, 1.0f, 1.0f, 0, 0, 1.0f);
        }
    }

    public void toggleSound(boolean soundOn) {
        isSoundOn = soundOn;
    }
}