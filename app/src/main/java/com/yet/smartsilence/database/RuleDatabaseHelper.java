package com.yet.smartsilence.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import android.database.Cursor;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import com.yet.smartsilence.database.models.RuleModel;

public class RuleDatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "smart_silence.db";
    public static final int DATABASE_VERSION = 1;

    public static final String TABLE_RULES = "rules";

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_ACTIVE = "active";
    public static final String COLUMN_LOCATION_NAME = "locationName";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_RADIUS = "radius";
    public static final String COLUMN_TIME_START = "timeStart";
    public static final String COLUMN_TIME_END = "timeEnd";
    public static final String COLUMN_DAYS_OF_WEEK = "daysOfWeek";

    private static final String CREATE_TABLE_RULES =
            "CREATE TABLE " + TABLE_RULES + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_TYPE + " TEXT NOT NULL, " +
                    COLUMN_ACTIVE + " INTEGER NOT NULL, " +
                    COLUMN_LOCATION_NAME + " TEXT, " +
                    COLUMN_LATITUDE + " REAL, " +
                    COLUMN_LONGITUDE + " REAL, " +
                    COLUMN_RADIUS + " INTEGER, " +
                    COLUMN_TIME_START + " TEXT, " +
                    COLUMN_TIME_END + " TEXT, " +
                    COLUMN_DAYS_OF_WEEK + " TEXT" +
                    ");";

    public RuleDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_RULES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RULES);
        onCreate(db);
    }

    public List<RuleModel> getActiveTimeRules() {
        List<RuleModel> rules = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_RULES,
                null,
                COLUMN_TYPE + "=? AND " + COLUMN_ACTIVE + "=?",
                new String[]{"time", "1"},
                null,
                null,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                RuleModel rule = new RuleModel();
                rule.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                rule.setType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE)));
                rule.setActive(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ACTIVE)) == 1);
                rule.setTimeStart(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME_START)));
                rule.setTimeEnd(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME_END)));
                rule.setDaysOfWeek(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DAYS_OF_WEEK)));
                rules.add(rule);
            } while (cursor.moveToNext());
            cursor.close();
        }

        return rules;
    }

    // function to add example rule to the data base
    public void insertTestTimeRule() {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_TYPE, "time");
        values.put(COLUMN_ACTIVE, 1);
        values.put(COLUMN_TIME_START, getTimePlusMinutes(2)); // בעוד 2 דקות
        values.put(COLUMN_TIME_END, getTimePlusMinutes(10));  // יסתיים בעוד 10 דקות
        values.put(COLUMN_DAYS_OF_WEEK, getTodayName()); // רק היום

        db.insert(TABLE_RULES, null, values);
    }

    private String getTimePlusMinutes(int minutes) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, minutes);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(cal.getTime());
    }

    private String getTodayName() {
        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK); // 1=Sunday, 2=Monday...
        return days[day - 1];
    }

    // function to log all the rules in the data base
    public void printAllRules() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_RULES, null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String type = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE));
                boolean active = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ACTIVE)) == 1;
                String timeStart = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME_START));
                String timeEnd = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME_END));
                String days = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DAYS_OF_WEEK));
                String location = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOCATION_NAME));
                double lat = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUDE));
                double lon = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE));
                int radius = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_RADIUS));

                Log.d("SmartSilence", "-----------------------------");
                Log.d("SmartSilence", "ID: " + id);
                Log.d("SmartSilence", "Type: " + type);
                Log.d("SmartSilence", "Active: " + active);
                Log.d("SmartSilence", "Time: " + timeStart + " - " + timeEnd);
                Log.d("SmartSilence", "Days: " + days);
                Log.d("SmartSilence", "Location: " + location);
                Log.d("SmartSilence", "Lat/Lon: " + lat + "/" + lon);
                Log.d("SmartSilence", "Radius: " + radius);
            } while (cursor.moveToNext());
        } else {
            Log.d("SmartSilence", "אין חוקים במאגר.");
        }

        cursor.close();
    }

    public void deleteAllRules() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_RULES, null, null);
    }

}
