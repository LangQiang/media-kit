package com.lq.mediakit;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.lq.mediakit.jni.MediaHelper;
import com.lq.mediakit.media.FrameExtractor10;
import com.lq.mediakit.utils.VideoUtils;
import com.lq.mediakit.widget.VideoSliceSeekBar;
import com.lq.mediakit.media.VideoTrimAdapter;


import java.io.File;
import java.util.Arrays;


public class VideoTrimActivity extends Activity implements View.OnClickListener {

    private static final String TAG = VideoTrimActivity.class.getSimpleName();

    private FFmpeg ffmpeg;
    private ProgressDialog progressDialog;

    private int screenWidth;
    private int screenHeight;

    private VideoView mVideoView;
    private Uri mVideoUri;

    private RecyclerView mRecyclerView;
    private VideoTrimAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private TextView mDurationTextView;
    private VideoSliceSeekBar mSeekBar;

    public FrameExtractor10 mFrameExtractor;

    private int cropDuration = 5000; // min
    private float duration;

    // milliseconds
    private float mStartTime;
    private float mEndTime;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        screenWidth = getResources().getDisplayMetrics().widthPixels;
        screenHeight = getResources().getDisplayMetrics().heightPixels;

        Log.d(TAG, "screen_width=" + screenWidth + " screen_height=" + screenHeight);

        Intent intent = getIntent();
        mVideoUri = intent.getParcelableExtra("video_uri");

        setContentView(R.layout.activity_video_trim);
        initView();

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(null);
        progressDialog.setCancelable(false);

        loadFFMpegBinary();


