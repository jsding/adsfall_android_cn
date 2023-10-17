package com.vivo.paydemo;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by XuPeng on 2018/5/22.
 */
public class LogView extends TextView {

    private static SpannableStringBuilder sLogContent = new SpannableStringBuilder();

    private static final int ERROR_COLOR = Color.RED;
    private static final int WARN_COLOR = Color.rgb(255, 128, 10);
    private static final int DEBUG_COLOR = Color.BLACK;

    private SimpleDateFormat mDateFormat;

    public LogView(Context context) {
        super(context);
        init();
    }

    public LogView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setMovementMethod(ScrollingMovementMethod.getInstance());
        mDateFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    }

    public void printE(CharSequence log) {
        println(log, ERROR_COLOR);
    }

    public void printD(CharSequence log) {
        println(log, DEBUG_COLOR);
    }

    public void printW(CharSequence log) {
        println(log, WARN_COLOR);
    }

    private void println(CharSequence log, int color) {
        String appendLog = "【" + mDateFormat.format(new Date()) + "】" + log + "\n";
        int start = sLogContent.length();
        int end = start + appendLog.length();
        sLogContent.append(appendLog);
        sLogContent.setSpan(new ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        flush();
    }

    public void clear() {
        sLogContent.clear();
        flush();
    }

    public void flush() {
        setText(sLogContent);
        int offset = getLineCount() * getLineHeight();
        if (offset > getHeight()) {
            scrollTo(0, offset - getHeight());
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        flush();
    }

    public String getLog() {
        return sLogContent.toString();
    }
}
