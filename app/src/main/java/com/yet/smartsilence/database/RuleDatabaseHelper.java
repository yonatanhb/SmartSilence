package com.yet.smartsilence.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.yet.smartsilence.database.models.RuleModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class RuleDatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME    = "smart_silence.db";
    public static final int    DATABASE_VERSION = 2;  // גרסה 2 עם daysMask

    public static final String TABLE_RULES      = "rules";
    public static final String COLUMN_ID        = "id";
    public static final String COLUMN_TYPE      = "type";
    public static final String COLUMN_ACTIVE    = "active";
    public static final String COLUMN_LOCATION_NAME = "locationName";
    public static final String COLUMN_LATITUDE  = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_RADIUS    = "radius";
    public static final String COLUMN_TIME_START= "timeStart";
    public static final String COLUMN_TIME_END  = "timeEnd";
    public static final String COLUMN_DAYS_MASK = "daysMask";

    private static final String CREATE_TABLE_RULES =
            "CREATE TABLE " + TABLE_RULES + " (" +
                    COLUMN_ID         + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_TYPE       + " TEXT NOT NULL, " +
                    COLUMN_ACTIVE     + " INTEGER NOT NULL, " +
                    COLUMN_LOCATION_NAME + " TEXT, " +
                    COLUMN_LATITUDE   + " REAL, " +
                    COLUMN_LONGITUDE  + " REAL, " +
                    COLUMN_RADIUS     + " INTEGER, " +
                    COLUMN_TIME_START + " TEXT, " +
                    COLUMN_TIME_END   + " TEXT, " +
                    COLUMN_DAYS_MASK  + " INTEGER NOT NULL DEFAULT 0" +
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
        // פשוט מוחק ובונה מחדש, כי הנתונים הישנים לא רלוונטיים
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RULES);
        onCreate(db);
    }

    /** מחזיר את כל חוקי הזמן הפעילים */
    public List<RuleModel> getActiveTimeRules() {
        List<RuleModel> rules = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_RULES,
                null,
                COLUMN_TYPE + "=? AND " + COLUMN_ACTIVE + "=?",
                new String[]{"time", "1"},
                null, null, null
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                RuleModel rule = new RuleModel();
                rule.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                rule.setType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE)));
                rule.setActive(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ACTIVE)) == 1);
                rule.setTimeStart(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME_START)));
                rule.setTimeEnd(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME_END)));
                rule.setDaysMask(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DAYS_MASK)));
                rules.add(rule);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return rules;
    }

    /** מוסיף כלל לדוגמה (רק היום, עוד דקה עד 10 דקות) */
    public void insertTestTimeRule() {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_TYPE, "time");
        values.put(COLUMN_ACTIVE, 1);
        values.put(COLUMN_TIME_START, getTimePlusMinutes(2));
        values.put(COLUMN_TIME_END, getTimePlusMinutes(10));
        values.put(COLUMN_DAYS_MASK, getTodayMask());

        db.insert(TABLE_RULES, null, values);
    }

    private String getTimePlusMinutes(int minutes) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, minutes);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(cal.getTime());
    }

    /** מחזיר ב-bitmask את היום הנוכחי (0=ראשון ... 6=שבת) */
    private int getTodayMask() {
        int idx = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1;
        return 1 << idx;
    }

    /** מדפיס ללוג את כל החוקים כולל daysMask */
    public void printAllRules() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_RULES, null);

        if (cursor.moveToFirst()) {
            do {
                int    id     = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String type   = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE));
                boolean active= cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ACTIVE)) == 1;
                String start  = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME_START));
                String end    = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME_END));
                int    mask   = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DAYS_MASK));
                String loc    = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOCATION_NAME));
                double lat    = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUDE));
                double lon    = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE));
                int    radius = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_RADIUS));

                Log.d("SmartSilence", "ID: "        + id);
                Log.d("SmartSilence", "Type: "      + type);
                Log.d("SmartSilence", "Active: "    + active);
                Log.d("SmartSilence", "Time: "      + start + "–" + end);
                Log.d("SmartSilence", "DaysMask: "  + Integer.toBinaryString(mask));
                Log.d("SmartSilence", "Location: "  + loc);
                Log.d("SmartSilence", "Lat/Lon: "   + lat + "/" + lon);
                Log.d("SmartSilence", "Radius: "    + radius);
            } while (cursor.moveToNext());
        } else {
            Log.d("SmartSilence", "אין חוקים במאגר.");
        }
        cursor.close();
    }

    /** מוחק את כל החוקים */
    public void deleteAllRules() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_RULES, null, null);
    }
}
