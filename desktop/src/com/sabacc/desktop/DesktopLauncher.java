package com.sabacc.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.sabacc.Sabacc;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.title = "Sabacc";
		config.width = 400;
		config.height = 800;
		new LwjglApplication(new Sabacc(), config);
	}
}
