package com.mowtiie.flashback.ui.decks;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.mowtiie.flashback.data.entity.Note;
import com.mowtiie.flashback.databinding.ItemNoteBinding;

public class NoteAdapter extends ListAdapter<Note, NoteAdapter.NoteViewHolder> {

    public interface OnNoteClick {
        void onNoteClick(Note note);
    }

    private final OnNoteClick clickListener;

    public NoteAdapter(OnNoteClick clickListener) {
        super(DIFF);
        this.clickListener = clickListener;
    }

    private static final DiffUtil.ItemCallback<Note> DIFF =
            new DiffUtil.ItemCallback<Note>() {

                @Override
                public boolean areItemsTheSame(@NonNull Note a, @NonNull Note b) {
                    return a.id == b.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull Note a, @NonNull Note b) {
                    return a.front.equals(b.front)
                            && a.back.equals(b.back)
                            && a.reverseEnabled == b.reverseEnabled;
                }
            };

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNoteBinding binding = ItemNoteBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new NoteViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        holder.bind(getItem(position), clickListener);
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {

        private final ItemNoteBinding binding;

        NoteViewHolder(ItemNoteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Note note, OnNoteClick listener) {
            binding.noteFront.setText(note.front);
            binding.noteBack.setText(note.back);
            binding.noteReverseBadge.setVisibility(
                    note.reverseEnabled ? View.VISIBLE : View.GONE);
            binding.getRoot().setOnClickListener(v -> listener.onNoteClick(note));
        }
    }
}
