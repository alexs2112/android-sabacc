package com.sabacc;

import com.badlogic.gdx.utils.Timer;

public class SabaccTimer {
    public boolean ended;
    public boolean started;

    private Timer.Task task;

    public SabaccTimer() {
        reset();
    }

    public void reset() {
        ended = false;
        started = false;
    }

    /**
     * Will wait a specified time, after which ended will be set as true
     * @param delay the number of seconds to wait for
     */
    public void call(float delay) {
        started = true;
        System.out.println("Timer Started");
        task = new Timer.Task() {
            @Override
            public void run() {
                System.out.println("Timer Ended");
                started = false;
                ended = true;
            }
        };
        Timer.instance().scheduleTask(task, delay);
    }
}
