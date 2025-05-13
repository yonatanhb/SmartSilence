package com.yet.smartsilence.utils;

import com.yet.smartsilence.database.models.RuleModel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class TimeUtils {

    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public static RuleModel findNextTimeRule(List<RuleModel> rules) {
        Calendar now = Calendar.getInstance();
        RuleModel nextRule = null;
        long minTimeDiff = Long.MAX_VALUE;

        for (RuleModel rule : rules) {
            if (rule.getTimeStart() == null || rule.getDaysOfWeek() == null) continue;

            String[] days = rule.getDaysOfWeek().split(",");
            for (String day : days) {
                Calendar ruleTime = getNextDayTime(day.trim(), rule.getTimeStart());
                if (ruleTime == null) continue;

                long diff = ruleTime.getTimeInMillis() - now.getTimeInMillis();
                if (diff >= 0 && diff < minTimeDiff) {
                    minTimeDiff = diff;
                    nextRule = rule;
                }
            }
        }

        return nextRule;
    }

    private static Calendar getNextDayTime(String dayName, String timeStr) {
        int dayOfWeek = getDayOfWeek(dayName);
        if (dayOfWeek == -1) return null;

        try {
            Date time = timeFormat.parse(timeStr);
            Calendar now = Calendar.getInstance();
            Calendar result = Calendar.getInstance();

            result.setTime(time);
            result.set(Calendar.SECOND, 0);
            result.set(Calendar.MILLISECOND, 0);
            result.set(Calendar.YEAR, now.get(Calendar.YEAR));
            result.set(Calendar.MONTH, now.get(Calendar.MONTH));
            result.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));

            while (result.get(Calendar.DAY_OF_WEEK) != dayOfWeek ||
                    result.before(now)) {
                result.add(Calendar.DAY_OF_YEAR, 1);
            }

            return result;
        } catch (ParseException e) {
            return null;
        }
    }

    private static int getDayOfWeek(String day) {
        switch (day.toLowerCase(Locale.ROOT)) {
            case "sun": return Calendar.SUNDAY;
            case "mon": return Calendar.MONDAY;
            case "tue": return Calendar.TUESDAY;
            case "wed": return Calendar.WEDNESDAY;
            case "thu": return Calendar.THURSDAY;
            case "fri": return Calendar.FRIDAY;
            case "sat": return Calendar.SATURDAY;
            default: return -1;
        }
    }

    public static boolean isRuleActiveNow(RuleModel rule) {
        Calendar now = Calendar.getInstance();
        String today = getDayName(now.get(Calendar.DAY_OF_WEEK));
        if (rule.getDaysOfWeek() == null || !rule.getDaysOfWeek().contains(today)) return false;

        try {
            SimpleDateFormat format = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date nowTime = format.parse(format.format(now.getTime()));
            Date start = format.parse(rule.getTimeStart());
            Date end = format.parse(rule.getTimeEnd());

            return nowTime != null && start != null && end != null && nowTime.after(start) && nowTime.before(end);
        } catch (Exception e) {
            return false;
        }
    }

    private static String getDayName(int day) {
        switch (day) {
            case Calendar.SUNDAY: return "Sun";
            case Calendar.MONDAY: return "Mon";
            case Calendar.TUESDAY: return "Tue";
            case Calendar.WEDNESDAY: return "Wed";
            case Calendar.THURSDAY: return "Thu";
            case Calendar.FRIDAY: return "Fri";
            case Calendar.SATURDAY: return "Sat";
            default: return "";
        }
    }
}
