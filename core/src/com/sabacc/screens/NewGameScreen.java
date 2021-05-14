package com.sabacc.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button.ButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.sabacc.Sabacc;

public class NewGameScreen implements Screen {
    final private Sabacc game;
    final private Skin uiSkin;
    final private Skin skin;
    final private Stage stage;
    final private OrthographicCamera camera;
    final private FitViewport viewport;

    // Some stats to pass into the game
    private int numOpponents;
    private int anteCost;
    private int startingCredits;
    private void defaultStats() {
        numOpponents = 5;
        anteCost = 20;
        startingCredits = 500;
    }

    // Keep track of each text box as they will repeatedly be edited
    private TextButton opponents;
    private TextButton ante;
    private TextButton credits;


    public NewGameScreen(final Sabacc game, final Skin uiSkin, final OrthographicCamera camera, final FitViewport viewport) {
        this.game = game;
        this.uiSkin = uiSkin;
        this.camera = camera;
        this.viewport = viewport;

        // Load the skin for this screen
        skin = new Skin();
        skin.addRegions(new TextureAtlas(Gdx.files.internal("initial.atlas")));

        // Set up buttons and stuff
        stage = new Stage(viewport);

        defaultStats();

        initializeButtons();
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
        // Clear the screen to the background colour
        ScreenUtils.clear(0,0,0.2f,1);

        // Update the camera
        camera.update();
        game.batch.setProjectionMatrix(camera.combined);

        // Draw some text and the stage
        game.batch.begin();
        stage.draw();
        game.batch.end();
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

    private void initializeButtons() {
        // Text box style
        TextButton.TextButtonStyle text = new TextButton.TextButtonStyle();
        text.font = game.font32;
        text.up = skin.getDrawable("textbox");

        // Centered text
        TextButton.TextButtonStyle titleStyle = new TextButton.TextButtonStyle();
        titleStyle.font = game.font32;
        titleStyle.up = uiSkin.getDrawable("background");
        TextButton title = new TextButton("NEW GAME", titleStyle);
        title.setPosition(0, 900);
        title.setWidth(600);
        title.setHeight(112);
        stage.addActor(title);

        // Increment button style
        ButtonStyle plusOne = new ButtonStyle();
        plusOne.up = skin.getDrawable("plus-one-up");
        plusOne.down = skin.getDrawable("plus-one-down");

        // Decrement button style
        ButtonStyle minusOne = new ButtonStyle();
        minusOne.up = skin.getDrawable("minus-one-up");
        minusOne.down = skin.getDrawable("minus-one-down");

        int y = 800;
        opponents = new TextButton("Opponents: " + numOpponents, text);
        opponents.setPosition(100, y);
        opponents.setWidth(400);
        opponents.setHeight(112);
        stage.addActor(opponents);

        Button incOpponents = new Button(plusOne);
        incOpponents.setWidth(96);
        incOpponents.setHeight(96);
        incOpponents.setPosition(501,y + 8);
        incOpponents.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                if (numOpponents < 7) {
                    numOpponents++;
                    opponents.setText("Opponents: " + numOpponents);
                }
            }
        });
        stage.addActor(incOpponents);

        Button decOpponents = new Button(minusOne);
        decOpponents.setWidth(96);
        decOpponents.setHeight(96);
        decOpponents.setPosition(4,y + 8);
        decOpponents.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                if (numOpponents > 1) {
                    numOpponents--;
                    opponents.setText("Opponents: " + numOpponents);
                }
            }
        });
        stage.addActor(decOpponents);

        y -= 124;

        ante = new TextButton("Ante: " + anteCost, text);
        ante.setPosition(100, y);
        ante.setWidth(400);
        ante.setHeight(112);
        stage.addActor(ante);

        Button incAnte = new Button(plusOne);
        incAnte.setWidth(96);
        incAnte.setHeight(96);
        incAnte.setPosition(501,y + 8);
        incAnte.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                anteCost += 5;
                ante.setText("Ante: " + anteCost);
            }
        });
        stage.addActor(incAnte);

        Button decAnte = new Button(minusOne);
        decAnte.setWidth(96);
        decAnte.setHeight(96);
        decAnte.setPosition(4,y + 8);
        decAnte.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                if (anteCost > 0) {
                    anteCost -= 5;
                    ante.setText("Ante: " + anteCost);
                }
            }
        });
        stage.addActor(decAnte);

        y -= 124;

        credits = new TextButton("Credits: " + startingCredits, text);
        credits.setPosition(100, y);
        credits.setWidth(400);
        credits.setHeight(112);
        stage.addActor(credits);

        Button incCredits = new Button(plusOne);
        incCredits.setWidth(96);
        incCredits.setHeight(96);
        incCredits.setPosition(501,y + 8);
        incCredits.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                startingCredits += 50;
                credits.setText("Credits: " + startingCredits);
            }
        });
        stage.addActor(incCredits);

        Button decCredits = new Button(minusOne);
        decCredits.setWidth(96);
        decCredits.setHeight(96);
        decCredits.setPosition(4,y + 8);
        decCredits.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                if (startingCredits > 100) {
                    startingCredits -= 50;
                    credits.setText("Credits: " + startingCredits);
                }
            }
        });
        stage.addActor(decCredits);

        y -= 124;

        TextButton.TextButtonStyle startGameStyle = new TextButton.TextButtonStyle();
        startGameStyle.font = game.font32;
        startGameStyle.up = uiSkin.getDrawable("button1-up");
        startGameStyle.down = uiSkin.getDrawable("button1-down");
        TextButton startGame = new TextButton("Start Game", startGameStyle);
        startGame.setPosition(0, 0);
        startGame.setWidth(600);
        startGame.setHeight(112);
        startGame.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);

                // Pass all of the set arguments into the new game
                game.setScreen(new GameScreen(game, uiSkin, camera, viewport, numOpponents, anteCost, startingCredits));
            }
        });
        stage.addActor(startGame);

        Gdx.input.setInputProcessor(stage);
    }
}
