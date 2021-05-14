package com.sabacc.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.sabacc.Player;
import com.sabacc.Sabacc;

public class GameOverScreen implements Screen {
    final private Player winner;

    final private Stage stage;

    public GameOverScreen(final Sabacc game, final Skin uiSkin, final OrthographicCamera camera, final FitViewport viewport, Player winner) {
        this.winner = winner;

        stage = new Stage(viewport);

        // A few "buttons" that are used only to center text
        TextButton.TextButtonStyle textStyle = new TextButton.TextButtonStyle();
        textStyle.font = game.font32;
        textStyle.up = uiSkin.getDrawable("background");
        TextButton title = new TextButton("Game Over", textStyle);
        title.setWidth(600);
        title.setHeight(112);
        title.setPosition(0,760);
        stage.addActor(title);

        String t = "Nobody won!";
        if (winner != null)
            t = winner.name() + " won, with " + winner.credits() + " credits!";
        textStyle.font = game.font24;
        TextButton author = new TextButton(t, textStyle);
        author.setWidth(600);
        author.setHeight(112);
        author.setPosition(0,730);
        stage.addActor(author);

        // Set the main button style
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = game.font32;
        buttonStyle.up = uiSkin.getDrawable("button1-up");
        buttonStyle.down = uiSkin.getDrawable("button1-down");

        // A button to go back to the start screen
        TextButton startButton = new TextButton("Main Menu", buttonStyle);
        startButton.setWidth(540);
        startButton.setHeight(112);
        startButton.setPosition(30,540);
        startButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                game.setScreen(new StartScreen(game, uiSkin, camera, viewport));
            }
        });
        stage.addActor(startButton);

        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
        // Clear the screen to the background colour
        ScreenUtils.clear(0,0,0.2f,1);

        // Draw some text and the stage
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {

    }
}
