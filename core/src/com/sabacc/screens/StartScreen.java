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
import com.sabacc.Sabacc;

public class StartScreen implements Screen {
    final private Sabacc game;
    final private Skin uiSkin;
    final private OrthographicCamera camera;
    final private FitViewport viewport;
    final private TextButton.TextButtonStyle buttonStyle;

    // Keep track of stages
    private Stage stage;

    public StartScreen(final Sabacc game, final Skin uiSkin, final OrthographicCamera camera, final FitViewport viewport) {
        this.game = game;
        this.uiSkin = uiSkin;
        this.camera = camera;
        this.viewport = viewport;

        // Set the main button style
        buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = game.font32;
        buttonStyle.up = uiSkin.getDrawable("button1-up");
        buttonStyle.down = uiSkin.getDrawable("button1-down");

        // Initialize all the different stages
        initializeStage();
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
        stage.dispose();
    }

    private void initializeStage() {
        stage = new Stage(viewport);

        int y = 600;
        TextButton startButton = new TextButton("Start New Game", buttonStyle);
        startButton.setWidth(540);
        startButton.setHeight(112);
        startButton.setPosition(30,y);
        startButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                game.setScreen(new NewGameScreen(game, uiSkin, camera, viewport));
            }
        });
        stage.addActor(startButton);

        y -= 112;
        TextButton rulesButton = new TextButton("Rules", buttonStyle);
        rulesButton.setWidth(540);
        rulesButton.setHeight(112);
        rulesButton.setPosition(30,y);
        rulesButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                //game.setScreen(new GameScreen(game, uiSkin, camera, viewport, 5));
            }
        });
        stage.addActor(rulesButton);

        y -= 112;
        TextButton settingsButton = new TextButton("Settings", buttonStyle);
        settingsButton.setWidth(540);
        settingsButton.setHeight(112);
        settingsButton.setPosition(30,y);
        settingsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                //game.setScreen(new GameScreen(game, uiSkin, camera, viewport, 5));
            }
        });
        stage.addActor(settingsButton);

        // A few "buttons" that are used only to center text
        TextButton.TextButtonStyle textStyle = new TextButton.TextButtonStyle();
        textStyle.font = game.font32;
        textStyle.up = uiSkin.getDrawable("background");
        TextButton title = new TextButton("S A B A C C", textStyle);
        title.setWidth(600);
        title.setHeight(112);
        title.setPosition(0,760);
        stage.addActor(title);

        //textStyle.font = game.msgFont;
        textStyle.font = game.font24;
        TextButton author = new TextButton("Urist2112", textStyle);
        author.setWidth(600);
        author.setHeight(112);
        author.setPosition(0,32);
        stage.addActor(author);

        Gdx.input.setInputProcessor(stage);
    }
}
