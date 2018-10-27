package com.lq.mediakit.media;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import com.lq.mediakit.R;
import com.lq.mediakit.widget.VideoSliceSeekBar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class VideoTrimAdapter extends RecyclerView.Adapter {

    private static final String TAG = VideoTrimAdapter.class.getSimpleName();

    private Context mContext;
    private List<Bitmap> mBitmaps;

    public float duration;
    private int mDurationLimit;

    private FrameExtractor10 mFrameExtractor;
    private VideoSliceSeekBar mSeekBar;

    private int screenWidth;


    private float perSecond;
    private int right;
    private int maxRight;

    private int pagePhotoCount = 8;
    public VideoTrimAdapter(Context context, long duration, int durationLimit,
                            FrameExtractor10 frameExtractor, VideoSliceSeekBar seekBar) {
        this.mContext = context;
        this.duration = duration;
        this.mDurationLimit = durationLimit;
        this.mFrameExtractor = frameExtractor;
        this.mSeekBar = seekBar;

        this.mBitmaps = new ArrayList<>();

        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        screenWidth = wm.getDefaultDisplay().getWidth();
    }

    public void addBitmap(Bitmap bitmap) {
        mBitmaps.add(bitmap);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new ViewHolder(inflater.inflate(R.layout.layout_video_thumb, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ViewHolder myHolder = (ViewHolder) holder;
        // myHolder.thumbImageView.setImageBitmap(mBitmaps.get(position));
        myHolder.thumbImageView.setImageBitmap(null);
        if (myHolder.task != null) {
            myHolder.task.cancel(false);
        }
        if (myHolder.bitmap != null) {
            myHolder.bitmap.recycle();
        }
        myHolder.bitmap = null;

        perSecond = duration / getItemCount() / 1000;

        // 需要加缓存
        myHolder.task = mFrameExtractor.newTask(myHolder, TimeUnit.SECONDS.toNanos((long) (position * perSecond)));
    }

    @Override
    public int getItemCount() {
        if((int)(duration/1000) > mDurationLimit){
            return Math.round((duration /1000/ mDurationLimit) * 8);
        }else{
            // 2s 取一帧 至少8帧
            return Math.max((int)duration / 1000 / 2, 8);
        }
    }

    public int getItemWidth() {
        return screenWidth / 8;
    }

    private class ViewHolder extends RecyclerView.ViewHolder implements FrameExtractor10.Callback {

        ImageView thumbImageView;
        Bitmap bitmap;
        AsyncTask<?, ?, ?> task;

        ViewHolder(View itemView) {
            super(itemView);
            thumbImageView = itemView.findViewById(R.id.iv_video_thumb);

            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) itemView.getLayoutParams();
            params.width = getItemWidth();
            itemView.setLayoutParams(params);
        }

        @Override
        public void onFrameExtracted(Bitmap bitmap, long timestamp) {
            if (bitmap != null) {
                this.bitmap = bitmap;
                thumbImageView.setImageBitmap(bitmap);
            }
        }
    }
}
