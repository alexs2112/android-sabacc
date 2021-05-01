package com.sabacc;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Array;

public class Deck {
    // An array of all cards in the game, for reshuffling the deck
    final private Array<Card> allCards;

    // The texture atlas and skin to use when allocating card images
    final private TextureAtlas atlas;
    final private Skin cardSkin;
    final private Drawable cardback;
    public Drawable cardback() { return cardback; }
    final private Drawable selected;
    public Drawable selected() { return selected; }

    // The current deck
    private Array<Card> cards;
    public Array<Card> cards() { return cards; }

    /**
     * Constructor, sets up the array of cards and generates the deck
     */
    public Deck() {
        allCards = new Array<Card>(false, 76);
        cards = new Array<Card>(false, 76);
        atlas = new TextureAtlas(Gdx.files.internal("cards.atlas"));
        cardSkin = new Skin();
        cardSkin.addRegions(atlas);
        cardback = cardSkin.getDrawable("cardback");
        selected = cardSkin.getDrawable("selected");
        generateDeck();
        refreshDeck();
    }

    /**
     * Create a new deck of cards, the configuration is:
     * - Four Suites (Sabers, Flasks, Coins, Staves):
     *   - Values 1-11                      flasks5
     *   - Ranked Cards:
     *     - Commander (12)                 saberscmdr
     *     - Mistress (13)                  coinsmstrss
     *     - Master (14)                    stavesmstr
     *     - Ace (15)                       sabersace
     * - Two copies of each face card
     *   - The Star (-17)                   star
     *   - The Evil One (-15)               evilone
     *   - Moderation (-14)                 moderation
     *   - Demise (-13)                     demise
     *   - Balance (-11)                    balance
     *   - Endurance (-8)                   endurance
     *   - Queen of Air and Darkness (-2)   queen
     *   - Idiot (0)                        idiot
     */
    private void generateDeck() {
        for (Card.Suite s : Card.Suite.values()) {
            if (s == Card.Suite.Face) {
                for (int i = 0; i < 2; i++) {
                    allCards.add(new Card("The Star", s, -17, cardSkin.getDrawable("star")));
                    allCards.add(new Card("The Evil One", s, -15, cardSkin.getDrawable("evilone")));
                    allCards.add(new Card("Moderation", s, -14, cardSkin.getDrawable("moderation")));
                    allCards.add(new Card("Demise", s, -13, cardSkin.getDrawable("demise")));
                    allCards.add(new Card("Balance", s, -11, cardSkin.getDrawable("balance")));
                    allCards.add(new Card("Endurance", s, -8, cardSkin.getDrawable("endurance")));
                    allCards.add(new Card("Queen of Air and Darkness", s, -2, cardSkin.getDrawable("queen")));
                    allCards.add(new Card("Idiot", s, 0, cardSkin.getDrawable("idiot")));
                }
                continue;
            }
            for (int i = 1; i <= 15; i++) {
                String name;
                String atlasName;
                if (i <= 11) {
                    name = i + " of " + s.name();
                    atlasName = s.name().toLowerCase() + i;
                } else if (i == 12) {
                    name = "Commander of " + s.name();
                    atlasName = s.name().toLowerCase() + "cmdr";
                } else if (i == 13) {
                    name = "Mistress of " + s.name();
                    atlasName = s.name().toLowerCase() + "mstrss";
                } else if (i == 14) {
                    name = "Master of " + s.name();
                    atlasName = s.name().toLowerCase() + "mstr";
                } else {
                    name = "Ace of " + s.name();
                    atlasName = s.name().toLowerCase() + "ace";
                }
                allCards.add(new Card(name, s, i, cardSkin.getDrawable(atlasName)));
            }
        }
    }

    /**
     * Copy all cards into the current array of cards
     */
    public void refreshDeck() {
        cards = new Array<Card>(allCards);
    }

    /**
     * Pop a random card from the deck
     */
    public Card drawCard() {
        if (cards.size == 0)
            return null;
        int i = (int)(Math.random() * cards.size);
        return cards.removeIndex(i);
    }
}
