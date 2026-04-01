package com.example.letterland;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class QuizRecordAdapter extends RecyclerView.Adapter<QuizRecordAdapter.ViewHolder> {

    private final List<QuizRecord> records;

    public QuizRecordAdapter(List<QuizRecord> records) {
        this.records = records;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_quiz_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        QuizRecord record = records.get(position);

        holder.tvStudentName.setText("Student: " + record.studentName);
        holder.tvScore.setText("Score: " + record.score + "/" + record.totalItems);

        // Color code the score: Green if passed (>= 7), Orange if okay (>= 4), Red if failed
        if (record.score >= 7) {
            holder.tvScore.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
        } else if (record.score >= 4) {
            holder.tvScore.setTextColor(android.graphics.Color.parseColor("#FF9800"));
        } else {
            holder.tvScore.setTextColor(android.graphics.Color.parseColor("#F44336"));
        }

        // Convert the raw millisecond time into a readable format
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());
        holder.tvTimestamp.setText(sdf.format(new Date(record.timestamp)));
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName, tvScore, tvTimestamp;

        public ViewHolder(View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvScore = itemView.findViewById(R.id.tvScore);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }
    }
}