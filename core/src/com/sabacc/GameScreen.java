package com.sabacc;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Queue;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.sabacc.gamestage.*;

public class GameScreen implements Screen {
    final public Sabacc game;
    final private OrthographicCamera camera;

    // An array of players in the game, with a reference to the main player
    final public Array<Player> players;
    final public Player player;

    // The index of the current player and a small helper function to increment it to
    // the next active player
    private int currentPlayer;
    public Player getCurrentPlayer() { return players.get(currentPlayer); }
    public void nextPlayer() {
        currentPlayer = (currentPlayer + 1) % players.size;
        while (players.get(currentPlayer).folded)
            currentPlayer = (currentPlayer + 1) % players.size;
    }

    // The game deck
    final public Deck deck;

    // The array of past messages
    final private Queue<String> messages;
    public void addMessage(String message) {
        System.out.println(message);
        messages.addLast(message);
        while (messages.size > game.maxMessages)
            messages.removeFirst();
    }

    // The buy in price for each game
    final private int ante;

    // The two pots
    private int mainPot;
    private int sabaccPot;

    // The current bid of the betting round
    private int currentBid;

    // If this is the start of the round, before the opening hands have been dealt
    // To handle preliminary betting before the opening hands
    private boolean startOfRound;

    // How many drawing rounds remaining until a player can call
    private int untilCall;

    // If the round has been called
    private boolean isCalled;

    // Between rounds, to display opponents hands (if any) and their hand values
    private boolean betweenRounds;

    // The timer used to add a delay between ai actions
    private SabaccTimer timer;

    // The currently selected card, defaults to null
    private Card selected;

    // Keep track of the current stage, either for the betting round or the drawing round
    final private FitViewport viewport;

    public DrawingStage drawingStage;
    private GameStage currentStage;
    public void setGameStage(GameStage next) {
        currentStage = next;
        currentStage.start();
    }

    // The skin for different buttons
    final public Skin uiSkin;
    final private Drawable playerbox;

    public GameScreen(final Sabacc game, int numOfOpponents, int ante) {

        // Set up some preliminary variables that are needed
        this.game = game;
        deck = new Deck();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, game.width, game.height);
        this.ante = ante;
        messages = new Queue<String>();
        betweenRounds = true;
        timer = new SabaccTimer();

        // Load the skin for the ui and the viewport for the different button stages
        uiSkin = new Skin();
        uiSkin.addRegions(new TextureAtlas(Gdx.files.internal("ui.atlas")));
        viewport = new FitViewport(game.width, game.height, camera);
        playerbox = uiSkin.getDrawable("player-box");

        // Set up all players
        players = new Array<Player>();
        player = new Player(true, "Alex", 500);
        players.add(player);
        for (int i = 0; i < numOfOpponents; i++) {
            players.add(new Player(false, "Opponent " + i, 500));
        }

        drawingStage = new DrawingStage(this, viewport);
        setGameStage(drawingStage);
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
        // Clear the screen to a dark blue colour
        ScreenUtils.clear(0, 0, 0.2f, 1);

        // Update the camera
        camera.update();
        game.batch.setProjectionMatrix(camera.combined);

        game.batch.begin();

        // Draw the players hand
        displayHand();

        // For now, just write the pot values at the top of the screen
        game.font24.draw(game.batch, "Main Pot: " + mainPot, 16, game.height - 24);
        game.font24.draw(game.batch, "Sabacc Pot: " + sabaccPot, 272, game.height - 24);

        // Write how many credits the player currently has
        game.font24.draw(game.batch, "Credits: " + player.credits(), 16, game.height - 48);

        // Write what the current bid is
        game.font24.draw(game.batch, "Bid: " + currentBid, 300, game.height - 48);

        // Write all game messages at the top of the screen
        for (int i = 0; i < messages.size; i++)
            game.msgFont.draw(game.batch, messages.get(i), 16, game.height - 80 - i*20);

        // Then draw each opponents box under the messages
        drawPlayerBoxes(game.height - 100 - game.maxMessages*20 - 96);

        game.batch.end();

        // Show the current stage below the other drawings
        //currentStage.draw();
        currentStage.show();

        // Take player input to determine if they selected a card in their hand
        if (Gdx.input.justTouched()) {
            selected = null;
            Vector3 touch = new Vector3();
            touch.set(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(touch);
            if (touch.y > 124 && touch.y < 124 + 172) {
                int i = (int)(touch.x / 120);
                if (i < player.numCards())
                    selected = player.hand().get(i);
            }
        }

        // Check the timer, if an ai event has to happen
        if (!getCurrentPlayer().isHuman) {
            System.out.println(timer.ended);
            if (timer.ended) {
                timer.reset();
                currentStage.aiAction();
            } else if (!timer.ended && !timer.started){
                timer.call(game.aiTurnLength);
            }
        }
    }

