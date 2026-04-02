package com.example.letterland;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private boolean isMusicOn = true;
    private SharedPreferences prefs;

    // Optimized background thread manager
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    Toast.makeText(this, getString(R.string.toast_camera_required), Toast.LENGTH_LONG).show();
                    finishAffinity();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }

        prefs = getSharedPreferences("LetterLandMemory", MODE_PRIVATE);
        SoundManager soundManager = SoundManager.getInstance(this);

        View layoutOnboarding = findViewById(R.id.layoutOnboarding);
        EditText etFirstPlayerName = findViewById(R.id.etFirstPlayerName);
        MaterialButton btnStartPlaying = findViewById(R.id.btnStartPlaying);

        Set<String> allProfiles = prefs.getStringSet("ALL_PROFILES", new HashSet<>());

        if (allProfiles.isEmpty()) {
            layoutOnboarding.setVisibility(View.VISIBLE);
        } else {
            layoutOnboarding.setVisibility(View.GONE);
        }

        btnStartPlaying.setOnClickListener(v -> {
            String newName = etFirstPlayerName.getText().toString().trim().toUpperCase();

            if (!newName.isEmpty()) {
                soundManager.playClick();
                Set<String> newProfiles = new HashSet<>();
                newProfiles.add(newName);

                prefs.edit().putStringSet("ALL_PROFILES", newProfiles).apply();
                prefs.edit().putString("ACTIVE_PROFILE", newName).apply();

                updatePlayerBadge();

                layoutOnboarding.animate().alpha(0f).setDuration(500).withEndAction(() -> {
                    layoutOnboarding.setVisibility(View.GONE);
                });

            } else {
                Toast.makeText(MainActivity.this, getString(R.string.toast_enter_name), Toast.LENGTH_SHORT).show();
            }
        });

        mediaPlayer = MediaPlayer.create(this, R.raw.menu_music);
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
        }

        MaterialButton btnPlay = findViewById(R.id.btnPlay);
        MaterialButton btnDatabases = findViewById(R.id.btnDatabases);
        MaterialButton btnExit = findViewById(R.id.btnExit);
        com.google.android.material.imageview.ShapeableImageView btnAdmin = findViewById(R.id.btnAdmin);
        MaterialButton btnMusicToggle = findViewById(R.id.btnMusicToggle);
        MaterialCardView playerBadge = findViewById(R.id.playerBadgeCard);

        btnPlay.setOnClickListener(v -> {
            soundManager.playClick();
            showPlayOptionsDialog();
        });

        btnDatabases.setOnClickListener(v -> {
            soundManager.playClick();
            hushMusic();
            startActivity(new Intent(MainActivity.this, AlmanacActivity.class));
        });

        playerBadge.setOnClickListener(v -> {
            soundManager.playClick();
            hushMusic();
            startActivity(new Intent(MainActivity.this, ProfilesActivity.class));
        });

        btnAdmin.setOnClickListener(v -> {
            soundManager.playClick();
            showAdminPinDialog();
        });

        btnExit.setOnClickListener(v -> {
            soundManager.playClick();
            showExitDialog();
        });

        btnMusicToggle.setOnClickListener(v -> {
            soundManager.playClick();
            if (mediaPlayer == null) return;

            if (isMusicOn) {
                mediaPlayer.pause();
                btnMusicToggle.setIconResource(android.R.drawable.ic_lock_silent_mode);
                isMusicOn = false;
                soundManager.toggleSound(false);
            } else {
                mediaPlayer.start();
                btnMusicToggle.setIconResource(android.R.drawable.ic_lock_silent_mode_off);
                isMusicOn = true;
                soundManager.toggleSound(true);
            }
        });
    }

    private void showPlayOptionsDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_play_options, null);
        builder.setView(view);

        android.app.AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialog.show();

        View btnWrite = view.findViewById(R.id.btnWriteLetters);
        if (btnWrite != null) {
            btnWrite.setOnClickListener(v -> {
                SoundManager.getInstance(this).playClick();
                dialog.dismiss();
                hushMusic();
                startActivity(new Intent(MainActivity.this, WriteActivity.class));
            });
        }

        View btnScan = view.findViewById(R.id.btnScanWords);
        if (btnScan != null) {
            btnScan.setOnClickListener(v -> {
                SoundManager.getInstance(this).playClick();
                dialog.dismiss();
                hushMusic();
                startActivity(new Intent(MainActivity.this, PlayActivity.class));
            });
        }

        MaterialButton btnQuiz = view.findViewById(R.id.btnQuizMode);
        TextView tvQuizHint = view.findViewById(R.id.tvQuizUnlockHint);

        if (btnQuiz != null && tvQuizHint != null) {
            btnQuiz.setEnabled(false);

            // Optimized: Using managed ExecutorService instead of raw Threads
            databaseExecutor.execute(() -> {
                String player = prefs.getString("ACTIVE_PROFILE", "Default");
                int wordCount = AppDatabase.getInstance(MainActivity.this).wordDao().getAllWordsForProfile(player).size();

                runOnUiThread(() -> {
                    if (wordCount >= 10) {
                        btnQuiz.setEnabled(true);
                        btnQuiz.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.quiz_unlocked_purple)));
                        tvQuizHint.setVisibility(View.GONE);
                    } else {
                        btnQuiz.setEnabled(false);
                        btnQuiz.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.quiz_locked_grey)));
                        int itemsNeeded = 10 - wordCount;
                        tvQuizHint.setText(getString(R.string.tv_quiz_unlock_hint, itemsNeeded));
                        tvQuizHint.setVisibility(View.VISIBLE);
                    }
                });
            });

            btnQuiz.setOnClickListener(v -> {
                SoundManager.getInstance(this).playClick();
                dialog.dismiss();
                hushMusic();
                startActivity(new Intent(MainActivity.this, QuizActivity.class));
            });
        }

        View btnCancel = view.findViewById(R.id.btnCancelOptions);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> {
                SoundManager.getInstance(this).playClick();
                dialog.dismiss();
            });
        }
    }

    private void showExitDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_exit, null);
        builder.setView(view);
        android.app.AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialog.show();

        view.findViewById(R.id.btnCancelExit).setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            dialog.dismiss();
        });

        view.findViewById(R.id.btnConfirmExit).setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            dialog.dismiss();
            finishAffinity();
        });
    }

    private void showAdminPinDialog() {
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_admin_pin, null);
        android.app.AlertDialog pinDialog = new android.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (pinDialog.getWindow() != null) {
            pinDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText etPin = dialogView.findViewById(R.id.etAdminPin);

        dialogView.findViewById(R.id.btnCancelPin).setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            pinDialog.dismiss();
        });

        dialogView.findViewById(R.id.btnConfirmPin).setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            String enteredPin = etPin.getText().toString();

            String savedPin = prefs.getString("ADMIN_PIN", "1234");

            if (enteredPin.equals(savedPin)) {
                pinDialog.dismiss();
                hushMusic();
                startActivity(new Intent(MainActivity.this, AdminActivity.class));
            } else {
                Toast.makeText(this, getString(R.string.toast_incorrect_pin), Toast.LENGTH_SHORT).show();
                etPin.setText("");
            }
        });

        pinDialog.show();
    }

    private void hushMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    private void updatePlayerBadge() {
        String activePlayer = prefs.getString("ACTIVE_PROFILE", "");
        TextView tvName = findViewById(R.id.tvActivePlayerName);
        ImageView ivPic = findViewById(R.id.ivActivePlayerPic);

        if (activePlayer.isEmpty()) {
            tvName.setText(getString(R.string.tv_select_player));
            ivPic.setImageResource(R.drawable.admin_pic);
        } else {
            tvName.setText(activePlayer);
            String avatarPath = prefs.getString("AVATAR_" + activePlayer, null);

            // Optimized: Using Glide instead of raw URI parsing to prevent memory crashes
            if (avatarPath != null) {
                Glide.with(this)
                        .load(avatarPath)
                        .placeholder(R.drawable.admin_pic)
                        .error(R.drawable.admin_pic)
                        .into(ivPic);
            } else {
                ivPic.setImageResource(R.drawable.admin_pic);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePlayerBadge();

        if (isMusicOn && mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }

        Set<String> allProfiles = prefs.getStringSet("ALL_PROFILES", new HashSet<>());
        View layoutOnboarding = findViewById(R.id.layoutOnboarding);

        if (allProfiles.isEmpty()) {
            layoutOnboarding.setAlpha(1f);
            layoutOnboarding.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        // Optimized: Shutdown thread executor to free system resources
        if (!databaseExecutor.isShutdown()) {
            databaseExecutor.shutdown();
        }
    }
}