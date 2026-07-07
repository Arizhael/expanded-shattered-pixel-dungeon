package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Light;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfMindVision;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.IncubusSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

public class Incubus extends Mob {

    private boolean incubusInvisible = false;
    private boolean surpriseAttacking = false;


    private int turnsOutOfHeroFOV = 0;
    private int lastHeroPos = -1;

    private static final String INCUBUS_INVISIBLE = "incubus_invisible";
    private static final String TURNS_OUT_OF_HERO_FOV = "turns_out_of_hero_fov";
    private static final String LAST_HERO_POS = "last_hero_pos";

    {
        spriteClass = IncubusSprite.class;

        HP = HT = 80;
        defenseSkill = 25;

        viewDistance = Light.DISTANCE;

        EXP = 12;
        maxLvl = 25;

        lootChance = 1f;

        properties.add(Property.DEMONIC);

        HUNTING = new IncubusHunting();
        FLEEING = new IncubusFleeing();
    }

    @Override
    public int damageRoll() {
        return Random.NormalIntRange(25, 30);
    }

    @Override
    public int attackSkill(Char target) {
        if ((incubusInvisible || surpriseAttacking) && target != null) {
            return INFINITE_ACCURACY;
        } else {
            return 40;
        }
    }

    @Override
    public int drRoll() {
        return super.drRoll() + Random.NormalIntRange(0, 10);
    }

    @Override
    public float speed() {
        return super.speed() * (incubusInvisible ? 2f : 1f);
    }

    @Override
    protected boolean doAttack(Char enemy) {

        surpriseAttacking = incubusInvisible;

        /* Al atacar se revela visualmente, pero surpriseAttacking conserva
         la precisión infinita durante este ataque¨*/
        setIncubusInvisible(false);

        boolean actedImmediately = super.doAttack(enemy);

        // Si el ataque ocurrió sin animación, no habra onAttackComplete()
        if (actedImmediately) {
            surpriseAttacking = false;
            if (isAlive()) {
                startFleeingFrom(enemy);
            }
        }

        return actedImmediately;
    }

    @Override
    public void onAttackComplete() {
        Char attacked = enemy;

        super.onAttackComplete();

        surpriseAttacking = false;

        if (isAlive()) {
            startFleeingFrom(attacked);
        }
    }

    @Override
    public int attackProc(Char enemy, int damage) {
        damage = super.attackProc(enemy, damage);

        // Por ahora solo aplica lisiado, como me fue dicho
        Buff.prolong(enemy, Cripple.class, Cripple.DURATION);

        return damage;
    }

    @Override
    public void damage(int dmg, Object src) {
        super.damage(dmg, src);

        if (!isAlive()) {
            return;
        }

        surpriseAttacking = false;

        if (Dungeon.hero != null && fieldOfView != null && fieldOfView[Dungeon.hero.pos]) {
            startFleeingFrom(Dungeon.hero);
        } else if (state != FLEEING) {
            state = WANDERING;
            setIncubusInvisible(true);
        }
    }

    @Override
    protected boolean act() {

        boolean wasSleeping = state == SLEEPING;

        if (Dungeon.hero != null && lastHeroPos == -1) {
            lastHeroPos = Dungeon.hero.pos;
        }

        if (state == SLEEPING) {
            setIncubusInvisible(false);
        } else if (state == FLEEING) {
            setIncubusInvisible(false);
        } else if (!surpriseAttacking && state == WANDERING) {
            setIncubusInvisible(true);
        }

        if (incubusInvisible) {
            alerted = false;
        }

        boolean result = super.act();

        /* Si estaba dormido, despertó viendo al héroe y el juego base lo pasó a HUNTING,
         entonces lo mandamos a huir DESPUÉS de que el Sleeping original ya mostró la "!"*/
        if (wasSleeping && state == HUNTING && enemySeen && enemy != null && !incubusInvisible) {
            startFleeingFrom(enemy);
        }

        if (incubusInvisible) {
            alerted = false;
        }

        return result;
    }

