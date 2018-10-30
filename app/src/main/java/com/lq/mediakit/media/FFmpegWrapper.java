package com.lq.mediakit.media;

import android.content.Context;
import android.graphics.Rect;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.FFmpegExecuteResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.lq.mediakit.utils.VideoUtils;

import java.io.File;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FFmpegWrapper {

    private static final String TAG = FFmpegWrapper.class.getSimpleName();

    public static final int ACTION_TRIM_VIDEO = 0x1;
    public static final int ACTION_CROP_VIDEO = 0x2;
    public static final int ACTION_COMPRESS_VIDEO = 0x3;

    public static final int ERROR_FFMPEG_NOT_LOADED = 0x10;
    public static final int ERROR_DEVICE_NOT_SUPPORED = 0x11;

    private static final String RE_TIME = ".*time=(.*?)\\s.*";

    private static FFmpegWrapper sInstance;

    private FFmpeg mFFmpeg;
    private Context mContext;

    private boolean isLoaded;

    public interface Callback extends FFmpegExecuteResponseHandler {}

    public static FFmpegWrapper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new FFmpegWrapper(context);
        }
        return sInstance;
    }

    private FFmpegWrapper(Context context) {
        this.mContext = context;
        loadFFMpegBinary();
    }

    /**
     * Load FFmpeg binary
     */
    private void loadFFMpegBinary() {
        if (isLoaded) return;
        try {
            if (mFFmpeg == null) {
                Log.d(TAG, "ffmpeg : era nulo");
                mFFmpeg = FFmpeg.getInstance(mContext);
            }

            mFFmpeg.loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onFailure() {
                    onFFmpegLoadFailed();
                }

                @Override
                public void onSuccess() {
                    isLoaded = true;
                    Log.d(TAG, "ffmpeg : correct Loaded");
                }
            });
        } catch (FFmpegNotSupportedException e) {
            onFFmpegLoadFailed();
        } catch (Exception e) {
            Log.d(TAG, "EXception no controlada : " + e);
            onFFmpegLoadFailed();
        }
    }

    private void onFFmpegLoadFailed() {
        isLoaded = false;
        Toast.makeText(mContext, "Load ffmpeg binary failed", Toast.LENGTH_LONG).show();
    }

    /**
     * Trim Video (Slice)
     */
    public void trimVideo(String srcPath, String destPath, float startTime, float endTime, Callback callback) {
        String[] complexCommand = {
                "-y",
                "-ss",
                "" + startTime,
                "-y",
                "-i", srcPath,
                "-to",
                "" + endTime,
                "-c", "copy",
                destPath
        };
        execFFmpegBinary(complexCommand, callback);
    }

    /**
     * Compress Video
     */
    public void compressVideo(String srcPath, String destPath, Callback callback) {
        Log.d(TAG, "");
        String[] complexCommand = {
                "-y",
                "-i", srcPath,
                "-s", "640x360",
                "-r", "24",
                "-vcodec", "mpeg4",
                "-b:v", "150k",
                "-b:a", "48000",
                "-ac", "2",
                "-ar", "22050",
                destPath
        };
        execFFmpegBinary(complexCommand, callback);
    }

    /**
     * Crop Video (Resolution)
     *
     * ffmpeg -i in.mp4 -filter:v "crop=out_w:out_h:x:y" out.mp4
     *
     * out_w is the width of the output rectangle (pixel)
     * out_h is the height of the output rectangle
     * x and y specify the top left corner of the output rectangle
     *
     * https://video.stackexchange.com/a/4571
     *
     * @param rect 计算是的时候使用的是比例，value = percent * 1000 （10.5% * 1000=105)，最多到1位小数
     */
    public void cropVideo(String srcPath, String destPath, Rect rect, Callback callback) {
        String crop = "crop=";
        crop += "in_w*" + rect.width() + "/1000:";
        crop += "in_h*" + rect.height() + "/1000:";
        crop += "in_w*" + rect.left + "/1000:";
        crop += "in_h*" + rect.top + "/1000";
        String[] complexCommand = {
                "-y",
                "-i", srcPath,
                "-filter:v", crop,
                "-c:a", "copy",
                destPath
        };
        execFFmpegBinary(complexCommand, callback);
    }

    /**
     * FFmpeg 是否可用
     */
    public boolean isAvailable() {
        return isLoaded;
    }

    private boolean isBusy() {
        return mFFmpeg.isFFmpegCommandRunning();
    }

    private boolean checkAvailability() {
        return isLoaded && !isBusy();
    }

    /**
     * Executing ffmpeg binary
     */
    private void execFFmpegBinary(final String[] command, final Callback callback) {
        if (!checkAvailability()) {
            callback.onFailure("FFmpeg binary load failed");
            return;
        }

        final long duration = getVideoDuration(command);

        Log.d(TAG, "exec command: " + Arrays.toString(command));
        try {
            mFFmpeg.execute(command, new ExecuteBinaryResponseHandler() {
                @Override
                public void onFailure(String msg) {
                    callback.onFailure(msg);
                    Log.d(TAG, "FAILED with output : " + msg);
                }

                @Override
                public void onSuccess(String msg) {
                    callback.onSuccess(msg);
                    Log.d(TAG, "SUCCESS with output : " + msg);
                }

                @Override
                public void onProgress(String output) {
                    float time = parseTime(output);
                    if (time > 0) {
                        callback.onProgress("" + time / (float)duration);
                    }
                    Log.d(TAG, "Started command : ffmpeg " + command);
                    Log.d(TAG, "progress : " + output);
                }

                @Override
                public void onStart() {
                    callback.onStart();
                    Log.d(TAG, "Started command : ffmpeg " + command);
                }

                @Override
                public void onFinish() {
                    callback.onFinish();
                    Log.d(TAG, "Finished command : ffmpeg " + command);

                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            callback.onFailure(e.toString());
        }
    }

    /**
     * 获取视频长度
     */
    private long getVideoDuration(String[] command) {
        for (int i = 0; i < command.length; i++) {
            if (command[i].equals("-i")) {
                return VideoUtils.getDuration(mContext, Uri.fromFile(new File(command[i+1])));
            }
        }
        return 0L;
    }

    /**
     * 解析进度
     *
     * Slice
     * frame=  196 fps= 19 q=31.0 size=     322kB time=00:00:08.35 bitrate= 315.9kbits/s dup=1 drop=0 speed=0.819xt
     *
     * Crop
     * frame=   84 fps= 14 q=29.0 size=      47kB time=00:00:03.08 bitrate= 123.6kbits/s speed=0.502x
     *
     * Compress
     * frame=   47 fps=4.9 q=31.0 size=     175kB time=00:00:02.96 bitrate= 483.4kbits/s dup=0 drop=9 speed=0.307x
     */
    private float parseTime(String output) {
        Pattern p = Pattern.compile(RE_TIME);
        Matcher m = p.matcher(output);
        if (!m.matches()) {
            return 0;
        }

        float time = 0;
        try {
            String timeStr = m.group(1);
            String[] components = timeStr.split(":");
            float hours = Float.valueOf(components[0]);
            float minutes = Float.valueOf(components[1]);
            float seconds = Float.valueOf(components[2]);
            time = hours * 60 * 60 + minutes * 60 + seconds;
        } catch (Exception e) {
            Log.d(TAG, "parse time error: " + e);
        }
        return time * 1000;
    }
}
