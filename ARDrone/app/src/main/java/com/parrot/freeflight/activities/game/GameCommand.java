package com.parrot.freeflight.activities.game;

/**
 * Created by shisy13 on 16/8/24.
 */
public class GameCommand {
    public String command;
    public float gaz;
    public float roll;
    public float pitch;
    public float yaw;

    public GameCommand(){
        this.command = "";
        gaz = 0;
        roll = 0;
        pitch = 0;
        yaw = 0;
    }
}
