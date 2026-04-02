package com.example.letterland;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.mlkit.common.MlKitException;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.vision.digitalink.DigitalInkRecognition;
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel;
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier;
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer;
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions;
import com.google.mlkit.vision.digitalink.Ink;

public class WriteActivity extends AppCompatActivity {

    private DrawingView drawingView;
    private TextView tvLiveText;

    private DigitalInkRecognizer recognizer;

    private String pendingWord = "";
    private String currentlyDetectedWord = "";

    private final Handler scanHandler = new Handler(Looper.getMainLooper());
    private Runnable scanRunnable;

    private final ActivityResultLauncher<Void> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(),
            bitmap -> {
                if (bitmap != null && !pendingWord.isEmpty()) {
                    new Thread(() -> saveToAlmanac(pendingWord, bitmap)).start();
                } else {
                    Toast.makeText(this, "No picture taken", Toast.LENGTH_SHORT).show();
                    resetCanvasAndText();
                }
            }
    );

    // 🚀 REMOVED: Gallery launcher has been deleted to enforce Camera-only!

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write);

        drawingView = findViewById(R.id.drawingView);
        tvLiveText = findViewById(R.id.tvLiveText);
        MaterialButton btnClear = findViewById(R.id.btnClear);
        MaterialButton btnProceed = findViewById(R.id.btnProceed);
        ImageButton btnBack = findViewById(R.id.btnBackWrite);

        btnBack.setOnClickListener(v -> finish());

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
                        Toast.makeText(this, "Failed to download offline model.", Toast.LENGTH_SHORT).show();
                        tvLiveText.setText("Error");
                    });

        } catch (MlKitException e) {
            e.printStackTrace();
        }

        drawingView.setOnDrawListener(new DrawingView.OnDrawListener() {
            @Override
            public void onDrawStarted() {
                if (scanRunnable != null) {
                    scanHandler.removeCallbacks(scanRunnable);
                }
            }

            @Override
            public void onDrawFinished() {
                scanRunnable = () -> performScan();
                scanHandler.postDelayed(scanRunnable, 600);
            }
        });

        btnClear.setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            resetCanvasAndText();
        });

        btnProceed.setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();

            if (currentlyDetectedWord.isEmpty()) {
                Toast.makeText(this, "I couldn't read that! Try writing clearer.", Toast.LENGTH_SHORT).show();
            } else {
                checkWordDatabase(currentlyDetectedWord);
            }
        });
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
                        String cleanWord = result.getCandidates().get(0).getText().toUpperCase();
                        cleanWord = cleanWord.trim();

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

    private void resetCanvasAndText() {
        drawingView.clearCanvas();
        tvLiveText.setText("...");
        currentlyDetectedWord = "";
        if (scanRunnable != null) {
            scanHandler.removeCallbacks(scanRunnable);
        }
    }

    private void checkWordDatabase(String word) {
        new Thread(() -> {
            String player = getSharedPreferences("LetterLandMemory", MODE_PRIVATE).getString("ACTIVE_PROFILE", "Default");
            WordEntry savedWord = AppDatabase.getInstance(this).wordDao().findWordForProfile(word, player);

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;

                if (savedWord != null) {
                    android.content.Intent intent = new android.content.Intent(WriteActivity.this, WordDetailActivity.class);
                    intent.putExtra("WORD_TEXT", savedWord.word);
                    intent.putExtra("IMAGE_PATH", savedWord.imagePath);
                    intent.putExtra("SOURCE_PAGE", "WRITE");
                    startActivity(intent);
                    resetCanvasAndText();
                } else {
                    showNewWordDialog(word);
                }
            });
        }).start();
    }

    private void showNewWordDialog(String wordToSave) {
        if (isFinishing() || isDestroyed()) return;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_new_word, null);
        AlertDialog customDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (customDialog.getWindow() != null) {
            customDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        TextView tvDetected = dialogView.findViewById(R.id.tvDetectedWord);
        tvDetected.setText(wordToSave);

        dialogView.findViewById(R.id.btnDialogCamera).setOnClickListener(v1 -> {
            SoundManager.getInstance(this).playShutter();
            pendingWord = wordToSave;
            takePictureLauncher.launch(null);
            customDialog.dismiss();
        });

        // 🚀 REMOVED: Gallery Button Click Listener has been deleted!

        dialogView.findViewById(R.id.btnDialogLater).setOnClickListener(v1 -> {
            SoundManager.getInstance(this).playClick();
            customDialog.dismiss();
            resetCanvasAndText();
        });

        customDialog.setCancelable(false);
        customDialog.show();
    }

    private void saveToAlmanac(String word, Bitmap bitmap) {
        String fileName = "word_" + word + "_" + System.currentTimeMillis() + ".jpg";
        java.io.File file = new java.io.File(getExternalFilesDir(null), fileName);

        try (java.io.FileOutputStream out = new java.io.FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);

            String player = getSharedPreferences("LetterLandMemory", MODE_PRIVATE).getString("ACTIVE_PROFILE", "Default");
            WordEntry newEntry = new WordEntry(word, player, file.getAbsolutePath());
            AppDatabase.getInstance(this).wordDao().insert(newEntry);

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;

                Toast.makeText(this, word + " saved!", Toast.LENGTH_SHORT).show();
                pendingWord = "";

                android.content.Intent intent = new android.content.Intent(WriteActivity.this, WordDetailActivity.class);
                intent.putExtra("WORD_TEXT", word);
                intent.putExtra("IMAGE_PATH", file.getAbsolutePath());
                intent.putExtra("SOURCE_PAGE", "WRITE");
                startActivity(intent);

                resetCanvasAndText();
            });
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

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