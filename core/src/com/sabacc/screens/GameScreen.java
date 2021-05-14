package com.sabacc.screens;

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
import com.sabacc.Card;
import com.sabacc.Deck;
import com.sabacc.Player;
import com.sabacc.Sabacc;
import com.sabacc.SabaccTimer;
import com.sabacc.gamestage.*;

public class GameScreen implements Screen {
    final public Sabacc game;
    final private OrthographicCamera camera;

    // An array of players in the game, with a reference to the main player
    final public Array<Player> players;
    final public Player player;

    // A couple variables set at the start of the game
    public int ante;

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
        sabaccShift();  // Try for a shift after each player's action
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
    public Card doubleTap;      // The current card the player can double tap, if tapped again within a time frame it will
                                // place the card into the interference field, otherwise gets set back to null
    final private int smallCardHeight;
    final private int smallCardWidth;
    final private int smallCardNum;     // how many small cards fit on the screen per row at once

    // A drawable that displays nothing, but is less ugly than nothing
    public Drawable noButton;

    // Some other general UI drawables
    final private Drawable uiLine;
    final private Drawable background;

    // The input multiplexer that holds standard input (such as selecting cards in hand) and input relevant to the current stage
    final public InputMultiplexer input;
    private Stage baseStage;
    private Array<PlayerButton> playerButtons;
    final private Vector3 inputTouch;

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
    final private Drawable lock;
    final private Drawable creditsBox;

    // The starting x coordinate of where to draw the player's hand, to enable horizontal scrolling
    private int startOfHand;

    // The starting y coordinates of various rectangles to enable vertical scrolling
    private int startScrollOpponents;
    private int maxScrollOpponents;

    // The starting y coordinate of each screen area, to enable vertical scrolling
    final private Rectangle menuRect;
    final public Rectangle potRect;            // Public as it is seen by the betting stage
    final private Rectangle messageRect;
    final private Rectangle opponentRect;
    final private Rectangle selectRect;
    final private Rectangle handRect;
    final public Rectangle buttonRect;
    private Rectangle currentRect;


