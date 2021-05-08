package com.sabacc.gamestage;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.sabacc.GameScreen;
import com.sabacc.Player;

/**
 * Set up the three buttons for the betting round
 * They are:
 *  - Bet (call the current bid)
 *  - Raise (raise the current bid)
 *  - Fold (drop out of the round)
 */
public class BettingStage implements GameStage {
    private final GameScreen main;
    private final FitViewport viewport;
    private Stage checkStage;
    private Stage raiseStage;
    private Stage currentStage;
    private int currentBid;

    public BettingStage(GameScreen main, FitViewport viewport) {
        this.main = main;
        this.viewport = viewport;

        // Set up the main button style for all the buttons
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = main.game.font32;
        buttonStyle.up = main.uiSkin.getDrawable("button3-up");
        buttonStyle.down = main.uiSkin.getDrawable("button3-down");

        initializeCheckStage(buttonStyle);
        initializeRaiseStage(buttonStyle);
    }

    @Override
    public void show() {
        currentStage.draw();

        if (currentBid > 0)
            main.game.font24.draw(main.game.batch, "Bid: " + currentBid, 300, main.game.height - 48);
    }

    @Override
    public void start() {
        // As the current bid starts at 0, set the start stage to the checking stage
        currentStage = checkStage;
        main.setStageInput(currentStage);

        // Reset the current bid of the round
        currentBid = 0;

        // Set each player to have not bet
        for (Player p : main.players) {
            p.currentBid = 0;
            p.hasBet = false;
        }
    }

    /**
     * Resets each players total bid for the round to be 0
     */
    public void resetBettingRound() {
        for (Player p : main.players)
            p.roundbid = 0;
    }

    /**
     * Run the next few AI players until the current player is a human player, or until the current
     * player has already matched the current bid, in which case the betting round ends
     */
    @Override
    public void aiAction() {
        Player p = main.getCurrentPlayer();

        // Toggle this player to have bet (or called the initial bet of 0)
        p.hasBet = true;

        // For all AI, for now just have them match the bid
        int newbid = p.makeBet(main.mainPot, currentBid, main.players, main.isCalled);
        if (newbid == -1) {
            p.folded = true;
            main.addMessage(p.name() + " has folded!");
        } else {
            if (newbid > currentBid)
                main.addMessage(p.name() + " raises to " + newbid + " credits");
            else if (newbid == currentBid) {
                if (newbid == 0)
                    main.addMessage(p.name() + " checks");
                else
                    main.addMessage(p.name() + " matches the bid of " + newbid + " credits");
            } else
                main.addMessage("ERROR: " + p.name() + " bets " + newbid + " credits");

            // Set the new currentBid
            currentBid = newbid;

            // Get the value of how much the player owes
            newbid -= p.currentBid;
            if (newbid < 0)
                System.out.println("ERROR: NEGATIVE RAISE");
            p.modifyCredits(-newbid);
            p.currentBid += newbid;
            p.roundbid += newbid;
            main.mainPot += newbid;
        }
        main.nextPlayer();
        tryToEndRound();
    }

    /**
     * Called at the end of every player action, human or AI to check if the round has ended
     * If the round has ended, change the gamestage in main
     */
    private void tryToEndRound() {
        if (main.getCurrentPlayer().currentBid == currentBid && main.getCurrentPlayer().hasBet) {

            // If all players (or all players except one) have folded, end the round
            if (allFolded()) {
                endRound();
                return;
            }

            // If this is the preliminary betting round, deal starting hands
            if (main.startOfRound) {
                main.startOfRound = false;
                main.dealStartingHand();
            }

            // If the game has been called, end the round
            if (main.isCalled) {
                endRound();
                return;
            }

            // At the end of the betting round is the drawing round
            main.setGameStage(main.drawingStage);
        }
    }

    /**
     * Determine if all players or all players minus 1 have folded so that the round ends
     * @return true if this condition is met, false otherwise
     */
    private boolean allFolded() {
        boolean oneNotFolded = false;
        for (Player p : main.players) {
            if (p.folded)
                continue;
            if (oneNotFolded)
                return false;
            else
                oneNotFolded = true;
        }
        return true;
    }

    /**
     * A function that calls the input player to match the current bid if they can afford
     * it, otherwise force them to fold
     * @param p the player to call the current bid
     */
    private void matchCurrentBid(Player p) {
        int v = currentBid - p.currentBid;
        if (v > p.credits()) {
            p.folded = true;
            main.nextPlayer();
            main.addMessage(p.name() + " has folded!");
            return;
        }

        if (currentBid == 0)
            main.addMessage(p.name() + " checks");
        else {
            main.addMessage(p.name() + " matches the bid of " + currentBid + " credits");
            p.modifyCredits(-v);
            p.currentBid += v;
            p.roundbid += v;
            main.mainPot += v;
        }
        main.nextPlayer();
    }

    /**
     * Raise the current bid to a new value denoted by value, and then bet enough to bring
     * player.currentBid to the new currentBid
     * @param newbid the value of the new current bid that the player is trying
     */
    private void playerRaise(Player player,  int newbid) {
        int v = newbid - player.currentBid;
        if (newbid < currentBid) {
            main.addMessage("Error: You cannot raise to lesser value");
            return;
        } else if (v > player.credits()) {
            main.addMessage("Error: You cannot raise to more credits than you have");
            return;
        }
        currentBid = newbid;
        player.modifyCredits(-v);
        player.currentBid += v;
        player.roundbid += v;
        player.hasBet = true;
        main.mainPot += v;
        main.addMessage(player.name() + " raises the current bid to " + currentBid);

        currentStage = checkStage;
        main.setStageInput(currentStage);
        main.nextPlayer();
        tryToEndRound();
    }

