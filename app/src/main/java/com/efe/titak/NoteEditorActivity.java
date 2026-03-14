package com.efe.titak;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.efe.titak.database.DatabaseHelper;
import com.efe.titak.model.Note;
import com.google.android.material.textfield.TextInputEditText;

public class NoteEditorActivity extends AppCompatActivity {

    private TextInputEditText etTitle, etContent;
    private Button btnSave;
    private DatabaseHelper dbHelper;
    private Note existingNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        etTitle = findViewById(R.id.et_title);
        etContent = findViewById(R.id.et_content);
        btnSave = findViewById(R.id.btn_save_note);
        dbHelper = new DatabaseHelper(this);

        // Check if editing existing note
        if (getIntent().hasExtra("note")) {
            existingNote = (Note) getIntent().getSerializableExtra("note");
            if (existingNote != null) {
                etTitle.setText(existingNote.getTitle());
                etContent.setText(existingNote.getContent());
            }
        }

        btnSave.setOnClickListener(v -> saveNote());
    }

    private void saveNote() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        if (title.isEmpty()) {
            etTitle.setError("Başlık gerekli");
            return;
        }

        if (existingNote != null) {
            existingNote.setTitle(title);
            existingNote.setContent(content);
            dbHelper.updateNote(existingNote);
        } else {
            Note newNote = new Note();
            newNote.setTitle(title);
            newNote.setContent(content);
            dbHelper.addNote(newNote);
        }

        Toast.makeText(this, "Not kaydedildi", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }
}
