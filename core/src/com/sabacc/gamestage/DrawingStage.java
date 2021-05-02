package com.sabacc.gamestage;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.sabacc.GameScreen;
import com.sabacc.Player;

public class DrawingStage implements GameStage {
    private final GameScreen main;
    private final FitViewport viewport;
    private Stage buildStage;
    private Stage callStage;
    private Stage currentStage;

    // How many drawing rounds are run until a player can call the hand
    private int untilCall;
    public void resetUntilCall() { untilCall = 0; }

    public DrawingStage(final GameScreen main, final FitViewport viewport) {
        this.main = main;
        this.viewport = viewport;

        untilCall = 4;

        // Set up the main button style for all the buttons
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = main.game.font32;
        buttonStyle.up = main.uiSkin.getDrawable("button3-up");
        buttonStyle.down = main.uiSkin.getDrawable("button3-down");

        initializeBuildStage(buttonStyle);
        initializeCallStage(buttonStyle);
    }

    /**
     * A helper function that has the input player draw a card
     * @param p player
     */
    private void drawCard(Player p) {
        p.addCard(main.deck.drawCard());
        main.addMessage(p.name() + " draws a card");
    }

    /**
     * Set up a new drawing round, swapping the stage and making sure all players have not yet gone
     */
    @Override
    public void start() {
        // Determine which stage should be used
        if (untilCall > 0)
            currentStage = buildStage;
        else
            currentStage = callStage;
        Gdx.input.setInputProcessor(currentStage);

        // Decrement how many rounds until a player can call
        untilCall--;
        System.out.println("Until Call: " + untilCall);

        // Set all players to have not gone yet
        for (Player p : main.players)
            p.hasDrawn = false;
    }

    /**
     * Run the next few AI players until the current player is a human player, or until the current
     * player has already already gone, in which case move to another betting round
     */
    @Override
    public void aiAction() {
        Player p = main.getCurrentPlayer();

        // p should always be an ai, fix this later
        if (!p.isHuman) {
            p.hasDrawn = true;

            // Get the players choice based on their ai
            int c = p.drawChoice(untilCall);
            if (c == -1) {
                // Call
                main.addMessage(p.name() + " calls the round");

                // DO ONE FINAL BETTING ROUND

                // isCalled = true;
                return;
            } else if (c == 0) {
                // Stand
                main.addMessage(p.name() + " stands");
            } else if (c == 1) {
                // Draw
                drawCard(p);
            }

            main.nextPlayer();
        }

        // After an AI action, try to end the round
        tryToEndRound();
    }

    /**
     * Called at the end of every player action, human or AI to check if the round has ended
     * If the round has ended, change the gamestage in main
     */
    private void tryToEndRound() {
        if (main.getCurrentPlayer().hasDrawn) {
            // CHANGE TO BETTING ROUND
            main.setGameStage(main.drawingStage);
        }
    }

    @Override
    public void show() {
        currentStage.draw();
    }

    @Override
    public void dispose() {
        buildStage.dispose();
        callStage.dispose();
    }

    private void initializeBuildStage(TextButton.TextButtonStyle buttonStyle) {
        buildStage = new Stage(viewport);

        // Draw Button
        TextButton drawButton = new TextButton("Draw", buttonStyle);
        drawButton.setWidth(200);
        drawButton.setHeight(128);
        drawButton.setPosition(0,0);
        drawButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                Player p = main.getCurrentPlayer();
                if (p.isHuman) {
                    p.hasDrawn = true;
                    drawCard(p);
                    main.nextPlayer();
                    tryToEndRound();
                } else {
                    System.out.println("TIMER");
                }
            }
        });

        // Stand Button
        TextButton standButton = new TextButton("Stand", buttonStyle);
        standButton.setWidth(200);
        standButton.setHeight(128);
        standButton.setPosition(200,0);
        standButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                Player p = main.getCurrentPlayer();
                if (p.isHuman) {
                    p.hasDrawn = true;
                    main.addMessage(p.name() + " stands");
                    main.nextPlayer();
                    tryToEndRound();
                }
            }
        });
        buildStage.addActor(drawButton);
        buildStage.addActor(standButton);
    }

    private void initializeCallStage(TextButton.TextButtonStyle buttonStyle) {
        callStage = new Stage(viewport);

        // Need a lot of repeated code as libgdx doesnt like adding a button to multiple stages

        // Draw Button
        TextButton drawButton = new TextButton("Draw", buttonStyle);
        drawButton.setWidth(200);
        drawButton.setHeight(128);
        drawButton.setPosition(0,0);
        drawButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                Player p = main.getCurrentPlayer();
                if (p.isHuman) {
                    p.hasDrawn = true;
                    drawCard(p);
                    main.nextPlayer();
                    tryToEndRound();
                }
            }
        });
        callStage.addActor(drawButton);
    }
}
