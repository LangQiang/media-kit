package com.lq.mediakit;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.lq.mediakit.media.FFmpegWrapper;
import com.lq.mediakit.media.FrameExtractor10;
import com.lq.mediakit.media.VideoTrimAdapter;
import com.lq.mediakit.utils.VideoUtils;
import com.lq.mediakit.widget.VideoSliceSeekBar;

import java.io.File;


public class VideoTrimActivity extends Activity implements View.OnClickListener {

    private static final String TAG = VideoTrimActivity.class.getSimpleName();

    private static final int MIN_CROP_LENGTH = 5;
    private static final int MAX_CROP_LENGTH = 10;

    private VideoView mVideoView;
    private Uri mVideoUri;

    private RecyclerView mRecyclerView;
    private VideoTrimAdapter mAdapter;

    private VideoSliceSeekBar mSeekBar;
    private TextView mDurationView;
    private ProgressDialog mProgressDialog;

    public FrameExtractor10 mFrameExtractor;

    private int mScreenWidth;
    private int mScreenHeight;

    private float mDuration;
    // milliseconds
    private float mStartTime;
    private float mEndTime;

    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mScreenWidth = getResources().getDisplayMetrics().widthPixels;
        mScreenHeight = getResources().getDisplayMetrics().heightPixels;

        Log.d(TAG, "screen_width=" + mScreenWidth + " screen_height=" + mScreenHeight);

        Intent intent = getIntent();
        mVideoUri = intent.getParcelableExtra("video_uri");

        setContentView(R.layout.activity_video_trim);
        initView();

