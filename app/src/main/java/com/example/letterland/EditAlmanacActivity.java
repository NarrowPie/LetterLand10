package com.example.letterland;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class EditAlmanacActivity extends AppCompatActivity {

    private RecyclerView rvEditAlmanac;
    private EditWordAdapter adapter;

    // 🌟 TRACK FILTER STATE
    private boolean isShowingStarredOnly = false;

    @Override
    protected void onResume() {
        super.onResume();
        loadWordsFromDatabase();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_almanac);

        rvEditAlmanac = findViewById(R.id.rvAlmanac);
        ImageButton btnBack = findViewById(R.id.btnBackAlmanac);
        ImageButton btnFilterStarred = findViewById(R.id.btnFilterStarred); // 🌟 LINK BUTTON

        TextView tvTitle = findViewById(R.id.tvAlmanacTitle);
        if(tvTitle != null) {
            tvTitle.setText("SELECT QUIZ WORDS");
            tvTitle.setTextColor(0xFF3F51B5);
        }

        btnBack.setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            finish();
        });

        // 🌟 FILTER BUTTON LOGIC
        btnFilterStarred.setOnClickListener(v -> {
            SoundManager.getInstance(this).playClick();
            isShowingStarredOnly = !isShowingStarredOnly;

            if (isShowingStarredOnly) {
                btnFilterStarred.setImageResource(android.R.drawable.btn_star_big_on);
            } else {
                btnFilterStarred.setImageResource(android.R.drawable.btn_star_big_off);
            }

            loadWordsFromDatabase();
        });

        int screenWidthDp = getResources().getConfiguration().screenWidthDp;
        int columns = Math.max(2, screenWidthDp / 160);

        rvEditAlmanac.setLayoutManager(new GridLayoutManager(this, columns));
        adapter = new EditWordAdapter(new ArrayList<>());
        rvEditAlmanac.setAdapter(adapter);

        loadWordsFromDatabase();
    }

    private void loadWordsFromDatabase() {
        new Thread(() -> {
            String player = getSharedPreferences("LetterLandMemory", MODE_PRIVATE).getString("ACTIVE_PROFILE", "Default");

            List<WordEntry> myWords;
            if (isShowingStarredOnly) {
                // 🌟 GET ONLY STARRED WORDS
                myWords = AppDatabase.getInstance(this).wordDao().getStarredWordsForProfile(player);
            } else {
                // 🌟 GET ALL WORDS
                myWords = AppDatabase.getInstance(this).wordDao().getAllWordsForProfile(player);
            }

            runOnUiThread(() -> adapter.updateData(myWords));
        }).start();
    }

    private class EditWordAdapter extends RecyclerView.Adapter<EditWordAdapter.WordViewHolder> {
        private List<WordEntry> words;

        public EditWordAdapter(List<WordEntry> words) {
            this.words = words;
        }

        public void updateData(List<WordEntry> newWords) {
            this.words = newWords;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public WordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_edit_word, parent, false);
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

            if (currentWord.isStarred) {
                holder.ivStar.setImageResource(android.R.drawable.btn_star_big_on);
            } else {
                holder.ivStar.setImageResource(android.R.drawable.btn_star_big_off);
            }

            holder.ivStar.setOnClickListener(v -> {
                SoundManager.getInstance(v.getContext()).playClick();

                currentWord.isStarred = !currentWord.isStarred;

                new Thread(() -> {
                    AppDatabase.getInstance(v.getContext()).wordDao().update(currentWord);

                    runOnUiThread(() -> {
                        notifyItemChanged(position);
                        if(currentWord.isStarred) {
                            Toast.makeText(v.getContext(), currentWord.word + " added to Quiz!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(v.getContext(), currentWord.word + " removed from Quiz.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }).start();
            });

            holder.itemView.setOnClickListener(v -> {
                SoundManager.getInstance(v.getContext()).playClick();
                android.content.Intent intent = new android.content.Intent(v.getContext(), WordDetailActivity.class);
                intent.putExtra("WORD_TEXT", currentWord.word);
                intent.putExtra("IMAGE_PATH", currentWord.imagePath);
                intent.putExtra("SOURCE_PAGE", "EDIT_ALMANAC");
                v.getContext().startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return words.size();
        }

        class WordViewHolder extends RecyclerView.ViewHolder {
            ImageView ivImage;
            ImageView ivStar;
            TextView tvWord;

            public WordViewHolder(@NonNull View itemView) {
                super(itemView);
                ivImage = itemView.findViewById(R.id.ivGalleryImage);
                tvWord = itemView.findViewById(R.id.tvGalleryWord);
                ivStar = itemView.findViewById(R.id.ivStar);
            }
        }
    }
}