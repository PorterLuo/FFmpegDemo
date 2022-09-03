package com.luohb.ffmpeg.util;

import android.content.Context;

import java.io.File;

public class FileUtils {

    private static final String AUDIO_PATH = "audio";
    public static String getAudioPath(Context context) {
        return getPath(context, AUDIO_PATH);
    }

    public static String getPath(Context context, String path) {
        return context.getExternalFilesDir(path).getPath() + File.separator;
    }
}
