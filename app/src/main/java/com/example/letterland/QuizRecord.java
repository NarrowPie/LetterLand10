package com.example.letterland;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "quiz_record_table")
public class QuizRecord {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String studentName;
    public int score;
    public int totalItems;
    public long timestamp;

    public QuizRecord(String studentName, int score, int totalItems, long timestamp) {
        this.studentName = studentName;
        this.score = score;
        this.totalItems = totalItems;
        this.timestamp = timestamp;
    }
}