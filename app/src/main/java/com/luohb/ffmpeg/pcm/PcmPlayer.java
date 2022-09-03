package com.luohb.ffmpeg.pcm;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.text.TextUtils;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class PcmPlayer {
    private PlayerThread mPlayerThread;
    public void start(String path){
        stop();
        mPlayerThread = new PlayerThread(path,
                AudioManager.STREAM_MUSIC,
                44100,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                AudioTrack.MODE_STREAM);
        mPlayerThread.start();
    }
    public void stop(){
        if (mPlayerThread == null) {
            return;
        }
        mPlayerThread.stopPlay();
        mPlayerThread = null;
    }

    private static class PlayerThread extends Thread {
        //pcm播放组件
        private AudioTrack mAudioTrack;
        //输入文件
        private FileInputStream mInFile;
        //输入文件地址
        private String mPath;
        //音频流格式
        private int mStreamType;
        //采样率
        private int mSampleRate;
        //声道
        private int mChannel;
        //编码格式
        private int mAudioFormat;
        //播放模式
        private int mMod;
        //音频缓存buffer
        private int mBufferSize;
        //是否停止播放
        private boolean mStopPlay;

        public PlayerThread(String mPath, int mStreamType, int mSampleRate,
                            int mChannel, int mAudioFormat, int mMod) {
            this.mPath = mPath;
            this.mStreamType = mStreamType;
            this.mSampleRate = mSampleRate;
            this.mChannel = mChannel;
            this.mAudioFormat = mAudioFormat;
            this.mMod = mMod;
        }

        @Override
        public void run() {
            super.run();
            initFile();
            initAudioTrack();
            play();
        }

        private void initFile(){
            if (TextUtils.isEmpty(mPath)) {
                return;
            }
            try {
                mInFile = new FileInputStream(mPath);
            } catch (FileNotFoundException exception) {
                exception.printStackTrace();
                mInFile = null;
            }
        }

        private void initAudioTrack(){
            mBufferSize = AudioTrack.getMinBufferSize(mSampleRate, mChannel, mAudioFormat);
            mAudioTrack = new AudioTrack(mStreamType, mSampleRate, mChannel, mAudioFormat, mBufferSize, mMod);
        }

        /**
         * 开始播放
         */
        private void play(){
            if (mAudioTrack == null || mInFile == null) {
                return;
            }
            byte[] data = new byte[mBufferSize];
            mAudioTrack.play();
            for (;;) {
                if (mStopPlay) {
                    release();
                    break;
                }
                int readSize = -1;
                try {
                    readSize = mInFile.read(data);
                }catch (IOException exception) {
                    exception.printStackTrace();
                }
                if (readSize <= 0) {
                    mStopPlay = true;
                    continue;
                }
                mAudioTrack.write(data, 0, readSize);
            }
        }

        /**
         * 停止播放
         */
        private void stopPlay(){
            mStopPlay = true;
            try {
                join(2000);
            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        }

        /**
         * 释放资源
         */
        private void release(){
            if (mAudioTrack != null) {
                mAudioTrack.stop();
                mAudioTrack.release();
                mAudioTrack = null;
            }
            if (mInFile != null) {
                try {
                    mInFile.close();
                }catch (IOException exception) {
                    exception.printStackTrace();
                }

            }
        }
    }
}
