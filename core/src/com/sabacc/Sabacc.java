package com.sabacc;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.sabacc.screens.StartScreen;

/**
 * Android implementation of Sabacc
 * Rules from http://sabacc.sourceforge.net/rules
 *
 * Font AnakinMono by opipik (http://fontstruct.com/fontstructions/show/773756)
 */
public class Sabacc extends Game {
	public SpriteBatch batch;
	public BitmapFont font32;
	public BitmapFont font24;
	public BitmapFont msgFont;

	// Width and height of the camera screens
	public int width;
	public int height;

	// Width and height of the physical screen
	public int screenWidth;
	public int screenHeight;

	// Ratio comparing the width and height of the camera screen to the physical screen
	public float ratioWidth;
	public float ratioHeight;

	// Some extra settings
	public int maxMessages;
	public float aiTurnLength;
	public boolean autoDisplayAndHide;	// Automatically open and close opponents at the end and start of rounds
	public float shiftChance;
	public float timeForDoubleTap;	// How long the player has between taps to double click a card to move it to the interference field
	
	@Override
	public void create () {
		batch = new SpriteBatch();
		font32 = new BitmapFont(Gdx.files.internal("anakin32.fnt"));
		font24 = new BitmapFont(Gdx.files.internal("anakin24.fnt"));
		msgFont = new BitmapFont(Gdx.files.internal("dejavu20.fnt"));

		// Calculate the camera dimensions
		// Width is constant, height is relative to width based on screen size
		screenWidth = Gdx.graphics.getWidth();
		screenHeight = Gdx.graphics.getHeight();
		width = 600;
		height = (int)(((double) screenHeight / (double) screenWidth)*width);
		ratioWidth = (float)width / (float)screenWidth;
		ratioHeight = (float)height / (float)screenHeight;

		// Set some extra settings
		maxMessages = 8;
		aiTurnLength = 0.2f;
		autoDisplayAndHide = true;
		shiftChance = 0.05f;	// 5% chance for a sabacc shift after every player takes a turn, should happen around once, twice is not uncommon, thrice is rare
		timeForDoubleTap = 0.5f;	// half a second

		// Make the graphics non-continuous, to save battery
		Gdx.graphics.setContinuousRendering(false);
		Gdx.graphics.requestRendering();

		// Load the skin for the ui, to be passed into different screens
		Skin uiSkin = new Skin();
		uiSkin.addRegions(new TextureAtlas(Gdx.files.internal("ui.atlas")));

		// Set the camera and viewport that will be passed into the Game Screen
		OrthographicCamera camera = new OrthographicCamera();
		camera.setToOrtho(false, width, height);
		FitViewport viewport = new FitViewport(width, height, camera);

		this.setScreen(new StartScreen(this, uiSkin, camera, viewport));
	}

	@Override
	public void render () {
		super.render();
	}
	
	@Override
	public void dispose () {
		batch.dispose();
		font32.dispose();
		font24.dispose();
		msgFont.dispose();
	}
}
