package com.overseas.gamesdk.demo.util;

import android.content.Context;
import android.widget.Toast;

public class ToastUtil {
    public static void show(Context context, CharSequence text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }
}
