package com.sabacc.gamestage;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.sabacc.Card;
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
    final private Array<Array<Player>> handValues;
    private Array<Player> suddenDemise;
    private final Array<Player> allInPlayers;
    private int roundStartPot;      // How much the main pot is at the start of the round
    private Player winner;

    public BettingStage(GameScreen main, FitViewport viewport) {
        this.main = main;
        this.viewport = viewport;

        // Set up the main button style for all the buttons
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = main.game.font32;
        buttonStyle.up = main.uiSkin.getDrawable("button3-up");
        buttonStyle.down = main.uiSkin.getDrawable("button3-down");

        // Keep track of all hand values, and the vector of players for each hand value
        handValues = new Array<Array<Player>>();
        for (int i = 0; i <= 24; i++)
            handValues.add(new Array<Player>());

        allInPlayers = new Array<Player>();

        initializeCheckStage(buttonStyle);
        initializeRaiseStage(buttonStyle);
    }

    @Override
    public void show() {
        if (currentStage == raiseStage)
            main.noButton.draw(main.game.batch,0,0,600,128);

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

        // Set the start of the main pot
        roundStartPot = main.mainPot;

        // Set each player to have not bet and to have not gone all in
        for (Player p : main.players) {
            p.currentBid = 0;
            p.hasBet = false;
        }
    }

    /**
     * Resets each players total bid for the round to be 0 and reset each players All In Status
     */
    public void resetBettingRound() {
        allInPlayers.clear();
        for (Player p : main.players) {
            p.roundbid = 0;
            p.allInValue = 0;
            p.isAllIn = false;
        }
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

        if (p.isAllIn) {
            // A lazy solution, but if this player is all in then dont have them do anything
            main.addMessage(p.name() + " is all in");
            main.nextPlayer();
            tryToEndRound();
            return;
        }
        // Get the AIs action
        int newbid = p.makeBet(main.mainPot, currentBid, main.players, main.isCalled);
        if (newbid == -1) {
            p.folded = true;
            main.addMessage(p.name() + " has folded!");
        } else if (newbid == -2) {
            goAllIn(p);
        } else {
            if (newbid > currentBid)
                main.addMessage(p.name() + " raises to " + newbid + " credits");
            else if (newbid == currentBid) {
                if (newbid == 0)
                    main.addMessage(p.name() + " checks");
                else
                    main.addMessage(p.name() + " matches the bid of " + newbid + " credits");
            } else if (newbid < -2) {
                main.addMessage("ERROR: " + p.name() + " betting a negative less than neg2 (" + newbid + ")");
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
            portionCreditsToAllIn(p);
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
     * it, otherwise they go all in
     * @param p the player to call the current bid
     */
    private void matchCurrentBid(Player p) {
        int v = currentBid - p.currentBid;
        if (v > p.credits()) {
            goAllIn(p);
            portionCreditsToAllIn(p);
            main.nextPlayer();
            tryToEndRound();
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
            portionCreditsToAllIn(p);
        }
        main.nextPlayer();
        tryToEndRound();
    }

    /**
     * Raise the current bid to a new value denoted by value, and then bet enough to bring
     * player.currentBid to the new currentBid
     * @param newbid the value of the new current bid that the player is trying
     */
    private void playerRaise(Player player,  int newbid) {
        if (!main.getCurrentPlayer().isHuman)
            return;
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
        portionCreditsToAllIn(player);
        main.addMessage(player.name() + " raises the current bid to " + currentBid);

        currentStage = checkStage;
        main.setStageInput(currentStage);
        main.nextPlayer();
        tryToEndRound();
    }

    /**
     * Iterate over the list of all in players and add credits to their all in value equal to their
     * currentBid whenever another player bets
     * @param c the current player who is allocating credits to the all in players
     */
    private void portionCreditsToAllIn(Player c) {
        for (Player p : allInPlayers) {
            // @todo Fix this later, does not work properly if c has already allocated an amount to p's allInValue
            p.allInValue += p.currentBid;

            // Debugging messages
            System.out.println(p.name() + " All In Value: " + p.allInValue);
        }
    }

    /**
     * Handle going all in for the specified player.
     * This does handle their bid and adding to the pot
     * @param p the player to go all in
     */
    private void goAllIn(Player p) {
        // Toggle all the necessary variables and set the players bid to their current credits
        if (p.credits() > currentBid)
            currentBid = p.credits();   // @todo Going all in with a raise breaks it a bit if nobody raises again later, handled for now in Player.java
        p.currentBid += p.credits();
        p.roundbid += p.credits();
        p.hasBet = true;
        main.mainPot += p.credits();
        allInPlayers.add(p);

        // The first time the player goes all in for the round, set their starting all in value and toggle them to all in
        if (!p.isAllIn) {
            p.allInValue = roundStartPot;
            p.isAllIn = true;
        }
        p.modifyCredits(-p.credits());
        portionCreditsToAllIn(p);

        // Then for each other non-folded player, increase this players allInValue by a portion
        // of that players current bid. This handles all players that have previously bet in the
        // round, portionCreditsToAllIn handles all future players who bet in the round

        for (Player o : main.players) {
            if (o == p)
                continue;
            p.allInValue += Math.min(o.currentBid, p.currentBid);
        }

        // Print a message
        main.addMessage(p.name() + " has gone all in!");

        // Debugging messages
        System.out.println(p.name() + " All In Value: " + p.allInValue);
    }

    /**
     * End the round and determine the winner. This happens after a final betting round
     * once the game has been called
     */
    private void endRound() {
        // First, find all the players who bombed out, they will be folded and they pay
        // credits equal to the main pot into the sabacc pot
        main.betweenRounds = true;
        if (!allFolded()) { // A check to account for if only one player is still in the hand, but bombed out
            for (Player p : main.players) {
                if (!p.folded) {
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

        int nonfold = sortAllHandValues();

        if (nonfold == 0)
            // Base case, no winners, everybody bombed out or folded
            main.addMessage("There was no winner this round!");
        else if (nonfold == 1) {
            // Everybody else folded, the one remaining player wins but cannot win on Pure Sabacc (or Bomb Out)
            for (Player p : main.players)
                if (!p.folded) {
                    winner = p;         // THIS IS LAZY, FIX LATER
                    break;
                }
            int value = main.mainPot;
            if (winner.isAllIn)
                value = winner.allInValue;
            main.addMessage(winner.name() + " won " + value + " credits as everybody else folded!");
            winner.modifyCredits(value);
            main.mainPot = main.mainPot - value;
        } else {
            // Otherwise, find the highest hand value, 24 is an idiot's array, 0 cannot happen
            int i;
            for (i = 24; i >= 0; i--) {
                if (i == 0)     // Base Case
                    main.addMessage("There was no winner this round!");
                else if (handValues.get(i).size == 1) {
                    // Exactly one winner
                    winner = handValues.get(i).get(0);

                    // Consider all in for only the main pot
                    int value = main.mainPot;
                    if (winner.isAllIn)
                        value = winner.allInValue;

                    if (i >= 23) {
                        // Pure Sabacc or Idiot's Array
                        main.addMessage(winner.name() + " won " + (value + main.sabaccPot) + " credits with a pure sabacc!");
                        winner.modifyCredits(value + main.sabaccPot);
                        main.mainPot = main.mainPot - value;
                        main.sabaccPot = 0;
                        break;
                    } else {
                        // Otherwise, regular hand
                        main.addMessage(winner.name() + " won " + value + " credits with a hand of " + i + "!");
                        winner.modifyCredits(value);
                        main.mainPot = main.mainPot - value;
                        break;
                    }
                } else if (handValues.get(i).size > 1){
                    // Multiple winners, enact Sudden Demise
                    // @todo Will not enact a second sudden demise if that comes up
                    main.addMessage("Multiple players with a hand of " + i + ", enacting Sudden Demise!");
                    suddenDemise = handValues.get(i);
                    Card c;
                    winner = null;
                    String s;
                    int newValue = -1;
                    for (Player p : suddenDemise) {
                        c = main.deck.drawCard();
                        p.addCard(c);
                        s = p.name() + " is dealt " + c.name;   // Done this way to set the entire message on one line, to fit on the screen
                        if (Math.abs(p.score()) > 23 || p.score() == 0) {
                            s += " and bombs out with " + p.score() + "!"; // Maybe add a note that penalties are not paid in sudden demise?
                        } else if (Math.abs(p.score()) > newValue) {
                            newValue = Math.abs(p.score());
                            winner = p;
                        }
                        main.addMessage(s);
                    }

                    // If all players in the sudden demise bomb out, then continue the for loop
                    // to find the next highest hand value
                    if (winner == null)
                        continue;

                    // Consider all in for only the main pot
                    int value = main.mainPot;
                    if (winner.isAllIn)
                        value = winner.allInValue;

                    if (i >= 23) {
                        // Pure Sabacc or Idiot's Array
                        main.addMessage(winner.name() + " won " + (value + main.sabaccPot) + " credits with a pure sabacc!");
                        winner.modifyCredits(value + main.sabaccPot);
                        main.mainPot = main.mainPot - value;
                        main.sabaccPot = 0;
                        break;
                    } else {
                        // Otherwise, regular hand
                        main.addMessage(winner.name() + " won " + value + " credits with a hand of " + i + "!");
                        winner.modifyCredits(value);
                        main.mainPot = main.mainPot - value;
                        break;
                    }
                }
            }

        }

        // After allocating credits, ask the player to start the next round
        main.setGameStage(main.nextRoundStage);
    }

    /**
     * Iterate over each player, if they are not folded then add them to the index corresponding to
     * their score
     * @return an integer value of how many players did not fold
     */
    private int sortAllHandValues() {
        clearHandValues();
        int nonfold = 0;    // Keep track of how many players folded
        for (Player p : main.players) {
            if (p.folded)
                continue;
            nonfold++;
            if (p.idiotsArray())
                handValues.get(24).add(p);
            else
                handValues.get(Math.abs(p.score())).add(p);
        }
        return nonfold;
    }

    /**
     * Iterate over the 2d array of hand values and clear each inner array
     */
    private void clearHandValues() {
        for (Array<Player> a : handValues)
            a.clear();
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
                // Automatically select the textfield and open the keyboard
                TextField f = (TextField) raiseStage.getActors().get(0);
                currentStage.setKeyboardFocus(f);
                f.getOnscreenKeyboard().show(true);
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
