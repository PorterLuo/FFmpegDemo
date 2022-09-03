package com.luohb.ffmpeg.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.luohb.ffmpeg.R;

public class FFmpegAPIActivity extends AppCompatActivity implements View.OnClickListener {
    private Button mButton;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ffmpeg_api);
        initView();
    }

    private void initView(){
        mButton = findViewById(R.id.media_info_button);
        mButton.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.media_info_button) {
            Intent intent = new Intent(this, MediaInfoActivity.class);
            startActivity(intent);
        }
    }
}
