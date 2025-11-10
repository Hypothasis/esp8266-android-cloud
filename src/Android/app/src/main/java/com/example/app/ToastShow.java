package com.example.app;

import android.content.Context;
import android.os.Looper;
import android.widget.Toast;

public class ToastShow implements Runnable {
    private static Context Context;
    private static String Message;
    public ToastShow(Context context, String message){
        Context = context;
        Message = message;
    }

    @Override
    public void run() {
        Looper.prepare();
        Toast ToastUI = new Toast(Context);
        ToastUI.setText(Message);
        ToastUI.show();
    }
}
