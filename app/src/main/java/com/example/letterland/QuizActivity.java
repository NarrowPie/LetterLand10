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
import com.bumptech.glide.request.RequestOptions;

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
                if (scanRunnable != null) scanHandler.removeCallbacks(scanRunnable);
            }
            @Override
            public void onDrawFinished() {
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
            List<WordEntry> allWords = AppDatabase.getInstance(this).wordDao().getAllWordsForProfile(player);

            runOnUiThread(() -> {
                // 🚀 CRITICAL FIX: Check if active
                if (isFinishing() || isDestroyed()) return;

                if (allWords.size() < 10) {
                    new AlertDialog.Builder(QuizActivity.this)
                            .setTitle("Not Enough Words")
                            .setMessage("Profile '" + player + "' only has " + allWords.size() + " words saved.\nYou need at least 10 items in your Almanac to play Quiz Mode!")
                            .setPositiveButton("OK", (dialog, which) -> finish())
                            .setCancelable(false)
                            .show();
                } else {
                    Collections.shuffle(allWords);
                    int limit = Math.min(allWords.size(), 10);
                    quizWords = allWords.subList(0, limit);
                    loadCurrentQuestion();
                }
            });
        }).start();
    }

    private void loadCurrentQuestion() {
        if (isFinishing() || isDestroyed()) return;

        try {
            resetCanvasAndText();
            tvProgress.setText((currentQuestionIndex + 1) + "/" + quizWords.size());

            WordEntry currentWord = quizWords.get(currentQuestionIndex);

            if (currentWord.imagePath != null && !currentWord.imagePath.isEmpty()) {

                RequestOptions options = new RequestOptions()
                        .fitCenter()
                        .dontTransform()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .disallowHardwareConfig()
                        .error(R.drawable.title_logo);

                Glide.with(this)
                        .load(currentWord.imagePath)
                        .apply(options)
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

            RequestOptions options = new RequestOptions()
                    .fitCenter()
                    .dontTransform()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .disallowHardwareConfig()
                    .error(R.drawable.title_logo);

            Glide.with(zoomDialog.getContext())
                    .load(currentWord.imagePath)
                    .apply(options)
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
        // 🚀 CRITICAL FIX: Ensure no delayed scans fire after moving to results!
        if (scanRunnable != null) scanHandler.removeCallbacks(scanRunnable);

        int score = 0;
        for (int i = 0; i < correctAnswers.size(); i++) {
            if (correctAnswers.get(i).equalsIgnoreCase(userAnswers.get(i))) {
                score++;
            }
        }

        String player = getSharedPreferences("LetterLandMemory", MODE_PRIVATE).getString("ACTIVE_PROFILE", "Default");
        long currentTime = System.currentTimeMillis();

        QuizRecord newRecord = new QuizRecord(player, score, correctAnswers.size(), currentTime);
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
        // 🚀 CRITICAL FIX: Abort if activity is closing to prevent crash
        if (isFinishing() || isDestroyed()) return;

        if (recognizer == null) {
            tvLiveText.setText("Loading AI...");
            return;
        }

        Ink ink = drawingView.getInk();
        if (ink.getStrokes().isEmpty()) return;

        recognizer.recognize(ink)
                .addOnSuccessListener(result -> {
                    if (isFinishing() || isDestroyed()) return; // Extra safety
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

    // 🚀 CRITICAL FIX: Clean up handlers when exiting to stop background crashes
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scanRunnable != null) {
            scanHandler.removeCallbacks(scanRunnable);
        }
        if (recognizer != null) {
            recognizer.close();
        }
    }
}