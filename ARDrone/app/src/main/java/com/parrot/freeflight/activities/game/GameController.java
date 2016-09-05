package com.parrot.freeflight.activities.game;

import android.util.Log;

import com.parrot.freeflight.activities.image.ImageToCommand;
import com.parrot.freeflight.service.DroneControlService;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by shisy13 on 16/8/24.
 */
public class GameController {
    final String LOG_TAG = getClass().getSimpleName();
    GameActivity gameActivity;
    DroneControlService controlService;

    Timer commandTimer;
    Thread controlThread;
    ImageToCommand imageToCommand;

    int ardroneStatus = 0;

    public GameController(GameActivity gameActivity, DroneControlService controlService){
        this.gameActivity = gameActivity;
        this.controlService = controlService;
        imageToCommand = new ImageToCommand(gameActivity);
        commandTimer = new Timer();
    }

    public void start(){
        final float gaz_roll_mod = -0.00175f;
        final float pitch_roll_mod = -0.002f;
        final float pitch_power = 0.01f;
        controlThread = new Thread(new Runnable() {
            @Override
            public void run() {
//                controlService.switchCamera(); //switch to the camera below
                controlService.triggerTakeOff();
                try {
                    Thread.sleep(3000);     // wait takeoff
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
//                controlService.moveForward(power); // slowly move forward

                controlService.setGaz(1.0f);
//                controlService.setRoll(roll_mod);
//                controlService.moveForward(0.1f);
                try {
                    Thread.sleep(6000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                controlService.setGaz(0.0f);

                while (ardroneStatus == 0 && !Thread.currentThread().isInterrupted()){

                    GameCommand command = imageToCommand.getCommand();

                    if (command.command.equals("stable")){
                        controlService.setProgressiveCommandEnabled(false);
                        controlService.setYaw(0.0f);
                        controlService.setRoll(0.0f);
                        controlService.setPitch(0.0f);
                    }
                    else {
                        if (command.yaw != 0){
                            controlService.setProgressiveCommandCombinedYawEnabled(true);
                            controlService.setYaw(command.yaw);
                        }
                        else {
                            controlService.setProgressiveCommandCombinedYawEnabled(false);
                            controlService.setYaw(0.0f);
                        }
                        controlService.setProgressiveCommandEnabled(true);
                        if (command.pitch != 0){
                            controlService.setPitch(command.pitch);
                        }
                        else if (command.yaw == 0){
                            controlService.setPitch(pitch_power);
                        }
                        else {
                            controlService.setPitch(0.0f);
                        }
                        if (command.roll != 0){
                            controlService.setRoll(command.roll);
                        }
                        else {
                            controlService.setRoll(0.0f);
                        }
                    }
//                    else if (command.pitch != 0){
//                        controlService.setYaw(0.0f);
//                        controlService.setRoll(0.0f);
////                        controlService.setPitch(0.0f);
//                        if (command.pitch < 0)
//                            controlService.setPitch(-pitch_power);
//                        else
//                            controlService.setPitch(pitch_power);
//                        controlService.setProgressiveCommandEnabled(true);
//                    }
//                    else if (command.roll != 0){
//                        controlService.setProgressiveCommandCombinedYawEnabled(false);
//                        controlService.moveForward(pitch_power);
//                        controlService.setYaw(0.0f);
//                        controlService.setRoll(command.roll);
//                        controlService.setProgressiveCommandEnabled(true);
//                    }
//                    else if (command.yaw != 0){
//                        controlService.setProgressiveCommandCombinedYawEnabled(false);
//                        controlService.setProgressiveCommandEnabled(false);
//                        controlService.moveForward(0.0f);
//                        controlService.setRoll(0.0f);
//                        controlService.setYaw(command.yaw);
//                        try {
//                            Thread.sleep(300);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                    else {
//                        controlService.setProgressiveCommandCombinedYawEnabled(false);
//                        controlService.setRoll(0.0f);
//                        controlService.setYaw(0.0f);
////                        controlService.moveForward(0.0f);
//                        controlService.moveForward(pitch_power);
//                        controlService.setProgressiveCommandEnabled(true);
//                    }
                }
            }
        });

        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, gameActivity, new BaseLoaderCallback(gameActivity) {
            @Override
            public void onManagerConnected(int status) {
                switch (status) {
                    case LoaderCallbackInterface.SUCCESS:
                    {
                        Log.i(LOG_TAG, "OpenCV loaded successfully");
                        controlThread.start();
                    } break;
                    default:
                    {
                        super.onManagerConnected(status);
                    } break;
                }
            }
        });
    }

    public void stop(){
        controlService.triggerTakeOff();
        ardroneStatus = -1;
        controlThread.interrupt();
    }
}
