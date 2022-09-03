package com.luohb.ffmpeg.ui.activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.luohb.ffmpeg.R;
import com.luohb.ffmpeg.pcm.PcmPlayer;
import com.luohb.ffmpeg.pcm.PcmRecord;
import com.luohb.ffmpeg.util.FileUtils;

public class PcmActivity extends AppCompatActivity implements View.OnClickListener {
    private TextView mPcmStart;
    private TextView mPcmStop;
    private TextView mPcmPlayStart;
    private TextView mPcmPlayStop;

    private String mPcmPath;
    private PcmRecord mPcmRecord;
    private PcmPlayer mPcmPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pcm);
        initView();
        initData();
    }

    private void initView(){
        mPcmStart = findViewById(R.id.pcm_start);
        mPcmStart.setOnClickListener(this);
        mPcmStop = findViewById(R.id.pcm_stop);
        mPcmStop.setOnClickListener(this);
        mPcmPlayStart = findViewById(R.id.pcm_play_start);
        mPcmPlayStart.setOnClickListener(this);
        mPcmPlayStop = findViewById(R.id.pcm_play_stop);
        mPcmPlayStop.setOnClickListener(this);
    }

    private void initData(){
        mPcmPath = FileUtils.getAudioPath(this) + "record.pcm";
        mPcmRecord = new PcmRecord();
        mPcmPlayer = new PcmPlayer();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.pcm_start) {
            mPcmRecord.start(mPcmPath);
        } else if (id == R.id.pcm_stop) {
            mPcmRecord.stop();
        } else if (id == R.id.pcm_play_start) {
            mPcmPlayer.start(mPcmPath);
        } else if (id == R.id.pcm_play_stop) {
            mPcmPlayer.stop();
        }
    }
}
