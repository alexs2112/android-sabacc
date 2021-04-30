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
    public void modifyCredits(int x) { credits -= x; }

    // Handling the hand
    private Array<Card> hand;
    public Array<Card> hand() { return hand; }
    public int numCards() { return hand.size; }

    // For now, cap the player at 5 card maximum
    public void addCard(Card c) {
        if (numCards() < 5)
            hand.add(c);
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
     * Calculate the score by totalling up the cards in the players hand
     * @return the value of the players hand
     */
    public int calculateScore() {
        int s = 0;
        for (Card c : hand)
            s += c.value;
        return s;
    }
}