        FFmpegWrapper.getInstance(this);
    }

    private void initView() {
        initVideoView();
        initRecyclerView();

        mDurationView = findViewById(R.id.tv_video_crop_duration);

        mSeekBar = findViewById(R.id.seek_bar_video_crop);
        mSeekBar.setSeekBarChangeListener(mSeekBarListener);

        findViewById(R.id.btn_video_trim).setOnClickListener(this);
        findViewById(R.id.btn_video_crop).setOnClickListener(this);
        findViewById(R.id.btn_video_compress).setOnClickListener(this);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle(null);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    }

    private void initVideoView() {
        mVideoView = findViewById(R.id.vv_video_crop);
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                onVideoPrepared();
            }
        });

        mVideoView.setVideoURI(mVideoUri);
        mVideoView.start();
    }

    private void initRecyclerView() {
        mRecyclerView = findViewById(R.id.recycler_view_video_crop);
        mRecyclerView.addOnScrollListener(mRecyclerViewScrollListener);

        LinearLayoutManager manager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        mRecyclerView.setLayoutManager(manager);

        mFrameExtractor = new FrameExtractor10();
        mFrameExtractor.setDataSource(VideoUtils.getVideoPath(this, mVideoUri));

        mAdapter = new VideoTrimAdapter(this, mVideoView.getDuration(), Integer.MAX_VALUE, mFrameExtractor, mSeekBar);
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onClick(View v) {
        Toast.makeText(this, ((Button)v).getText(), Toast.LENGTH_SHORT).show();
        switch (v.getId()) {
            case R.id.btn_video_trim:
                trimVideo();
                break;
            case R.id.btn_video_crop:
                cropVideo();
                break;
            case R.id.btn_video_compress:
                compressVideo();
                break;
        }
    }

    private void trimVideo() {
        String srcPath = VideoUtils.getVideoPath(this, mVideoUri);
        String destPath = new File(Environment.getExternalStorageDirectory(), "trim.mp4").getAbsolutePath();
        // String startTime = String.format("%.1f", mStartTime / 1000);
        // String endTime = String.format("%.1f", mEndTime / 1000);

        FFmpegWrapper.getInstance(this).trimVideo(srcPath, destPath, mStartTime/1000, mEndTime/1000, mFFmpegCallback);
    }

    private void cropVideo() {
        String srcPath = VideoUtils.getVideoPath(this, mVideoUri);
        String destPath = new File(Environment.getExternalStorageDirectory(), "crop.mp4").getAbsolutePath();

        Rect rect = new Rect(100, 100, 500, 500);
        FFmpegWrapper.getInstance(this).cropVideo(srcPath, destPath, rect, mFFmpegCallback);
    }

    private void compressVideo() {
        String srcPath = VideoUtils.getVideoPath(this, mVideoUri);
        String destPath = new File(Environment.getExternalStorageDirectory(), "compress.mp4").getAbsolutePath();

        FFmpegWrapper.getInstance(this).compressVideo(srcPath, destPath, mFFmpegCallback);
    }

    private void pauseVideo() {
        if (mVideoView.isPlaying()) {
            mVideoView.pause();
        }
    }

    private void resumeVideo() {
        if (!mVideoView.isPlaying()) {
            mVideoView.start();
        }
    }

    private void onVideoPrepared() {
        mDuration = mVideoView.getDuration();

        int minDiff = (int)(MIN_CROP_LENGTH / mDuration * 100) + 1;
        mSeekBar.setProgressMinDiff(minDiff > 100 ? 100 : minDiff);

        mAdapter.duration = mDuration;
        mAdapter.notifyDataSetChanged();
    }

    /****************************************/
    /************** Callbacks ***************/
    /****************************************/

    private int mRecyclerViewOffsetX;
    private boolean shouldUpdateCropDuration;

    private final RecyclerView.OnScrollListener mRecyclerViewScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            shouldUpdateCropDuration = true;
            if (newState == 1) {
                pauseVideo();
            } else {
                resumeVideo();
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            mRecyclerViewOffsetX = recyclerView.computeHorizontalScrollOffset();
            onVideoCropSectionChange(0, 0);
        }
    };

    /* 距离做屏幕边缘 */
    private float mLeftThumbXInScreen = 0;
    private float mRightThumbXInScreen = -1;

    private final VideoSliceSeekBar.SeekBarChangeListener mSeekBarListener = new VideoSliceSeekBar.SeekBarChangeListener() {
        /**
         * SeekBar 滑动回调
         * @param leftThumb  左侧按钮在 SeekBar 中的位置（0-100）
         * @param rightThumb 右侧按钮在 SeekBar 中的位置（0-100）
         * @param whichSide 0 左侧；1 右侧
         */
        @Override
        public void seekBarValueChanged(float leftThumb, float rightThumb, int whichSide) {
            // 距离屏幕左边缘的距离
            mLeftThumbXInScreen  = (int)mSeekBar.getX() + (int)(mSeekBar.getWidth() * leftThumb/100);
            mRightThumbXInScreen = (int)mSeekBar.getX() + (int)(mSeekBar.getWidth() * rightThumb/100);

            onVideoCropSectionChange(whichSide, 1);
        }

        @Override
        public void onSeekStart() {
            shouldUpdateCropDuration = true;
            pauseVideo();
        }

        @Override
        public void onSeekEnd() {
            resumeVideo();
        }
    };

    /**
     * 处理视频剪辑区域变化
     * @param whichSide 0 左边；1 右边；
     * @param source 0 RecyclerView 滑动触发； 1 SeekBar 滑动触发
     */
    private void onVideoCropSectionChange(int whichSide, int source) {
        if (mRightThumbXInScreen == -1) {
            mRightThumbXInScreen = mScreenWidth;
        }

        // Thumb 的位置系映射到 RecyclerView 上
        float leftThumbOffsetX = mRecyclerViewOffsetX + mLeftThumbXInScreen;
        float rightThumbOffsetX = mRecyclerViewOffsetX + mRightThumbXInScreen;

        // 计算百分比
        float totalWidth = mAdapter.getItemWidth() * mAdapter.getItemCount();

        float seekPos;
        // 滑动 RecyclerView 时使用 LeftThumb
        if (whichSide == 0 || source == 0) {
            seekPos = (leftThumbOffsetX / totalWidth) * mDuration;
        } else if (whichSide == 1) {
            seekPos = (rightThumbOffsetX / totalWidth) * mDuration;
        } else {
            return;
        }

        float cropDuration = (rightThumbOffsetX - leftThumbOffsetX) / totalWidth * mDuration;

        mStartTime = seekPos;
        mEndTime = seekPos + cropDuration;

        Log.d(TAG, String.format("seek from: %.1f, to: %.1f, len: %.1f, total_len: %.1f", mStartTime/1000, mEndTime/1000, (mEndTime - mStartTime)/1000, mDuration));

        if (mVideoView != null) {
            mVideoView.seekTo((int)seekPos);
        }

        if (shouldUpdateCropDuration) {
            mDurationView.setText(String.format("%.1fs", cropDuration / 1000));
        }
    }

    /**
     * FFmpeg 回调
     */
    private FFmpegWrapper.Callback mFFmpegCallback = new FFmpegWrapper.Callback() {
        @Override
        public void onStart() {
            mProgressDialog.setProgress(0);
            mProgressDialog.show();
        }

        @Override
        public void onFinish() {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mProgressDialog.hide();
                }
            }, 3);
        }

        @Override
        public void onProgress(String progress) {
            Log.d(TAG, "onProgress: " + progress);
            mProgressDialog.setProgress((int)(Float.valueOf(progress) * 100));
            mProgressDialog.show();
        }

        @Override
        public void onSuccess(String msg) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mProgressDialog.hide();
                }
            }, 3);
        }

        @Override
        public void onFailure(String msg) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mProgressDialog.hide();
                }
            }, 3);
        }
    };
}
