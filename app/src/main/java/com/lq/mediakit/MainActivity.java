package com.lq.mediakit;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private TextView clip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        final TextView tv = (TextView) findViewById(R.id.sample_text);
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String srcUrl = new File(Environment.getExternalStorageDirectory(),"b.mp4").getAbsolutePath();
                String water = new File(Environment.getExternalStorageDirectory(),"bw.gif").getAbsolutePath();
                String out = new File(Environment.getExternalStorageDirectory(),"output1.mp4").getAbsolutePath();
                MediaHelper.getInstance().addGifWater(srcUrl, water, out,298,253,0.5f,0.5f, new MediaHelper.CallBack() {
                    @Override
                    public void onStart() {
                        Log.e(TAG,"完成");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tv.setText("开始");
                            }
                        });
                    }

                    @Override
                    public void onProgress(final int progress, final int duration) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tv.setText(progress * 100 / duration + "%");
                            }
                        });
                        if (duration > 0) {
                            Log.e(TAG,progress + "  " + duration);
                        }
                    }

                    @Override
                    public void onEnd() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tv.setText("完成");
                            }
                        });

                        Log.e(TAG,"完成");
                    }
                });
            }
        });
        clip = findViewById(R.id.clip);
        clip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String srcUrl = new File(Environment.getExternalStorageDirectory(),"d1.mp4").getAbsolutePath();
                String out = new File(Environment.getExternalStorageDirectory(),"out_d1.mp4").getAbsolutePath();
                MediaHelper.getInstance().videoClips(srcUrl, out,"00:00:08","00:00:10",new MediaHelper.CallBack() {
                    @Override
                    public void onStart() {
                        Log.e(TAG,"完成");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                clip.setText("开始");
                            }
                        });
                    }

                    @Override
                    public void onProgress(final int progress, final int duration) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                clip.setText(progress * 100 / duration + "%");
                            }
                        });
                        if (duration > 0) {
                            Log.e(TAG,progress + "  " + duration);
                        }
                    }

                    @Override
                    public void onEnd() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                clip.setText("完成");
                            }
                        });

                        Log.e(TAG,"完成");
                    }
                });
            }
        });
    }

}
