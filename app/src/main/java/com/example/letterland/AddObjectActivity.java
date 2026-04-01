package com.example.letterland;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class AddObjectActivity extends AppCompatActivity {

    private EditText etNewWord;
    private ImageView ivSelectedImage;
    private Bitmap selectedBitmap = null;

    // 📸 Handles Taking a Picture
    private final ActivityResultLauncher<Void> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(),
            bitmap -> {
                if (bitmap != null) {
                    selectedBitmap = bitmap;
                    ivSelectedImage.setImageBitmap(bitmap);
                } else {
                    Toast.makeText(this, "No picture taken", Toast.LENGTH_SHORT).show();
                }
            }
    );

    // 🖼️ Handles Picking from Gallery
    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                        selectedBitmap = bitmap;
                        ivSelectedImage.setImageBitmap(bitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_object);

        etNewWord = findViewById(R.id.etNewWord);
        ivSelectedImage = findViewById(R.id.ivSelectedImage);

        MaterialButton btnCamera = findViewById(R.id.btnCamera);
        MaterialButton btnGallery = findViewById(R.id.btnGallery);
        MaterialButton btnSave = findViewById(R.id.btnSaveObject);
        MaterialButton btnBack = findViewById(R.id.btnAddObjectBack);

        btnBack.setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            finish();
        });

        btnCamera.setOnClickListener(v -> {
            SoundManager.getInstance(this).playShutter(); // Assuming you have playShutter()
            takePictureLauncher.launch(null);
        });

        btnGallery.setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            pickImageLauncher.launch("image/*");
        });

        btnSave.setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            saveObjectToDatabase();
        });
    }

    private void saveObjectToDatabase() {
        String word = etNewWord.getText().toString().trim().toUpperCase();

        if (word.isEmpty()) {
            Toast.makeText(this, "Please type a word name!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedBitmap == null) {
            Toast.makeText(this, "Please add an image!", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            // Get the current student's profile so the word is saved for them
            String player = getSharedPreferences("LetterLandMemory", MODE_PRIVATE).getString("ACTIVE_PROFILE", "Default");

            // Check if this word already exists to prevent duplicates
            WordEntry existingWord = AppDatabase.getInstance(this).wordDao().findWordForProfile(word, player);
            if (existingWord != null) {
                runOnUiThread(() -> Toast.makeText(this, "This word already exists in the Almanac!", Toast.LENGTH_SHORT).show());
                return;
            }

            // Save the image safely to the app's internal storage
            String fileName = "word_" + word + "_" + System.currentTimeMillis() + ".jpg";
            File file = new File(getExternalFilesDir(null), fileName);

            try (FileOutputStream out = new FileOutputStream(file)) {
                selectedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);

                // Insert the new word and image path into the Database!
                WordEntry newEntry = new WordEntry(word, player, file.getAbsolutePath());
                AppDatabase.getInstance(this).wordDao().insert(newEntry);

                runOnUiThread(() -> {
                    Toast.makeText(this, word + " successfully added to Almanac!", Toast.LENGTH_LONG).show();
                    finish(); // Go back to the Admin Panel automatically
                });

            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Failed to save image.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}