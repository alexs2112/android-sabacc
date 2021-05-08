package com.sabacc.gamestage;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.sabacc.GameScreen;

/**
 * One large button to start the next round of play
 */
public class NextRoundStage implements GameStage {
    private final GameScreen main;
    private final FitViewport viewport;

    private final Stage stage;

    public NextRoundStage(final GameScreen main, final FitViewport viewport) {
        this.main = main;
        this.viewport = viewport;
        stage = new Stage(viewport);

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = main.game.font32;
        buttonStyle.up = main.uiSkin.getDrawable("button1-up");
        buttonStyle.down = main.uiSkin.getDrawable("button1-down");

        TextButton startButton = new TextButton("Start Next Round", buttonStyle);
        startButton.setWidth(600);
        startButton.setHeight(128);
        startButton.setPosition(0,0);
        startButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                main.startNewRound();
            }
        });

        stage.addActor(startButton);
    }

    @Override
    public void show() {
        stage.draw();
    }

    @Override
    public void start() {
        main.setStageInput(stage);
    }

    @Override
    public void aiAction() {

    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
