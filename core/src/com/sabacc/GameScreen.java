package com.sabacc;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
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
        players.get(currentPlayer).updateButton(); // Update this players button
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
        messages.addFirst(message);
    }

    // The two pots
    public int mainPot;
    public int sabaccPot;

    // If this is the start of the round, before the opening hands have been dealt
    // To handle preliminary betting before the opening hands
    public boolean startOfRound;

    // If the round has been called
    public boolean isCalled;

    // The timer used to add a delay between ai actions
    final public SabaccTimer timer;

    // The currently selected card, defaults to null
    private Card selected;
    final private int smallCardHeight;
    final private int smallCardWidth;
    final private int smallCardNum;     // how many small cards fit on the screen per row at once

    // A drawable that displays nothing, but is less ugly than nothing
    public Drawable noButton;

    // Some other general UI drawables
    final private Drawable selectCover;
    final private Drawable background;

    // The input multiplexer that holds standard input (such as selecting cards in hand) and input relevant to the current stage
    final public InputMultiplexer input;
    private InputAdapter baseInput;
    private Stage baseStage;
    private Array<PlayerButton> playerButtons;
    private Vector3 inputTouch = new Vector3();

    // A set of gamestages that are swapped between
    final private FitViewport viewport;
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
        // Remove all previous stage based input processors past the base input adapter and the base Stage
        for (int i = 2; i < input.size(); i++) {
            input.removeProcessor(i);
        }
        input.addProcessor(next);
    }

    // The skin for different buttons
    final public Skin uiSkin;
    final private Drawable playerbox;

    // The starting x coordinate of where to draw the player's hand, to enable horizontal scrolling
    private int startOfHand;

    // The starting y coordinates of various rectangles to enable vertical scrolling
    private int startScrollOpponents;
    private int maxScrollOpponents;

    // The starting y coordinate of each screen area, to enable vertical scrolling
    private Rectangle menuRect;
    public Rectangle potRect;            // Public as it is seen by the betting stage
    private Rectangle messageRect;
    private Rectangle opponentRect;
    private Rectangle selectRect;
    private Rectangle handRect;
    public Rectangle buttonRect;
    private Rectangle currentRect;


    public GameScreen(final Sabacc game, int numOfOpponents, Skin uiSkin) {
        // Set up some preliminary variables that are needed
        this.game = game;
        deck = new Deck();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, game.width, game.height);
        messages = new Queue<String>();
        timer = new SabaccTimer(this);
        this.uiSkin = uiSkin;
        noButton = uiSkin.getDrawable("button1-up");

        // Initialize where each screen area exists
        buttonRect = new Rectangle(0,0,600,112); // 96 is a little small, but 128 is a little big
        handRect = new Rectangle(0, buttonRect.y + buttonRect.height, 600, 172);
        selectRect = new Rectangle(0, handRect.y + handRect.height, 600, 72);
        opponentRect = new Rectangle(0, selectRect.y + selectRect.height, 600, game.height - 288 - (selectRect.y + selectRect.height));
        messageRect = new Rectangle(0, game.height - 288, 600, 160);
        potRect = new Rectangle(0, game.height - 128, 600, 64);
        menuRect = new Rectangle(0, game.height - 64, 600, 64);

        // Load the viewport for the different button stages
        // Keep track of the current stage, either for the betting round or the drawing round
        viewport = new FitViewport(game.width, game.height, camera);
        playerbox = uiSkin.getDrawable("player-box");
        selectCover = uiSkin.getDrawable("select-rect");
        background = uiSkin.getDrawable("background");

        // Default small card size (for opponents) is 70x96 px
        smallCardWidth = 70;
        smallCardHeight = 96;
        smallCardNum = 600 / smallCardWidth;

        // Set up all players
        players = new Array<Player>();
        player = new Player(true, "Alex", game.startingCredits);
        players.add(player);
        Player p;
        for (int i = 0; i < numOfOpponents; i++) {
            p = new Player(false, "Opponent " + i, game.startingCredits);
            p.setBidRange(0.2f, 0.6f);
            players.add(p);
        }

        // Initialize all game stages
        drawingStage = new DrawingStage(this, viewport);
        bettingStage = new BettingStage(this, viewport);
        nextRoundStage = new NextRoundStage(this, viewport);

        // Initializes the base input stage to handle hand actions
        input = new InputMultiplexer();
        initializeBaseInput();

        // Start the game
        displayPlayerHands();
        setGameStage(nextRoundStage);
    }

    public void displayPlayerHands() {
        for (Player p : players) {
            p.displayHand = true;
            if (!p.folded && game.autoDisplayAndHide && p.button != null)
                p.button.setChecked(true);
            p.updateButton();
        }
        updateButtonPositions();
    }
    public void hidePlayerHands() {
        for (Player p : players) {
            p.displayHand = false;
            if (game.autoDisplayAndHide && p.button != null)
                p.button.setChecked(false);
            p.updateButton();
        }
        updateButtonPositions();
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
        // Clear the screen to the background colour
        ScreenUtils.clear(game.backgroundColour);

        // Update the camera
        camera.update();
        game.batch.setProjectionMatrix(camera.combined);

        game.batch.begin();

        // Draw each opponents UI stuff first as it can be covered by everything else
        baseStage.draw();
        drawPlayerHands();

        // Draw the players hand
        displayHand();

        // For now just draw a big background box from messages to the top of the screen, to block
        // off opponents going under messages and looking ugly
        background.draw(game.batch, 0, messageRect.y, 600, messageRect.height + potRect.height + menuRect.height);

        // For now, just write the pot values at the top of the screen
        game.font24.draw(game.batch, "Main Pot: " + mainPot, 16, potRect.y + 48);
        game.font24.draw(game.batch, "Sabacc Pot: " + sabaccPot, 272, potRect.y + 48);

        // Write how many credits the player currently has
        game.font24.draw(game.batch, "Credits: " + player.credits(), 16, potRect.y + 24);

        // Write all game messages at the top of the screen
        for (int i = 0; i < Math.min(game.maxMessages, messages.size); i++)
            game.msgFont.draw(game.batch, messages.get(i), 16, messageRect.y + i*20 + 20);

        // Show the current stage on top of everything when it is the players turn
        // @todo this is a bit lazy to check the current stage here
        if (getCurrentPlayer().isHuman || currentStage == nextRoundStage)
            currentStage.show();
        else
            noButton.draw(game.batch, 0,0,buttonRect.width,buttonRect.height);
        game.batch.end();

        // Start a short delay timer for the next ai player
        if (!getCurrentPlayer().isHuman)
            if (!timer.started)
                timer.call(game.aiTurnLength);
    }

    /**
     * Simply draw the cards in the players hand, immediately above the hand write the players
     * current score
     * @todo add some little arrows or something if there are cards off screen to remind the player they can scroll
     */
    private void displayHand() {
        selectCover.draw(game.batch, 0, selectRect.y, 600, selectRect.height);
        background.draw(game.batch, 0, handRect.y, 600, handRect.height);

        for (int i = 0; i < player.hand().size; i++) {
            Card c = player.hand().get(i);
            c.image.draw(game.batch, i * 120 - startOfHand, handRect.y, 120, 172);

            // If the card is selected by the player, draw the selected border over it
            if (c == selected)
                deck.selected().draw(game.batch, i * 120 - startOfHand, handRect.y, 120, 172);
        }
        if (player.numCards() > 0)
            game.font24.draw(game.batch, "Value: " + player.score(), 16, selectRect.y + 32);

        // Write the name of the currently selected card above the players hand value
        if (selected != null)
            game.font24.draw(game.batch, selected.name, 16, selectRect.y + 56);
    }

    /**
     * For each player, if their button is toggled and their cards are shown, show their cards
     */
    private void drawPlayerHands() {
        int y;
        int sy = (int)(opponentRect.y + opponentRect.height) - 64 + startScrollOpponents;
        Player p;
        for (PlayerButton b : playerButtons) {
            if (b == null)
                continue;
            y = sy;
            sy -= 64 + 6;
            y -= smallCardHeight;
            if (!b.isChecked())
                continue;
            p = b.player();
            for (int i = 0; i < p.hand().size; i++) {
                if (i == 0) {
                    playerbox.draw(game.batch, 0, y, 900, smallCardHeight);
                    sy -= smallCardHeight;
                }

                if (i > 0 && i % smallCardNum == 0) {
                    y -= smallCardHeight;
                    playerbox.draw(game.batch, 0, y, 900, smallCardHeight);
                    sy -= smallCardHeight;
                }

                if (p.displayHand)
                    p.hand().get(i).image.draw(game.batch, (i % smallCardNum) * smallCardWidth, y, smallCardWidth, smallCardHeight);
                else
                    deck.cardback().draw(game.batch, (i % smallCardNum) * smallCardWidth, y, smallCardWidth, smallCardHeight);
            }
            y -= smallCardHeight;
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
        addMessage("Ante is " + game.ante);
        hidePlayerHands();
        deck.refreshDeck();
        for (Player p : players) {
            p.refreshScore();
            p.hand().clear();
        }
        drawingStage.resetUntilCall();
        bettingStage.resetBettingRound();

        // Trying to fix a crash that happens when a bunch of players bomb out
        for (int i = 0; i < players.size; i++) {
            if (players.get(i).credits() >= game.ante * 2)
                continue;
            addMessage(players.get(i).name() + " drops from the game!");

            // Remove the player and its associated button
            // @todo not sure if removing the button works properly
            players.removeIndex(i);
            playerButtons.removeIndex(i);
            updateButtonPositions();
            i--;
        }

        // For now, automatically ante each player
        for (Player p : players) {
            p.folded = false;
            p.modifyCredits(-(game.ante*2));
            p.updateButton();
        }
        mainPot += game.ante * players.size;
        sabaccPot += game.ante * players.size;

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
        for (Player p : players) {
            for (int i = 0; i < 2; i++) {
                p.addCard(deck.drawCard());
            }
            p.updateButton();
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
        baseStage.dispose();
        uiSkin.dispose();
    }

    /**
     * Set up the base input adapter to deal with viewing cards and scrolling
     */
    private void initializeBaseInput() {

        // Sets up the base input adapter for scrolling and touching cards
        baseInput = new InputAdapter() {
            @Override
            public boolean touchDown(int x, int y, int pointer, int button) {
                selected = null;
                inputTouch.set(x, y, 0);
                camera.unproject(inputTouch);

                if (inputTouch.y > potRect.y && inputTouch.y < menuRect.y)
                    currentRect = potRect;
                else if (inputTouch.y > messageRect.y)
                    currentRect = messageRect;
                else if (inputTouch.y > opponentRect.y)
                    currentRect = opponentRect;
                else if (inputTouch.y > selectRect.y)
                    currentRect = selectRect;
                else if (inputTouch.y > handRect.y) {
                    currentRect = handRect;
                    int i = (int) ((inputTouch.x + startOfHand) / 120);
                    if (i < player.numCards()) {
                        selected = player.hand().get(i);
                        return true;
                    }
                }
                return false;
            }

            private int startX = -1;
            private int startY = -1;
            private int leftmost;
            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                inputTouch.set(screenX, screenY, 0);
                camera.unproject(inputTouch);

                if (startX == -1)
                    startX = (int) inputTouch.x;

                if (startY == -1)
                    startY = (int) inputTouch.y;

                if (currentRect == handRect) {
                    // Allow the player to scroll their hand left or right if they have too many cards
                    leftmost = Math.max(0, (player.numCards() - 5) * 120);
                    startOfHand += (int)(startX - inputTouch.x);
                    if (startOfHand > leftmost)
                        startOfHand = leftmost;
                    else if (startOfHand < 0)
                        startOfHand = 0;
                } else if (currentRect == opponentRect) {
                    // Allow the player to scroll their opponents up or down, to see ones that would otherwise not fit on the screen
                    startScrollOpponents = Math.max(0, Math.min(startScrollOpponents - (int)(startY - inputTouch.y), maxScrollOpponents));
                    updateButtonPositions();
                }

                startX = (int) inputTouch.x;
                startY = (int) inputTouch.y;
                return false;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                startX = -1;
                startY = -1;
                currentRect = null;
                return false;
            }
        };

        // Sets up each player button
        baseStage = new Stage(viewport);
        playerButtons = new Array<PlayerButton>();
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = game.font24;
        style.up = uiSkin.getDrawable("player-button-up");
        style.down = uiSkin.getDrawable("player-button-down");
        style.checked = uiSkin.getDrawable("player-button-checked");
        for (Player p : players) {
            if (p.isHuman) {
                // Humans do not have buttons currently, just set as null in the array to keep things proper when iterating
                playerButtons.add(null);
                continue;
            }
            final PlayerButton button = new PlayerButton(p, style);
            button.setWidth(600);
            button.setHeight(64);
            button.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent e, float x, float y) {
                    // @todo making sure the button is in bounds is kind of awkward, also expensive
                    inputTouch.set(0, Gdx.input.getY(), 0);
                    camera.unproject(inputTouch);
                    if (inputTouch.y < opponentRect.y || inputTouch.y > opponentRect.y + opponentRect.height)
                        button.setChecked(!button.isChecked());
                    else {
                        updateMaxScrollOpponents();
                        startScrollOpponents = Math.max(0, Math.min(startScrollOpponents, maxScrollOpponents));
                        updateButtonPositions();    // After toggling, update positions of each button
                    }
                }
            });
            p.button = button;
            playerButtons.add(button);
            baseStage.addActor(button);
        }
        updateButtonPositions();
        input.addProcessor(baseInput);
        input.addProcessor(baseStage);
        Gdx.input.setInputProcessor(input);
    }

    /**
     * Updates the position of each player button based on if previous ones are toggled or not
     */
    private void updateButtonPositions() {
        int y = (int)(opponentRect.y + opponentRect.height) - 64 + startScrollOpponents;
        maxScrollOpponents = 0;
        for (PlayerButton b : playerButtons) {
            if (b == null)
                continue;
            b.setPosition(0, y);
            y -= 64 + 6;   // An extra buffer of 6 pixels between buttons
            maxScrollOpponents += 70;
            if (b.isChecked())
                if (b.player().numCards() > 0) {
                    y -= smallCardHeight * ((b.player().numCards() / smallCardNum) + 1);
                    maxScrollOpponents += smallCardHeight * ((b.player().numCards() / smallCardNum) + 1);
                    //@todo maxScrollOpponents is handled lazily, fix later
                }
        }
        maxScrollOpponents -= opponentRect.height;
        Gdx.graphics.requestRendering();
    }

    /**
     * A small helper method that updates maxScrollOpponents without updating all button positions
     * Used for toggling opponents open or closed
     */
    private void updateMaxScrollOpponents() {
        maxScrollOpponents = 0;
        for (PlayerButton b : playerButtons) {
            if (b == null)
                continue;
            maxScrollOpponents += 70;
            if (b.isChecked())
                if (b.player().numCards() > 0)
                    maxScrollOpponents += smallCardHeight * ((b.player().numCards() / smallCardNum) + 1);
        }
    }
}

