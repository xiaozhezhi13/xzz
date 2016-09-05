package com.parrot.freeflight.activities.image;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.parrot.freeflight.activities.game.GameCommand;
import com.parrot.freeflight.ui.gl.GLBGVideoSprite;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.Arrays;

/**
 * Created by shisy13 on 16/8/23.
 */
public class ImageToCommand {
    private final String LOG_TAG = getClass().getSimpleName();

    Context context;
    GLBGVideoSprite glbgVideoSprite;

    int lastCnt = 0;

    public ImageToCommand(Context context){
        this.context = context;
        glbgVideoSprite = new GLBGVideoSprite(context.getResources());
        glbgVideoSprite.setAlpha(1.0f);
    }

    float lastpower = 0.0f;

//    public GameCommand getCommand(){
//        GameCommand command = new GameCommand();
//        // read photo to bitmap
//        Bitmap bitmap = loadImage();
//        Mat mat = new Mat();
//        Utils.bitmapToMat(bitmap, mat);
//        // use CJM
//        Mat hsv = ImageProcessor.hsvFilter(mat);
//        Point[] line = ImageProcessor.findLinesP(mat);
//        Utils.matToBitmap(hsv, bitmap);
//        PointF[] points = ImageProcessor.centroid(bitmap);
//        if (!bitmap.isRecycled()){
//            bitmap.recycle();
//            System.gc();
//        }
//        float offset = (float) Math.sqrt(points[0].x*points[0].x+points[1].x*points[1].x);
//
//        float power = (float) (Math.pow(2, offset)-1);
//        power = (float) (Math.pow(2, power) - 1) / 300;
////        power = power - lastpower;
////        lastpower = power;
//
//        float yawthre = 0.05f;
//        float rollthre = 0.1f;
//        float kthre = 2.0f;
//
//        float k;
//        if (line == null)
//            k = 100.0f;
//        else
//            k = (float) ((line[0].y - line[1].y)/(line[0].x - line[1].x));
//
//        if (points[0].x < -1 && points[0].y < -1 && points[1].x < -1 && points[1].y < -1){
//            command.command = "stable";
//        }
//        else if (points[0].x < -1 && points[0].y < -1 && (points[1].x > -1 || points[1].y > -1)  && Math.abs(k) > kthre){
//            command.pitch = power;
//        }
//        else if (points[1].x < -1 && points[1].y < -1 && (points[0].x > -1 || points[0].y > -1)  && Math.abs(k) > kthre){
//            command.pitch = -power;
//        }
//        else {
//            Log.d(LOG_TAG, "k:" + k);
//            if (points[0].x < -rollthre && points[1].x < -rollthre && Math.abs(k) > kthre){
//                command.roll = -power;
//            }
//            else if (points[0].x > rollthre && points[1].x > rollthre && Math.abs(k) > kthre){
//                command.roll = power;
//            }
//            else if (k > 0 && k < 0.5) {
//                command.yaw = (float) 0.1/k;
//            }
//            else if (k < 0 && k >-0.5){
//                command.yaw = (float) 0.1/k;
//            }
//        }
//
//        Log.d(LOG_TAG, "center:"+points[0].x + "," + points[0].y + ";" + points[1].x + "," + points[1].y);
//        Log.d(LOG_TAG, "command:" + command.pitch + "," + command.roll + "," + command.yaw);
//        return command;
//    }

    public GameCommand getCommand(){
        GameCommand command = new GameCommand();
        // read photo to bitmap
        Bitmap bitmap = loadImage();
        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);
        // use CJM
//        Mat hsv = ImageProcessor.hsvFilter(mat);
        Point[] line = ImageProcessor.findLinesP(mat);
//        Utils.matToBitmap(hsv, bitmap);
//        PointF[] points = ImageProcessor.centroid(bitmap);
        if (!bitmap.isRecycled()){
            bitmap.recycle();
            System.gc();
        }

        float k;
        if (line == null) {
            k = 100.0f;
            command.command = "stable";
        }
        else {
            k = (float) ((line[0].y - line[1].y) / (line[0].x - line[1].x));
            Point center = new Point((line[0].x+line[1].x)/2, (line[0].y+line[1].y)/2);

            float rollThre = 0.1f;
            float kThre = 2.0f;
            float pitchThre = 0.4f;

            if (Math.abs(center.x) > rollThre){
                int sign = 1;
                if (center.x < 0)
                    sign = -1;
                float power = (float) (Math.pow(2, Math.abs(center.x))-1);
                power = (float) (Math.pow(2, power) - 1) / 300;
                command.roll = sign * power;
            }
            if (Math.abs(center.y) > pitchThre){
                int sign = 1;
                if (center.y < 0)
                    sign = -1;
                float power = (float) (Math.pow(2, Math.abs(center.y))-1);
                power = (float) (Math.pow(2, power) - 1) / 100;
                command.pitch = sign * power;
            }
            if (Math.abs(k) < kThre ){
                int sign = 1;
                if (k < 0)
                    sign = -1;
                float power = (float) (Math.pow(2, Math.abs((2-k)/2))-1);
                command.yaw = sign * power;
            }
            Log.d(LOG_TAG, "line:"+line[0].x + "," + line[0].y + ";" + line[1].x + "," + line[1].y);
        }


        Log.d(LOG_TAG, "command:" + command.pitch + "," + command.roll + "," + command.yaw);
        return command;
    }


    private Bitmap loadImage(){
//        // find image path of Android
//        FilenameFilter filenameFilter = new FilenameFilter() {
//            @Override
//            public boolean accept(File dir, String filename) {
//                return filename.toLowerCase().endsWith(".jpg");
//            }
//        };
//        File[] images = ardroneDir.listFiles(filenameFilter);
//
//        // wait for the photo ready
//        while (images.length == lastCnt){
//            try {
//                Thread.sleep(10);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            images = ardroneDir.listFiles(filenameFilter);
//        }
//        lastCnt = images.length;
//        Arrays.sort(images);
//        for (File image: images){
//            Log.d(LOG_TAG, image.getName());
//        }
//
//        // read the latest photo
//        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
//        Bitmap bitmap = BitmapFactory.decodeFile(images[images.length-1].getAbsolutePath(),bmOptions);


        while (!glbgVideoSprite.updateVideoFrame()){
        }
        Bitmap bitmap = glbgVideoSprite.getVideoBitmap();
        saveBitmap(bitmap);
        return bitmap;
    }

    private void saveBitmap(Bitmap bitmap){


        File imageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        Log.d(LOG_TAG, imageDir.getAbsolutePath());
        File ardroneDir = new File(imageDir, "AR.Drone");
        Log.d(LOG_TAG, ardroneDir.getAbsolutePath());
        File save = new File(ardroneDir, "test.jpg");

        try {
            FileOutputStream out = new FileOutputStream(save);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.MediaColumns.DATA, save.getAbsolutePath());
        context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }
}
