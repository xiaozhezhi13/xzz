package com.parrot.freeflight.activities.image;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.Log;
import android.widget.Toast;

import java.lang.Double;

import com.parrot.freeflight.ui.hud.Image;
import com.parrot.freeflight.updater.config.Config;
import com.parrot.freeflight.utils.SystemUtils;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Size;

import java.util.ArrayList;
import java.util.List;


//import java.lang.Object;

/**
 * Created by shisy13 on 16/8/22.
 */
public class ImageProcessor {

    static final String LOG_TAG = ImageProcessor.class.getSimpleName();

    static Bitmap processImage(Bitmap image) {
        Bitmap bitmap = image.copy(Bitmap.Config.ARGB_8888, false);//创造备份，防止原来的图像被破坏
        Mat matOring = new Mat();
        Utils.bitmapToMat(bitmap, matOring);
        Mat   mat = hsvFilter(matOring);

//        Mat erodeStruct= Imgproc.getStructuringElement(Imgproc.MORPH_ERODE,new Size(11,11));
//        Mat  dilateStruct=Imgproc.getStructuringElement(Imgproc.MORPH_DILATE,new Size(11,11));
//        Imgproc.erode(mat,mat,erodeStruct);  //图像腐蚀
//        Imgproc.dilate(mat,mat,dilateStruct);  //图像膨胀
        Double[] RedBall=lookForRedBall(mat);
        Point pt=new Point();    //球的中心点
        pt.x=(int)Math.round(RedBall[0]);
        pt.y=(int)Math.round(RedBall[1]);
        int radius =(int)Math.round((Double)RedBall[2]); //球半径

        Imgproc.circle(matOring,pt,radius,new Scalar(255,0,255,0),8);  //画在球的位置画绿色圆
        Log.e("50cm远红球的半径为:",radius+"个像素");
        Utils.matToBitmap(matOring, bitmap);
        return bitmap;
    }

    /**
     *
     * @param bmp  黑白两色图
     * @return
     */
    static Mat  findCircles(Mat bmp){
        Log.e("注意！","开始识别圆！");
        Mat circles = new Mat();
        Mat blackwhite = hsvFilter(bmp);
        Imgproc.HoughCircles(blackwhite, circles, Imgproc.CV_HOUGH_GRADIENT, 2, 10); //hough变换找圆

        //Imgproc.cvtColor(bmpGray,bmpMat,Imgproc.COLOR_GRAY2RGBA,4);
        Log.e("霍夫圆检测", "共检测出 " +  circles.cols()+"个圆"+circles.rows());

        for (int i=0; i < circles.cols();i++)
        {
            double circle[] = circles.get(0,i);
            if (circle==null)
                break;
            Point pt = new Point(Math.round(circle[0]),Math.round(circle[1]));
            int radius=(int)Math.round(circle[2]);

            Imgproc.circle(bmp,pt,radius,new Scalar(255,0,255,0), 8);
            Log.e("哈哈","已经画圆");
        }
        // bmp.recycle();
        return bmp;
    }


    /**
     * 路径在图像中一般呈平行四边形，计算形心的位置
     * 目的是：根据形心与图像中心的差，动态调整四旋翼的路径
     * 输入：经过处理后的二色图像（红与黑）
     * 以图形中心为坐标中心，x轴向右，y轴向上，各自范围[-1,1]
     * 返回路径中心的坐标，[-1,1]之间
     * 若返回[-2,-2]，表示图像中白点少于100个，摄像机未拍到路径
     * 上下两部分
     */

