package com.sabacc.gamestage;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Null;
import com.sabacc.Player;

/**
 * A button that displays some basic player stats, needs to have update() called whenever the player
 * values change
 * As this is only for myself, its pretty stripped down. Following TextButton as a pseudo-template
 * @author alexs2112
 */
public class PlayerButton extends Button {
    private TextButtonStyle style;
    final private Player player;
    public Player player() { return player; }
    final private Label nameLabel;    // Left aligned name
    final private Label statLabel;    // Right aligned stats, credits, cards in hand, hand value


    public PlayerButton(Player player, TextButtonStyle style) {
        super();
        setStyle(style);
        this.player = player;
        this.style = style;

        // Initialize the labels
        nameLabel = new Label(" " + player.name(), new Label.LabelStyle(style.font, style.fontColor));
        nameLabel.setAlignment(Align.left);
        add(nameLabel).expand().fill();
        statLabel = new Label("C:" + player.credits() + " H:" + player.numCards() + " S:?? ", new Label.LabelStyle(style.font, style.fontColor));
        statLabel.setAlignment(Align.right);
        add(statLabel).expand().fill();
    }

    public void setStyle (ButtonStyle style) {
        if (style == null) throw new NullPointerException("style cannot be null");
        if (!(style instanceof TextButtonStyle)) throw new IllegalArgumentException("style must be a PlayerButtonStyle.");
        this.style = (TextButtonStyle)style;
        super.setStyle(style);

        if (nameLabel != null) {
            TextButtonStyle playerButtonStyle = (TextButtonStyle)style;
            Label.LabelStyle labelStyle = nameLabel.getStyle();
            labelStyle.font = playerButtonStyle.font;
            labelStyle.fontColor = playerButtonStyle.fontColor;
            nameLabel.setStyle(labelStyle);
        }
        if (statLabel != null) {
            TextButtonStyle playerButtonStyle = (TextButtonStyle)style;
            Label.LabelStyle labelStyle = statLabel.getStyle();
            labelStyle.font = playerButtonStyle.font;
            labelStyle.fontColor = playerButtonStyle.fontColor;
            statLabel.setStyle(labelStyle);
        }
    }

    public TextButtonStyle getStyle () {
        return style;
    }

    /**
     * The main important method, this should be called whenever values change, this updates the player text if needed
     */
    public void update() {
        String s = "C:" + player.credits();
        if (player.folded)
            s += " Folded! ";
        else {
            s += " H:" + player.numCards();
            if (player.displayHand)
                s += " S:" + player.score() + " ";
            else {
                if (player.numField() > 0)
                    s += " S:?" + player.fieldValue() + " ";
                else
                    s += " S:?? ";
            }
        }
        statLabel.setText(s);   // This checks if the new string is different before setting it
    }

    /** Copied from TextButton */
    public void draw (Batch batch, float parentAlpha) {
        nameLabel.getStyle().fontColor = getFontColor();
        statLabel.getStyle().fontColor = getFontColor();
        super.draw(batch, parentAlpha);
    }

    /** Returns the appropriate label font color from the style based on the current button state. */
    protected @Null Color getFontColor () {
        if (isDisabled() && style.disabledFontColor != null) return style.disabledFontColor;
        if (isPressed()) {
            if (isChecked() && style.checkedDownFontColor != null) return style.checkedDownFontColor;
            if (style.downFontColor != null) return style.downFontColor;
        }
        if (isOver()) {
            if (isChecked()) {
                if (style.checkedOverFontColor != null) return style.checkedOverFontColor;
            } else {
                if (style.overFontColor != null) return style.overFontColor;
            }
        }
        boolean focused = hasKeyboardFocus();
        if (isChecked()) {
            if (focused && style.checkedFocusedFontColor != null) return style.checkedFocusedFontColor;
            if (style.checkedFontColor != null) return style.checkedFontColor;
            if (isOver() && style.overFontColor != null) return style.overFontColor;
        }
        if (focused && style.focusedFontColor != null) return style.focusedFontColor;
        return style.fontColor;
    }

}