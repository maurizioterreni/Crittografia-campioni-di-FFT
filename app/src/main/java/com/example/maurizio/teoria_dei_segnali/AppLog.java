package com.example.maurizio.teoria_dei_segnali;

/**
 * Created by Maurizio on 03/06/2015.
 */
import android.util.Log;

public class AppLog {
    private static final String APP_TAG = "AudioRecorder";

    public static int logString(String message){
        return Log.i(APP_TAG,message);
    }
}
