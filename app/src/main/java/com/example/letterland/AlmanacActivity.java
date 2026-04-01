package com.example.letterland;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AlmanacActivity extends AppCompatActivity {

    private RecyclerView rvAlmanac;
    private WordAdapter adapter;

    @Override
    protected void onResume() {
        super.onResume();
        loadWordsFromDatabase();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_almanac);

        rvAlmanac = findViewById(R.id.rvAlmanac);
        ImageButton btnBack = findViewById(R.id.btnBackAlmanac);

        btnBack.setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            finish();
        });

        // 🌟 DYNAMIC DEVICE LAYOUT: Automatically calculates how many columns to show!
        // This completely prevents massive stretched images on tablets or in landscape mode.
        int screenWidthDp = getResources().getConfiguration().screenWidthDp;
        int columns = Math.max(2, screenWidthDp / 160); // Creates a new column for every 160dp of space

        rvAlmanac.setLayoutManager(new GridLayoutManager(this, columns));
        adapter = new WordAdapter(new ArrayList<>());
        rvAlmanac.setAdapter(adapter);

        loadWordsFromDatabase();
    }

    private void loadWordsFromDatabase() {
        new Thread(() -> {
            String player = getSharedPreferences("LetterLandMemory", MODE_PRIVATE).getString("ACTIVE_PROFILE", "Default");
            List<WordEntry> myWords = AppDatabase.getInstance(this).wordDao().getAllWordsForProfile(player);

            runOnUiThread(() -> adapter.updateData(myWords));
        }).start();
    }

    // ==========================================
    // 🌟 THE ALMANAC GRID ADAPTER
    // ==========================================
    private class WordAdapter extends RecyclerView.Adapter<WordAdapter.WordViewHolder> {
        private List<WordEntry> words;

        public WordAdapter(List<WordEntry> words) {
            this.words = words;
        }

        public void updateData(List<WordEntry> newWords) {
            this.words = newWords;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public WordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_word, parent, false);
            return new WordViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull WordViewHolder holder, int position) {
            WordEntry currentWord = words.get(position);
            holder.tvWord.setText(currentWord.word);

            try {
                holder.ivImage.setImageURI(Uri.parse(currentWord.imagePath));
            } catch (Exception e) {
                e.printStackTrace();
            }

            holder.itemView.setOnClickListener(v -> {
                SoundManager.getInstance(v.getContext()).playClick();
                android.content.Intent intent = new android.content.Intent(v.getContext(), WordDetailActivity.class);
                intent.putExtra("WORD_TEXT", currentWord.word);
                intent.putExtra("IMAGE_PATH", currentWord.imagePath);
                intent.putExtra("SOURCE_PAGE", "ALMANAC");
                v.getContext().startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return words.size();
        }

        class WordViewHolder extends RecyclerView.ViewHolder {
            ImageView ivImage;
            TextView tvWord;

            public WordViewHolder(@NonNull View itemView) {
                super(itemView);
                ivImage = itemView.findViewById(R.id.ivGalleryImage);
                tvWord = itemView.findViewById(R.id.tvGalleryWord);
            }
        }
    }
}