package com.efe.titak.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.efe.titak.R;
import com.efe.titak.model.Note;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {
    private Context context;
    private List<Note> notes;
    private OnNoteClickListener listener;

    public interface OnNoteClickListener {
        void onNoteClick(Note note);
        void onNoteDelete(Note note);
    }

    public NoteAdapter(Context context, List<Note> notes, OnNoteClickListener listener) {
        this.context = context;
        this.notes = notes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = notes.get(position);
        holder.tvTitle.setText(note.getTitle());
        holder.tvContent.setText(note.getContent());

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        holder.tvDate.setText(sdf.format(new Date(note.getTimestamp())));

        holder.itemView.setOnClickListener(v -> listener.onNoteClick(note));
        holder.btnDelete.setOnClickListener(v -> listener.onNoteDelete(note));
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvContent, tvDate;
        View btnDelete;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_note_title);
            tvContent = itemView.findViewById(R.id.tv_note_content);
            tvDate = itemView.findViewById(R.id.tv_note_date);
            btnDelete = itemView.findViewById(R.id.btn_delete_note);
        }
    }
}
