package com.yet.smartsilence.utils;

import com.yet.smartsilence.database.models.RuleModel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class TimeUtils {
    private static final SimpleDateFormat timeFormat =
            new SimpleDateFormat("HH:mm", Locale.getDefault());

    // English day names for internal scheduling
    private static final String[] EN_DAYS = {
            "Sun","Mon","Tue","Wed","Thu","Fri","Sat"
    };

    // Hebrew day labels for UI rendering (א–ש)
    private static final String[] HE_DAYS = {
            "א","ב","ג","ד","ה","ו","ש"
    };

    /**
     * מוצא את החוק הבא להתבצע מתוך רשימת החוקים.
     * עובד על ה-daysMask של כל RuleModel.
     */
    public static RuleModel findNextTimeRule(List<RuleModel> rules) {
        Calendar now = Calendar.getInstance();
        RuleModel nextRule = null;
        long minDiff = Long.MAX_VALUE;

        for (RuleModel rule : rules) {
            if (rule.getTimeStart() == null) continue;

            int mask = rule.getDaysMask();
            for (int i = 0; i < 7; i++) {
                if ((mask & (1 << i)) == 0) continue;

                Calendar candidate = getNextDayTime(i, rule.getTimeStart());
                if (candidate == null) continue;
                long diff = candidate.getTimeInMillis() - now.getTimeInMillis();
                if (diff >= 0 && diff < minDiff) {
                    minDiff = diff;
                    nextRule = rule;
                }
            }
        }
        return nextRule;
    }

    /** ממחזיר Calendar ללוח הזמנים הבא של היום-index (0=Sun…6=Sat) בשעה timeStr */
    private static Calendar getNextDayTime(int dayIndex, String timeStr) {
        try {
            Date parsed = timeFormat.parse(timeStr);
            Calendar now = Calendar.getInstance();
            Calendar result = Calendar.getInstance();

            result.setTime(parsed);
            result.set(Calendar.SECOND, 0);
            result.set(Calendar.MILLISECOND, 0);
            result.set(Calendar.YEAR, now.get(Calendar.YEAR));
            result.set(Calendar.MONTH, now.get(Calendar.MONTH));
            result.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));

            // הזזה עד שמגיעים ל-dayIndex העתידי
            while (result.get(Calendar.DAY_OF_WEEK) != dayIndex + 1  // Calendar.SUNDAY=1
                    || result.before(now)) {
                result.add(Calendar.DAY_OF_YEAR, 1);
            }
            return result;
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * בודק אם חוק פעיל כרגע:
     * 1. היום נכלל ב-mask, 2. השעה הנוכחית בין start ל-end.
     */
    public static boolean isRuleActiveNow(RuleModel rule) {
        Calendar now = Calendar.getInstance();
        int todayIndex = now.get(Calendar.DAY_OF_WEEK) - 1; // 0=Sun…6=Sat

        if ((rule.getDaysMask() & (1 << todayIndex)) == 0) {
            return false;
        }
        try {
            Date nowTime = timeFormat.parse(timeFormat.format(now.getTime()));
            Date start = timeFormat.parse(rule.getTimeStart());
            Date end   = timeFormat.parse(rule.getTimeEnd());
            return nowTime != null
                    && start != null
                    && end   != null
                    && !nowTime.before(start)
                    && !nowTime.after(end);
        } catch (Exception e) {
            return false;
        }
    }

    /** המרה: ביטמסק → מחרוזת עברית "א,ב,ג…" (ל-UI) */
    public static String maskToHebDays(int mask) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < HE_DAYS.length; i++) {
            if ((mask & (1 << i)) != 0) {
                if (sb.length() > 0) sb.append(",");
                sb.append(HE_DAYS[i]);
            }
        }
        return sb.toString();
    }

    /** המרה: מחרוזת עברית "א,ג,ה" → ביטמסק (ל-migration או ל־parsing באנגלית) */
    public static int hebDaysStringToMask(String heb) {
        if (heb == null || heb.isEmpty()) return 0;
        int mask = 0;
        for (String token : heb.split(",")) {
            token = token.trim();
            for (int i = 0; i < HE_DAYS.length; i++) {
                if (HE_DAYS[i].equals(token)) {
                    mask |= (1 << i);
                    break;
                }
            }
        }
        return mask;
    }

    /** לחילופין, אם צריך מערך של bools ל־updateDaysCircles */
    public static boolean[] maskToBooleanArray(int mask) {
        boolean[] days = new boolean[7];
        for (int i = 0; i < 7; i++) {
            days[i] = (mask & (1 << i)) != 0;
        }
        return days;
    }
}
