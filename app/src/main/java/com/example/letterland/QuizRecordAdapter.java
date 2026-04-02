package com.example.letterland;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class QuizRecordAdapter extends RecyclerView.Adapter<QuizRecordAdapter.RecordViewHolder> {
    private List<QuizRecord> records = new ArrayList<>();
    private SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());

    public void setRecords(List<QuizRecord> records) {
        this.records = records;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_quiz_record, parent, false);
        return new RecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordViewHolder holder, int position) {
        QuizRecord record = records.get(position);

        holder.tvPlayerName.setText("Player: " + record.playerName);
        holder.tvScore.setText("Score: " + record.score + " / " + record.totalItems);
        holder.tvDate.setText(sdf.format(new Date(record.timestamp)));

        // 🚀 NEW: Make the item clickable!
        // We pass the saved data over, and flag "IS_HISTORY" as true!
        holder.itemView.setOnClickListener(v -> {
            SoundManager.getInstance(v.getContext()).playClick();
            Intent intent = new Intent(v.getContext(), QuizResultActivity.class);

            if (record.correctAnswers != null && record.userAnswers != null) {
                intent.putStringArrayListExtra("CORRECT_ANSWERS", new ArrayList<>(record.correctAnswers));
                intent.putStringArrayListExtra("USER_ANSWERS", new ArrayList<>(record.userAnswers));
            }
            intent.putExtra("IS_HISTORY", true);

            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    class RecordViewHolder extends RecyclerView.ViewHolder {
        TextView tvPlayerName, tvScore, tvDate;

        public RecordViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPlayerName = itemView.findViewById(R.id.tvRecordPlayer);
            tvScore = itemView.findViewById(R.id.tvRecordScore);
            tvDate = itemView.findViewById(R.id.tvRecordDate);
        }
    }
}