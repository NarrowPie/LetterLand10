package com.example.letterland;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class WordDetailActivity extends AppCompatActivity {

    private TextToSpeech textToSpeech;
    private String wordText;
    private String imagePath;
    private TextView tvWord;
    private ImageView ivPicture;

    // ==========================================
    // 📸 CAMERA & GALLERY LAUNCHERS FOR NEW IMAGE
    // ==========================================
    private final ActivityResultLauncher<Void> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(),
            bitmap -> {
                if (bitmap != null) {
                    new Thread(() -> updateImageInDatabase(bitmap)).start();
                } else {
                    Toast.makeText(this, "No picture taken", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    new Thread(() -> {
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                            updateImageInDatabase(bitmap);
                        } catch (Exception e) {
                            e.printStackTrace();
                            runOnUiThread(() -> Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show());
                        }
                    }).start();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_detail);

        // 🌟 SAFE VIEW BINDINGS
        ivPicture = findViewById(R.id.ivDetailPicture);
        tvWord = findViewById(R.id.tvDetailWord);
        View btnBack = findViewById(R.id.btnDetailBack);
        View btnSpeak = findViewById(R.id.btnSpeak);

        // ALMANAC BUTTONS
        View layoutEditButtons = findViewById(R.id.layoutEditButtons);
        View btnRename = findViewById(R.id.btnDetailRename);
        View btnNewImage = findViewById(R.id.btnDetailNewImage);
        View btnDelete = findViewById(R.id.btnDetailDelete);

        // SCANNER BUTTON
        View btnScanAgain = findViewById(R.id.btnScanAgain);

        // WRITE MODE BUTTONS
        View llWriteControls = findViewById(R.id.llWriteControls);
        View btnWriteDelete = findViewById(R.id.btnWriteDelete);
        View btnWriteAgain = findViewById(R.id.btnWriteAgain);

        wordText = getIntent().getStringExtra("WORD_TEXT");
        imagePath = getIntent().getStringExtra("IMAGE_PATH");
        String sourcePage = getIntent().getStringExtra("SOURCE_PAGE");

        if (wordText != null) tvWord.setText(wordText);
        if (imagePath != null) ivPicture.setImageURI(Uri.parse(imagePath));

        // ==========================================
        // 🌟 SMART VISIBILITY LOGIC
        // ==========================================
        if ("ALMANAC".equals(sourcePage)) {
            if (btnScanAgain != null) btnScanAgain.setVisibility(View.GONE);
            if (llWriteControls != null) llWriteControls.setVisibility(View.GONE);
            if (layoutEditButtons != null) layoutEditButtons.setVisibility(View.VISIBLE);
            if (btnSpeak != null) btnSpeak.setVisibility(View.VISIBLE);

        } else if ("SCANNER".equals(sourcePage)) {
            if (layoutEditButtons != null) layoutEditButtons.setVisibility(View.GONE);
            if (llWriteControls != null) llWriteControls.setVisibility(View.GONE);
            if (btnScanAgain != null) btnScanAgain.setVisibility(View.VISIBLE);

        } else if ("WRITE".equals(sourcePage)) {
            if (layoutEditButtons != null) layoutEditButtons.setVisibility(View.GONE);
            if (btnScanAgain != null) btnScanAgain.setVisibility(View.GONE);
            if (llWriteControls != null) llWriteControls.setVisibility(View.VISIBLE);
            if (btnSpeak != null) btnSpeak.setVisibility(View.VISIBLE);

        } else {
            if (btnScanAgain != null) btnScanAgain.setVisibility(View.GONE);
            if (layoutEditButtons != null) layoutEditButtons.setVisibility(View.GONE);
            if (llWriteControls != null) llWriteControls.setVisibility(View.GONE);
        }

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.US);
            }
        });

        if (btnSpeak != null) {
            btnSpeak.setOnClickListener(v -> {
                if (wordText != null) {
                    textToSpeech.speak(wordText, TextToSpeech.QUEUE_FLUSH, null, null);
                }
            });
        }

        // ==========================================
        // ✨ RENAME & IMAGE LOGIC
        // ==========================================
        if (btnNewImage != null) {
            btnNewImage.setOnClickListener(v -> {
                SoundManager.getInstance(this).playClick();
                new AlertDialog.Builder(this)
                        .setTitle("Change Picture")
                        .setMessage("How do you want to add the new picture for '" + wordText + "'?")
                        .setPositiveButton("Camera", (dialog, which) -> {
                            SoundManager.getInstance(this).playShutter();
                            takePictureLauncher.launch(null);
                        })
                        .setNeutralButton("Gallery", (dialog, which) -> pickImageLauncher.launch("image/*"))
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        if (btnRename != null) {
            btnRename.setOnClickListener(v -> {
                EditText input = new EditText(this);
                input.setText(wordText);

                new AlertDialog.Builder(this)
                        .setTitle("Rename Object")
                        .setView(input)
                        .setPositiveButton("Save", (dialog, which) -> {
                            String newName = input.getText().toString().toUpperCase().trim();

                            if (!newName.isEmpty() && !newName.equals(wordText)) {
                                new Thread(() -> {
                                    String player = getSharedPreferences("LetterLandMemory", MODE_PRIVATE).getString("ACTIVE_PROFILE", "Default");

                                    // 🚀 NEW LOGIC: Check if the new name already exists BEFORE renaming!
                                    WordEntry checkExisting = AppDatabase.getInstance(this).wordDao().findWordForProfile(newName, player);

                                    if (checkExisting != null) {
                                        // It already exists! Block the action and warn the user.
                                        runOnUiThread(() -> {
                                            Toast.makeText(this, "The word '" + newName + "' already exists! Please choose a different name.", Toast.LENGTH_LONG).show();
                                        });
                                    } else {
                                        // It's safe to rename! Proceed as normal.
                                        WordEntry oldEntry = AppDatabase.getInstance(this).wordDao().findWordForProfile(wordText, player);

                                        if (oldEntry != null) {
                                            WordEntry newEntry = new WordEntry(newName, oldEntry.profileName, oldEntry.imagePath);
                                            AppDatabase.getInstance(this).wordDao().insert(newEntry);
                                            AppDatabase.getInstance(this).wordDao().delete(oldEntry);

                                            runOnUiThread(() -> {
                                                wordText = newName;
                                                tvWord.setText(wordText);
                                                Toast.makeText(this, "Renamed to " + newName, Toast.LENGTH_SHORT).show();
                                            });
                                        }
                                    }
                                }).start();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        // ==========================================
        // 🌟 ROUTE BOTH DELETE BUTTONS TO THE CUSTOM DIALOG
        // ==========================================
        if (btnDelete != null) btnDelete.setOnClickListener(v -> showCustomDeleteDialog());
        if (btnWriteDelete != null) btnWriteDelete.setOnClickListener(v -> showCustomDeleteDialog());

        // Navigation
        if (btnScanAgain != null) btnScanAgain.setOnClickListener(v -> finish());
        if (btnWriteAgain != null) btnWriteAgain.setOnClickListener(v -> finish());
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    // ==========================================
    // 🌟 CUSTOM DELETE DIALOG FUNCTION
    // ==========================================
    private void showCustomDeleteDialog() {
        SoundManager.getInstance(this).playClick();

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete, null);
        AlertDialog deleteDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (deleteDialog.getWindow() != null) {
            deleteDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        dialogView.findViewById(R.id.btnCancelDelete).setOnClickListener(v1 -> {
            SoundManager.getInstance(this).playClick();
            deleteDialog.dismiss();
        });

        dialogView.findViewById(R.id.btnConfirmDelete).setOnClickListener(v1 -> {
            SoundManager.getInstance(this).playClick();
            deleteDialog.dismiss();
            deleteWordFromDatabase();
        });

        deleteDialog.show();
    }

    // ==========================================
    // 🗑️ DELETE DATA FUNCTION
    // ==========================================
    private void deleteWordFromDatabase() {
        new Thread(() -> {
            String player = getSharedPreferences("LetterLandMemory", MODE_PRIVATE).getString("ACTIVE_PROFILE", "Default");

            WordEntry entry = AppDatabase.getInstance(this).wordDao().findWordForProfile(wordText, player);
            if (entry != null) {
                AppDatabase.getInstance(this).wordDao().delete(entry);
                // 🚀 Optional: If you want to track manual deletions inside the detail screen too:
                AppDatabase.getInstance(this).logDao().insertLog(new LogEntry("DELETED WORD", "Word: " + entry.word + " (Profile: " + entry.profileName + ")", System.currentTimeMillis()));
            }

            if (imagePath != null) {
                java.io.File file = new java.io.File(imagePath);
                if (file.exists()) {
                    file.delete();
                }
            }

            runOnUiThread(() -> {
                android.widget.Toast.makeText(this, wordText + " deleted!", android.widget.Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }

    // ==========================================
    // 💾 SAVE NEW IMAGE TO DATABASE FUNCTION
    // ==========================================
    private void updateImageInDatabase(Bitmap bitmap) {
        String fileName = "word_" + wordText + "_" + System.currentTimeMillis() + ".jpg";
        java.io.File file = new java.io.File(getExternalFilesDir(null), fileName);

        try (java.io.FileOutputStream out = new java.io.FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            String newImagePath = file.getAbsolutePath();

            String player = getSharedPreferences("LetterLandMemory", MODE_PRIVATE).getString("ACTIVE_PROFILE", "Default");

            WordEntry updatedEntry = new WordEntry(wordText, player, newImagePath);
            AppDatabase.getInstance(this).wordDao().insert(updatedEntry);

            runOnUiThread(() -> {
                imagePath = newImagePath;
                ivPicture.setImageURI(Uri.parse(imagePath));
                Toast.makeText(this, "Picture Updated!", Toast.LENGTH_SHORT).show();
            });

        } catch (java.io.IOException e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(this, "Error saving picture!", Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}