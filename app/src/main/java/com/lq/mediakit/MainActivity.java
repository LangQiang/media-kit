package com.lq.mediakit;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.lq.mediakit.utils.VideoUtils;

import android.widget.VideoView;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_PICK_GALLERY_VIDEO = 100;

    private VideoView mVideoView;
    private Button mPickVideoBtn;
    private Button mClipVideoBtn;
    private Button mCompressVideoBtn;
    private TextView mTextView;

    private Uri mSelectedVideoUri;
    private String mDestVideoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mVideoView = findViewById(R.id.vv_main_preview);

        mPickVideoBtn = findViewById(R.id.btn_main_pick_video);
        mClipVideoBtn = findViewById(R.id.btn_main_clip_video);
        mCompressVideoBtn = findViewById(R.id.btn_main_compress_video);

        mTextView = findViewById(R.id.tv_main_msg);

        mPickVideoBtn.setOnClickListener(this);
        mClipVideoBtn.setOnClickListener(this);
        mCompressVideoBtn.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mSelectedVideoUri != null) {
            mVideoView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mVideoView.isPlaying()) {
            mVideoView.pause();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_main_pick_video:
                pickVideo();
                break;
            case R.id.btn_main_clip_video:
                clipVideo();
                break;
            case R.id.btn_main_compress_video:
                compressVideo();
                break;
        }
        Toast.makeText(this, ((Button)v).getText(), Toast.LENGTH_SHORT).show();
    }

    private void pickVideo() {
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Video"), REQUEST_PICK_GALLERY_VIDEO);
    }

    private void clipVideo() {
        String srcVideoPath = VideoUtils.getVideoPath(this, mSelectedVideoUri);
        Toast.makeText(this, srcVideoPath, Toast.LENGTH_SHORT).show();

        Log.i(TAG, "selected video path: " + srcVideoPath);

        mTextView.setText(srcVideoPath);
    }

    private void compressVideo() {
        Intent intent = new Intent(this, VideoTrimActivity.class);
        intent.putExtra("video_uri", mSelectedVideoUri);
        startActivity(intent);
    }

    private void previewVideo(String videoPath) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || requestCode != REQUEST_PICK_GALLERY_VIDEO) return;

        mSelectedVideoUri = data.getData();

        Log.i(TAG, "Selected video path: " + mSelectedVideoUri);

        mVideoView.setVideoURI(mSelectedVideoUri);
        mVideoView.start();

        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mVideoView.seekTo(0);
                mVideoView.start();
            }
        });
    }
}
