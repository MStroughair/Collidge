package com.collidge;
//import android.view.MotionEvent;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.Timer;
/*
* Created by Daniel on 20/01/2015.
*/
public class Fight extends GameState
{

    public Music fightMusic;
    Music punch=Gdx.audio.newMusic(Gdx.files.internal("strongpunch.mp3"));


    private double PlayerDam;
    Player playr;
    boolean waitingForTouch=false;
    private double[] damage;
    private int ActionType;
    private int ActionId;
    private boolean comboing;
    private boolean targeting=false;
    private boolean targetingInterfaceToggle = gsm.getTouchToggle();
    private boolean damagingItemUsed=false;
    private int expEarned;
    private int monsterCode=-1;
    private boolean defend;
    private FightMenu fMenu;
    private int enemyCount,enemiesLeft;
    private Enemy[] enemies;
    Animation playerIdle;
    Attack move;
    private int damage_taken;
    PopUpText damageNums=new PopUpText();
    SpriteBatch batch;
    Texture texture ;
    Sprite healthBar, healthBackground, EnergyIcon, portrait;
    Sprite tutorialPopUp;
    Boolean tutorialActive = true;
    Boolean tutorial2Active = false;
    Boolean firstTime = true;
    Sprite menuContainer;
    Sprite selector;
    Sprite player;
    Combo combo;
    private Boolean attackingAnim=false;
    private BitmapFont battleFont;
    private TargetPicker targetPicker;
    private Sprite background;
    private Sprite targetArrow,targetReticule,backArrow;
    private int animCount;
    private int[] enemyX,enemyY;
    Timer.Task damager=new Timer.Task()
    {
        @Override
        public void run()
        {
//if damage was dealt to the player, subtract health
            if (damage[0] > 1)
            {
                damage[0]--;
                playr.changeHealth(-1);
                damage_taken++;
//check if the player died, if he did, end the fight
                if (playr.getCurrentHealth() <= 0)
                {
                    expEarned=0;
                    DeathState();
                }
            }
//changes enemy health based on damage done, if enemies die, give their experience to the player and kill them
            for(int i=1;i<damage.length;i++)
            {
                if ((!enemies[i-1].getDead())&&damage[i] >=1)
                {
                    damage[i]--;
                    enemies[i-1].changeHealth(-1);
                    if (enemies[i-1].getHealth() <= 0)
                    {
                        damage[i]=0;
                        expEarned+=enemies[i-1].getExpValue();
                        enemiesLeft--;
                        if(enemiesLeft<=0)
                        {
                            endFight();
                        }
                    }
                }
            }
        }
    };
    //allows the overall player class to be changed within the fight, so that e.g. it can gain experience
    Fight(GameStateManager gsm,Player player)
    {
        super(gsm);
        playr=player;
        EnemySets BasicSet=new EnemySets();
        enemies=BasicSet.getEnemies("Pack"); //Uses the "Pack" EnemyCollection from the EnemySets class. Pack contains up to 7 Freshers.
    }
    Fight(GameStateManager gsm,Player player,String Enemy)
    {
        super(gsm);
        playr=player;
        EnemySets BasicSet=new EnemySets();
        enemies=BasicSet.getEnemies(Enemy);
        punch.setVolume((float)gsm.getVolume()/4);
    }
    @Override
    public void initialize()
    {
        fightMusic = Gdx.audio.newMusic(Gdx.files.internal("mymusic2.mp3"));
        fightMusic.play();
        fightMusic.setVolume((float)gsm.musicLevel/4);

// testAnim=new Animation("walkingRight.png",10);
        combo=new Combo();
        expEarned=0;
//gets the number and type of enemies to fight
        enemyCount=enemies.length;
        enemiesLeft=enemyCount;
        damage=new double[enemies.length+1]; // damage[0] is player damage taken, damage[1] is for the first enemy, etc.
        enemyX=new int[enemiesLeft];
        enemyY=new int[enemiesLeft];
        for(int i=0;i<enemyX.length;i++)
        {
            enemyX[i]=((int) (screenWidth / 2 + (i * (screenWidth / (double) (3 * enemyCount)))));
        }
        for(int i=0;i<enemyY.length;i++)
        {
            enemyY[i]=screenHeight / 10 + (int) ((((enemyCount) - (i + 1)) / (double) (enemyCount)) * (screenHeight / 2));
        }
        move=new Attack(); //calls the attack class
//enemies=new Enemy[enemyCount];
//allows the player to select a particular enemy to attack
        targetPicker=new TargetPicker(enemies,0);
//sprite_enemy = new Sprite[enemies.length];
//addTextures(sprite_enemy, enemies);
//player and enemy healthbars and attack/tactics panel
        batch = new SpriteBatch();
        texture = new Texture("barHorizontal_green_mid.png");
        healthBar = new Sprite(texture);
        texture = new Texture("barHorizontal_red_mid.png");
        healthBackground = new Sprite(texture);
        texture = new Texture("tooltipBackground.png");
        tutorialPopUp = new Sprite(texture);
        texture = new Texture("EnemySelect.png");
        selector = new Sprite(texture);
        texture = new Texture("blue_circle.png");
        EnergyIcon = new Sprite(texture);
        texture = new Texture("portrait.png");
        portrait = new Sprite(texture);
        texture =new Texture ("background2.png");
        background=new Sprite(texture);
        background.setPosition(0,0);
        background.setSize(Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
        portrait.setPosition(screenWidth / 30 - 3*(screenWidth / 50), 25 * screenHeight / 30);
        portrait.setSize(37*screenWidth/80, 4*screenHeight / 20);
        healthBar.setPosition(screenWidth / 30 + (screenWidth / 50), 25 * screenHeight / 30);
        healthBar.setSize((4 * (screenWidth / 10)), screenHeight / 10);
        healthBackground.setPosition(screenWidth / 30 + (screenWidth / 50), 10 * screenHeight / 30);
        healthBackground.setSize((int) ((playr.getHealth() * (4 * (screenWidth / 10))) / ((double) playr.getHealth())), (int) (screenHeight / 20.0));
        texture = new Texture("panelInset_beige.png");
        menuContainer = new Sprite(texture);
        texture=new Texture("walking_right_animation.png");
        TextureRegion[][] region = TextureRegion.split(texture,32,32);
        texture=new Texture("arrow_up_blue.png");
        targetArrow=new Sprite(texture);
        targetArrow.setSize(screenWidth/12f,screenWidth/12f);
        targetArrow.setOriginCenter();
        texture=new Texture("targetReticule.png");
        targetReticule=new Sprite(texture);
        targetReticule.setSize(targetArrow.getWidth(),targetArrow.getHeight());
        targetReticule.setOriginCenter();
        texture= new Texture("backArrow.png");
        backArrow=new Sprite(texture);
        backArrow.setSize(targetReticule.getWidth(), targetReticule.getHeight());
        backArrow.setOriginCenter();
//calls fightMenu class
        fMenu=new FightMenu(playr, gsm.getMenuToggle(),gsm.demoMode);
        waitingForTouch=true;
        battleFont = new BitmapFont();
        Timer.instance().start();
    }
    public void update()
    {
//(int)(((double)(4*(screenWidth/10)))*((double)playr.getCurrentEnergy()/playr.getHealth()))
        damageNums.update();
        playr.idleAnim.update(Gdx.graphics.getDeltaTime());
        if(combo.comboing)
        {
            comboing=true;
            combo.update();
        }
        else if(comboing)
        {
            if(monsterCode==-1)
            {
                comboing=false;
            }
            else if(monsterCode==-2)
            {

                if(attackingAnim)
                {
                    if (playr.attackAnim[ActionId - 1].getTimesPlayed() > animCount)
                    {
                        animCount = -1;
                        comboing = false;
                        playr.attackAnim[ActionId - 1].stop();
                        attackingAnim = false;
                        playerTurnPart3();
                    }
                    else
                    {
                        playr.attackAnim[ActionId - 1].update(Gdx.graphics.getDeltaTime());

                    }
                }
            }
            else if(monsterCode<enemies.length)
            {
                if(defend)
                {
                    defend=false;
                    defendTurn(playr, enemies, monsterCode);
                }
                else if(enemies[monsterCode].attackAnimation.getTimesPlayed()>animCount||enemies[monsterCode].getDead())
                {
                    enemyTurnPart2();
                    enemies[monsterCode].attackAnimation.stop();
                    animCount=-1;
                }
                else
                {
                    enemies[monsterCode].attackAnimation.update(Gdx.graphics.getDeltaTime());
                }
            }
        }
        Timer.instance().clear();
        Timer.instance().start();
        Timer.instance().postTask(damager);
    }
    @Override
    public void draw()
    {
        batch.begin();

        background.draw(batch);

        portrait.setPosition(0,screenHeight-((int)(portrait.getHeight()*.9)+battleFont.getLineHeight()));
//draws green health bar and red background. Background size is based on max health and doesn't change- at full hp the bar appears fully green.
        healthBar.setPosition(screenWidth / 30 + (screenWidth / 50), portrait.getY()+(int)(9*portrait.getHeight()/20.0));
        healthBackground.setPosition(healthBar.getX(), healthBar.getY());
        healthBackground.setSize((int)( 35*portrait.getWidth()/40.0),healthBar.getHeight());
        healthBackground.draw(batch);
        healthBackground.setOriginCenter();
        healthBar.setSize((int)((playr.getCurrentHealth()*((int)( 35*portrait.getWidth()/40.0)))/((double)playr.getHealth())),(int)(18*portrait.getHeight()/50.0));
        healthBar.draw(batch);
        battleFont.draw(batch,"MR MAN",healthBackground.getOriginX(),healthBackground.getY()+(healthBackground.getHeight()+battleFont.getLineHeight()));
        battleFont.draw(batch, playr.getCurrentHealth() + "/"+playr.getHealth() ,healthBackground.getOriginX(),(healthBackground.getY()+battleFont.getLineHeight()));
        if(!attackingAnim)
        {
            batch.draw(playr.idleAnim.getFrame(), screenWidth / 5, screenHeight / 20, screenWidth / 8, screenHeight / 5);
        }
        else
        {
            batch.draw(playr.attackAnim[ActionId-1].getFrame(), screenWidth / 5, screenHeight / 20, screenWidth / 8, screenHeight / 5);
        }
        EnergyIcon.setSize((screenHeight / 20f),(screenHeight / 20f)); //Code Allowing for generation of Energy Icons
        healthBackground.setColor(Color.BLUE);
        healthBackground.setPosition(portrait.getX()+(portrait.getWidth()/10), (int)((healthBar.getY())-(healthBar.getHeight()*.9)));
        healthBackground.setSize((int)(((71*portrait.getWidth()/80.0))*(playr.getCurrentEnergy()/(double)playr.getEnergy())),(int)(healthBackground.getHeight()*.9));
        healthBackground.draw(batch);
        healthBackground.setColor(Color.WHITE);
        battleFont.setColor(Color.WHITE);
        battleFont.draw(batch, playr.getCurrentEnergy()+"" ,healthBackground.getX()+(healthBackground.getWidth()/2),healthBackground.getY()+healthBackground.getHeight());
        portrait.draw(batch);
//Sets colour and size of battle font, draws "HP" for player health
        battleFont.setColor(Color.BLACK);
        battleFont.setScale(screenWidth/400);
// Enemy drawing loop
        for(int i=0;i<enemies.length;i++)
        {
            if(!enemies[i].getDead())
            {
                int target=0;
                if((targeting&&targetPicker.getCurrentTarget()==i)||monsterCode==i)
                {
//target=Gdx.graphics.getWidth()/10;
                    healthBackground.setPosition((3f * screenWidth /5f), screenHeight - (battleFont.getLineHeight()*3));
                    healthBackground.setSize(2*screenWidth/6f, battleFont.getLineHeight());
                    healthBar.setPosition(healthBackground.getX(),healthBackground.getY());
                    healthBar.setSize((int)(healthBackground.getWidth()*((double)enemies[i].getHealth()/(double)enemies[i].getMaxHealth())),healthBackground.getHeight());
                    healthBackground.setSize(healthBackground.getWidth()-(int)(healthBackground.getWidth()*.05),healthBar.getHeight());
                    healthBackground.draw(batch);
                    healthBar.draw(batch);
                    battleFont.setColor(Color.WHITE);
                    battleFont.draw(batch,enemies[i].getName(),healthBackground.getX(),healthBackground.getY()+battleFont.getLineHeight()*2);
                    battleFont.setColor(Color.BLACK);
                    battleFont.draw(batch, enemies[i].getHealth() + "", healthBackground.getX(), healthBackground.getY()+battleFont.getLineHeight());
                    selector.setPosition(enemyX[i] - enemies[i].width/8, enemyY[i] - enemies[i].height/8);
                    selector.setSize(5 * enemies[i].width/4, 5 * enemies[i].height/4);
                    selector.draw(batch);
                }
                else
                {
                    target=0;
                }
/*int enemyCountTemp;
int iTemp;
if (i < 5) { //2 rows of enemies, 5 in each
//batch.draw(enemies[i].animation.getFrame(), ((int) (screenWidth / 2 + (i * (screenWidth / (double) (3 * enemyCount))))) - target, screenHeight / 10 + (int) ((((enemyCount) - (i + 1)) / (double) (enemyCount)) * (screenHeight / 2)), enemies[i].width, enemies[i].height);
if (enemyCount > 5){
enemyCountTemp = 5;
}
else {
enemyCountTemp = enemyCount;
}
batch.draw(enemies[i].animation.getFrame(),
((int) (screenWidth / 2 + (i * (screenWidth / (double) (3 * enemyCountTemp))))) - target,
screenHeight / 18 + (int) ((((enemyCountTemp) - (i + 1)) / (double) (enemyCountTemp)) * (screenHeight / 1.8)),
enemies[i].width,
enemies[i].height);
}
else if (i < 9 && i >= 5) {
//batch.draw(enemies[i].animation.getFrame(), ((int) (screenWidth / 2 + enemies[i].width + (i * (screenWidth / (double) (3 * enemyCount))))) - target, screenHeight / 10 + (int) ((((enemyCount) - (i + 1 - 5)) / (double) (enemyCount)) * (screenHeight / 2)), enemies[i].width, enemies[i].height);
enemyCountTemp = enemyCount - 5;
iTemp = i-5;
batch.draw(enemies[i].animation.getFrame(),
((int) (screenWidth / 2 + 1.5*enemies[i].width + (iTemp * (screenWidth / (double) (3 * enemyCountTemp))))) - target,
screenHeight / 28 + (int) ((((enemyCountTemp) - (iTemp + 1)) / (double) (enemyCountTemp)) * (screenHeight / 2)),
enemies[i].width,
enemies[i].height);
}
else {
enemyCountTemp = enemyCount - 9;
iTemp = i-9;
}*/

/*sprite_enemy[i].setSize(screenWidth/12f, screenWidth/12f);
sprite_enemy[i].setPosition(screenWidth/2f, screenHeight/12f);
sprite_enemy[i].draw(batch);*/
                if(targeting&&(targetPicker.getCurrentTarget()+targetPicker.getTargetingId()>=i&&targetPicker.getCurrentTarget()-targetPicker.getTargetingId()<=i))
                {
/*battleFont.setColor(Color.RED);
battleFont.draw(batch, "Tap to choose a target!", screenWidth/20, screenHeight/2);
battleFont.setColor(Color.BLACK);
/*if (i < 5) {
if (enemyCount > 5){
enemyCountTemp = 5;
}
else {
enemyCountTemp = enemyCount;
}
selector.setPosition(((int) (screenWidth / 2 + (i * (screenWidth / (double) (3 * enemyCountTemp))))) - target,
screenHeight / 18 + (int) ((((enemyCountTemp) - (i + 1)) / (double) (enemyCountTemp)) * (screenHeight / 1.8)));
}
else if (i < 9 && i >= 5) {
enemyCountTemp = enemyCount - 5;
iTemp = i-5;
selector.setPosition(
((int) (screenWidth / 2 + 1.5*enemies[i].width + (iTemp * (screenWidth / (double) (3 * enemyCountTemp))))) - target,
screenHeight / 28 + (int) ((((enemyCountTemp) - (iTemp + 1)) / (double) (enemyCountTemp)) * (screenHeight / 2)));
}
else {
enemyCountTemp = enemyCount - 9;
iTemp = i-9;
selector.setPosition(((int) (screenWidth / 2 + 2.5*enemies[i].width + (iTemp * (screenWidth / (double) (3 * enemyCountTemp))))) - target,
2*screenHeight / 5 + (int) ((((enemyCountTemp) - (iTemp + 1)) / (double) (enemyCountTemp)) * (screenHeight / 2)));
}
selector.setSize(enemies[i].width,enemies[i].height);
if (targetPicker.targetHighlighted == true) { //don't draw the selector box unless a target has been tapped
battleFont.setColor(Color.RED);
battleFont.draw(batch, "Tap again to confirm!", screenWidth/20, screenHeight/2 - battleFont.getLineHeight());
battleFont.setColor(Color.BLACK);
selector.draw(batch);
}*/
                }
                if(monsterCode==i)
                {
                    batch.draw(enemies[i].attackAnimation.getFrame(), enemyX[i], enemyY[i], enemies[i].width, enemies[i].height);
                }
                else
                {
                    batch.draw(enemies[i].animation.getFrame(), ((int) (screenWidth / 2 + (i * (screenWidth / (double) (3 * enemyCount))))) - target, screenHeight / 10 + (int) ((((enemyCount) - (i + 1)) / (double) (enemyCount)) * (screenHeight / 2)), enemies[i].width, enemies[i].height);
                    enemies[i].animation.update(Gdx.graphics.getDeltaTime());
                }
/*else
{
enemies[i].animation.pause();
}*/
            }
        }
        if(targeting) //draws old targeting interface
        {
            targetArrow.setRotation(90);
            targetArrow.setPosition(screenWidth / 10, screenHeight / 2);
            targetReticule.setPosition(targetArrow.getX() + targetArrow.getWidth(), targetArrow.getY());
            if(!targetingInterfaceToggle) {
                targetArrow.draw(batch);
                targetReticule.draw(batch);
                targetArrow.setRotation(-90);
                targetArrow.setPosition(targetReticule.getX() + targetReticule.getWidth(), targetReticule.getY());
                targetArrow.draw(batch);
                backArrow.setPosition(targetReticule.getX(), targetReticule.getY() - backArrow.getHeight());
                backArrow.draw(batch);
            }
            backArrow.setPosition(targetReticule.getX(), targetReticule.getY() - backArrow.getHeight());
            backArrow.draw(batch);
        }
        if(damageNums.popUps.size()>0)
        {
            damageNums.draw(batch);
        }
        if(!fMenu.actionSelected)
        {
            fMenu.draw(batch,screenWidth,screenHeight);
        }

        if (tutorialActive == true && playr.getLevel() <= 1) {
            if (fMenu.menuStyle2) {
                tutorialPopUp.setPosition(screenWidth / 8, screenHeight / 8);
                tutorialPopUp.setSize(screenWidth / 2f, screenHeight / 3f);
                tutorialPopUp.draw(batch);
                battleFont.drawWrapped(batch, "Tap a menu icon to select an action. Swipe upwards on an icon to display the tooltip of an item or attack.", tutorialPopUp.getX(), tutorialPopUp.getY() + tutorialPopUp.getHeight(), tutorialPopUp.getWidth(), BitmapFont.HAlignment.CENTER);
            }
            else {
                tutorialPopUp.setPosition(screenWidth / 8, screenHeight / 12);
                tutorialPopUp.setSize(screenWidth / 2f, 4*screenHeight / 9f);
                tutorialPopUp.draw(batch);
                battleFont.drawWrapped(batch, "Swipe left or right to navigate through the menu. Tap the icon in the centre to select that action. Swipe upwards to display the tooltip of an item or attack.", tutorialPopUp.getX(), tutorialPopUp.getY()  + tutorialPopUp.getHeight(), tutorialPopUp.getWidth(), BitmapFont.HAlignment.CENTER);
            }
            waitingForTouch = true;
        }

        if (tutorial2Active == true) {
            tutorialPopUp.setPosition(screenWidth / 8, screenHeight / 30);
            tutorialPopUp.setSize(screenWidth / 2f, 2*screenHeight / 3f);
            tutorialPopUp.draw(batch);
            battleFont.drawWrapped(batch,
                    "Tap an enemy to target them. When you use an attack you will have to perform a combo and the damage of your attack will depend on how well you do. After you attack, it's the enemies' turn and you'll have to defend right away!",
                    tutorialPopUp.getX(), tutorialPopUp.getY() + tutorialPopUp.getHeight(), tutorialPopUp.getWidth(), BitmapFont.HAlignment.CENTER);
            waitingForTouch = true;
        }

        if(combo.comboing) //if in combo phase
        {
            combo.draw(batch);
        }
        batch.end();
    }
    //----------------------------------------------------------------------------------
// these are just input methods that must be implemented
//----------------------------------------------------------------------------------
    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button)
    {
        return false;
    }
    @Override
    public boolean touchDown(float x, float y, int pointer, int button)
    {
        return false;
    }
    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button)
    {
        return false;
    }
    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer)
    {
        return false;
    }
    @Override
    public boolean longPress(float x, float y)
    {
        return false;
    }
    @Override
    public boolean fling(float velocityX, float velocityY, int button)
    {
        return false;
    }
    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY)
    {
        if (waitingForTouch&&!fMenu.actionSelected)
        {
            fMenu.pan(x, y, deltaX, deltaY);
        }
        else if (combo.comboing)
        {
            combo.pan(x, y, deltaX, deltaY);
            return true;
        }
        return false;
    }
    @Override
    public boolean panStop(float x, float y, int pointer, int button) {
        if (waitingForTouch) {
            if (!fMenu.actionSelected) {
                fMenu.panStop(x, y); //when you swipe, display the tooltip
            } else if (combo.comboing) {
                combo.panStop(x, y, pointer, button);
                return true;
            }
        }
        return false;
    }
    @Override
    public boolean zoom(float initialDistance, float distance)
    {
        return false;
    }
    @Override
    public boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2)
    {
        return false;
    }
    //-------------------------------------------------------------------------------------------------------------------------
