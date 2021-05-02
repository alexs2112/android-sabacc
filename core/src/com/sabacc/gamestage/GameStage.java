package com.sabacc.gamestage;

import com.badlogic.gdx.scenes.scene2d.Stage;

public interface GameStage {

    /**
     * Called each time the current screen is rendered, mostly drawing stages belonging to
     * this object
     */
    public abstract void show();

    /**
     * Starts this game stage. Usually involves toggling different things on players and
     * setting the input processor to the current stage
     */
    public abstract void start();

    /**
     * Is triggered when the current screen is rendered with nextEvent set to true
     * This allows us to time events to give some delay between ai actions
     */
    public abstract void aiAction();

    /**
     * Call this when we are done with this GameStage
     */
    public abstract void dispose();

}
