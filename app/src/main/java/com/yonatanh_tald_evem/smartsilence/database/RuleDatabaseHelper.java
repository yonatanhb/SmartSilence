package com.yonatanh_tald_evem.smartsilence.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.yonatanh_tald_evem.smartsilence.database.models.RuleModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class RuleDatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME    = "smart_silence.db";
    public static final int    DATABASE_VERSION = 4;

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

    /** מחזיר את כל חוקי המיקום הפעילים */
    public List<RuleModel> getActiveLocationRules() {
        List<RuleModel> rules = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        // שאילתה ששולפת רק חוקי מיקום פעילים
        Cursor cursor = db.query(
                TABLE_RULES,
                null,
                COLUMN_TYPE + "=? AND " + COLUMN_ACTIVE + "=?",
                new String[]{"location", "1"},
                null, null, null
        );

        if (cursor != null && cursor.moveToFirst()) {
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

    /** מוסיף כלל לדוגמה (רק היום, עוד דקה עד 10 דקות) */
    public void insertTestTimeRule() {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_TYPE, "time");
        values.put(COLUMN_RULE_NAME, "example");
        values.put(COLUMN_ACTIVE, 1);
        values.put(COLUMN_TIME_START, getTimePlusMinutes(2));
        values.put(COLUMN_TIME_END, getTimePlusMinutes(10));
        values.put(COLUMN_DAYS_MASK, 0b01111111);

        db.insert(TABLE_RULES, null, values);
    }

    public void insertManualTimeRule(RuleModel rule) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_TYPE, "time");
        values.put(COLUMN_RULE_NAME, rule.getRuleName());
        values.put(COLUMN_ACTIVE, rule.isActive() ? 1 : 0);
        values.put(COLUMN_TIME_START, rule.getTimeStart());
        values.put(COLUMN_TIME_END, rule.getTimeEnd());
        values.put(COLUMN_DAYS_MASK, rule.getDaysMask());

        // לא רלוונטי לחוקי זמן – נכניס null
        values.putNull(COLUMN_LOCATION_NAME);
        values.putNull(COLUMN_LATITUDE);
        values.putNull(COLUMN_LONGITUDE);
        values.putNull(COLUMN_RADIUS);

        db.insert(TABLE_RULES, null, values);
    }

    /** מחזיר חוק ספציפי לפי ID */
    public RuleModel getRuleById(int id) {
        SQLiteDatabase db = getReadableDatabase();
        RuleModel rule = null;

        Cursor cursor = db.query(
                TABLE_RULES,
                null, // כל העמודות
                COLUMN_ID + "=?", // תנאי WHERE
                new String[]{String.valueOf(id)}, // ערך התנאי
                null, null, null, "1" // LIMIT 1
        );

        if (cursor != null && cursor.moveToFirst()) {
            rule = new RuleModel(); // רק אם מצאנו משהו, ניצור אובייקט
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

            // ערכים ריקים עבור שדות מיקום
            values.putNull(COLUMN_LOCATION_NAME);
            values.putNull(COLUMN_LATITUDE);
            values.putNull(COLUMN_LONGITUDE);
            values.putNull(COLUMN_RADIUS);
        } else {
            values.put(COLUMN_LOCATION_NAME, rule.getLocationName());
            values.put(COLUMN_LATITUDE, rule.getLatitude());
            values.put(COLUMN_LONGITUDE, rule.getLongitude());
            values.put(COLUMN_RADIUS, rule.getRadius());

            // ערכים ריקים עבור שדות זמן
            values.putNull(COLUMN_TIME_START);
            values.putNull(COLUMN_TIME_END);
            values.put(COLUMN_DAYS_MASK, 0);
        }

        int rows = db.update(TABLE_RULES, values, COLUMN_ID + "=?", new String[]{String.valueOf(rule.getId())});
        return rows > 0;
    }


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

        // לשדות שאינם רלוונטיים (חוק מיקום) - אפשר להשאיר ריקים/null
        values.putNull(COLUMN_TIME_START);
        values.putNull(COLUMN_TIME_END);
        values.put(COLUMN_DAYS_MASK, 0); // לא רלוונטי למיקום

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

    /** מחזיר את כל החוקים מכל הסוגים */
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

        if (cursor != null && cursor.moveToFirst()) {
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

    public String getDaysString(int daysMask) {
        if (daysMask == 0b01111111) {
            return "כל יום";
        }

        String[] days = {"א׳", "ב׳", "ג׳", "ד׳", "ה׳", "ו׳", "ש׳"};
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < 7; i++) {
            if ((daysMask & (1 << i)) != 0) {
                if (result.length() > 0) result.append(", ");
                result.append(days[i]);
            }
        }

        return result.toString();
    }

    /** מוחק חוק לפי מזהה עם לוגים */
    public boolean deleteRuleById(int id) {
        SQLiteDatabase db = getWritableDatabase();
        Log.d("SmartSilence", "מנסה למחוק חוק עם ID: " + id);

        int rowsAffected = db.delete(TABLE_RULES, COLUMN_ID + "=?", new String[]{String.valueOf(id)});

        if (rowsAffected > 0) {
            Log.d("SmartSilence", "החוק נמחק בהצלחה. rowsAffected = " + rowsAffected);
            return true;
        } else {
            Log.w("SmartSilence", "לא נמצא חוק למחיקה עם ID: " + id);
            return false;
        }
    }

    public void updateNowActiveStatus(int ruleId, boolean nowActive) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NOW_ACTIVE, nowActive ? 1 : 0);
        db.update(TABLE_RULES, values, COLUMN_ID + "=?", new String[]{String.valueOf(ruleId)});
    }

    /** מחזיר את כל החוקים שכרגע פעילים בפועל (nowActive = 1) */
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

        if (cursor != null && cursor.moveToFirst()) {
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

    /** מחזיר את החוק המתוזמן הבא שאינו פעיל כרגע (לפי שעה ויום) */
    public RuleModel getNextScheduledTimeRule() {
        List<RuleModel> rules = getActiveTimeRules();
        Calendar now = Calendar.getInstance();
        int nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        int todayIndex = (now.get(Calendar.DAY_OF_WEEK) - 1 + 7) % 7; // 0 = ראשון, 6 = שבת

        RuleModel nextRule = null;
        long minTimeUntil = Long.MAX_VALUE;

        for (RuleModel rule : rules) {
            for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
                int dayIndex = (todayIndex + dayOffset) % 7;
                if ((rule.getDaysMask() & (1 << dayIndex)) == 0) continue;

                try {
                    String[] parts = rule.getTimeStart().split(":");
                    int ruleMinutes = Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);

                    // אם זה היום, נוודא שהחוק עדיין לא התחיל
                    if (dayOffset == 0 && ruleMinutes <= nowMinutes) continue;

                    // חישוב זמן עד מועד החוק (בדקות)
                    long timeUntil = dayOffset * 24 * 60L + (ruleMinutes - nowMinutes);
                    if (timeUntil < minTimeUntil) {
                        minTimeUntil = timeUntil;
                        nextRule = rule;
                    }

                } catch (Exception e) {
                    Log.e("SmartSilence", "שגיאה בפענוח זמן החוק: " + rule.getTimeStart(), e);
                }
            }
        }

        return nextRule;
    }

}