//-------------------------------------------------------------------------------------------------------------------------
    @Override
    public boolean tap(float x, float y, int count, int button) {
//if selecting an action from the fight menu. Actions have an ID and a type.
        if (waitingForTouch) {
            if (tutorialActive) {
                tutorialActive = false;
            } else if (!fMenu.actionSelected) {
//tap the menu to select an action
                fMenu.tap(x, y);
                if (fMenu.actionSelected) {
                    ActionId = fMenu.getActionId();
                    ActionType = fMenu.getActionType();
                    playerTurn(playr, enemies);
                }
            }
//targeting an enemy after selecting an action
            else if (targeting) {
                tutorial2Active = false;
/*for (int i = 0; i < enemies.length; i++)
{
// if (x> screenWidth/2) { //arbitrary x values at the moment, vaguely at the right side of the screen
// if (y < 9 * screenHeight / 10 - (int) (((enemyCount - (i + 1)) / (double) (enemyCount)) * (screenHeight / 2))
// && y > 9 * screenHeight / 10 - (int) (((enemyCount - (i + 1)) / (double) (enemyCount)) * (screenHeight / 2)) - enemies[0].height) {
*/
                if (y < targetReticule.getY() && y > targetReticule.getY() - targetReticule.getHeight()) {
                    if (x < targetReticule.getX() && x > targetReticule.getX() - targetReticule.getWidth()) {
                        targetPicker.Left();
                    } else if (x > targetReticule.getX() + targetReticule.getWidth() && x < targetReticule.getX() + (targetReticule.getWidth() * 2)) {
                        targetPicker.Right();
                    } else if (x > targetReticule.getX() && x < targetReticule.getX() + targetReticule.getWidth()) {
                        targetPicker.Select();
                    }
                    if (targetPicker.targetSelected) //move on to the next part of combat after a target is selected
                    {
                        targeting = false;
                        playerTurnPart2();
                        return true;
                    }
                } else if (x > targetReticule.getX() && x < targetReticule.getX() + targetReticule.getWidth() && y > targetReticule.getY() && y < targetReticule.getY() + targetReticule.getHeight()) {
                    targeting = false;
                    waitingForTouch = true;
                    fMenu.actionSelected = false;
                }
                if (x > enemyX[0] && x < enemyX[enemyX.length - 1] + enemies[enemyX.length - 1].width) {
                    for (int i = 0; i < enemyX.length; i++) {
                        if (x > enemyX[i] && x < enemyX[i] + enemies[i].width) {
                            if (!enemies[i].getDead()) {
                                if (screenHeight - y > enemyY[i] && screenHeight - y < enemyY[i] + enemies[i].height) {
                                    targetPicker.goTo(i);
                                }
                            }
                        }
                    }
                    if (targetPicker.targetSelected) //move on to the next part of combat after a target is selected
                    {
                        targeting = false;
                        playerTurnPart2();
                        return true;
                    }
                }
//}
            } else if (combo.comboing) //if in combo phase, accept combo input
            {
                combo.tap((int) x, (int) y);
            }
        }

            return false;
    }

    private void playerTurn(Player player,Enemy[] monsters)
    {
        PlayerDam=0;

        monsterCode=-2;
        if(ActionType==2) //flee
        {
            if(fMenu.getMoveString(ActionType,ActionId).equals("Recharge"))
            {
                playr.changeEnergy((int)(playr.getIntelligence()*2));
                playerTurnEnd();
            }
            else if(fMenu.getMoveString(ActionType,ActionId).equals("Flee"))
            {
                expEarned=0;
                endFight();
                return;
            }
            else if(fMenu.getMoveString(ActionType,ActionId).equals("DIE"))
            {
                damage[0]=9999999;
                damageNums.Add("999999999",.1f,.4f);
                return;
            }
        }
        if(ActionType==3) //use an item
        {
            player.useItem(fMenu.getMoveString(ActionType,ActionId));
            if (player.itemDamage != 0){
                PlayerDam = player.itemDamage;
                damagingItemUsed = true;
                targetPicker.reset(enemies, 100);

                targeting = true;
                return;
            }
        }
        if(ActionType==1) //attack
        {
            if (firstTime && playr.getLevel() < 2) {
                tutorial2Active = true;
                firstTime = false;
            }
            targetPicker.reset(enemies, player.attackRange(fMenu.getMoveString(ActionType, ActionId)));
            targeting = true;
            return;
        }
        playerTurnEnd();
    }
    private void playerTurnPart2() //Initiating combo
    {
        if (!damagingItemUsed)
        {
            combo.initiateCombo(ActionId - 1, this);
            comboing = true;
            animCount=playr.attackAnim[ActionId - 1].getTimesPlayed();
            playr.attackAnim[ActionId-1].setCurrentFrame(0);
            playr.attackAnim[ActionId - 1].play();
            attackingAnim=true;
        }
        else
        {
            playerTurnPart3();
        }
    }
    private void playerTurnPart3() //After the combo, applying the multipliers, dealing damage
    {


        //if a damaging item was used PlayerDam is already set
        if (damagingItemUsed) {
            for (int i = -targetPicker.getTargetingId(); i <= targetPicker.getTargetingId(); i++) {
                if (targetPicker.getSelectedTarget() + i >= 0 && targetPicker.getSelectedTarget() + i < enemies.length)
                {


                    damage[targetPicker.getSelectedTarget() + 1 + i] += PlayerDam;
                    if (!enemies[targetPicker.getSelectedTarget() + i].getDead())
                    {
                        damageNums.Add
                                (
                                        String.valueOf(-(int) PlayerDam),
                                        (float) (enemyX[targetPicker.getSelectedTarget() + i] + (enemies[targetPicker.getSelectedTarget() + i].width / 2)) / screenWidth,
                                        ((float) (enemyY[targetPicker.getSelectedTarget() + i] + enemies[targetPicker.getSelectedTarget() + i].height) / screenHeight)
                                );


                    }
                }
            }

            damagingItemUsed = false;
        }



        else {
            //playr.changeEnergy(-(playr.getAttackEnergyCosts(fMenu.getMoveString(ActionType, ActionId))));
            playr.changeEnergy(-(playr.getEnergyCost(fMenu.getMoveString(ActionType, ActionId))));
            for (int i = -targetPicker.getTargetingId(); i <= targetPicker.getTargetingId(); i++)
            {

                if (targetPicker.getSelectedTarget() + i >= 0 && targetPicker.getSelectedTarget() + i < enemies.length)
                {
                    //PlayerDam = playr.attackPicker(fMenu.getMoveString(ActionType, ActionId));
                    PlayerDam = playr.getAttackMultiplier(fMenu.getMoveString(ActionType, ActionId));

                    if (enemies[targetPicker.getSelectedTarget() + i].getDefence() > 0)
                    {
                        PlayerDam *= (playr.getAttack() / enemies[targetPicker.getSelectedTarget() + i].getDefence());
                    } else
                    {
                        PlayerDam *= playr.getAttack();
                    }
                    PlayerDam *= Math.abs(combo.skill);
                    if (PlayerDam < 1)
                    {
                        PlayerDam = 1;
                    }
                    if (combo.skill > .9)
                    {
                        PlayerDam++;
                    }

                    damage[targetPicker.getSelectedTarget() + 1 + i] += PlayerDam;
                    if (!enemies[targetPicker.getSelectedTarget() + i].getDead())
                    {
                        damageNums.Add
                                (
                                        String.valueOf(-(int) PlayerDam),
                                        (float) (enemyX[targetPicker.getSelectedTarget() + i] + (enemies[targetPicker.getSelectedTarget() + i].width / 2)) / screenWidth,
                                        ((float) (enemyY[targetPicker.getSelectedTarget() + i] + enemies[targetPicker.getSelectedTarget() + i].height) / screenHeight)
                                );

                        punch.play();

                    }
                }
            }
        }
        playerTurnEnd();

    }
    //at the end of the player's turn, if there are enemies left, start the enemy's turn, otherwise if all are dead end the fight
    private void playerTurnEnd()
    {
        if(enemiesLeft>0&&playr.getCurrentHealth()>0)
        {
            monsterCode=-1;
            enemyTurn(playr, enemies,0);
        }
        else if (playr.getCurrentHealth()<=0)
        {
            DeathState();
        }
        else
        {
            endFight();
        }

        damageNums.Add("DEFEND!",.2f,.5f,0f,.01f,Color.BLUE,10,10.0);

    }
    private void enemyTurn(Player player,Enemy[] monsters,int monsterId)
    {
        monsterCode=monsterId;
        monsters[monsterId].attackAnimation.setCurrentFrame(0);
        animCount=monsters[monsterId].attackAnimation.getTimesPlayed();
        monsters[monsterId].attackAnimation.play();
        defend=false;
        if(monsterId<=monsters.length-1&&!monsters[monsterId].getDead())
        {
            comboing = true;
        }
        else
        {
            boolean fight=false;
            while(monsterId<monsters.length)
            {
                if(!monsters[monsterId].getDead())
                {
                    monsterCode=monsterId;
                    fight=true;
                    monsterId=monsters.length;
                }
                monsterId++;
            }
            if(fight)
            {
                enemyTurn(player, monsters, monsterCode);
            }
            else
            {
                waitingForTouch=true;
                fMenu.actionSelected=false;
            }
        }
    }
    private void enemyTurnPart2()
    {
        if(!enemies[monsterCode].getDead())
        {
            combo.initiateCombo(enemies[monsterCode].attackType, this);
            defend = true;
        }
        else
        {
            defend=true;
        }
    }
    private void defendTurn(Player player,Enemy[] monsters,int monsterId)
    {
        double dam=-1;
        if(monsterId>=monsters.length||monsterId<0)
        {
        }
        else if(!monsters[monsterId].getDead()&&player.getCurrentHealth()>0)
        {
            if(player.getDefence()>0)
            {
                dam=(monsters[monsterId].getAttack() / player.getDefence());
            }
            else
            {
                dam+=monsters[monsterId].getAttack();
            }
            dam*=(1-(combo.skill*combo.skill));
            if (dam <= 1)
            {
                if(combo.skill<.9)
                {
                    if(!gsm.demoMode)
                    {
                        damage[0]++;

                    }
                    damageNums.Add(String.valueOf(1), .15f, .3f);
                    punch.play();
                }
                else
                {
                    damageNums.Add("Blocked!",.15f,.3f,Color.WHITE,150,4.0);
                }
            }
            else if(combo.skill<.95)
            {
                if(!gsm.demoMode)
                {
                    damage[0] += dam;

                }
                damageNums.Add(String.valueOf((int)dam),.15f,.3f);
                punch.play();

                if (player.getCurrentHealth() <= 0)
                {
                    enemiesLeft = -1;
                    monsterCode=-1;
                }
            }
            else
            {
                damageNums.Add("Blocked!",.15f,.3f,Color.WHITE,150,4.0);
            }
        }
        monsterCode++;
        if(monsterCode>=monsters.length)
        {
            monsterCode=-1;
        }
        if(enemiesLeft>0&&monsterCode==-1)
        {
            waitingForTouch=true;
            fMenu.actionSelected=false;
            playr.changeEnergy(playr.getIntelligence());
            fMenu.refreshMenus(playr);

        }
        else
        {
            enemyTurn(player,monsters,monsterCode);
        }
        defend=false;
    }
    private void endFight()
    {
        damage[0]=0;
        int[]ratings = combo.num_ratings;
        combo.delete();
        fightMusic.dispose();
        Timer.instance().clear();
        Timer.instance().stop();
        batch.dispose();
        texture.dispose();
        battleFont.dispose();
        if (expEarned == 0)
            gsm.endFight();
        else
            gsm.StartWinState(playr,expEarned, enemies, damage_taken, ratings);
    }
    private void DeathState() //this function is called to check when the fight is over, and then display a splash screen
    {
        damage[0]=0;
        combo.delete();
        fightMusic.dispose();
        batch.dispose();
        texture.dispose();
        battleFont.dispose();
        Timer.instance().clear();
        Timer.instance().stop();
        gsm.StartDeathState(playr);
    }


}