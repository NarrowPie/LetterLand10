package com.example.letterland;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

        // 🚀 HARDWARE ACCELERATION FIX: Forces the graphics unit to render the image properly inside the rounded MaterialCardView
        ivQuizImage.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

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

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private void loadCurrentQuestion() {
        try {
            resetCanvasAndText();
            tvProgress.setText((currentQuestionIndex + 1) + "/" + quizWords.size());

            WordEntry currentWord = quizWords.get(currentQuestionIndex);
            ivQuizImage.setImageBitmap(null);

            if (currentWord.imagePath != null && !currentWord.imagePath.isEmpty()) {
                new Thread(() -> {
                    try {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        BitmapFactory.decodeFile(currentWord.imagePath, options);

                        options.inSampleSize = calculateInSampleSize(options, 300, 300);
                        options.inJustDecodeBounds = false;

                        // 🚀 MEMORY FIX: RGB_565 uses 50% less memory than ARGB_8888, preventing OpenGL black-texture limits
                        options.inPreferredConfig = Bitmap.Config.RGB_565;

                        Bitmap scaledBitmap = BitmapFactory.decodeFile(currentWord.imagePath, options);

                        runOnUiThread(() -> {
                            if (scaledBitmap != null) {
                                ivQuizImage.setImageBitmap(scaledBitmap);
                            } else {
                                Toast.makeText(QuizActivity.this, "Image file corrupted. Please re-add it in Almanac.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showZoomedImageDialog() {
        SoundManager.getInstance(this).playClick();

        Dialog zoomDialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        zoomDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        zoomDialog.setContentView(R.layout.dialog_zoom_image);

        if (zoomDialog.getWindow() != null) {
            zoomDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        ImageView ivZoomed = zoomDialog.findViewById(R.id.ivZoomedImage);

        // 🚀 HARDWARE ACCELERATION FIX for the Zoomed Dialog
        ivZoomed.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        ivZoomed.setImageBitmap(null);

        WordEntry currentWord = quizWords.get(currentQuestionIndex);

        if (currentWord.imagePath != null && !currentWord.imagePath.isEmpty()) {
            new Thread(() -> {
                try {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(currentWord.imagePath, options);

                    options.inSampleSize = calculateInSampleSize(options, 1024, 1024);
                    options.inJustDecodeBounds = false;

                    // 🚀 MEMORY FIX: Halve the texture size to prevent black screens
                    options.inPreferredConfig = Bitmap.Config.RGB_565;

                    Bitmap scaledBitmap = BitmapFactory.decodeFile(currentWord.imagePath, options);

                    runOnUiThread(() -> {
                        if (scaledBitmap != null) {
                            ivZoomed.setImageBitmap(scaledBitmap);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }

        zoomDialog.findViewById(R.id.rootZoomLayout).setOnClickListener(v1 -> {
            SoundManager.getInstance(this).playClick();
            zoomDialog.dismiss();
        });

        zoomDialog.show();
    }

    private void showCustomConfirmDialog() {
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
        if (recognizer == null) {
            tvLiveText.setText("Loading AI...");
            return;
        }

        Ink ink = drawingView.getInk();
        if (ink.getStrokes().isEmpty()) return;

        recognizer.recognize(ink)
                .addOnSuccessListener(result -> {
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
                    tvLiveText.setText("...");
                });
    }
}