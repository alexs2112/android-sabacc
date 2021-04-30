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
import com.badlogic.gdx.utils.Array;
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

    // The currently selected card, defaults to null
    private Card selected;

    // Keep track of the current stage, either for the betting round or the drawing round
    private Stage currentStage;
    private Stage bettingStage;
    private Stage drawingStage;
    final private FitViewport viewport;

    // The skin for different buttons
    final private Skin uiSkin;

    public GameScreen(final Sabacc game, int numOfOpponents, int ante) {

        // Set up some preliminary variables that are needed
        this.game = game;
        deck = new Deck();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, game.width, game.height);
        this.ante = ante;

        // Load the skin for the ui and the viewport for the different button stages
        uiSkin = new Skin();
        uiSkin.addRegions(new TextureAtlas(Gdx.files.internal("ui.atlas")));
        viewport = new FitViewport(game.width, game.height, camera);

        // Set up all players
        players = new Array<Player>();
        player = new Player(true, "Alex", 500);
        players.add(player);
        for (int i = 0; i < numOfOpponents; i++) {
            players.add(new Player(false, "Opponent " + i, 500));
        }

        initializeBettingButtons();
        initializeDrawingButtons();

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

        displayHand();

        if (selected != null)
            game.font24.draw(game.batch, selected.name, 32, 1000);

        // For now, just write the pot values at the top of the screen in tiny font
        game.font24.draw(game.batch, "Main Pot: " + mainPot, 16, game.height - 24);
        game.font24.draw(game.batch, "Sabacc Pot: " + sabaccPot, 272, game.height - 24);

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
     * Simply draw the cards in the players hand
     */
    private void displayHand() {
        for (int i = 0; i < player.hand().size; i++) {
            Card c = player.hand().get(i);
            c.image.draw(game.batch, i * 120, 128, 120, 172);

            // If the card is selected by the player, draw the selected border over it
            if (c == selected)
                deck.selected().draw(game.batch, i * 120, 128, 120, 172);
        }
    }

    /**
     * Unfold each player, check to make sure each player can afford to ante the next round
     * If they cannot, they leave the table
     */
    private void startNewRound() {
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
    }

    /**
     * Deal the starting hand of 2 cards to each player
     */
    private void dealStartingHand() {
        System.out.println("Dealing starting hands!");
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

            // If this is the preliminary betting round, deal starting hands
            if (startOfRound) {
                startOfRound = false;
                dealStartingHand();
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
     * it, otherwise force them to fld
     * @param p the player to call the current bid
     */
    private void matchCurrentBid(Player p) {
        int v = currentBid - p.currentBid;
        if (v > p.credits()) {
            p.folded = true;
            nextPlayer();
            System.out.println(p.name() + " has folded!");
            return;
        }
        p.modifyCredits(-v);
        p.currentBid += v;
        mainPot += v;
        System.out.println(p.name() + " matches the current bid of " + currentBid + " credits");
    }

    /**
     *
     */
    private void newDrawingRound() {
        System.out.println("New Drawing Round!");
        swapStage(drawingStage); // Set the stage to the drawing stage

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
        if (p.hasDrawn)
            newBettingRound();

        // For all AI
        else if (!p.isPlayer) {
            p.hasDrawn = true;
            if (Math.random() < 0.5)
                drawCard(p);
            else
                System.out.println(p.name() + " stands");
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
       System.out.println(p.name() + " draws a card");
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
     * Set up the two buttons for the drawing
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
                    System.out.println(p.name() + " stands");
                    nextPlayer();
                    runDrawingRound();
                }
            }
        });

        drawingStage.addActor(drawButton);
        drawingStage.addActor(standButton);
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
        uiSkin.dispose();
    }
}