    public GameScreen(final Sabacc game, Skin uiSkin, OrthographicCamera camera, FitViewport viewport,
                      int numOfOpponents, int ante, int startingCredits) {
        // @todo clean up this ugly constructor

        // Set up some preliminary variables that are needed
        this.game = game;
        deck = new Deck();
        this.camera = camera;
        messages = new Queue<String>();
        timer = new SabaccTimer(this);
        this.uiSkin = uiSkin;
        noButton = uiSkin.getDrawable("button1-up");
        inputTouch = new Vector3();
        this.ante = ante;

        // Initialize where each screen area exists
        // @todo make rectangles dynamic so the screen can be resized if needed
        buttonRect = new Rectangle(0,0,600,112); // 96 is a little small, but 128 is a little big
        handRect = new Rectangle(0, buttonRect.y + buttonRect.height, 600, 172);
        selectRect = new Rectangle(0, handRect.y + handRect.height, 600, 72);
        opponentRect = new Rectangle(0, selectRect.y + selectRect.height, 600, game.height - 288 - (selectRect.y + selectRect.height));
        messageRect = new Rectangle(0, game.height - 288, 600, 160);
        potRect = new Rectangle(0, game.height - 128, 600, 48);
        menuRect = new Rectangle(0, game.height - 80, 600, 80);

        // Load the viewport for the different button stages
        // Keep track of the current stage, either for the betting round or the drawing round
        this.viewport = viewport;
        playerbox = uiSkin.getDrawable("player-box");
        uiLine = uiSkin.getDrawable("line");
        background = uiSkin.getDrawable("background");
        lock = uiSkin.getDrawable("lock");
        creditsBox = uiSkin.getDrawable("credits");

        // Default small card size (for opponents) is 70x96 px
        smallCardWidth = 70;
        smallCardHeight = 96;
        smallCardNum = 600 / smallCardWidth;

        // Set up all players
        players = new Array<Player>();
        player = new Player(true, "Urist", startingCredits);
        players.add(player);
        Player p;
        for (int i = 0; i < numOfOpponents; i++) {
            p = new Player(false, "Opponent " + i, startingCredits);
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
        ScreenUtils.clear(0, 0, 0.2f, 1);

        // Update the camera
        camera.update();
        game.batch.setProjectionMatrix(camera.combined);

        game.batch.begin();

        // Draw each opponents UI stuff first as it can be covered by everything else
        baseStage.draw();
        drawPlayerHands();

        // Draw the players hand
        displayHand();

        // For now just draw a big background box from messages to the top of the pots, to block
        // off opponents going under messages and looking ugly
        background.draw(game.batch, 0, messageRect.y, 600, messageRect.height + potRect.height);

        // Write all game messages at the top of the screen
        uiLine.draw(game.batch, 0, messageRect.y, 600, 4);
        for (int i = 0; i < Math.min(game.maxMessages, messages.size); i++)
            game.msgFont.draw(game.batch, messages.get(i), 16, messageRect.y + i*20 + 26);

        // For now, just write the pot values at the top of the screen
        uiLine.draw(game.batch, 0, menuRect.y - 4, 600, 4);
        game.font24.draw(game.batch, "Main Pot: " + mainPot, 16, potRect.y + 34);
        game.font24.draw(game.batch, "Sabacc Pot: " + sabaccPot, 272, potRect.y + 34);

        // Write how many credits the player currently has next to the menu button
        creditsBox.draw(game.batch, 0, menuRect.y, 400, menuRect.height);
        game.font24.draw(game.batch, "Credits: " + player.credits(), 80, menuRect.y + 48);

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
        background.draw(game.batch, 0, handRect.y, 600, handRect.height + selectRect.height);
        uiLine.draw(game.batch, 0, selectRect.y + selectRect.height - 4, 600, 4);
        int i;
        Card c;
        for (i = 0; i < player.numCards(); i++) {
            c = player.hand().get(i);
            c.image.draw(game.batch, i * 120 - startOfHand, handRect.y, 120, 172);

            // If the card is selected by the player, draw the selected border over it
            if (c == selected)
                deck.selected().draw(game.batch, i * 120 - startOfHand, handRect.y, 120, 172);
        }
        for (int j = 0; j < player.numField(); j++) {
            c = player.field().get(j);
            c.image.draw(game.batch, (i+j) * 120 - startOfHand, handRect.y, 120, 172);

            // If the card is selected by the player, draw the selected border over it
            if (c == selected)
                deck.selected().draw(game.batch, (i+j) * 120 - startOfHand, handRect.y, 120, 172);

            // Draw the lock icon to show that this card is in the interference field
            lock.draw(game.batch, (i+j+1) * 120 - startOfHand - 48, handRect.y + handRect.height - 48, 32, 32);
        }

        if (player.numCards() + player.numField() > 0)
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

            // Display the opponents cards in hand if available, the cardback if not
            for (int i = 0; i < p.numCards() + p.numField(); i++) {
                if (i == 0) {
                    playerbox.draw(game.batch, 0, y, 900, smallCardHeight);
                    sy -= smallCardHeight;
                }

                if (i > 0 && i % smallCardNum == 0) {
                    y -= smallCardHeight;
                    playerbox.draw(game.batch, 0, y, 900, smallCardHeight);
                    sy -= smallCardHeight;
                }

                if (i < p.numCards()) {
                    if (p.displayHand)
                        p.hand().get(i).image.draw(game.batch, (i % smallCardNum) * smallCardWidth, y, smallCardWidth, smallCardHeight);
                    else
                        deck.cardback().draw(game.batch, (i % smallCardNum) * smallCardWidth, y, smallCardWidth, smallCardHeight);
                } else {
                    p.field().get(i - p.numCards()).image.draw(game.batch, (i % smallCardNum) * smallCardWidth, y, smallCardWidth, smallCardHeight);
                    lock.draw(game.batch, (i % smallCardNum) * smallCardWidth + (smallCardWidth - 32), y + smallCardHeight - 32, 24, 24);
                }
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
        addMessage("Ante is " + ante);
        hidePlayerHands();
        deck.refreshDeck();
        for (Player p : players) {
            p.refreshScore();
            p.hand().clear();
            p.field().clear();
        }
        drawingStage.resetUntilCall();
        bettingStage.resetBettingRound();

        // Trying to fix a crash that happens when a bunch of players bomb out
        for (int i = 0; i < players.size; i++) {
            if (players.get(i).credits() >= ante * 2)
                continue;
            addMessage(players.get(i).name() + " drops from the game!");

            // Remove the player and its associated button
            players.removeIndex(i);
            if (playerButtons.get(i) != null)
                playerButtons.get(i).setVisible(false);
            playerButtons.removeIndex(i);
            updateButtonPositions();
            i--;
        }

        // @todo have the button change to End Game, rather than Start Next Round when the game is over
        if (players.size == 1) {
            game.setScreen(new GameOverScreen(game, uiSkin, camera, viewport, players.get(0)));
            return;
        }
        if (players.size == 0) {
            game.setScreen(new GameOverScreen(game, uiSkin, camera, viewport, null));
            return;
        }

        // For now, automatically ante each player
        for (Player p : players) {
            p.folded = false;
            p.modifyCredits(-(ante*2));
            p.updateButton();
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
        for (Player p : players) {
            for (int i = 0; i < 2; i++) {
                p.addCard(deck.drawCard());
            }
            p.updateButton();
        }
    }

    /**
     * Randomly determines if a Sabacc Shift should happen. A chance after each player makes
     * a move
     * Replaces the current deck with a new deck, for each player they replace each card in their
     * hand with new cards. Cards in their Interference Field are not affected
     */
    public void sabaccShift() {
        if (Math.random() > game.shiftChance)
            return;
        addMessage("A Sabacc Shift has occurred!!");
        deck.refreshDeck();
        for (Player p : players)
            p.sabaccShift(deck);
    }

    /**
     * Removes the card from the players hand and adds it to their interference field, to
     * be protected from Sabacc Shifts but know to other players
     * @param p the player
     * @param c the card in their hand
     */
    public void fieldCard(Player p, Card c) {
        addMessage(p.name() + " places " + c.name + " into the Interference Field");
        p.fieldCard(c);
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
        // If the player double taps a card, field it
        // Otherwise, start a new timer to see if they double tap another card
        // Allow the player to scroll their hand left or right if they have too many cards
        // Allow the player to scroll their opponents up or down, to see ones that would otherwise not fit on the screen
        InputAdapter baseInput = new InputAdapter() {
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
                    if (i < player.numCards() + player.numField()) {
                        if (i < player.numCards()) {
                            selected = player.hand().get(i);
                            if (selected == doubleTap) {
                                // If the player double taps a card, field it
                                fieldCard(player, doubleTap);
                                doubleTap = null;
                            } else {
                                // Otherwise, start a new timer to see if they double tap another card
                                doubleTap = selected;
                                timer.timeDoubleTap(doubleTap, game.timeForDoubleTap);
                            }
                            return true;
                        } else {
                            selected = player.field().get(i - player.numCards());
                            return true;
                        }
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
                    leftmost = Math.max(0, (player.numCards() + player.numField() - 5) * 120);
                    startOfHand += (int) (startX - inputTouch.x);
                    if (startOfHand > leftmost)
                        startOfHand = leftmost;
                    else if (startOfHand < 0)
                        startOfHand = 0;
                } else if (currentRect == opponentRect) {
                    // Allow the player to scroll their opponents up or down, to see ones that would otherwise not fit on the screen
                    startScrollOpponents = Math.max(0, Math.min(startScrollOpponents - (int) (startY - inputTouch.y), maxScrollOpponents));
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

        baseStage = new Stage(viewport);

        // Sets up each player button
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
                        //updateButtonPositions();    // After toggling, update positions of each button
                    }
                }

                @Override
                public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                    super.touchUp(event, x, y, pointer, button);
                    updateButtonPositions();
                }
            });
            p.button = button;
            playerButtons.add(button);
            baseStage.addActor(button);
        }

        // Set up the top settings button
        style = new TextButton.TextButtonStyle();
        style.font = game.font24;
        style.up = uiSkin.getDrawable("settings-button-up");
        style.down = uiSkin.getDrawable("settings-button-down");
        TextButton settingsButton = new TextButton("Settings", style);
        settingsButton.setWidth(200);
        settingsButton.setHeight(menuRect.height);
        settingsButton.setPosition(400,menuRect.y);
        settingsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                // Pause any timers
                // @todo Open settings gamestage
            }
        });
        baseStage.addActor(settingsButton);

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
                if (b.player().totalCards() > 0) {
                    y -= smallCardHeight * ((b.player().totalCards() / smallCardNum) + 1);
                    maxScrollOpponents += smallCardHeight * ((b.player().totalCards() / smallCardNum) + 1);
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
                if (b.player().totalCards() > 0)
                    maxScrollOpponents += smallCardHeight * ((b.player().totalCards() / smallCardNum) + 1);
        }
    }
}

