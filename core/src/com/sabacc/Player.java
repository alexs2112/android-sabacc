package com.sabacc;

import com.badlogic.gdx.utils.Array;

public class Player {
    // A player has to have a name
    final private String name;
    public String name() { return name; }

    // A bool to determine if this is a player or not to check if the game requires user input
    final public boolean isHuman;

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
    public boolean hasBet;   // To check if the player has matched the current bet
    public boolean folded;
    public boolean hasDrawn; // Not necessarily drawn a card, has gone in the drawing round

    public Player(boolean isPlayer, String name, int credits) {
        this.isHuman = isPlayer;
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

    /**
     * A method that determines how much this player wants to bet, raise, or if they fold
     * @param mainPot the current value of the main pot
     * @param bid the current bid of the hand
     * @param players the array of players in the game
     * @return -1 if the player folds, an integer of how much they bet otherwise
     */
    public int makeBet(int mainPot, int bid, Array<Player> players) {
        // For now, make it extremely simple, if the hand value is abs(19-22), make a bet
        // of 10% of the pot value
        // If the hand value is abs(23), make a bet of 20%
        // If the hand value is bombed out, then fold
        // If the player cannot afford to call the bid, fold
        // If this player has all 5 cards in hand but a value of < 14, fold
        // Otherwise, call
        int aScore = Math.abs(score);
        if (bid > 0 && bid - currentBid > credits)
            return -1;
        if (aScore > 23 && bid > 0)
            return -1;
        if (aScore == 23)
            return bid - currentBid + Math.max((int)(mainPot * 0.2), credits);
        if (aScore > 18)
            return bid - currentBid + Math.max((int)(mainPot * 0.1), credits);
        if (numCards() == 5 && aScore < 14 && bid > 0)
            return -1;

        return bid - currentBid;
    }

    /**
     * A method that decides if this player should call, stand, or draw in the drawing round
     * @param untilCall how many rounds left until the round can be called
     * @return -1 if this player calls, 0 if they stand, 1 if they draw
     */
    public int drawChoice(int untilCall) {
        if (numCards() == 5 && untilCall <= 0)
            return -1;
        if (numCards() == 5)
            return 0;
        if (score < 16 && score > 0)
            return 1;
        if (score < 0 && score > -18)
            return 1;
        if (untilCall <= 0 && (score > 17 || score < -17))
            return -1;
        return 0;

    }
}
