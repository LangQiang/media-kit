package com.lq.mediakit;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        //System.loadLibrary("native-lib");

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        final TextView tv = (TextView) findViewById(R.id.sample_text);
        //tv.setText(stringFromJNI());
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //startActivity(new Intent(MainActivity.this, EncryptActivity.class));
                String srcUrl = new File(Environment.getExternalStorageDirectory(),"b.mp4").getAbsolutePath();
                String water = new File(Environment.getExternalStorageDirectory(),"bw.gif").getAbsolutePath();
                String out = new File(Environment.getExternalStorageDirectory(),"output1.mp4").getAbsolutePath();
                MediaHelper.getInstance().addGifWater(srcUrl, water, out,298,253,0.5f,0.5f, new MediaHelper.CallBack() {
                    @Override
                    public void onStart() {
                        Log.e("progress","完成");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tv.setText("开始");
                            }
                        });
//                        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
//                            Log.e("progress","start UI线程:" + Thread.currentThread().getName());
//
//                        } else {
//                            Log.e("progress","start 子线程:" + Thread.currentThread().getName());
//                        }
                    }

                    @Override
                    public void onProgress(final int progress, final int duration) {
//                        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
//                            Log.e("progress","progress UI线程:" + Thread.currentThread().getName());
//
//                        } else {
//                            Log.e("progress","progress 子线程:" + Thread.currentThread().getName());
//                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tv.setText(progress * 100 / duration + "%");
                            }
                        });
                        if (duration > 0) {
                            Log.e("progress",progress + "  " + duration);
                        }
                    }

                    @Override
                    public void onEnd() {
//                        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
//                            Log.e("progress","onEnd UI线程:" + Thread.currentThread().getName());
//
//                        } else {
//                            Log.e("progress","onEnd 子线程:" + Thread.currentThread().getName());
//                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tv.setText("完成");
                            }
                        });

                        Log.e("progress","完成");
                    }
                });
            }
        });
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
}
