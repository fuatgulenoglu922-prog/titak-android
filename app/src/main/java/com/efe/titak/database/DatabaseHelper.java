package com.efe.titak.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.efe.titak.model.APIKey;
import com.efe.titak.model.Note;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "notes.db";
    private static final int DATABASE_VERSION = 2;

    private static final String TABLE_NOTES = "notes";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_CONTENT = "content";
    private static final String COLUMN_TIMESTAMP = "timestamp";

    private static final String TABLE_API_KEYS = "api_keys";
    private static final String COLUMN_API_ID = "id";
    private static final String COLUMN_API_NAME = "name";
    private static final String COLUMN_API_KEY = "api_key";
    private static final String COLUMN_API_BASE_URL = "base_url";
    private static final String COLUMN_API_MODEL = "model";
    private static final String COLUMN_API_CREATED_AT = "created_at";
    private static final String COLUMN_API_IS_ACTIVE = "is_active";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE_NOTES = "CREATE TABLE " + TABLE_NOTES + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TITLE + " TEXT,"
                + COLUMN_CONTENT + " TEXT,"
                + COLUMN_TIMESTAMP + " INTEGER" + ")";
        db.execSQL(CREATE_TABLE_NOTES);

        String CREATE_TABLE_API_KEYS = "CREATE TABLE " + TABLE_API_KEYS + "("
                + COLUMN_API_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_API_NAME + " TEXT,"
                + COLUMN_API_KEY + " TEXT,"
                + COLUMN_API_BASE_URL + " TEXT,"
                + COLUMN_API_MODEL + " TEXT,"
                + COLUMN_API_CREATED_AT + " INTEGER,"
                + COLUMN_API_IS_ACTIVE + " INTEGER" + ")";
        db.execSQL(CREATE_TABLE_API_KEYS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE " + TABLE_API_KEYS + " ("
                    + COLUMN_API_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_API_NAME + " TEXT,"
                    + COLUMN_API_KEY + " TEXT,"
                    + COLUMN_API_BASE_URL + " TEXT,"
                    + COLUMN_API_MODEL + " TEXT,"
                    + COLUMN_API_CREATED_AT + " INTEGER,"
                    + COLUMN_API_IS_ACTIVE + " INTEGER" + ")");
        } else {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_API_KEYS);
            onCreate(db);
        }
    }

    public long addNote(Note note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, note.getTitle());
        values.put(COLUMN_CONTENT, note.getContent());
        values.put(COLUMN_TIMESTAMP, System.currentTimeMillis());
        long id = db.insert(TABLE_NOTES, null, values);
        db.close();
        return id;
    }

    public Note getNote(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NOTES, new String[]{COLUMN_ID, COLUMN_TITLE, COLUMN_CONTENT, COLUMN_TIMESTAMP},
                COLUMN_ID + "=?", new String[]{String.valueOf(id)}, null, null, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                Note note = new Note(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getLong(3)
                );
                cursor.close();
                return note;
            }
            cursor.close();
        }
        return null;
    }

    public List<Note> getAllNotes() {
        List<Note> notesList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_NOTES + " ORDER BY " + COLUMN_TIMESTAMP + " DESC";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Note note = new Note(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getLong(3)
                );
                notesList.add(note);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return notesList;
    }

    public int updateNote(Note note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, note.getTitle());
        values.put(COLUMN_CONTENT, note.getContent());
        values.put(COLUMN_TIMESTAMP, System.currentTimeMillis());

        return db.update(TABLE_NOTES, values, COLUMN_ID + " = ?", new String[]{String.valueOf(note.getId())});
    }

    public void deleteNote(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOTES, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public long addAPIKey(APIKey apiKey) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_API_NAME, apiKey.getName());
        values.put(COLUMN_API_KEY, apiKey.getApiKey());
        values.put(COLUMN_API_BASE_URL, apiKey.getBaseUrl());
        values.put(COLUMN_API_MODEL, apiKey.getModel());
        values.put(COLUMN_API_CREATED_AT, System.currentTimeMillis());
        values.put(COLUMN_API_IS_ACTIVE, apiKey.isActive() ? 1 : 0);
        long id = db.insert(TABLE_API_KEYS, null, values);
        db.close();
        return id;
    }

    public APIKey getAPIKey(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_API_KEYS, new String[]{COLUMN_API_ID, COLUMN_API_NAME, COLUMN_API_KEY, COLUMN_API_BASE_URL, COLUMN_API_MODEL, COLUMN_API_CREATED_AT, COLUMN_API_IS_ACTIVE},
                COLUMN_API_ID + "=?", new String[]{String.valueOf(id)}, null, null, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                APIKey apiKey = new APIKey(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getLong(5),
                        cursor.getInt(6) == 1
                );
                cursor.close();
                return apiKey;
            }
            cursor.close();
        }
        return null;
    }

    public List<APIKey> getAllAPIKeys() {
        List<APIKey> apiKeysList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_API_KEYS + " ORDER BY " + COLUMN_API_CREATED_AT + " DESC";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                APIKey apiKey = new APIKey(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getLong(5),
                        cursor.getInt(6) == 1
                );
                apiKeysList.add(apiKey);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return apiKeysList;
    }

    public int updateAPIKey(APIKey apiKey) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_API_NAME, apiKey.getName());
        values.put(COLUMN_API_KEY, apiKey.getApiKey());
        values.put(COLUMN_API_BASE_URL, apiKey.getBaseUrl());
        values.put(COLUMN_API_MODEL, apiKey.getModel());
        values.put(COLUMN_API_IS_ACTIVE, apiKey.isActive() ? 1 : 0);

        return db.update(TABLE_API_KEYS, values, COLUMN_API_ID + " = ?", new String[]{String.valueOf(apiKey.getId())});
    }

    public void deleteAPIKey(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_API_KEYS, COLUMN_API_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void setActiveAPIKey(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_API_IS_ACTIVE, 1);
        db.update(TABLE_API_KEYS, values, COLUMN_API_ID + " = ?", new String[]{String.valueOf(id)});
        
        ContentValues inactiveValues = new ContentValues();
        inactiveValues.put(COLUMN_API_IS_ACTIVE, 0);
        db.update(TABLE_API_KEYS, inactiveValues, COLUMN_API_ID + " != ?", new String[]{String.valueOf(id)});
        db.close();
    }
}
