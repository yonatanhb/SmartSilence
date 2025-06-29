package com.yonatanh_tald_eveb.smartsilence.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.yonatanh_tald_eveb.smartsilence.R;

import java.util.ArrayList;
import java.util.List;

// Custom view that displays days of the week in Hebrew (א–ש) as selectable circles
public class WeekDaysView extends LinearLayout {
    private static final String[] HEB_DAYS = {"א","ב","ג","ד","ה","ו","ש"};
    private final List<TextView> dayViews = new ArrayList<>(7);
    private int daysMask = 0;
    private boolean selectable = false;

    public WeekDaysView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        setOrientation(HORIZONTAL);
        init(ctx);
    }

    // Initializes the day views and click behavior
    private void init(Context ctx) {
        int sizeDp = 32;
        int marginDp = 8;
        LayoutParams lp = new LayoutParams(
                dpToPx(ctx, sizeDp), dpToPx(ctx, sizeDp));
        lp.setMarginEnd(dpToPx(ctx, marginDp));

        for (int i = 0; i < 7; i++) {
            TextView tv = new TextView(ctx);
            tv.setLayoutParams(new LayoutParams(lp));
            tv.setGravity(Gravity.CENTER);
            tv.setText(HEB_DAYS[i]);
            tv.setTextSize(14);
            final int index = i;

            // Handle click to toggle selected day
            tv.setOnClickListener(v -> {
                if (!selectable) return;
                boolean now = ((daysMask & (1<<index)) != 0);
                setDayActive(index, !now);
            });

            dayViews.add(tv);
            addView(tv);
        }
        refreshViews();
    }

    // Updates each day view appearance based on daysMask
    private void refreshViews() {
        for (int i = 0; i < 7; i++) {
            TextView tv = dayViews.get(i);
            if ((daysMask & (1<<i)) != 0) {
                // Active (selected) day
                tv.setBackground(ContextCompat.getDrawable(getContext(), com.yonatanh_tald_eveb.smartsilence.R.drawable.circle_day_background));
                tv.setTextColor(ContextCompat.getColor(getContext(), R.color.text_primary));
            } else {
                // Inactive (not selected)
                tv.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.circle_day_background_inactive));
                tv.setTextColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
            }
        }
    }

    // Sets the bit for a given day index as active or inactive
    private void setDayActive(int index, boolean active) {
        if (active) daysMask |=  (1 << index);
        else       daysMask &= ~(1 << index);
        refreshViews();
    }

    // Converts dp (density-independent pixels) to actual pixels
    private int dpToPx(Context ctx, int dp) {
        float density = ctx.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // Sets the selected days using a bitmask
    public void setDaysMask(int mask) {
        this.daysMask = mask;
        refreshViews();
    }

    // Returns the currently selected days as a bitmask
    public int getDaysMask() {
        return daysMask;
    }

    // Enables or disables selection via touch
    public void setSelectable(boolean sel) {
        this.selectable = sel;
    }
}

