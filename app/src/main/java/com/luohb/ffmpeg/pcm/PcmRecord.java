package com.luohb.ffmpeg.pcm;
import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.text.TextUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class PcmRecord {
    private RecorderThread mRecorderThread;

    public void start(String path) {
        stop();
        mRecorderThread = new RecorderThread(path,
                MediaRecorder.AudioSource.MIC,
                44100,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);
        mRecorderThread.start();
    }

    public void stop() {
        if (mRecorderThread == null) {
            return;
        }
        mRecorderThread.stopRecord();
        ;
        mRecorderThread = null;
    }

    private static class RecorderThread extends Thread {
        //pcm录制组件
        private AudioRecord mAudioRecord;
        //输出文件
        private FileOutputStream mOutFile;
        //输出文件地址
        private String mPath;
        //声音来源
        private int mAudioSource;
        //采样率
        private int mSampleRate;
        //声道
        private int mChannel;
        //编码格式
        private int mAudioFormat;
        //音频缓存buffer
        private int mBufferSize;
        //是否停止录制
        private boolean mStopRecord;

        public RecorderThread(String mPath, int mAudioSource, int mSampleRate,
                              int mChannel, int mAudioFormat) {
            this.mPath = mPath;
            this.mAudioSource = mAudioSource;
            this.mSampleRate = mSampleRate;
            this.mChannel = mChannel;
            this.mAudioFormat = mAudioFormat;
        }

        @Override
        public void run() {
            super.run();
            initFile();
            initAudioRecord();
            startRecord();
        }

        /**
         * 初始化文件
         */
        private void initFile() {
            if (TextUtils.isEmpty(mPath)) {
                return;
            }
            File file = new File(mPath);
            if (file.exists()) {
                file.delete();
            }
            try {
                mOutFile = new FileOutputStream(mPath);
            } catch (FileNotFoundException exception) {
                exception.printStackTrace();
                mOutFile = null;
            }
        }

        @SuppressLint("MissingPermission")
        private void initAudioRecord() {
            mBufferSize = AudioRecord.getMinBufferSize(mSampleRate, mChannel, mAudioFormat);
            mAudioRecord = new AudioRecord(mAudioSource, mSampleRate,
                    mChannel, mAudioFormat, mBufferSize);
        }

        private void startRecord(){
            if (mAudioRecord == null || mOutFile == null) {
                return;
            }
            byte[] data = new byte[mBufferSize];
            mAudioRecord.startRecording();

            for (;;){
                if (mStopRecord) {
                    release();
                    break;
                }
                int readSize = mAudioRecord.read(data, 0, mBufferSize);
                if (readSize <= 0) {
                    mStopRecord = true;
                    continue;
                }
                try {
                    mOutFile.write(data);
                }catch (IOException exception) {
                    exception.printStackTrace();
                }
            }
        }

        private void stopRecord(){
            mStopRecord = true;
            try {
                join(2000);
            }catch (InterruptedException exception) {
                exception.printStackTrace();
            }

        }

        private void release(){
            if (mAudioRecord != null) {
                mAudioRecord.stop();
                mAudioRecord.release();
                mAudioRecord = null;
            }
            if (mOutFile != null) {
                try {
                    mOutFile.close();
                    mOutFile = null;
                }catch (IOException exception) {
                    exception.printStackTrace();
                }

            }
        }
        
    }
}