    @Override
    public void notice() {
        // Si está invisible no debe mostrar el símbolo de alerta ("!")
        if (!incubusInvisible) {
            super.notice();
        }
    }

    @Override
    public void updateSpriteState() {
        super.updateSpriteState();
        updateIncubusSpriteState();
    }

    @Override
    public Item createLoot() {
        return new PotionOfMindVision();
    }

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(INCUBUS_INVISIBLE, incubusInvisible);
        bundle.put(TURNS_OUT_OF_HERO_FOV, turnsOutOfHeroFOV);
        bundle.put(LAST_HERO_POS, lastHeroPos);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        incubusInvisible = bundle.getBoolean(INCUBUS_INVISIBLE);
        turnsOutOfHeroFOV = bundle.getInt(TURNS_OUT_OF_HERO_FOV);
        lastHeroPos = bundle.getInt(LAST_HERO_POS);
        updateIncubusSpriteState();
    }

    public boolean isIncubusInvisible() {
        return incubusInvisible;
    }

    public void revealBySearch() {
        if (incubusInvisible) {
            surpriseAttacking = false;
            setIncubusInvisible(false);

            if (Dungeon.hero != null) {
                startFleeingFrom(Dungeon.hero);
            } else {
                state = FLEEING;
            }
        }
    }

    private void startFleeingFrom(Char enemy) {

        setIncubusInvisible(false);
        turnsOutOfHeroFOV = 0;

        if (enemy != null) {
            this.enemy = enemy;
            this.target = enemy.pos;
            this.lastHeroPos = enemy.pos;
        } else if (Dungeon.hero != null) {
            this.enemy = Dungeon.hero;
            this.target = Dungeon.hero.pos;
            this.lastHeroPos = Dungeon.hero.pos;
        }

        state = FLEEING;
    }

    private void setIncubusInvisible(boolean invisible) {
        incubusInvisible = invisible;
        updateIncubusSpriteState();
    }

    private void updateIncubusSpriteState() {
        if (sprite == null) {
            return;
        }

        if (incubusInvisible) {
            sprite.add(CharSprite.State.INVISIBLE);
        } else {
            sprite.remove(CharSprite.State.INVISIBLE);
        }
    }

    protected class IncubusHunting extends Hunting {

        @Override
        public boolean act(boolean enemyInFOV, boolean justAlerted) {

            /* Si el íncubo está visible y detecta al héroe,
             no debe atacar, debe intentar escapar primero.*/
            if (!incubusInvisible && enemyInFOV && enemy != null) {
                startFleeingFrom(enemy);
                return FLEEING.act(enemyInFOV, justAlerted);
            }

            return super.act(enemyInFOV, justAlerted);
        }
    }

    protected class IncubusFleeing extends Fleeing {

        @Override
        public boolean act(boolean enemyInFOV, boolean justAlerted) {

            setIncubusInvisible(false);

            if (Dungeon.hero != null) {

                if (enemyInFOV) {
                    lastHeroPos = Dungeon.hero.pos;
                }

                if (Dungeon.level.heroFOV[pos]) {
                    turnsOutOfHeroFOV = 0;
                } else {
                    turnsOutOfHeroFOV++;
                }

                if (turnsOutOfHeroFOV >= 5) {
                    turnsOutOfHeroFOV = 0;

                    setIncubusInvisible(true);

                    enemy = Dungeon.hero;
                    target = lastHeroPos != -1 ? lastHeroPos : Dungeon.hero.pos;

                    state = HUNTING;

                    spend(TICK);
                    return true;
                }
            }

            return super.act(enemyInFOV, justAlerted);
        }

        @Override
        protected void escaped() {
            // No se usa el escape aleatorio normal de Mob.Fleeing.
            // El íncubo debe esperar 5 turnos fuera del campo de visión del héroe.
        }

        @Override
        protected void nowhereToRun() {
            setIncubusInvisible(false);
            super.nowhereToRun();
        }
    }
}