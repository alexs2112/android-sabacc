package com.sabacc;

import com.badlogic.gdx.utils.Array;
import com.sabacc.gamestage.PlayerButton;

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

    // Handling the Interference Field
    private final Array<Card> field;
    public Array<Card> field() { return field; }
    public void fieldCard(Card c) {
        if (hand.removeValue(c, true))
            field.add(c);
    }
    public int numField() { return field.size; }

    // The current score
    private int score;
    public int score() { return score; }
    public void refreshScore() { score = 0; }

    // Some other player bools that can be toggled
    public boolean displayHand;    // If the score and hand should be displayed between rounds
    public PlayerButton button;
    public void updateButton() { if (button != null) button.update(); }

    /**
     * A function to simply add a card to the players hand and add its value
     * @param c the card to add
     */
    public void addCard(Card c) {
        score += c.value;
        hand.add(c);
    }

    /**
     * A helper function to implement a sabacc shift for this one player, replaces all the cards
     * in their hand with a new card from the deck
     * @param deck the deck to draw from
     */
    public void sabaccShift(Deck deck) {
        int n = numCards();
        for (Card c : hand)
            score -= c.value;
        hand.clear();
        while (n > 0) {
            n--;
            addCard(deck.drawCard());
        }
    }

    // Some variables for each game round
    public int currentBid;
    public boolean hasBet;   // To check if the player has matched the current bet
    public boolean folded;
    public boolean hasDrawn; // Not necessarily drawn a card, has gone in the drawing round

    // Some values if the player has gone all in or not
    public boolean isAllIn;
    public int allInValue;

    public Player(boolean isPlayer, String name, int credits) {
        this.isHuman = isPlayer;
        this.name = name;
        this.credits = credits;
        hand = new Array<Card>();
        field = new Array<Card>();
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
     * @return -1 if the player folds, -2 if the player goes all in, or an integer of how much this player will bet
     */
    public int makeBet(int mainPot, int bid, Array<Player> players, boolean isCalled) {
        // For now, make it extremely simple
        // Folding Conditions: (-1)
        //  - If the player cannot afford to call the bid and their hand is bad
        //  - If the current bid is greater than this players max bid and they do not have a pure sabacc
        //  - If this player has all 5 cards in hand but a hand value less than 14
        //  - If the hand value is bombed out, then fold
        // All in Conditions: (-2)
        //  - If the player cannot afford to call the bid and their hand is good
        //  - @todo If the player has less than 2x the ante credits, for now just set at 40 so would have to drop otherwise
        // Raising Conditions:
        //  - If the player has a hand > 17, raise by minbid
        //  - If the player has a pure sabacc, raise by double minbid
        //  - If the player would bid and the current bid or their round bid is already > those values, just call
        // Otherwise, just call
        int aScore = Math.abs(score);

        // If they have or will have less than 40 credits, always go all in as they will have to drop if they fold
        if (bid - currentBid > 0 && credits - (bid - currentBid) < 40)
            return -2;

        // If they cannot afford to call:
        if (bid - currentBid > credits) {
            // If their hand is bad, fold
            if (aScore < 20)
                return -1;
            // If their hand is good, go all in
            else
                return -2;
        }

        // Fold if there is a bid and this hand sucks
        if (bid - maxbid > 0 && aScore < 17)
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
            return bid - currentBid + (int)(credits * 2 * minbid);

        // If they have a good hand, raise the bid by minbid
        if (aScore > 17 && bid < minbid * credits && roundbid < maxbid * credits)
            return bid - currentBid + (int)(credits * minbid);

        // Otherwise, match the bid
        return bid - currentBid;
    }

    /**
     * A method that decides if this player should call, stand, or draw in the drawing round
     * Very simple for now, keep drawing until their hand value is 18-23, then call
     * @param untilCall how many rounds left until the round can be called
     * @return -1 if this player calls, 0 if they stand, 1 if they draw
     */
    public int drawChoice(int untilCall) {
        if (Math.abs(score) < 18 || (Math.abs(score) > 24 && Math.abs(score) < 40))
            return 1;
        if (untilCall <= 0 && Math.abs(score) > 17 && Math.abs(score) < 24)
            return -1;
        if (untilCall <= 0 && Math.random() < 0.2) // Randomly call 20% of the time if able, to prevent random loops
            return -1;
        return 0;
    }
}