        String dest = new File(Environment.getExternalStorageDirectory(), "out.mp4").getAbsolutePath();
        File file = new File(dest);
        file.deleteOnExit();
    }

    /**
     * Load FFmpeg binary
     */
    private void loadFFMpegBinary() {
        try {
            if (ffmpeg == null) {
                Log.d(TAG, "ffmpeg : era nulo");
                ffmpeg = FFmpeg.getInstance(this);
            }
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onFailure() {
                    showUnsupportedExceptionDialog();
                }

                @Override
                public void onSuccess() {
                    Log.d(TAG, "ffmpeg : correct Loaded");
                }
            });
        } catch (FFmpegNotSupportedException e) {
            showUnsupportedExceptionDialog();
        } catch (Exception e) {
            Log.d(TAG, "EXception no controlada : " + e);
        }
    }

    private void showUnsupportedExceptionDialog() {
        new AlertDialog.Builder(VideoTrimActivity.this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Not Supported")
                .setMessage("Device Not Supported")
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        VideoTrimActivity.this.finish();
                    }
                })
                .create()
                .show();

    }

    /**
     * Executing ffmpeg binary
     */
    private void execFFmpegBinary(final String[] command) {
        try {
            ffmpeg.execute(command, new ExecuteBinaryResponseHandler() {
                @Override
                public void onFailure(String s) {
                    Log.d(TAG, "FAILED with output : " + s);
                }

                @Override
                public void onSuccess(String s) {
                    Log.d(TAG, "SUCCESS with output : " + s);
                }

                @Override
                public void onProgress(String s) {
                    Log.d(TAG, "Started command : ffmpeg " + command);
                        progressDialog.setMessage("progress : splitting video " + s);
                    Log.d(TAG, "progress : " + s);
                }

                @Override
                public void onStart() {
                    Log.d(TAG, "Started command : ffmpeg " + command);
                    progressDialog.setMessage("Processing...");
                    progressDialog.show();
                }

                @Override
                public void onFinish() {
                    Log.d(TAG, "Finished command : ffmpeg " + command);
                    progressDialog.dismiss();

                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            // do nothing for now
        }
    }


    private void initView() {
        initVideoView();
        initRecyclerView();

        mDurationTextView = findViewById(R.id.tv_video_crop_duration);

        mSeekBar = findViewById(R.id.seek_bar_video_crop);
        mSeekBar.setSeekBarChangeListener(mSeekBarListener);

        findViewById(R.id.btn_video_crop_cancel).setOnClickListener(this);
        findViewById(R.id.btn_video_crop_confirm).setOnClickListener(this);

        // ProgressBar
        mProgressBar = findViewById(R.id.pbar_video_crop);
        mProgressBar.setVisibility(View.INVISIBLE);
        mProgressBar.setMax(100);
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

        mLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mFrameExtractor = new FrameExtractor10();
        mFrameExtractor.setDataSource(VideoUtils.getVideoPath(this, mVideoUri));

        mAdapter = new VideoTrimAdapter(this, mVideoView.getDuration(), Integer.MAX_VALUE, mFrameExtractor, mSeekBar);
        mRecyclerView.setAdapter(mAdapter);
    }

    private void onVideoPrepared() {
        duration = mVideoView.getDuration();

        int minDiff = (int)(cropDuration / duration * 100) + 1;
        mSeekBar.setProgressMinDiff(minDiff > 100 ? 100 : minDiff);

        mAdapter.duration = duration;
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_video_crop_cancel:
                finish();
                Toast.makeText(this, "Cancel", Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn_video_crop_confirm:
//                cropVideo();
                trimVideoUsingFFmpegBinary();
                Toast.makeText(this, "Start Clip", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void trimVideoUsingFFmpegBinary() {
        String sour = VideoUtils.getVideoPath(this, mVideoUri);
        sour = new File(Environment.getExternalStorageDirectory(), "source.mp4").getAbsolutePath();
        String dest = new File(Environment.getExternalStorageDirectory(), "out.mp4").getAbsolutePath();
        String startTime = String.format("%.1f", mStartTime / 1000);
        String duration = String.format("%.1f", (mEndTime - mStartTime) / 1000);

        String[] complexCommand = {"-ss", "" + startTime, "-y", "-i", sour, "-t", "" + duration, "-c", "copy" , dest};
        execFFmpegBinary(complexCommand);
    }


    private ProgressBar mProgressBar;

    private void cropVideo() {
        String sour = VideoUtils.getVideoPath(this, mVideoUri);
        sour = new File(Environment.getExternalStorageDirectory(), "source.mp4").getAbsolutePath();
        String dest = new File(Environment.getExternalStorageDirectory(), "out.mp4").getAbsolutePath();
        String startTime = String.format("%.1f", mStartTime / 1000);
        String duration = String.format("%.1f", (mEndTime - mStartTime) / 1000);

        Log.d(TAG, "crop start: " + startTime + " length:" + duration);
        MediaHelper.getInstance().videoClips(sour, dest, startTime, duration, new MediaHelper.CallBack() {
            @Override
            public void onStart() {
                Log.d(TAG, "crop video started");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mProgressBar.setVisibility(View.VISIBLE);
                        mProgressBar.setProgress(0);
                    }
                });
            }

            @Override
            public void onProgress(final float progress, final float duration) {
                Log.d(TAG, "crop video in progress: " + progress + " duration: " + duration);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mProgressBar.setProgress((int)(progress/duration * 100));
                    }
                });
            }

            @Override
            public void onEnd() {
                Log.d(TAG, "crop video finished");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mProgressBar.setVisibility(View.INVISIBLE);
                    }
                });
            }
        });
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

    // Listeners

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
            mRightThumbXInScreen = screenWidth;
        }

        // Thumb 的位置系映射到 RecyclerView 上
        float leftThumbOffsetX = mRecyclerViewOffsetX + mLeftThumbXInScreen;
        float rightThumbOffsetX = mRecyclerViewOffsetX + mRightThumbXInScreen;

        // 计算百分比
        float totalWidth = mAdapter.getItemWidth() * mAdapter.getItemCount();

        float seekPos;
        // 滑动 RecyclerView 时使用 LeftThumb
        if (whichSide == 0 || source == 0) {
            seekPos = (leftThumbOffsetX / totalWidth) * duration;
        } else if (whichSide == 1) {
            seekPos = (rightThumbOffsetX / totalWidth) * duration;
        } else {
            return;
        }

        float cropDuration = (rightThumbOffsetX - leftThumbOffsetX) / totalWidth * duration;

        mStartTime = seekPos;
        mEndTime = seekPos + cropDuration;

        Log.d(TAG, String.format("seek from: %.1f, to: %.1f, len: %.1f, total_len: %.1f", mStartTime/1000, mEndTime/1000, (mEndTime - mStartTime)/1000, duration));

        if (mVideoView != null) {
            mVideoView.seekTo((int)seekPos);
        }

        if (shouldUpdateCropDuration) {
            mDurationTextView.setText(String.format("%.1fs", cropDuration / 1000));
        }
    }

}
