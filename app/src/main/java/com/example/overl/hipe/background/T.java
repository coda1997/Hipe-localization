package com.example.overl.hipe.background;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by zhang on 2018/4/2.
 */

public class T {
    private static Toast toast;
    public static void show(Context context, String msg, int duration, int bgcolor) {
        if (toast == null) {
            toast = Toast.makeText(context, msg, duration);
            toast.setGravity(Gravity.TOP, 0, 0);
            TextView v = toast.getView().findViewById(android.R.id.message);
            v.setTextColor(Color.WHITE);
            v.setTextSize(15);
        }
        View view = toast.getView();
        view.setBackgroundResource(bgcolor);
        toast.setView(view);
        toast.setText(msg);
        toast.setDuration(duration);
        toast.show();
    }
    public static void show(Context context, String msg, int duration) {
        if (toast == null) {
            toast = Toast.makeText(context, msg, duration);
            toast.setGravity(Gravity.TOP, 0, 0);
            TextView v = toast.getView().findViewById(android.R.id.message);
            v.setTextColor(Color.WHITE);
            v.setTextSize(15);
        }
        toast.setText(msg);
        toast.setDuration(duration);
        toast.show();
    }

}
