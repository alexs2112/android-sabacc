package com.sabacc;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Queue;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class GameScreen implements Screen {
    final Sabacc game;
    private OrthographicCamera camera;

    // An array of players in the game, with a reference to the main player
    private Array<Player> players;
    private Player player;

    // The index of the current player and a small helper function to increment it to
    // the next active player
    private int currentPlayer;
    private void nextPlayer() {
        currentPlayer = (currentPlayer + 1) % players.size;
        while (players.get(currentPlayer).folded)
            currentPlayer = (currentPlayer + 1) % players.size;
    }

    // The game deck
    private Deck deck;

    // The array of past messages
    private Queue<String> messages;
    private void addMessage(String message) {
        System.out.println(message);
        messages.addLast(message);
        while (messages.size > game.maxMessages)
            messages.removeFirst();
    }

    // The buy in price for each game
    private int ante;

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

    // The currently selected card, defaults to null
    private Card selected;

    // Keep track of the current stage, either for the betting round or the drawing round
    private Stage currentStage;
    private Stage bettingStage;
    private Stage drawingStage;     // Without the option to call
    private Stage drawingStage2;    // With the option to call
    final private FitViewport viewport;

    // The skin for different buttons
    final private Skin uiSkin;
    final private Drawable playerbox;

    public GameScreen(final Sabacc game, int numOfOpponents, int ante) {

        // Set up some preliminary variables that are needed
        this.game = game;
        deck = new Deck();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, game.width, game.height);
        this.ante = ante;
        messages = new Queue<String>();

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

        initializeBettingButtons();
        initializeDrawingButtons();
        initializeDrawingButtonsWithCall();

        startNewRound();
        newBettingRound();
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

        // Show the current stage
        currentStage.draw();

        game.batch.begin();

        // Draw the players hand
        displayHand();

        // For now, just write the pot values at the top of the screen
        game.font24.draw(game.batch, "Main Pot: " + mainPot, 16, game.height - 24);
        game.font24.draw(game.batch, "Sabacc Pot: " + sabaccPot, 272, game.height - 24);

        // Write how many credits the player currently has
        game.font24.draw(game.batch, "Credits: " + player.credits(), 16, game.height - 48);

        // Write all game messages at the top of the screen
        for (int i = 0; i < messages.size; i++)
            game.msgFont.draw(game.batch, messages.get(i), 16, game.height - 80 - i*20);

        // Then draw each opponents box under the messages
        drawPlayerBoxes(game.height - 100 - game.maxMessages*20 - 96);

        game.batch.end();

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

            for (int c = 0; c < p.numCards(); c++) {
                // If game is not over
                deck.cardback().draw(game.batch, 530 - (c*70), sy - 96*(i-1), 70, 96);
                // Else show the card face up and write the value beneath C
            }
        }
    }

    /**
     * Unfold each player, check to make sure each player can afford to ante the next round
     * If they cannot, they leave the table
     */
    private void startNewRound() {
        // Start by refreshing the deck and clearing each players hand
        deck.refreshDeck();
        for (Player p : players) {
            p.refreshScore();
            p.hand().clear();
        }

        for (int i = 0; i < players.size; i++) {
            if (players.get(i).credits() >= ante * 2)
                continue;
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
    private void swapStage(Stage newStage) {
        currentStage = newStage;
        Gdx.input.setInputProcessor(currentStage);
    }

    /**
     * Run a new betting round
     * Set the current bid to 0
     * Set each players current bid to 0
     * Iterate over each opponent and have them make a bid according to their ai
     *
     * For now, start with the player, and then ask each opponent for input
     */
    private void newBettingRound() {
        System.out.println("New Betting Round!");
        swapStage(bettingStage); // Set the stage to the betting stage
        currentBid = 5;         // For now set it to 5 every time so we can easily know when betting is done
        for (Player p : players)
            p.currentBid = 0;

        // After the bid has been set, run a betting round
        runBettingRound();
    }

    /**
     * Run the next few AI players until the current player is a human player, or until the current
     * player has already matched the current bid, in which case the betting round ends
     */
    private void runBettingRound() {
        Player p = players.get(currentPlayer);
        System.out.println("Current Player = " + p.name());

        // If the current player has already called the bid, then the betting round is over
        if (p.currentBid == currentBid) {

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
        }

        // For all AI, for now just have them match the bid
        else if (!p.isPlayer) {
            matchCurrentBid(p);
            nextPlayer();
            runBettingRound();
        }
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
            nextPlayer();
            addMessage(p.name() + " has folded!");
            return;
        }
        p.modifyCredits(-v);
        p.currentBid += v;
        mainPot += v;
        addMessage(p.name() + " matches the current bid of " + currentBid + " credits");
    }

    /**
     * Set up a new drawing round, swapping the stage and making sure all players have not yet gone
     */
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

    /**
     * Run the next few AI players until the current player is a human player, or until the current
     * player has already already gone, in which case move to another betting round
     */
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
            if (Math.random() < 0.5)
                drawCard(p);
            else
                addMessage(p.name() + " stands");
            nextPlayer();
            runDrawingRound();
        }
    }

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
            return;
        }
        if (currentScore == 23 || winner.idiotsArray()) {
            addMessage(winner.name() + " won with a pure sabacc!");
            winner.modifyCredits(mainPot + sabaccPot);
            mainPot = 0;
            sabaccPot = 0;
        } else {
            addMessage(winner.name() + " won with a hand of " + currentScore + "!");
            winner.modifyCredits(mainPot);
            mainPot = 0;
        }

        // After allocating credits, start the next round
        startNewRound();
    }

    /**
     * Set up the three buttons for the betting round
     * They are:
     *  - Set (call the current bid)
     *  - Bet (raise the current bid)
     *  - Fold (drop out of the round)
     */
    private void initializeBettingButtons() {
        bettingStage = new Stage(viewport);

        // All 3 buttons use the same style
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = game.font32;
        buttonStyle.up = uiSkin.getDrawable("button3-up");
        buttonStyle.down = uiSkin.getDrawable("button3-down");

        // SET
        TextButton setButton = new TextButton("Set", buttonStyle);
        setButton.setWidth(200);
        setButton.setHeight(128);
        setButton.setPosition(0,0);
        setButton.addListener(new ClickListener() {
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

        // BET
        TextButton betButton = new TextButton("Bet", buttonStyle);
        betButton.setWidth(200);
        betButton.setHeight(128);
        betButton.setPosition(200,0);
        betButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                // for now just dont let the player bet
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

        bettingStage.addActor(setButton);
        bettingStage.addActor(betButton);
        bettingStage.addActor(foldButton);
    }

    /**
     * Set up the two buttons for the drawing before a player can call
     * They are:
     *  - Draw (draw a card)
     *  - Stand (don't draw a card)
     */
    private void initializeDrawingButtons() {
        drawingStage = new Stage(viewport);

        // Both buttons use the same style
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = game.font32;
        buttonStyle.up = uiSkin.getDrawable("button2-up");
        buttonStyle.down = uiSkin.getDrawable("button2-down");

        // DRAW
        TextButton drawButton = new TextButton("Draw", buttonStyle);
        drawButton.setWidth(300);
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
        standButton.setWidth(300);
        standButton.setHeight(128);
        standButton.setPosition(300,0);
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

        drawingStage.addActor(drawButton);
        drawingStage.addActor(standButton);
    }

    /**
     * Set up the three buttons for the drawing after a player can call
     * They are:
     *  - Draw (draw a card)
     *  - Stand (don't draw a card)
     *  - Call (end the round after one final betting round)
     */
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
        bettingStage.dispose();
        drawingStage.dispose();
        drawingStage2.dispose();
        uiSkin.dispose();
    }
}
