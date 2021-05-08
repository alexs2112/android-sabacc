package com.sabacc;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Queue;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.Timer;
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
    public Player getCurrentPlayer() {
        // Double check, this is a bit lazy, fix it later
        while (currentPlayer >= players.size)
            currentPlayer = (currentPlayer + 1) % players.size;
        return players.get(currentPlayer);
    }
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
    public int mainPot;
    public int sabaccPot;

    // If this is the start of the round, before the opening hands have been dealt
    // To handle preliminary betting before the opening hands
    public boolean startOfRound;

    // If the round has been called
    public boolean isCalled;

    // Between rounds, to display opponents hands (if any) and their hand values
    public boolean betweenRounds;

    // The timer used to add a delay between ai actions
    final public SabaccTimer timer;

    // The currently selected card, defaults to null
    private Card selected;

    // Keep track of the current stage, either for the betting round or the drawing round
    final private FitViewport viewport;

    // The input multiplexer that holds standard input (such as selecting cards in hand) and input relevant to the current stage
    final public InputMultiplexer input;
    public InputAdapter baseInput;

    // A set of gamestages that are swapped between
    public DrawingStage drawingStage;
    public BettingStage bettingStage;
    public NextRoundStage nextRoundStage;
    public GameStage currentStage;
    public void setGameStage(GameStage next) {
        currentStage = next;
        currentStage.start();
    }

    /**
     * Sets the current input processor and removes all previous ones that are not the base input
     * @param next the next input processor to be set to
     */
    public void setStageInput(InputProcessor next) {
        // Remove all previous stage based input processors past the base input stage
        for (int i = 1; i < input.size(); i++) {
            input.removeProcessor(i);
        }
        input.addProcessor(next);
    }

    // The skin for different buttons
    final public Skin uiSkin;
    final private Drawable playerbox;

    // The starting x coordinate of where to draw the player's hand, to enable scrolling
    // This is likely to be negative at all times, it cannot be above 0
    private int startOfHand;

    public GameScreen(final Sabacc game, int numOfOpponents, int ante, Skin uiSkin) {
        // Set up some preliminary variables that are needed
        this.game = game;
        deck = new Deck();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, game.width, game.height);
        this.ante = ante;
        messages = new Queue<String>();
        betweenRounds = true;
        timer = new SabaccTimer(this);
        this.uiSkin = uiSkin;

        // Initializes the base input stage to handle hand actions
        input = new InputMultiplexer();
        initializeBaseInput();

        // Load the viewport for the different button stages
        viewport = new FitViewport(game.width, game.height, camera);
        playerbox = uiSkin.getDrawable("player-box");

        // Set up all players
        players = new Array<Player>();
        player = new Player(true, "Alex", 500);
        players.add(player);
        Player p;
        for (int i = 0; i < numOfOpponents; i++) {
            p = new Player(false, "Opponent " + i, 500);
            p.setBidRange(0.2f, 0.6f);
            players.add(p);
        }

        // Initialize all game stages
        drawingStage = new DrawingStage(this, viewport);
        bettingStage = new BettingStage(this, viewport);
        nextRoundStage = new NextRoundStage(this, viewport);

        // Start the game
        setGameStage(nextRoundStage);
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

        // Write all game messages at the top of the screen
        for (int i = 0; i < messages.size; i++)
            game.msgFont.draw(game.batch, messages.get(i), 16, game.height - 80 - i*20);

        // Then draw each opponents box under the messages
        drawPlayerBoxes(game.height - 100 - game.maxMessages*20 - 96);

        // Show the current stage on top of everything
        currentStage.show();

        game.batch.end();

        // Start a short delay timer for the next ai player
        if (!getCurrentPlayer().isHuman)
            if (!timer.started)
                timer.call(game.aiTurnLength);
    }

    /**
     * Set up the base input adapter to deal with viewing cards and scrolling
     */
    private void initializeBaseInput() {
        baseInput = new InputAdapter() {
            @Override
            public boolean touchDown(int x, int y, int pointer, int button) {
                selected = null;
                Vector3 touch = new Vector3();
                touch.set(x, y, 0);
                camera.unproject(touch);
                if (touch.y > 124 && touch.y < 124 + 172) {
                    int i = (int)((touch.x + startOfHand) / 120);
                    if (i < player.numCards()) {
                        selected = player.hand().get(i);
                        return true;
                    }
                }
                return false;
            }

            private int startX = -1;
            private int leftmost;
            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                if (startX == -1) {
                    if (screenY * game.ratioHeight > game.height - 124 || screenY * game.ratioHeight < game.height - (124 + 172))
                        return false;
                    startX = screenX;
                }

                // Make sure that the hand remains in bounds
                leftmost = Math.max(0, (player.numCards() - 5) * 120);
                startOfHand += (int)((startX - screenX) * game.ratioWidth);
                if (startOfHand > leftmost)
                    startOfHand = leftmost;
                else if (startOfHand < 0)
                    startOfHand = 0;
                System.out.println(startOfHand);

                startX = screenX;
                return false;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                startX = -1;
                return false;
            }
        };

        input.addProcessor(baseInput);
        Gdx.input.setInputProcessor(input);
    }

    /**
     * Simply draw the cards in the players hand, immediately above the hand write the players
     * current score
     * @todo add some little arrows or something if there are cards off screen to remind the player they can scroll
     */
    private void displayHand() {
        for (int i = 0; i < player.hand().size; i++) {
            Card c = player.hand().get(i);
            c.image.draw(game.batch, i * 120 - startOfHand, 128, 120, 172);

            // If the card is selected by the player, draw the selected border over it
            if (c == selected)
                deck.selected().draw(game.batch, i * 120 - startOfHand, 128, 120, 172);
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
    public void startNewRound() {
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
        drawingStage.resetUntilCall();
        bettingStage.resetBettingRound();

        // Trying to fix a crash that happens when a bunch of players bomb out
        int max = players.size;
        for (int i = 0; i < max; i++) {
            if (players.get(i).credits() >= ante * 2)
                continue;
            addMessage(players.get(i).name() + " drops from the game!");
            players.removeIndex(i);
            i--;
            max--;
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
        isCalled = false;

        // Rounds begin with a preliminary betting round
        setGameStage(bettingStage);
    }

    /**
     * Deal the starting hand of 2 cards to each player
     */
    public void dealStartingHand() {
        addMessage("Dealing starting hands!");
        for (int i = 0; i < 2; i++) {
            for (Player p : players) {
                p.addCard(deck.drawCard());
            }
        }
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
        nextRoundStage.dispose();
        uiSkin.dispose();
    }
}

