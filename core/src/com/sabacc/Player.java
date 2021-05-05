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

    // A decimal of the current amount of credits, when betting a player will bet a minimum of their
    // minbid and fold if it is above their maxbid
    private float minbid;
    private float maxbid;
    public void setBidRange(float min, float max) { minbid = min; maxbid = max; }

    // How much a player has bid over an entire betting round, to stop them from just raising every
    // single time it is their turn to raise
    public int roundbid;

    // Handling the hand
    private final Array<Card> hand;
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
     * @return -1 if the player folds, an integer of how much the new current bid is
     */
    public int makeBet(int mainPot, int bid, Array<Player> players, boolean isCalled) {
        // For now, make it extremely simple
        // Folding Conditions:
        //  - If the player cannot afford to call the bid
        //  - If the current bid is greater than this players max bid
        //  - If this player has all 5 cards in hand but a hand value less than 14
        //  - If the hand value is bombed out, then fold
        // Raising Conditions:
        //  - If the player has a hand > 17, raise by minbid
        //  - If the player has a pure sabacc, raise by double minbid
        //  - If the player would bid and the current bid or their round bid is already > those values, just call
        // Otherwise, just call
        int aScore = Math.abs(score);

        // If they cannot afford to call, then fold
        if (bid - currentBid > credits)
            return -1;
        // If the current bid is greater than the max bid and you do not have a pure sabacc, fold
        if (bid > maxbid * mainPot && aScore != 23)
            return -1;
        // If they are bombed out and there is a nonzero bid, fold
        if (aScore > 23 && (bid > 0 || isCalled))
            return -1;
        // If they have a bad hand with no chance of drawing more cards, fold
        if (numCards() == 5 && aScore < 14 && bid > 0)
            return -1;

        // If they have a pure sabacc, raise the bid by double minbid
        if (aScore == 23 && bid < 2*minbid * credits && roundbid < 2*maxbid * credits)
            return bid + (int)(credits * 2 * minbid);

        // If they have a good hand, raise the bid by minbid
        if (aScore > 17 && bid < minbid * credits && roundbid < maxbid * credits)
            return bid + (int)(credits * minbid);

        // Otherwise, match the bid
        return bid;
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
        if (untilCall <= 0 && Math.abs(score) > 17 && Math.abs(score) < 24)
            return -1;
        return 0;

    }
}
