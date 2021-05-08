package com.sabacc;

import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

public class Card {
    public enum Suit {
        Face,
        Sabers,
        Flasks,
        Coins,
        Staves
    }
    final public String name;
    final public int value;
    final public Suit suit;
    final public Drawable image;
    public Card(String name, Suit suit, int value, Drawable image) {
        this.name = name;
        this.suit = suit;
        this.value = value;
        this.image = image;
    }
}
