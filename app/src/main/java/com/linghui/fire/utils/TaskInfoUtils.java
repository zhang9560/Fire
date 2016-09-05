package com.linghui.fire.utils;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import com.linghui.fire.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Created by yhzhang on 2015/11/25.
 */
public class TaskInfoUtils {

    public static SpannableStringBuilder getRefundSpeedText(Context context, int refundSpeed) {
        if (refundSpeed <=0) {
            return new SpannableStringBuilder(context.getString(R.string.task_refund_speed));
        } else {
            SpannableStringBuilder text = new SpannableStringBuilder(context.getString(R.string.task_refund_speed_with_stars));
            ForegroundColorSpan span = new ForegroundColorSpan(context.getResources().getColor(R.color.text_color_yellow));

            if (refundSpeed > 24) {
                text.setSpan(span, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (refundSpeed > 15) {
                text.setSpan(span, 0, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (refundSpeed > 6) {
                text.setSpan(span, 0, 3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (refundSpeed > 3) {
                text.setSpan(span, 0, 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                text.setSpan(span, 0, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            return text;
        }
    }

    public static boolean isEmptyValue(String value) {
        return TextUtils.isEmpty(value) || value.equals("null");
    }

    public static class CountdownString {
        public CountdownString(String string, long leftTime) {
            mString = string;
            mLeftTime = leftTime;
        }

        public long getLeftTime() {
            return mLeftTime;
        }

        @Override
        public String toString() {
            return mString;
        }

        private String mString;
        private long mLeftTime;
    }

    public static final long ONE_HOUR = 60 * 60 * 1000;
    public static final long ONE_DAY = 60 * 60 * 24 * 1000;
    public static final long HALF_DAY = 60 * 60 * 12 * 1000;
    private static final SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    public static CountdownString getCountdownString(Context context, int prefixRes, String start, String expired, long duration) {
        try {
            long startTime = sDateFormat.parse(start).getTime();
            long expiredTime = sDateFormat.parse(expired).getTime();
            expiredTime = (expiredTime - startTime > duration) ? startTime + duration : expiredTime;
            long now = System.currentTimeMillis();

            if (now <= expiredTime - 1000) { // 已经过期
                int leftTime = (int) ((expiredTime - now) / 1000);
                int hours = leftTime / 3600;
                int minutes = (leftTime % 3600) / 60;
                int seconds = (leftTime % 3600) % 60;
                return new CountdownString(context.getString(prefixRes, context.getString(R.string.countdown_output_format, hours, minutes, seconds)), leftTime);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return new CountdownString(context.getString(prefixRes, context.getString(R.string.countdown_expired)), 0);
    }
}
