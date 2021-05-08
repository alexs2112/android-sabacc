package com.sabacc.gamestage;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.sabacc.GameScreen;
import com.sabacc.Player;

/**
 * Set up the three buttons for the drawing after a player can call
 * They are:
 *  - Draw (draw a card)
 *  - Stand (don't draw a card)
 *  - Call (end the round after one final betting round)
 */
public class DrawingStage implements GameStage {
    private final GameScreen main;
    private final FitViewport viewport;
    private Stage buildStage;
    private Stage callStage;
    private Stage currentStage;

    // How many drawing rounds are run until a player can call the hand
    private int untilCall;
    public void resetUntilCall() { untilCall = 4; }

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
        // @todo Make this more elegant later
        if (main.deck.isEmpty())
            main.addMessage("ERROR: " + p.name() + " tries to draw but the deck is empty");
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
        main.setStageInput(currentStage);

        // Decrement how many rounds until a player can call
        untilCall--;

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
                main.isCalled = true;
                main.setGameStage(main.bettingStage);
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
            main.setGameStage(main.bettingStage);
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
        currentStage.dispose();
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

        // Set up a red call button that doesnt do anything
        TextButton.TextButtonStyle redButtonStyle = new TextButton.TextButtonStyle();
        redButtonStyle.font = main.game.font32;
        redButtonStyle.up = main.uiSkin.getDrawable("button3-unavailable");
        TextButton callButton = new TextButton("Call", redButtonStyle);
        callButton.setWidth(200);
        callButton.setHeight(128);
        callButton.setPosition(400,0);

        buildStage.addActor(drawButton);
        buildStage.addActor(standButton);
        buildStage.addActor(callButton);
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

        // CALL
        TextButton callButton = new TextButton("Call", buttonStyle);
        callButton.setWidth(200);
        callButton.setHeight(128);
        callButton.setPosition(400,0);
        callButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                Player p = main.getCurrentPlayer();
                if (p.isHuman) {
                    main.addMessage(p.name() + " calls the round");
                    main.setGameStage(main.bettingStage);
                    main.isCalled = true;
                }
            }
        });

        callStage.addActor(drawButton);
        callStage.addActor(standButton);
        callStage.addActor(callButton);
    }
}
