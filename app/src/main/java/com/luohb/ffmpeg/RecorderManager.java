package com.luohb.ffmpeg;

import android.media.MediaRecorder;

public class RecorderManager {
    MediaRecorder mMediaRecorder;
    public void startRecord(){
        if (mMediaRecorder == null) {
            mMediaRecorder = new MediaRecorder();
        }
    }
}
