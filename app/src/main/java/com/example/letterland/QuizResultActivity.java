package com.example.letterland;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;

public class QuizResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_result);

        TextView tvEmoji = findViewById(R.id.tvEmoji);
        TextView tvFinalScore = findViewById(R.id.tvFinalScore);
        RecyclerView rvResults = findViewById(R.id.rvResults);
        View btnTryAgain = findViewById(R.id.btnTryAgain);

        findViewById(R.id.btnMainMenu).setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            finish();
        });

        if (btnTryAgain != null) {
            btnTryAgain.setOnClickListener(v -> {
                SoundManager.getInstance(this).playClick();
                startActivity(new android.content.Intent(this, QuizActivity.class));
                finish();
            });
        }

        ArrayList<String> correctAnswers = getIntent().getStringArrayListExtra("CORRECT_ANSWERS");
        ArrayList<String> userAnswers = getIntent().getStringArrayListExtra("USER_ANSWERS");
        boolean isHistory = getIntent().getBooleanExtra("IS_HISTORY", false);

        // 🚀 THE FIX: Hide "Play Again" if we are viewing past logs from the Admin Panel
        if (isHistory && btnTryAgain != null) {
            btnTryAgain.setVisibility(View.GONE);
        }

        if (correctAnswers == null || userAnswers == null) return;

        // CALCULATE SCORE
        int score = 0;
        for (int i = 0; i < correctAnswers.size(); i++) {
            if (correctAnswers.get(i).equalsIgnoreCase(userAnswers.get(i))) {
                score++;
            }
        }

        // UPDATE UI DYNAMICALLY
        tvFinalScore.setText(score + "/" + correctAnswers.size());

        float percentage = (float) score / correctAnswers.size();

        if (percentage >= 0.7f) {
            tvEmoji.setText("😊");
            tvFinalScore.setTextColor(Color.parseColor("#4CAF50")); // Green
        } else if (percentage >= 0.4f) {
            tvEmoji.setText("😐");
            tvFinalScore.setTextColor(Color.parseColor("#FF9800")); // Orange
        } else {
            tvEmoji.setText("☹️");
            tvFinalScore.setTextColor(Color.parseColor("#F44336")); // Red
        }

        // SETUP LIST
        rvResults.setLayoutManager(new LinearLayoutManager(this));
        rvResults.setAdapter(new ResultAdapter(correctAnswers, userAnswers));
    }

    // 🚀 YOUR INNER ADAPTER: (Works perfectly, no extra file needed!)
    private class ResultAdapter extends RecyclerView.Adapter<ResultAdapter.ResultViewHolder> {
        private final ArrayList<String> correct;
        private final ArrayList<String> user;

        public ResultAdapter(ArrayList<String> correct, ArrayList<String> user) {
            this.correct = correct;
            this.user = user;
        }

        @NonNull
        @Override
        public ResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_quiz_result, parent, false);
            return new ResultViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ResultViewHolder holder, int position) {
            String correctWord = correct.get(position);
            String userWord = user.get(position);

            holder.tvCorrectWord.setText(correctWord);
            holder.tvUserAnswer.setText(userWord);

            // Background color logic
            if (correctWord.equalsIgnoreCase(userWord)) {
                holder.cardItem.setCardBackgroundColor(Color.parseColor("#4CAF50")); // Green
            } else {
                holder.cardItem.setCardBackgroundColor(Color.parseColor("#EF5350")); // Red
            }
        }

        @Override
        public int getItemCount() {
            return correct.size();
        }

        class ResultViewHolder extends RecyclerView.ViewHolder {
            TextView tvCorrectWord, tvUserAnswer;
            MaterialCardView cardItem;

            public ResultViewHolder(@NonNull View itemView) {
                super(itemView);
                tvCorrectWord = itemView.findViewById(R.id.tvCorrectWord);
                tvUserAnswer = itemView.findViewById(R.id.tvUserAnswer);
                cardItem = itemView.findViewById(R.id.cardResultItem);
            }
        }
    }
}