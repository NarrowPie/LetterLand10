package com.example.letterland;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {

    private Context context;
    private List<WordEntry> wordList;
    private OnItemCheckListener checkListener;

    // Interface to let the Activity know when to toggle the "Select All" box
    public interface OnItemCheckListener {
        void onItemCheckStateChanged();
    }

    public LogAdapter(Context context, List<WordEntry> wordList, OnItemCheckListener checkListener) {
        this.context = context;
        this.wordList = wordList;
        this.checkListener = checkListener;
    }

    public void updateData(List<WordEntry> newList) {
        this.wordList = newList;
        notifyDataSetChanged();
    }

    // 🚀 NEW: Check or Uncheck all items
    public void selectAll(boolean isSelected) {
        for (WordEntry word : wordList) {
            word.isSelected = isSelected;
        }
        notifyDataSetChanged();
    }

    // 🚀 NEW: Gather up all the items that have a checkmark
    public List<WordEntry> getSelectedWords() {
        List<WordEntry> selected = new ArrayList<>();
        for (WordEntry word : wordList) {
            if (word.isSelected) {
                selected.add(word);
            }
        }
        return selected;
    }

    // 🚀 NEW: Check if every single item is selected
    public boolean isAllSelected() {
        if (wordList == null || wordList.isEmpty()) return false;
        for (WordEntry word : wordList) {
            if (!word.isSelected) return false;
        }
        return true;
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        WordEntry wordEntry = wordList.get(position);

        holder.tvLogWord.setText(wordEntry.word);

        if (wordEntry.imagePath != null && !wordEntry.imagePath.isEmpty()) {
            holder.ivLogImage.setImageURI(Uri.parse(wordEntry.imagePath));
        } else {
            holder.ivLogImage.setImageResource(R.drawable.admin_pic);
        }

        String dateTimeString = "Unknown Date";
        if (wordEntry.imagePath != null) {
            try {
                File file = new File(Uri.parse(wordEntry.imagePath).getPath());
                if (file.exists()) {
                    long lastMod = file.lastModified();
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.US);
                    dateTimeString = sdf.format(new Date(lastMod));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        String details = "Collected by: " + wordEntry.profileName + "\nDate: " + dateTimeString;
        holder.tvLogDetails.setText(details);

        // Disconnect listener temporarily so we don't trigger false clicks while scrolling
        holder.cbSelectLog.setOnCheckedChangeListener(null);
        holder.cbSelectLog.setChecked(wordEntry.isSelected);

        // Reconnect listener for user clicks
        holder.cbSelectLog.setOnCheckedChangeListener((buttonView, isChecked) -> {
            wordEntry.isSelected = isChecked;
            checkListener.onItemCheckStateChanged();
        });
    }

    @Override
    public int getItemCount() {
        return wordList != null ? wordList.size() : 0;
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        ImageView ivLogImage;
        TextView tvLogWord, tvLogDetails;
        CheckBox cbSelectLog;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            ivLogImage = itemView.findViewById(R.id.ivLogImage);
            tvLogWord = itemView.findViewById(R.id.tvLogWord);
            tvLogDetails = itemView.findViewById(R.id.tvLogDetails);
            cbSelectLog = itemView.findViewById(R.id.cbSelectLog);
        }
    }
}