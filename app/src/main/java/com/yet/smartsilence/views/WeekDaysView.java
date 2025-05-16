package com.yet.smartsilence.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.yet.smartsilence.R;

import java.util.ArrayList;
import java.util.List;

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
            tv.setOnClickListener(v -> {
                if (!selectable) return;
                // הפיכה של הביט ב-mask
                boolean now = ((daysMask & (1<<index)) != 0);
                setDayActive(index, !now);
            });
            dayViews.add(tv);
            addView(tv);
        }
        refreshViews();
    }

    private void refreshViews() {
        for (int i = 0; i < 7; i++) {
            TextView tv = dayViews.get(i);
            if ((daysMask & (1<<i)) != 0) {
                tv.setBackground(ContextCompat.getDrawable(getContext(), com.yet.smartsilence.R.drawable.circle_day_background));
                tv.setTextColor(ContextCompat.getColor(getContext(), R.color.text_primary));
            } else {
                tv.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.circle_day_background_inactive));
                tv.setTextColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
            }
        }
    }

    private void setDayActive(int index, boolean active) {
        if (active) daysMask |=  (1 << index);
        else       daysMask &= ~(1 << index);
        refreshViews();
    }

    private int dpToPx(Context ctx, int dp) {
        float density = ctx.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }


    /** API חיצוני */
    public void setDaysMask(int mask) {
        this.daysMask = mask;
        refreshViews();
    }
    public int getDaysMask() {
        return daysMask;
    }
    /** בקרה האם אפשר ללחוץ ולבחור (default=false) */
    public void setSelectable(boolean sel) {
        this.selectable = sel;
    }
}

