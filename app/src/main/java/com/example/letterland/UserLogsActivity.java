package com.example.letterland;

import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class UserLogsActivity extends AppCompatActivity {

    private RecyclerView rvLogsList;
    private LogAdapter logAdapter;
    private CheckBox cbSelectAll;
    private MaterialButton btnDeleteSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_logs);

        MaterialButton btnBack = findViewById(R.id.btnLogsBack);
        rvLogsList = findViewById(R.id.rvUserLogsList);
        cbSelectAll = findViewById(R.id.cbSelectAll);
        btnDeleteSelected = findViewById(R.id.btnDeleteSelected);

        btnBack.setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            finish();
        });

        rvLogsList.setLayoutManager(new LinearLayoutManager(this));

        // Initialize Adapter with the check listener to toggle "Select All" automatically
        logAdapter = new LogAdapter(this, new ArrayList<>(), () -> {
            updateSelectAllCheckboxState();
        });
        rvLogsList.setAdapter(logAdapter);

        loadDataFromDatabase();

        // 🚀 SELECT ALL CLICK LOGIC
        cbSelectAll.setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            boolean isChecked = cbSelectAll.isChecked();
            logAdapter.selectAll(isChecked);
        });

        // 🚀 DELETE BUTTON CLICK LOGIC
        btnDeleteSelected.setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            List<WordEntry> itemsToDelete = logAdapter.getSelectedWords();

            if (itemsToDelete.isEmpty()) {
                Toast.makeText(this, "No items selected!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show confirmation dialog before deleting
            new AlertDialog.Builder(this)
                    .setTitle("Delete Logs")
                    .setMessage("Are you sure you want to delete " + itemsToDelete.size() + " item(s)? This will permanently remove them from the game and almanac.")
                    .setPositiveButton("DELETE", (dialog, which) -> {
                        deleteSelectedItems(itemsToDelete);
                    })
                    .setNegativeButton("CANCEL", null)
                    .show();
        });
    }

    private void loadDataFromDatabase() {
        AppDatabase db = AppDatabase.getInstance(this);
        // This query now automatically returns the list sorted with the newest at the top!
        List<WordEntry> collectedWords = db.wordDao().getAllWords();

        runOnUiThread(() -> {
            logAdapter.updateData(collectedWords);
            updateSelectAllCheckboxState(); // Reset select all box based on new list
        });
    }

    private void updateSelectAllCheckboxState() {
        cbSelectAll.setChecked(logAdapter.isAllSelected() && logAdapter.getItemCount() > 0);
    }

    private void deleteSelectedItems(List<WordEntry> itemsToDelete) {
        AppDatabase db = AppDatabase.getInstance(this);

        // Loop through all selected items and delete them
        for (WordEntry word : itemsToDelete) {

            // 1. Delete from App Database
            db.wordDao().delete(word);

            // 🚀🚀🚀 NEW: SAVE THIS DELETION TO THE HISTORY LOG! 🚀🚀🚀
            db.logDao().insertLog(new LogEntry("DELETED WORD", "Word: " + word.word + " (Profile: " + word.profileName + ")", System.currentTimeMillis()));

            // 2. Delete the actual picture file from phone storage to save space!
            if (word.imagePath != null) {
                try {
                    java.io.File file = new java.io.File(android.net.Uri.parse(word.imagePath).getPath());
                    if (file.exists()) {
                        file.delete();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        Toast.makeText(this, itemsToDelete.size() + " item(s) deleted!", Toast.LENGTH_SHORT).show();

        // Uncheck the select all box
        cbSelectAll.setChecked(false);

        // Reload the list to remove the deleted items from the screen
        loadDataFromDatabase();
    }
}