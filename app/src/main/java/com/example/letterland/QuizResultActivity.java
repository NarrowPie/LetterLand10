package com.example.letterland;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class QuizResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_result);

        ArrayList<String> correctAnswers = getIntent().getStringArrayListExtra("CORRECT_ANSWERS");
        ArrayList<String> userAnswers = getIntent().getStringArrayListExtra("USER_ANSWERS");

        // 🚀 NEW: Detect if we are viewing this from the Admin Panel Logs
        boolean isHistory = getIntent().getBooleanExtra("IS_HISTORY", false);

        RecyclerView rvResults = findViewById(R.id.rvQuizResults);
        TextView tvFinalScore = findViewById(R.id.tvFinalScore);
        View btnResultHome = findViewById(R.id.btnResultHome);
        View btnResultPlayAgain = findViewById(R.id.btnResultPlayAgain);

        if (correctAnswers != null && userAnswers != null && rvResults != null) {
            rvResults.setLayoutManager(new LinearLayoutManager(this));
            QuizResultAdapter adapter = new QuizResultAdapter(correctAnswers, userAnswers);
            rvResults.setAdapter(adapter);

            int score = 0;
            for (int i = 0; i < correctAnswers.size(); i++) {
                if (correctAnswers.get(i).equalsIgnoreCase(userAnswers.get(i))) {
                    score++;
                }
            }
            if (tvFinalScore != null) tvFinalScore.setText(score + "/" + correctAnswers.size());
        }

        // 🚀 NEW: If it's a historical record, hide "Play Again"
        if (isHistory && btnResultPlayAgain != null) {
            btnResultPlayAgain.setVisibility(View.GONE);
        }

        if (btnResultHome != null) {
            btnResultHome.setOnClickListener(v -> {
                SoundManager.getInstance(this).playClick();
                finish();
            });
        }

        if (btnResultPlayAgain != null) {
            btnResultPlayAgain.setOnClickListener(v -> {
                SoundManager.getInstance(this).playClick();
                startActivity(new android.content.Intent(this, QuizActivity.class));
                finish();
            });
        }
    }
}