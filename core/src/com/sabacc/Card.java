package com.sabacc;

import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

public class Card {
    public enum Suite {
        Face,
        Sabers,
        Flasks,
        Coins,
        Staves
    }
    final public String name;
    final public int value;
    final public Suite suite;
    final public Drawable image;
    public Card(String name, Suite suite, int value, Drawable image) {
        this.name = name;
        this.suite = suite;
        this.value = value;
        this.image = image;
    }
}
