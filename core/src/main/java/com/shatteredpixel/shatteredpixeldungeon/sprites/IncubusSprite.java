package com.shatteredpixel.shatteredpixeldungeon.sprites;

public class IncubusSprite extends SuccubusSprite {

    private static final int INCUBUS_TINT = 0xFF5555;

    public IncubusSprite() {
        super();
        hardlight(INCUBUS_TINT);
    }

    @Override
    public void resetColor() {
        super.resetColor();
        hardlight(INCUBUS_TINT);
    }
}