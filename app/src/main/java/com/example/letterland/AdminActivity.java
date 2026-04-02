package com.example.letterland;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class AdminActivity extends AppCompatActivity {

    private MaterialButton btnAdminBack;
    private MaterialButton btnAdminUserLogs;
    private MaterialButton btnAdminQuizRecord;
    private MaterialButton btnAdminAddObject;
    private MaterialButton btnAdminDeletedLogs;
    private MaterialButton btnAdminEditAlmanac; // 🚀 NEW BUTTON
    private MaterialButton btnResetPin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // Link UI
        btnAdminBack = findViewById(R.id.btnAdminBack);
        btnAdminUserLogs = findViewById(R.id.btnAdminUserLogs);
        btnAdminQuizRecord = findViewById(R.id.btnAdminQuizRecord);
        btnAdminAddObject = findViewById(R.id.btnAdminAddObject);
        btnAdminDeletedLogs = findViewById(R.id.btnAdminDeletedLogs);
        btnAdminEditAlmanac = findViewById(R.id.btnAdminEditAlmanac); // 🚀 LINK NEW BUTTON
        btnResetPin = findViewById(R.id.btnResetPin);

        // Back Button
        btnAdminBack.setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            finish();
        });

        // 1. OPEN HISTORY LOGS SCREEN
        btnAdminUserLogs.setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            startActivity(new Intent(AdminActivity.this, UserLogsActivity.class));
        });

        // 2. OPEN QUIZ RECORD SCREEN
        btnAdminQuizRecord.setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            startActivity(new Intent(AdminActivity.this, QuizRecordActivity.class));
        });

        // 3. OPEN ADD OBJECT SCREEN
        btnAdminAddObject.setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            startActivity(new Intent(AdminActivity.this, AddObjectActivity.class));
        });

        // 4. OPEN DELETED LOGS SCREEN
        btnAdminDeletedLogs.setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            startActivity(new Intent(AdminActivity.this, DeletedLogsActivity.class));
        });

        // 5. OPEN EDIT ALMANAC SCREEN (STAR SYSTEM)
        btnAdminEditAlmanac.setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            startActivity(new Intent(AdminActivity.this, EditAlmanacActivity.class));
        });

        // 6. RESET PIN LOGIC
        btnResetPin.setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            showResetPinDialog();
        });
    }

    private void showResetPinDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_reset_pin, null);
        AlertDialog pinDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (pinDialog.getWindow() != null) {
            pinDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText etCurrentPin = dialogView.findViewById(R.id.etCurrentPin);
        EditText etNewPin = dialogView.findViewById(R.id.etNewPin);

        dialogView.findViewById(R.id.btnCancelReset).setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            pinDialog.dismiss();
        });

        dialogView.findViewById(R.id.btnConfirmReset).setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();

            String currentEntered = etCurrentPin.getText().toString();
            String newPinEntered = etNewPin.getText().toString();

            SharedPreferences prefs = getSharedPreferences("LetterLandMemory", MODE_PRIVATE);
            String savedPin = prefs.getString("ADMIN_PIN", "1234");

            if (!currentEntered.equals(savedPin)) {
                Toast.makeText(this, "Current PIN is incorrect!", Toast.LENGTH_SHORT).show();
            } else if (newPinEntered.length() < 4) {
                Toast.makeText(this, "New PIN must be 4 digits!", Toast.LENGTH_SHORT).show();
            } else {
                prefs.edit().putString("ADMIN_PIN", newPinEntered).apply();
                Toast.makeText(this, "PIN successfully updated!", Toast.LENGTH_SHORT).show();
                pinDialog.dismiss();
            }
        });

        pinDialog.show();
    }
}