    /**
     * End the round and determine the winner. This happens after a final betting round
     * once the game has been called
     */
    private void endRound() {
        // First, find all the players who bombed out, they will be folded and they pay
        // credits equal to the main pot into the sabacc pot

        // Dont check for bombouts if everyone folded, when a player wins when everyone folds
        // their score doesnt matter
        main.betweenRounds = true;
        if (!allFolded()) {
            for (Player p : main.players) {
                if (!p.folded) {
                    System.out.println(p.name() + " : " + p.score());
                    if (Math.abs(p.score()) > 23) {
                        int value = Math.min(main.mainPot, p.credits());
                        p.folded = true;
                        p.modifyCredits(-value);
                        main.sabaccPot += value;
                        main.addMessage(p.name() + " has bombed out!");
                    }
                }
            }
        }

        // Then iterate over each non-folded player and compare them to the winner
        // Currently does not account for multiple tied winners
        Player winner = null;
        int currentScore = 0;
        int nonfold = 0;    // Keep track of how many players folded
        for (Player p : main.players) {
            if (p.folded)
                continue;
            nonfold++;
            if (winner == null) {
                winner = p;
                currentScore = Math.abs(p.score());
            } else {
                if (Math.abs(p.score()) > currentScore || p.idiotsArray()) {
                    winner = p;
                    currentScore = Math.abs(p.score());
                }
            }
        }
        if (winner == null) {
            main.addMessage("There was no winner this round!");
        } else if (nonfold == 1) {
            main.addMessage(winner.name() + " won " + main.mainPot + " credits as everybody else folded!");
            winner.modifyCredits(main.mainPot);
            main.mainPot = 0;
        } else if (currentScore == 23 || winner.idiotsArray()) {
            main.addMessage(winner.name() + " won " + (main.mainPot + main.sabaccPot) + " credits with a pure sabacc!");
            winner.modifyCredits(main.mainPot + main.sabaccPot);
            main.mainPot = 0;
            main.sabaccPot = 0;
        } else {
            main.addMessage(winner.name() + " won " + main.mainPot + " credits with a hand of " + currentScore + "!");
            winner.modifyCredits(main.mainPot);
            main.mainPot = 0;
        }

        // After allocating credits, ask the player to start the next round
        main.setGameStage(main.nextRoundStage);
    }

    @Override
    public void dispose() {
        checkStage.dispose();
        raiseStage.dispose();
        currentStage.dispose();
    }

    private void initializeCheckStage(TextButton.TextButtonStyle buttonStyle) {
        checkStage = new Stage(viewport);

        // Check
        TextButton betButton = new TextButton("Check", buttonStyle);
        betButton.setWidth(200);
        betButton.setHeight(128);
        betButton.setPosition(0,0);
        betButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                Player p = main.getCurrentPlayer();
                if (p.isHuman) {
                    p.hasBet = true;
                    matchCurrentBid(p);
                }
            }
        });

        // Raise
        TextButton raiseButton = new TextButton("Raise", buttonStyle);
        raiseButton.setWidth(200);
        raiseButton.setHeight(128);
        raiseButton.setPosition(200,0);
        raiseButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                currentStage = raiseStage;
                main.setStageInput(currentStage);
            }
        });

        // Fold
        TextButton foldButton = new TextButton("Fold", buttonStyle);
        foldButton.setWidth(200);
        foldButton.setHeight(128);
        foldButton.setPosition(400,0);
        foldButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                Player p = main.getCurrentPlayer();
                if (p.isHuman) {
                    main.addMessage(p.name() + " has folded!");
                    p.folded = true;
                    main.nextPlayer();
                }
            }
        });

        checkStage.addActor(betButton);
        checkStage.addActor(raiseButton);
        checkStage.addActor(foldButton);
    }

    /**
     * Initialize the bet setting stage
     */
    private void initializeRaiseStage(TextButton.TextButtonStyle buttonStyle) {
        raiseStage = new Stage(viewport);

        // The start of where the text field and buttons will be drawn
        int y = main.game.height / 2 + 100;

        TextField.TextFieldStyle style = new TextField.TextFieldStyle();
        style.font = main.game.font32;
        style.background = main.uiSkin.getDrawable("button1-up");
        style.fontColor = Color.WHITE;
        final TextField field = new TextField("", style);
        field.setPosition(0, y);
        field.setAlignment(Align.center);
        field.setSize(600,96);
        field.setTextFieldFilter(new TextField.TextFieldFilter.DigitsOnlyFilter());
        field.setTextFieldListener(new TextField.TextFieldListener() {
            @Override
            public void keyTyped(TextField textField, char c) {
                System.out.print(field.getText());
                if (c == '\n')
                    playerRaise(main.player, Integer.parseInt(field.getText()));
            }
        });
        raiseStage.addActor(field);

        y -= 96;

        // Accept
        TextButton acceptButton = new TextButton("Accept", buttonStyle);
        acceptButton.setWidth(200);
        acceptButton.setHeight(96);
        acceptButton.setPosition(0,y);
        acceptButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                playerRaise(main.player, Integer.parseInt(field.getText()));
            }
        });
        raiseStage.addActor(acceptButton);

        // Cancel
        TextButton cancelButton = new TextButton("Cancel", buttonStyle);
        cancelButton.setWidth(200);
        cancelButton.setHeight(96);
        cancelButton.setPosition(400,y);
        cancelButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                currentStage = checkStage;
                main.setStageInput(currentStage);
            }
        });
        raiseStage.addActor(cancelButton);
    }
}