    /**
     * Simply draw the cards in the players hand, immediately above the hand write the players
     * current score
     */
    private void displayHand() {
        for (int i = 0; i < player.hand().size; i++) {
            Card c = player.hand().get(i);
            c.image.draw(game.batch, i * 120, 128, 120, 172);

            // If the card is selected by the player, draw the selected border over it
            if (c == selected)
                deck.selected().draw(game.batch, i * 120, 128, 120, 172);
        }
        if (player.numCards() > 0)
            game.font24.draw(game.batch, "Value: " + player.score(), 16, 332);

        // Write the name of the currently selected card above the players hand value
        if (selected != null)
            game.font24.draw(game.batch, selected.name, 16, 356);
    }

    /**
     * Draw each players box to contain info about that player under messages
     * @param sy the starting y coordinate
     */
    private void drawPlayerBoxes(int sy) {
        int y = 0;
        for (int i = 1; i < players.size; i++) {
            y = sy - 96*(i-1) + 84;
            Player p = players.get(i);
            playerbox.draw(game.batch, 0, sy - 96*(i-1), 600, 96);

            game.font24.draw(game.batch, p.name(), 12, y);
            y -= 24;
            game.font24.draw(game.batch, "C: " + p.credits(), 12, y);
            y -= 24;

            if (betweenRounds)
                game.font24.draw(game.batch, "H: " + p.score(), 12, y);
            else {
                if (p.folded)
                    game.font24.draw(game.batch, "H: Folded", 12, y);
                else
                    game.font24.draw(game.batch, "H: ?", 12, y);
            }

            for (int c = 0; c < p.numCards(); c++) {
                // If game is not over, draw the card back
                if (!betweenRounds)
                    deck.cardback().draw(game.batch, 530 - (c*70), sy - 96*(i-1), 70, 96);
                // Otherwise, draw the card face and the value of that player's hand under C
                else
                    p.hand().get(c).image.draw(game.batch, 530 - (c*70), sy - 96*(i-1), 70, 96);
            }
        }
    }

    /**
     * Unfold each player, check to make sure each player can afford to ante the next round
     * If they cannot, they leave the table
     */
    private void startNewRound() {
        // Start by refreshing the deck and clearing each players hand
        messages.clear();
        addMessage("Starting new round!");
        addMessage("Ante is " + ante);
        betweenRounds = false;
        deck.refreshDeck();
        for (Player p : players) {
            p.refreshScore();
            p.hand().clear();
        }

        for (int i = 0; i < players.size; i++) {
            if (players.get(i).credits() >= ante * 2)
                continue;
            addMessage(players.get(i).name() + " drops from the game!");
            players.removeIndex(i);
            i--;
        }

        // For now, automatically ante each player
        for (Player p : players) {
            p.folded = false;
            p.modifyCredits(-(ante*2));
        }
        mainPot += ante * players.size;
        sabaccPot += ante * players.size;

        // Notify the game that this is a new round, to deal a hand after the first betting round
        startOfRound = true;

        // Typically 4 pot building rounds before a player can call the hand
        untilCall = 4;
        isCalled = false;

        // Rounds begin with a preliminary betting round
        //newBettingRound();
    }

    /**
     * Deal the starting hand of 2 cards to each player
     */
    private void dealStartingHand() {
        addMessage("Dealing starting hands!");
        for (int i = 0; i < 2; i++) {
            for (Player p : players) {
                p.addCard(deck.drawCard());
            }
        }
    }

    /**
     * A simple helper method to swap the stage
     * @param newStage the next stage to be swapped to
     */
    /*
    private void swapStage(Stage newStage) {
        currentStage = newStage;
        Gdx.input.setInputProcessor(currentStage);
    }*/

    /**
     * Run a new betting round
     * Set the current bid to 0
     * Set each players current bid to 0
     * Iterate over each opponent and have them make a bid according to their ai
     *
     * For now, start with the player, and then ask each opponent for input
     */
    /*
    private void newBettingRound() {
        System.out.println("New Betting Round!");
        swapStage(bettingStage); // Set the stage to the betting stage

        // Reset the current bid of the round
        currentBid = 0;

        // Set each player to have not bet
        for (Player p : players) {
            p.currentBid = 0;
            p.hasBet = false;
        }

        // After the bid has been set, run a betting round
        runBettingRound();
    }
     */