    static public PointF[] centroid(Bitmap bmp) {
        //  PointF pointF = new PointF();
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        int pixColor = 0; //像素信息
        int pixR = 0;
        int pixG = 0;
        int pixB = 0;
        int whiteUpNum = 0; //黑白图中，白点的总个数
        int whiteDownNum = 0; //黑白图中，白点的总个数
        PointF[] pointFs = new PointF[2];
        pointFs[0] = new PointF();
        pointFs[1] = new PointF();

        int[] pixels = new int[width * height];
        bmp.getPixels(pixels, 0, width, 0, 0, width, height);   //读取像素信息
        double centerXup = 0;  //形心的x坐标
        double centerXdown = 0;  //形心的x坐标
        double centerYup = 0; //形心的y坐标
        double centerYdown = 0; //形心的y坐标
        int halfHeight = height / 2;


        for (int i = 0; i < halfHeight; i++) {
            for (int j = 0; j < width; j++) {

                pixColor = pixels[i * width + j];
                pixR = Color.red(pixColor);
                pixG = Color.green(pixColor);
                pixB = Color.blue(pixColor);

                //如果红色通道大于0，则为红色，则累加centerx，centery,
                //否则为黑色，不累加
                if (pixR == 255 && pixG == 255 && pixB == 255) {
                    whiteUpNum = whiteUpNum + 1;
                    centerXup += j;
                    centerYup += (height - i);
                }

            }
        }

        centerXup = 2 * centerXup / whiteUpNum / width - 1;
        centerYup = 2 * centerYup / whiteUpNum / height - 1;
        pointFs[0].x = (float) centerXup;
        pointFs[0].y = (float) centerYup;
        Log.d(LOG_TAG, "whiteUpNum" + whiteUpNum);
        if (whiteUpNum < 1000) {
            pointFs[0].x = (float) -2.0;
            pointFs[0].y = (float) -2.0;

        }

        for (int i = halfHeight; i < height; i++) {
            for (int j = 0; j < width; j++) {

                pixColor = pixels[i * width + j];
                pixR = Color.red(pixColor);
                pixG = Color.green(pixColor);
                pixB = Color.blue(pixColor);

                //如果红色通道大于0，则为红色，则累加centerx，centery,
                //否则为黑色，不累加
                if (pixR == 255 && pixG == 255 && pixB == 255) {
                    whiteDownNum = whiteDownNum + 1;
                    centerXdown += j;
                    centerYdown += (height - i);
                }

            }
        }
        centerXdown = 2 * centerXdown / whiteDownNum / width - 1;
        centerYdown = 2 * centerYdown / whiteDownNum / height - 1;
        pointFs[1].x = (float) centerXdown;
        pointFs[1].y = (float) centerYdown;

        Log.d(LOG_TAG, "whiteDownNum" + whiteDownNum);
        if (whiteDownNum < 1000) {
            pointFs[1].x = (float) -2.0;
            pointFs[1].y = (float) -2.0;

        }

        return pointFs;
    }



    /**
     * @param origin
     * @return
     */
    static public Mat hsvFilter(Mat origin) {
//        Imgproc.erode(origin,origin,null);
        //   Imgproc.dilate(origin,origin,null);

        Mat originHSV = new Mat();
        Imgproc.cvtColor(origin, originHSV, Imgproc.COLOR_BGR2HSV, 3);

        Mat lower = new Mat();
        Mat upper = new Mat();
//        Core.inRange(originHSV, new Scalar(0, 80, 50), new Scalar(50, 255, 255), lower);
        Core.inRange(originHSV, new Scalar(0, 80, 70), new Scalar(0, 255, 255), lower);
//        Core.inRange(originHSV, new Scalar(120, 80, 50), new Scalar(179, 255, 255), upper);
        Core.inRange(originHSV, new Scalar(100, 80, 70), new Scalar(130, 255, 255), upper);

        Mat red = new Mat();
        Core.addWeighted(lower, 1.0, upper, 1.0, 0.0, red);
        Imgproc.GaussianBlur(red, red, new Size(9, 9), 2, 2);
        return red;
    }


    static public Mat findLines(Mat bmp){

        Mat blackwhite = hsvFilter(bmp);
        Mat lines = new Mat();
        Imgproc.HoughLinesP(blackwhite, lines,  1, Math.PI/180, 50, 200, 200);
        Point start;
        Point end;
        for (int x = 0; x < lines.cols(); x++)
        {
            double[] vec = lines.get(0, x);
            double x1 = vec[0],
                    y1 = vec[1],
                    x2 = vec[2],
                    y2 = vec[3];
            start = new Point(x1, y1);
            end = new Point(x2, y2);
            Imgproc.line(bmp, start, end, new Scalar(0,255,0), 3);
        }
        return bmp;
    }

