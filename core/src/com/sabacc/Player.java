package com.sabacc;

import com.badlogic.gdx.utils.Array;

public class Player {
    // A player has to have a name
    final private String name;
    public String name() { return name; }

    // A bool to determine if this is a player or not to check if the game requires user input
    final public boolean isPlayer;

    // Handling credits
    private int credits;
    public int credits() { return credits; }
    public void modifyCredits(int x) { credits += x; }

    // Handling the hand
    private Array<Card> hand;
    public Array<Card> hand() { return hand; }
    public int numCards() { return hand.size; }

    // The current score
    private int score;
    public int score() { return score; }
    public void refreshScore() { score = 0; }

    /**
     * A function to simply add a card to the players hand
     * Will not add if the player has 5 cards, due to screen size restraints
     * Fix this later
     */
    public void addCard(Card c) {
        if (numCards() < 5) {
            score += c.value;
            hand.add(c);
        }
    }

    // Some variables for each game round
    public int currentBid;
    public boolean folded;
    public boolean hasDrawn; // Not necessarily drawn a card, has gone in the drawing round

    public Player(boolean isPlayer, String name, int credits) {
        this.isPlayer = isPlayer;
        this.name = name;
        this.credits = credits;
        hand = new Array<Card>();
    }

    /**
     * Determine if the player currently has the idiots array, consisting of
     * the Idiot (0), 2, 3
     * @return true if an idiot's array is held
     */
    public boolean idiotsArray() {
        if (numCards() != 3)
            return false;
        boolean zero = false;
        boolean two = false;
        boolean three = false;
        for (Card c : hand) {
            if (c.value == 0)
                zero = true;
            else if (c.value == 2)
                two = true;
            else if (c.value == 3)
                three = true;
            else
                return false;
        }
        return zero && two && three;
    }
}
