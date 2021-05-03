package com.sabacc;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

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

	// Some extra settings
	public int maxMessages;
	public float aiTurnLength;
	
	@Override
	public void create () {
		batch = new SpriteBatch();
		font32 = new BitmapFont(Gdx.files.internal("anakin32.fnt"));
		font24 = new BitmapFont(Gdx.files.internal("anakin24.fnt"));
		msgFont = new BitmapFont(Gdx.files.internal("dejavu20.fnt"));

		// Calculate the camera dimensions
		// Width is constant, height is relative to width based on screen size
		width = 600;
		height = (int)(((double)Gdx.graphics.getHeight() / (double) Gdx.graphics.getWidth())*width);

		// Set some extra settings
		maxMessages = 8;
		aiTurnLength = 0.3f;

		this.setScreen(new GameScreen(this, 3, 20));
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
