package com.luohb.ffmpeg.ui.activity;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.luohb.ffmpeg.R;

public class FFmpegInfoActivity extends AppCompatActivity {
    private TextView mVersion;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ffmpeg_info);
        mVersion = findViewById(R.id.ffmpeg_version);
        initData();
    }

    private void initData(){
        mVersion.setText("FFmpeg的版本号: " +getFFmpegVersion());
    }

    public native String getFFmpegVersion();
}
