package com.sabacc;

import com.badlogic.gdx.utils.Timer;

public class SabaccTimer {
    public boolean started;
    final private GameScreen screen;
    private Timer.Task task;

    public SabaccTimer(GameScreen s) {
        screen = s;
        started = false;
    }

    /**
     * Will wait a specified time, after which ended will be set as true
     * @param delay the number of seconds to wait for
     */
    public void call(float delay) {
        started = true;
        task = new Timer.Task() {
            @Override
            public void run() {
                started = false;
                if (screen.currentStage != null)
                    screen.currentStage.aiAction();
            }
        };
        Timer.instance().scheduleTask(task, delay);
    }

    /**
     *
     */
    public void timeDoubleTap(final Card c, float delay) {
        task = new Timer.Task() {
            @Override
            public void run() {
                // If the timer runs out and we are still waiting for the same double tap, set it to null
                if (c == screen.doubleTap)
                    screen.doubleTap = null;
                // Otherwise, ignore this
            }
        };
        Timer.instance().scheduleTask(task, delay);
    }
}
