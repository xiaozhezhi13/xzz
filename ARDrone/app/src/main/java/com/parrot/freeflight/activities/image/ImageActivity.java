package com.parrot.freeflight.activities.image;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.parrot.freeflight.R;
import com.parrot.freeflight.ui.hud.Image;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.InstallCallbackInterface;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

public class ImageActivity extends Activity {
    final String LOG_TAG = getClass().getSimpleName();

    ImageProcessor imageProcessor;

    ImageView imageBefore;
    ImageView imageAfter;
    Bitmap before;
    Bitmap after;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        int screenWidth = size.x;
        int screenHeight = size.y;

        imageProcessor = new ImageProcessor();
        setContentView(R.layout.activity_image);
        imageBefore = (ImageView) findViewById(R.id.image_before);
        imageAfter = (ImageView) findViewById(R.id.image_after);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(screenWidth/2, screenHeight);
        imageBefore.setLayoutParams(layoutParams);
        imageAfter.setLayoutParams(layoutParams);

        before = BitmapFactory.decodeResource(getResources(), R.drawable.cm100);
        imageBefore.setImageBitmap(before);
    }

    @Override
    protected void onResume() {
        super.onResume();

        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                switch (status) {
                    case LoaderCallbackInterface.SUCCESS:
                    {
                        Log.i(LOG_TAG, "OpenCV loaded successfully");
                        after = imageProcessor.processImage(before);
                        imageAfter.setImageBitmap(after);
                    } break;
                    default:
                    {
                        super.onManagerConnected(status);
                    } break;
                }
            }
        });
    }

    @Override
    protected void onPause() {
        if (!before.isRecycled())
            before.recycle();
        if (!after.isRecycled())
            after.recycle();
        System.gc();
        super.onPause();
    }
}