    /**
     * Run the next few AI players until the current player is a human player, or until the current
     * player has already matched the current bid, in which case the betting round ends
     */
    /*
    private void runBettingRound() {
        Player p = players.get(currentPlayer);
        System.out.println("Current Player = " + p.name());

        // If the current player has already called the bid, then the betting round is over
        if (p.currentBid == currentBid && p.hasBet) {

            // If all players (or all players except one) have folded, end the round
            if (allFolded()) {
                endRound();
                return;
            }

            // If this is the preliminary betting round, deal starting hands
            if (startOfRound) {
                startOfRound = false;
                dealStartingHand();
            }

            // If the game has been called, end the round
            if (isCalled) {
                endRound();
                return;
            }

            // At the end of the betting round is the drawing round
            newDrawingRound();
            return;
        }

        // Toggle this player to have bet (or called the initial bet of 0)
        p.hasBet = true;

        // For all AI, for now just have them match the bid
        if (!p.isPlayer) {
            int v = p.makeBet(mainPot, currentBid, players);
            if (v == -1) {
                p.folded = true;
                nextPlayer();
                addMessage(p.name() + " has folded!");
            } else {
                if (v > currentBid)
                    addMessage(p.name() + " raises to " + v + " credits");
                else if (v == currentBid) {
                    if (v == 0)
                        addMessage(p.name() + " checks");
                    else
                        addMessage(p.name() + " matches the bid of " + v + " credits");
                }
                else
                    addMessage("ERROR: " + p.name() + " bets " + v + " credits");
                p.modifyCredits(-(v - p.currentBid));
                p.currentBid += v;
                currentBid = v;
                mainPot += v;
            }
            nextPlayer();
            runBettingRound();
        }
    }

     */

    /**
     * A function that calls the input player to match the current bid if they can afford
     * it, otherwise force them to fold
     * @param p the player to call the current bid
     */
    private void matchCurrentBid(Player p) {
        int v = currentBid - p.currentBid;
        if (v > p.credits()) {
            p.folded = true;
            nextPlayer();
            addMessage(p.name() + " has folded!");
            return;
        }
        p.modifyCredits(-v);
        p.currentBid += v;
        mainPot += v;

        if (currentBid == 0)
            addMessage(p.name() + " checks");
        else
            addMessage(p.name() + " matches the bid of " + v + " credits");
    }

    /**
     * Raise the current bid to a new value denoted by value, and then bet enough to bring
     * player.currentBid to the new currentBid
     * @param value the string representation of the new bid, it is always an integer
     */
    /*
    private void playerRaise(String value) {
        int newbid = Integer.parseInt(value);

        int v = newbid - player.currentBid;
        if (newbid < currentBid) {
            addMessage("Error: You cannot raise to lesser value");
            return;
        } else if (v > player.credits()) {
            addMessage("Error: You cannot raise to more credits than you have");
            return;
        }
        currentBid = newbid;
        player.modifyCredits(-v);
        player.currentBid += v;
        mainPot += v;
        addMessage(player.name() + " raises the current bid to " + currentBid);
        swapStage(bettingStage);
        nextPlayer();
        runBettingRound();
    }

     */

    /**
     * Set up a new drawing round, swapping the stage and making sure all players have not yet gone
     */
    /*
    private void newDrawingRound() {
        System.out.println("New Drawing Round!");
        // Set the stage to the drawing stage, either with or without call
        if (untilCall > 0)
            swapStage(drawingStage);
        else
            swapStage(drawingStage2);

        // Set all players to have not gone yet
        for (Player p : players)
            p.hasDrawn = false;

        // After the bid has been set, run a drawing round
        runDrawingRound();
    }

     */

    /**
     * Run the next few AI players until the current player is a human player, or until the current
     * player has already already gone, in which case move to another betting round
     */
    /*
    private void runDrawingRound() {
        Player p = players.get(currentPlayer);
        System.out.println("Current Player = " + p.name());

        // If the current player has already gone this round, then the round ends and we move on to a new betting round
        if (p.hasDrawn) {
            untilCall--;
            newBettingRound();
        }

        // For all AI
        else if (!p.isPlayer) {
            p.hasDrawn = true;

            // Get the players choice based on their ai
            int c = p.drawChoice(untilCall);
            if (c == -1) {
                // Call
                addMessage(p.name() + " calls the round");
                newBettingRound();
                isCalled = true;
                return;
            } else if (c == 0) {
                // Stand
                addMessage(p.name() + " stands");
            } else if (c == 1) {
                // Draw
                drawCard(p);
            }

            nextPlayer();
            runDrawingRound();
        }
    }
     */

