package com.luohb.ffmpeg;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.luohb.ffmpeg.ui.activity.FFmpegAPIActivity;
import com.luohb.ffmpeg.ui.activity.PcmActivity;

import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks, View.OnClickListener {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.audio_activity).setOnClickListener(this);
        findViewById(R.id.ffmpeg_api_activity).setOnClickListener(this);

        String[] permission = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA};
        if (EasyPermissions.hasPermissions(this,permission)) {
            //todo
        } else {
            EasyPermissions.requestPermissions(this, "申请权限",10001,permission);
        }
    }


    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.audio_activity) {
            Intent intent = new Intent();
            intent.setClass(this, PcmActivity.class);
            startActivity(intent);
        } else if (id == R.id.ffmpeg_api_activity) {
            Intent intent = new Intent(this, FFmpegAPIActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        //todo
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String ffmpegInfo();
    public native void swrInit();
}