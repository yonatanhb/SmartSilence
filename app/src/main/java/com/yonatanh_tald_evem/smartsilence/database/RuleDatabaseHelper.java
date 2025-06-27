package com.yonatanh_tald_evem.smartsilence.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.yonatanh_tald_evem.smartsilence.R;
import com.yonatanh_tald_evem.smartsilence.database.models.RuleModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class RuleDatabaseHelper extends SQLiteOpenHelper {
    // Database configuration
    public static final String DATABASE_NAME    = "smart_silence.db";
    public static final int    DATABASE_VERSION = 4;

    // Table and column names
    public static final String TABLE_RULES      = "rules";
    public static final String COLUMN_ID        = "id";
    public static final String COLUMN_TYPE      = "type";
    public static final String COLUMN_RULE_NAME  = "name";
    public static final String COLUMN_ACTIVE    = "active";
    public static final String COLUMN_LOCATION_NAME = "locationName";
    public static final String COLUMN_LATITUDE  = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_RADIUS    = "radius";
    public static final String COLUMN_TIME_START= "timeStart";
    public static final String COLUMN_TIME_END  = "timeEnd";
    public static final String COLUMN_DAYS_MASK = "daysMask";
    public static final String COLUMN_NOW_ACTIVE = "nowActive";

    private final Context context;


    // SQL to create the rules table
    private static final String CREATE_TABLE_RULES =
            "CREATE TABLE " + TABLE_RULES + " (" +
                    COLUMN_ID         + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_TYPE       + " TEXT NOT NULL, " +
                    COLUMN_RULE_NAME  + " TEXT, " +
                    COLUMN_ACTIVE     + " INTEGER NOT NULL, " +
                    COLUMN_LOCATION_NAME + " TEXT, " +
                    COLUMN_LATITUDE   + " REAL, " +
                    COLUMN_LONGITUDE  + " REAL, " +
                    COLUMN_RADIUS     + " INTEGER, " +
                    COLUMN_TIME_START + " TEXT, " +
                    COLUMN_TIME_END   + " TEXT, " +
                    COLUMN_DAYS_MASK  + " INTEGER NOT NULL DEFAULT 0, " +
                    COLUMN_NOW_ACTIVE + " INTEGER NOT NULL DEFAULT 0" +
                    ");";

    public RuleDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    // Called only when the DB is first created
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_RULES);
    }

    // Called when upgrading DB version (drop and recreate table)
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RULES);
        onCreate(db);
    }

    // Inserts a user-defined time-based rule
    public void insertManualTimeRule(RuleModel rule) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_TYPE, "time");
        values.put(COLUMN_RULE_NAME, rule.getRuleName());
        values.put(COLUMN_ACTIVE, rule.isActive() ? 1 : 0);
        values.put(COLUMN_TIME_START, rule.getTimeStart());
        values.put(COLUMN_TIME_END, rule.getTimeEnd());
        values.put(COLUMN_DAYS_MASK, rule.getDaysMask());

        // Set irrelevant location fields to null
        values.putNull(COLUMN_LOCATION_NAME);
        values.putNull(COLUMN_LATITUDE);
        values.putNull(COLUMN_LONGITUDE);
        values.putNull(COLUMN_RADIUS);

        db.insert(TABLE_RULES, null, values);
    }

    // Inserts a location-based rule
    public void insertLocationRule(String ruleName, String locationName, double latitude, double longitude, int radius, boolean active) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_TYPE, "location");
        values.put(COLUMN_RULE_NAME, ruleName);
        values.put(COLUMN_ACTIVE, active ? 1 : 0);
        values.put(COLUMN_LOCATION_NAME, locationName);
        values.put(COLUMN_LATITUDE, latitude);
        values.put(COLUMN_LONGITUDE, longitude);
        values.put(COLUMN_RADIUS, radius);

        values.putNull(COLUMN_TIME_START);
        values.putNull(COLUMN_TIME_END);
        values.put(COLUMN_DAYS_MASK, 0);

        db.insert(TABLE_RULES, null, values);
    }

    // Returns all rules (time + location)
    public List<RuleModel> getAllRules() {
        List<RuleModel> rules = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_RULES,
                null,
                null,
                null,
                null, null, null
        );

        if (cursor.moveToFirst()) {
            do {
                RuleModel rule = new RuleModel();
                rule.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                rule.setRuleName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RULE_NAME)));
                rule.setType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE)));
                rule.setActive(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ACTIVE)) == 1);
                rule.setTimeStart(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME_START)));
                rule.setTimeEnd(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME_END)));
                rule.setDaysMask(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DAYS_MASK)));
                rule.setLocationName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOCATION_NAME)));
                rule.setLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUDE)));
                rule.setLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE)));
                rule.setRadius(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_RADIUS)));
                rules.add(rule);
            } while (cursor.moveToNext());
            cursor.close();
        }

        return rules;
    }

    // Retrieves a rule by its ID
    public RuleModel getRuleById(int id) {
        SQLiteDatabase db = getReadableDatabase();
        RuleModel rule = null;

        Cursor cursor = db.query(
                TABLE_RULES,
                null,
                COLUMN_ID + "=?",
                new String[]{String.valueOf(id)},
                null, null, null, "1"
        );

        if (cursor.moveToFirst()) {
            rule = new RuleModel();
            rule.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
            rule.setRuleName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RULE_NAME)));
            rule.setType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE)));
            rule.setActive(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ACTIVE)) == 1);
            rule.setTimeStart(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME_START)));
            rule.setTimeEnd(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME_END)));
            rule.setDaysMask(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DAYS_MASK)));
            rule.setLocationName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOCATION_NAME)));
            rule.setLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUDE)));
            rule.setLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE)));
            rule.setRadius(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_RADIUS)));
            cursor.close();
        }

        db.close();
        return rule;
    }

    // Returns all active time-based rules
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

        if (cursor.moveToFirst()) {
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

    // Returns the next upcoming time-based rule (based on time and day)
    public RuleModel getNextScheduledTimeRule() {
        List<RuleModel> rules = getActiveTimeRules();
        Calendar now = Calendar.getInstance();
        int nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        int todayIndex = (now.get(Calendar.DAY_OF_WEEK) - 1 + 7) % 7;

        RuleModel nextRule = null;
        long minTimeUntil = Long.MAX_VALUE;

        for (RuleModel rule : rules) {
            for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
                int dayIndex = (todayIndex + dayOffset) % 7;
                if ((rule.getDaysMask() & (1 << dayIndex)) == 0) continue;

                try {
                    String[] parts = rule.getTimeStart().split(":");
                    int ruleMinutes = Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);

                    if (dayOffset == 0 && ruleMinutes <= nowMinutes) continue;

                    long timeUntil = dayOffset * 24 * 60L + (ruleMinutes - nowMinutes);
                    if (timeUntil < minTimeUntil) {
                        minTimeUntil = timeUntil;
                        nextRule = rule;
                    }

                } catch (Exception e) {
                    Log.e("SmartSilence", context.getString(R.string.log_rule_time_parse_error, rule.getTimeStart()), e);
                }
            }
        }

        return nextRule;
    }

    // Returns all active location-based rules
    public List<RuleModel> getActiveLocationRules() {
        List<RuleModel> rules = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_RULES,
                null,
                COLUMN_TYPE + "=? AND " + COLUMN_ACTIVE + "=?",
                new String[]{"location", "1"},
                null, null, null
        );

        if (cursor.moveToFirst()) {
            do {
                RuleModel rule = new RuleModel();
                rule.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                rule.setRuleName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RULE_NAME)));
                rule.setType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE)));
                rule.setActive(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ACTIVE)) == 1);
                rule.setLocationName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOCATION_NAME)));
                rule.setLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUDE)));
                rule.setLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE)));
                rule.setRadius(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_RADIUS)));
                rules.add(rule);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return rules;
    }

    // Returns rules that are currently active (nowActive = 1)
    public List<RuleModel> getCurrentlyActiveRules() {
        List<RuleModel> rules = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_RULES,
                null,
                COLUMN_NOW_ACTIVE + "=?",
                new String[]{"1"},
                null, null, null
        );

        if (cursor.moveToFirst()) {
            do {
                RuleModel rule = new RuleModel();
                rule.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                rule.setRuleName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RULE_NAME)));
                rule.setType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE)));
                rule.setActive(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ACTIVE)) == 1);
                rule.setTimeStart(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME_START)));
                rule.setTimeEnd(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME_END)));
                rule.setDaysMask(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DAYS_MASK)));
                rule.setLocationName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOCATION_NAME)));
                rule.setLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUDE)));
                rule.setLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE)));
                rule.setRadius(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_RADIUS)));
                rule.setNowActive(true);
                rules.add(rule);
            } while (cursor.moveToNext());
            cursor.close();
        }

        return rules;
    }

    // Updates a rule by ID
    public boolean updateRuleById(RuleModel rule) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_RULE_NAME, rule.getRuleName());
        values.put(COLUMN_TYPE, rule.getType());
        values.put(COLUMN_ACTIVE, rule.isActive() ? 1 : 0);

        if ("time".equals(rule.getType())) {
            values.put(COLUMN_TIME_START, rule.getTimeStart());
            values.put(COLUMN_TIME_END, rule.getTimeEnd());
            values.put(COLUMN_DAYS_MASK, rule.getDaysMask());

            values.putNull(COLUMN_LOCATION_NAME);
            values.putNull(COLUMN_LATITUDE);
            values.putNull(COLUMN_LONGITUDE);
            values.putNull(COLUMN_RADIUS);
        } else {
            values.put(COLUMN_LOCATION_NAME, rule.getLocationName());
            values.put(COLUMN_LATITUDE, rule.getLatitude());
            values.put(COLUMN_LONGITUDE, rule.getLongitude());
            values.put(COLUMN_RADIUS, rule.getRadius());

            values.putNull(COLUMN_TIME_START);
            values.putNull(COLUMN_TIME_END);
            values.put(COLUMN_DAYS_MASK, 0);
        }

        int rows = db.update(TABLE_RULES, values, COLUMN_ID + "=?", new String[]{String.valueOf(rule.getId())});
        return rows > 0;
    }

    // Updates nowActive status for a rule
    public void updateNowActiveStatus(int ruleId, boolean nowActive) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NOW_ACTIVE, nowActive ? 1 : 0);
        db.update(TABLE_RULES, values, COLUMN_ID + "=?", new String[]{String.valueOf(ruleId)});
    }

    // Deletes a specific rule by ID, with log
    public boolean deleteRuleById(int id) {
        SQLiteDatabase db = getWritableDatabase();
        Log.d("SmartSilence", context.getString(R.string.log_attempting_to_delete_rule, id));

        int rowsAffected = db.delete(TABLE_RULES, COLUMN_ID + "=?", new String[]{String.valueOf(id)});

        if (rowsAffected > 0) {
            Log.d("SmartSilence", context.getString(R.string.log_rule_deleted_success, rowsAffected));
            return true;
        } else {
            Log.w("SmartSilence", context.getString(R.string.log_rule_delete_not_found, id));
            return false;
        }
    }

    // Converts bitmask of days into readable string
    public String getDaysString(int daysMask) {
        if (daysMask == 0b01111111) {
            return context.getString(R.string.days_everyday);
        }

        String[] days = {
                context.getString(R.string.day_sunday),
                context.getString(R.string.day_monday),
                context.getString(R.string.day_tuesday),
                context.getString(R.string.day_wednesday),
                context.getString(R.string.day_thursday),
                context.getString(R.string.day_friday),
                context.getString(R.string.day_saturday)
        };

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            if ((daysMask & (1 << i)) != 0) {
                if (result.length() > 0) result.append(", ");
                result.append(days[i]);
            }
        }

        return result.toString();
    }
}
