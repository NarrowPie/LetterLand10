package com.example.letterland;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import com.google.mlkit.common.MlKitException;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.vision.digitalink.DigitalInkRecognition;
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel;
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier;
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer;
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions;
import com.google.mlkit.vision.digitalink.Ink;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuizActivity extends AppCompatActivity {

    private DrawingView drawingView;
    private TextView tvLiveText, tvProgress;
    private ImageView ivQuizImage;

    private DigitalInkRecognizer recognizer;

    private final Handler scanHandler = new Handler(Looper.getMainLooper());
    private Runnable scanRunnable;

    private List<WordEntry> quizWords = new ArrayList<>();
    private ArrayList<String> correctAnswers = new ArrayList<>();
    private ArrayList<String> userAnswers = new ArrayList<>();

    private int currentQuestionIndex = 0;
    private String currentlyDetectedWord = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        drawingView = findViewById(R.id.quizDrawingView);
        tvLiveText = findViewById(R.id.tvLiveText);
        tvProgress = findViewById(R.id.tvQuizProgress);
        ivQuizImage = findViewById(R.id.ivQuizImage);

        findViewById(R.id.btnBackQuiz).setOnClickListener(v -> finish());

        findViewById(R.id.btnQuizClear).setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            resetCanvasAndText();
        });

        findViewById(R.id.cardQuizImage).setOnClickListener(v -> {
            if (!quizWords.isEmpty()) {
                showZoomedImageDialog();
            }
        });

        tvLiveText.setText("Loading AI...");

        try {
            DigitalInkRecognitionModelIdentifier modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag("en-US");
            DigitalInkRecognitionModel model = DigitalInkRecognitionModel.builder(modelIdentifier).build();

            RemoteModelManager manager = RemoteModelManager.getInstance();
            manager.download(model, new DownloadConditions.Builder().build())
                    .addOnSuccessListener(aVoid -> {
                        recognizer = DigitalInkRecognition.getClient(
                                DigitalInkRecognizerOptions.builder(model).build());

                        tvLiveText.setText("...");

                        if (!drawingView.getInk().getStrokes().isEmpty()) {
                            performScan();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to load Handwriting AI.", Toast.LENGTH_SHORT).show();
                        tvLiveText.setText("Error");
                    });

        } catch (MlKitException e) {
            e.printStackTrace();
        }

        drawingView.setOnDrawListener(new DrawingView.OnDrawListener() {
            @Override
            public void onDrawStarted() {
                SoundManager.getInstance(QuizActivity.this).startScratchSound();
                if (scanRunnable != null) scanHandler.removeCallbacks(scanRunnable);
            }
            @Override
            public void onDrawFinished() {
                SoundManager.getInstance(QuizActivity.this).stopScratchSound();
                scanRunnable = () -> performScan();
                scanHandler.postDelayed(scanRunnable, 400);
            }
        });

        findViewById(R.id.btnQuizProceed).setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();

            if (currentlyDetectedWord.isEmpty() || currentlyDetectedWord.equals("...")) {
                Toast.makeText(this, "Please write an answer first!", Toast.LENGTH_SHORT).show();
                return;
            }

            showCustomConfirmDialog();
        });

        new Thread(() -> {
            String player = getSharedPreferences("LetterLandMemory", MODE_PRIVATE).getString("ACTIVE_PROFILE", "Default");

            List<WordEntry> quizReadyWords = AppDatabase.getInstance(this).wordDao().getStarredWordsForProfile(player);

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;

                if (quizReadyWords.size() < 10) {
                    showLockedDialog();
                } else {
                    Collections.shuffle(quizReadyWords);
                    int limit = Math.min(quizReadyWords.size(), 10);
                    quizWords = quizReadyWords.subList(0, limit);
                    loadCurrentQuestion();
                }
            });
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SoundManager.getInstance(this).startBackgroundMusic();
    }

    @Override
    protected void onPause() {
        super.onPause();
        SoundManager.getInstance(this).pauseBackgroundMusic();
        SoundManager.getInstance(this).stopScratchSound();
    }

    private void showLockedDialog() {
        if (isFinishing() || isDestroyed()) return;

        Dialog lockedDialog = new Dialog(this);
        lockedDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        lockedDialog.setContentView(R.layout.dialog_mode_locked);
        lockedDialog.setCancelable(false);

        if (lockedDialog.getWindow() != null) {
            lockedDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        lockedDialog.findViewById(R.id.btnDialogOk).setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            lockedDialog.dismiss();

            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        lockedDialog.show();
    }

    private void loadCurrentQuestion() {
        if (isFinishing() || isDestroyed()) return;

        try {
            resetCanvasAndText();
            tvProgress.setText((currentQuestionIndex + 1) + "/" + quizWords.size());

            WordEntry currentWord = quizWords.get(currentQuestionIndex);

            if (currentWord.imagePath != null && !currentWord.imagePath.isEmpty()) {

                // 🚀 THE FINAL FIX: Force Glide to down-scale the massive camera image
                // to 250x250 pixels before sending it to the GPU.
                Glide.with(this)
                        .load(currentWord.imagePath)
                        .override(250, 250) // Stops the GPU out-of-memory crash!
                        .centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .error(R.drawable.title_logo)
                        .into(ivQuizImage);

            } else {
                ivQuizImage.setImageDrawable(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showZoomedImageDialog() {
        if (isFinishing() || isDestroyed()) return;

        SoundManager.getInstance(this).playClick();

        Dialog zoomDialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        zoomDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        zoomDialog.setContentView(R.layout.dialog_zoom_image);

        if (zoomDialog.getWindow() != null) {
            zoomDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        ImageView ivZoomed = zoomDialog.findViewById(R.id.ivZoomedImage);

        WordEntry currentWord = quizWords.get(currentQuestionIndex);

        if (currentWord.imagePath != null && !currentWord.imagePath.isEmpty()) {

            // 🚀 Force a safe size for the full-screen popup as well
            Glide.with(zoomDialog.getContext())
                    .load(currentWord.imagePath)
                    .override(800, 800)
                    .fitCenter()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .error(R.drawable.title_logo)
                    .into(ivZoomed);
        }

        zoomDialog.findViewById(R.id.rootZoomLayout).setOnClickListener(v1 -> {
            SoundManager.getInstance(this).playClick();
            zoomDialog.dismiss();
        });

        zoomDialog.show();
    }

    private void showCustomConfirmDialog() {
        if (isFinishing() || isDestroyed()) return;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_quiz_confirm, null);
        AlertDialog confirmDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (confirmDialog.getWindow() != null) {
            confirmDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        TextView tvMessage = dialogView.findViewById(R.id.tvConfirmMessage);
        tvMessage.setText("Is '" + currentlyDetectedWord + "' your final answer?");

        dialogView.findViewById(R.id.btnCancelConfirm).setOnClickListener(v1 -> {
            SoundManager.getInstance(this).playClick();
            confirmDialog.dismiss();
        });

        dialogView.findViewById(R.id.btnYesConfirm).setOnClickListener(v1 -> {
            SoundManager.getInstance(this).playClick();
            confirmDialog.dismiss();

            correctAnswers.add(quizWords.get(currentQuestionIndex).word);
            userAnswers.add(currentlyDetectedWord);

            currentQuestionIndex++;

            if (currentQuestionIndex < quizWords.size()) {
                loadCurrentQuestion();
            } else {
                goToResults();
            }
        });

        confirmDialog.show();
    }

    private void goToResults() {
        if (scanRunnable != null) scanHandler.removeCallbacks(scanRunnable);

        int score = 0;
        for (int i = 0; i < correctAnswers.size(); i++) {
            if (correctAnswers.get(i).equalsIgnoreCase(userAnswers.get(i))) {
                score++;
            }
        }

        String player = getSharedPreferences("LetterLandMemory", MODE_PRIVATE).getString("ACTIVE_PROFILE", "Default");
        long currentTime = System.currentTimeMillis();

        QuizRecord newRecord = new QuizRecord(player, score, correctAnswers.size(), currentTime, correctAnswers, userAnswers);
        AppDatabase.getInstance(this).quizRecordDao().insertRecord(newRecord);

        Intent intent = new Intent(this, com.example.letterland.QuizResultActivity.class);
        intent.putStringArrayListExtra("CORRECT_ANSWERS", correctAnswers);
        intent.putStringArrayListExtra("USER_ANSWERS", userAnswers);
        startActivity(intent);
        finish();
    }

    private void resetCanvasAndText() {
        drawingView.clearCanvas();

        if (recognizer == null) {
            tvLiveText.setText("Loading AI...");
        } else {
            tvLiveText.setText("...");
        }

        currentlyDetectedWord = "";
        if (scanRunnable != null) scanHandler.removeCallbacks(scanRunnable);
    }

    private void performScan() {
        if (isFinishing() || isDestroyed()) return;

        if (recognizer == null) {
            tvLiveText.setText("Loading AI...");
            return;
        }

        Ink ink = drawingView.getInk();
        if (ink.getStrokes().isEmpty()) return;

        recognizer.recognize(ink)
                .addOnSuccessListener(result -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (!result.getCandidates().isEmpty()) {
                        String cleanWord = result.getCandidates().get(0).getText().toUpperCase().trim();

                        if (cleanWord.length() > 10) {
                            cleanWord = cleanWord.substring(0, 10);
                        }

                        currentlyDetectedWord = cleanWord;
                        tvLiveText.setText(cleanWord);
                    }
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) return;
                    tvLiveText.setText("...");
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SoundManager.getInstance(this).stopScratchSound();
        if (scanRunnable != null) {
            scanHandler.removeCallbacks(scanRunnable);
        }
        if (recognizer != null) {
            recognizer.close();
        }
    }
}