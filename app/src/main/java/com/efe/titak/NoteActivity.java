package com.efe.titak;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.efe.titak.adapter.NoteAdapter;
import com.efe.titak.database.DatabaseHelper;
import com.efe.titak.model.Note;

import java.util.List;

public class NoteActivity extends AppCompatActivity implements NoteAdapter.OnNoteClickListener {

    private RecyclerView rvNotes;
    private NoteAdapter adapter;
    private DatabaseHelper dbHelper;
    private List<Note> notes;
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        rvNotes = findViewById(R.id.rv_notes);
        emptyView = findViewById(R.id.empty_view);

        dbHelper = new DatabaseHelper(this);

        findViewById(R.id.fab_add_note).setOnClickListener(v -> openNoteEditor(null));

        // Setup RecyclerView
        rvNotes.setLayoutManager(new LinearLayoutManager(this));

        loadNotes();
    }

    private void loadNotes() {
        notes = dbHelper.getAllNotes();
        if (adapter == null) {
            adapter = new NoteAdapter(this, notes, this);
            rvNotes.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }

        if (notes.isEmpty()) {
            rvNotes.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            rvNotes.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    private void openNoteEditor(Note note) {
        // For simplicity, using an alert dialog to edit/add notes
        // Ideally, this would be a separate Activity
        Intent intent = new Intent(this, NoteEditorActivity.class);
        if (note != null) {
            intent.putExtra("note", note);
        }
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            loadNotes();
        }
    }

    @Override
    public void onNoteClick(Note note) {
        openNoteEditor(note);
    }

    @Override
    public void onNoteDelete(Note note) {
        new AlertDialog.Builder(this)
                .setTitle("Sil")
                .setMessage("Bu notu silmek istediğinize emin misiniz?")
                .setPositiveButton("Evet", (dialog, which) -> {
                    dbHelper.deleteNote(note.getId());
                    loadNotes();
                    Toast.makeText(this, "Not silindi", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hayır", null)
                .show();
    }
}