    static public Point[] findLinesP(Mat bmp){
        Mat blackwhite = hsvFilter(bmp);
        Mat lines = new Mat();
        Imgproc.HoughLinesP(blackwhite, lines,  1, Math.PI/180, 50, 300, 200);
        Point start;
        Point end;
        double length = 0.0;
        Point[] points = new Point[2];
        if (lines.cols() == 0){
            return null;
        }
        for (int x = 0; x < lines.cols(); x++)
        {
            double[] vec = lines.get(0, x);
            double x1 = vec[0],
                    y1 = vec[1],
                    x2 = vec[2],
                    y2 = vec[3];
            start = new Point(x1, y1);
            end = new Point(x2, y2);
            double tmpl = Math.pow(start.x-end.x, 2) + Math.pow(start.y-end.y, 2);
            if (tmpl > length){
                points[0] = start;
                points[1] = end;
                length = tmpl;
            }
        }
        if (points[0].y < points[1].y){
            double tmp = points[0].y;
            points[0].y = points[1].y;
            points[1].y = tmp;
            tmp = points[0].x;
            points[0].x = points[1].x;
            points[1].x = tmp;
        }
        return points;
    }

    /**
     *定位红球
     *若发现多个红球，只返回半径最大的球
     *当未定位到红球的时候，返回[-2,-2,-2,-2,-2];
     * @param blackwhite 黑白两色图
     * @return double[5] 依次为：小球中心的x坐标，y坐标，半径大小，相对图像中心的x,y坐标(范围均为[-1, 1])
     */
    static Double[]  lookForRedBall(Mat blackwhite){

        Double[] redBall = {-2.0, -2.0, -2.0, -2.0, -2.0};
        int width=blackwhite.width();
        int height=blackwhite.height();
        int x=0;
        int y=0;
       int  iCannyUpperThreshold = 100;
        int iMinRadius = 20;
       int  iMaxRadius = 400;
        int  iAccumulator = 300;

        Double radius=0.0;
        double scale=2.0;
        double fx=scale;  //宽度放大系数
        double fy=scale;   //高度放大系数
        Size size=new Size();
        size.width=0.0;
        size.height=0.0;
        Mat resizedMat = new Mat();
       // Imgproc.resize(blackwhite,resizedMat,size,fx,fy,Imgproc.INTER_LINEAR);//线性插值，放大图形2倍
        Log.e("注意！","开始定位红球！");
        Mat circles = new Mat();
        long timePre= System.currentTimeMillis();
      //  Imgproc.HoughCircles(resizedMat, circles, Imgproc.CV_HOUGH_GRADIENT, 2,100,iCannyUpperThreshold,50,iMinRadius,400); //hough变换找圆
     Imgproc.HoughCircles(blackwhite, circles, Imgproc.CV_HOUGH_GRADIENT, 2,300,iCannyUpperThreshold,50,iMinRadius,400); //hough变换找圆
        long timePos= System.currentTimeMillis();
        long timeUsed=timePos-timePre;
        Log.e("函数HoughCircles用时：",""+timeUsed);
        Log.e("霍夫圆检测", "共检测出 " +  circles.cols()+"个球");
//        if (circles.cols()==0){     //如果未找到红球，直接返回
//            Log.e("错误!!!","定位红球失败!!!");
//            return redBall;
//        }
        if(circles.cols()>1){
            Log.e("警告！","红球个数多于一个！将只定位半径最大的红球！");
        }

        //寻找半径最大的红球
//        for (int i=0; i < circles.cols();i++)
//        {
            double circle[] = circles.get(0,0);
          //  if(circle[2]>radius)
         //   {
                redBall[0] = circle[0];
                redBall[1] = circle[1];
                redBall[2] = circle[2];
                redBall[3] = 2.0*circle[0]/width-1.0;
                redBall[4] = 2.0*circle[1]/height-1.0;
             //   radius=circle[2];
           // }
       // }
        Log.d("正常","已定位红球位置!");
//        if(resizedMat!=null)
//        {
//            resizedMat.release();
//        }

        return redBall;
    }





}
