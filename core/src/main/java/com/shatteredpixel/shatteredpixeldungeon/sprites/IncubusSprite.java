package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.watabou.noosa.TextureFilm;

public class IncubusSprite extends MobSprite {

    public IncubusSprite() {
        super();

        texture(Assets.Sprites.INCUBUS);

        TextureFilm frames = new TextureFilm(texture, 12, 15);

        idle = new Animation(8, true);
        idle.frames(frames, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 2, 2, 2, 1);

        run = new Animation(15, true);
        run.frames(frames, 3, 4, 5, 6, 7, 8);

        attack = new Animation(12, false);
        attack.frames(frames, 9, 10, 11);

        die = new Animation(10, false);
        die.frames(frames, 12);

        play(idle);
    }

}