    /**
     * A helper function that has the input player draw a card
     * @param p player
     */
    private void drawCard(Player p) {
       p.addCard(deck.drawCard());
       addMessage(p.name() + " draws a card");
    }

    /**
     * Determine if all players or all players minus 1 have folded so that the round ends
     * @return true if this condition is met, false otherwise
     */
    private boolean allFolded() {
        boolean oneNotFolded = false;
        for (Player p : players) {
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
     * End the round and determine the winner. This happens after a final betting round
     * once the game has been called
     */
    private void endRound() {
        // First, find all the players who bombed out, they will be folded and they pay
        // credits equal to the main pot into the sabacc pot
        betweenRounds = true;
        for (Player p : players) {
            if (!p.folded) {
                System.out.println(p.name() + " : " + p.score());
                if (Math.abs(p.score()) > 23) {
                    p.folded = true;
                    p.modifyCredits(-mainPot);
                    sabaccPot += mainPot;
                    addMessage(p.name() + " has bombed out!");
                }
            }
        }

        // Then iterate over each non-folded player and compare them to the winner
        // Currently does not account for multiple tied winners
        Player winner = null;
        int currentScore = 0;
        for (Player p : players) {
            if (p.folded)
                continue;
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
            addMessage("There was no winner this round!");
        }
        else if (currentScore == 23 || winner.idiotsArray()) {
            addMessage(winner.name() + " won " + (mainPot + sabaccPot) + " credits with a pure sabacc!");
            winner.modifyCredits(mainPot + sabaccPot);
            mainPot = 0;
            sabaccPot = 0;
        } else {
            addMessage(winner.name() + " won " + mainPot + " credits with a hand of " + currentScore + "!");
            winner.modifyCredits(mainPot);
            mainPot = 0;
        }

        // After allocating credits, ask the player to start the next round
       // swapStage(nextRoundStage);
    }

    /**
     * Set up the three buttons for the betting round
     * They are:
     *  - Bet (call the current bid)
     *  - Raise (raise the current bid)
     *  - Fold (drop out of the round)
     */
    /*
    private void initializeBettingButtons() {
        bettingStage = new Stage(viewport);

        // All 3 buttons use the same style
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = game.font32;
        buttonStyle.up = uiSkin.getDrawable("button3-up");
        buttonStyle.down = uiSkin.getDrawable("button3-down");

        // BET
        TextButton betButton = new TextButton("Bet", buttonStyle);
        betButton.setWidth(200);
        betButton.setHeight(128);
        betButton.setPosition(0,0);
        betButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                Player p = players.get(currentPlayer);
                if (!p.isPlayer)
                    return;
                matchCurrentBid(p);
                nextPlayer();
                runBettingRound();
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
                swapStage(setBetStage);
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
                Player p = players.get(currentPlayer);
                if (p.isPlayer) {
                    p.folded = true;
                    nextPlayer();
                    runBettingRound();
                }
            }
        });

        bettingStage.addActor(betButton);
        bettingStage.addActor(raiseButton);
        bettingStage.addActor(foldButton);
    }

     */

    /**
     * Set up the two buttons for the drawing before a player can call
     * They are:
     *  - Draw (draw a card)
     *  - Stand (don't draw a card)
     */
    /*
    private void initializeDrawingButtons() {
        drawingStage = new Stage(viewport);

        // Both active buttons use the same style
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = game.font32;
        buttonStyle.up = uiSkin.getDrawable("button3-up");
        buttonStyle.down = uiSkin.getDrawable("button3-down");

        // DRAW
        TextButton drawButton = new TextButton("Draw", buttonStyle);
        drawButton.setWidth(200);
        drawButton.setHeight(128);
        drawButton.setPosition(0,0);
        drawButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                Player p = players.get(currentPlayer);
                if (p.isPlayer) {
                    p.hasDrawn = true;
                    drawCard(p);
                    nextPlayer();
                    runDrawingRound();
                }
            }
        });

        // STAND
        TextButton standButton = new TextButton("Stand", buttonStyle);
        standButton.setWidth(200);
        standButton.setHeight(128);
        standButton.setPosition(200,0);
        standButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                Player p = players.get(currentPlayer);
                if (p.isPlayer) {
                    p.hasDrawn = true;
                    addMessage(p.name() + " stands");
                    nextPlayer();
                    runDrawingRound();
                }
            }
        });

        // Both buttons use the same style
        TextButton.TextButtonStyle redButtonStyle = new TextButton.TextButtonStyle();
        redButtonStyle.font = game.font32;
        redButtonStyle.up = uiSkin.getDrawable("button3-unavailable");

        // CALL THAT DOESNT DO ANYTHING HERE
        TextButton callButton = new TextButton("Call", redButtonStyle);
        callButton.setWidth(200);
        callButton.setHeight(128);
        callButton.setPosition(400,0);

        drawingStage.addActor(drawButton);
        drawingStage.addActor(standButton);
        drawingStage.addActor(callButton);
    }

     */

    /**
     * Set up the three buttons for the drawing after a player can call
     * They are:
     *  - Draw (draw a card)
     *  - Stand (don't draw a card)
     *  - Call (end the round after one final betting round)
     */
    /*
    private void initializeDrawingButtonsWithCall() {
        drawingStage2 = new Stage(viewport);

        // Both buttons use the same style
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = game.font32;
        buttonStyle.up = uiSkin.getDrawable("button3-up");
        buttonStyle.down = uiSkin.getDrawable("button3-down");

        // DRAW
        TextButton drawButton = new TextButton("Draw", buttonStyle);
        drawButton.setWidth(200);
        drawButton.setHeight(128);
        drawButton.setPosition(0,0);
        drawButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                Player p = players.get(currentPlayer);
                if (p.isPlayer) {
                    p.hasDrawn = true;
                    drawCard(p);
                    nextPlayer();
                    runDrawingRound();
                }
            }
        });

        // STAND
        TextButton standButton = new TextButton("Stand", buttonStyle);
        standButton.setWidth(200);
        standButton.setHeight(128);
        standButton.setPosition(200,0);
        standButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                Player p = players.get(currentPlayer);
                if (p.isPlayer) {
                    p.hasDrawn = true;
                    addMessage(p.name() + " stands");
                    nextPlayer();
                    runDrawingRound();
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
                Player p = players.get(currentPlayer);
                if (p.isPlayer) {
                    addMessage(p.name() + " calls the round");
                    newBettingRound();
                    isCalled = true;
                }
            }
        });

        drawingStage2.addActor(drawButton);
        drawingStage2.addActor(standButton);
        drawingStage2.addActor(callButton);
    }

     */

    /**
     * One large button to start the next round of play
     */
    /*
    private void initializeNextRoundButton() {
        nextRoundStage = new Stage(viewport);

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = game.font32;
        buttonStyle.up = uiSkin.getDrawable("button1-up");
        buttonStyle.down = uiSkin.getDrawable("button1-down");

        TextButton startButton = new TextButton("Start Next Round", buttonStyle);
        startButton.setWidth(600);
        startButton.setHeight(128);
        startButton.setPosition(0,0);
        startButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                startNewRound();
            }
        });

        nextRoundStage.addActor(startButton);
    }

     */

    /**
     * Initialize the bet setting stage
     */
    /*
    private void initializeSettingBet() {
        setBetStage = new Stage(viewport);

        // The start of where the text field and buttons will be drawn
        int y = game.height / 2 + 100;

        TextField.TextFieldStyle style = new TextField.TextFieldStyle();
        style.font = game.font24;
        style.background = uiSkin.getDrawable("button1-up");
        style.fontColor = Color.WHITE;
        final TextField field = new TextField("", style);
        field.setPosition(0, y);
        field.setSize(600,96);
        field.setTextFieldFilter(new TextFieldFilter.DigitsOnlyFilter());
        field.setTextFieldListener(new TextField.TextFieldListener() {
            @Override
            public void keyTyped(TextField textField, char c) {
                System.out.print(field.getText());
                if (c == '\n')
                    playerRaise(field.getText());
            }
        });
        setBetStage.addActor(field);

        y -= 96;

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = game.font32;
        buttonStyle.up = uiSkin.getDrawable("small-button3-up");
        buttonStyle.down = uiSkin.getDrawable("small-button3-down");

        // Accept
        TextButton acceptButton = new TextButton("Accept", buttonStyle);
        acceptButton.setWidth(200);
        acceptButton.setHeight(96);
        acceptButton.setPosition(0,y);
        acceptButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                playerRaise(field.getText());
            }
        });
        setBetStage.addActor(acceptButton);

        // Cancel
        TextButton cancelButton = new TextButton("Cancel", buttonStyle);
        cancelButton.setWidth(200);
        cancelButton.setHeight(96);
        cancelButton.setPosition(400,y);
        cancelButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                swapStage(bettingStage);
            }
        });
        setBetStage.addActor(cancelButton);
    }

     */

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
        currentStage.dispose();
        //bettingStage.dispose();
        drawingStage.dispose();
        //drawingStage2.dispose();
        //nextRoundStage.dispose();
        //setBetStage.dispose();
        uiSkin.dispose();
    }
}
