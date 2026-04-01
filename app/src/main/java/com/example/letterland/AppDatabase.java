package com.example.letterland;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

// 🚀 Changed version to 5 and added QuizRecord.class!
@Database(entities = {WordEntry.class, LogEntry.class, QuizRecord.class}, version = 5, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract WordDao wordDao();
    public abstract LogDao logDao();
    public abstract QuizRecordDao quizRecordDao(); // 🚀 NEW DAO

    private static AppDatabase instance;

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "letterland_database")
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build();
        }
        return instance;
    }
}