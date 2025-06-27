package com.yonatanh_tald_evem.smartsilence.utils;

import com.yonatanh_tald_evem.smartsilence.database.models.RuleModel;

import java.text.SimpleDateFormat;
import java.util.*;

public class TimeUtils {
    //Checks whether the given rule is currently active
    public static boolean isRuleActiveNow(RuleModel rule) {
        Calendar now = Calendar.getInstance();
        int todayIndex = now.get(Calendar.DAY_OF_WEEK) - 1; // 0=Sun…6=Sat

        if ((rule.getDaysMask() & (1 << todayIndex)) == 0) {
            return false;
        }
        try {
            // Create new formatter using current locale
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

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

}
