package com.collidge;

/**
 * Created by Kris on 30-Jan-15.
 */

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Vector2;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class MapPlayer extends Sprite
{
    private Vector2 velocity = new Vector2(); //movement velocity
    private float speed = 60*2;
    private TiledMapTileLayer collisionlayer;
    public boolean withinOneOfNpc;
    private boolean [] freeDirections = new boolean[4];

    long startTime;
    long currentTime;

    int direction = UP;
    private static final int UP = 0;
    private static final int DOWN = 1;
    private static final int LEFT = 2;
    private static final int RIGHT = 3;

    ArrayList<NPC> npcList;
    //Java holds objects in memory as long as there is a reference to it. Therefore you can make local textureregions, textures and pass them to the animation object.
    private Animation walkingAnimation[];
    private Animation outlineAnimation[];

    /* Declan adding for movement in tiles */
    private float initX= 0;
    private float initY = 0;
    private float endX= 0;
    private float endY = 0;

    private int lastpoint;

    boolean stopped = false; // called on TouchUp is true
    boolean stopping = false;
    //boolean inTile = false; //Ensures is in Tile when stopped

    int tileID; //tile sprite ends up in

    public MapPlayer(Sprite sprite, TiledMapTileLayer collisionlayer, ArrayList<NPC> list)
    {
        super(sprite);
        // Normal Character
        //create 4 textures to fit into an array of 4
        Texture walkingTextures[] = new Texture[4];
        walkingTextures[UP] = new Texture("back_player.png");
        walkingTextures[DOWN] = new Texture("front_player.png");
        walkingTextures[RIGHT] = new Texture("right_player.png");
        walkingTextures[LEFT] = new Texture("left_player.png");

        //create 4 animations to hold 4 textureRegions each
        walkingAnimation = new Animation[4];
        //Create 16 TextureRegions because split returns a multidimensional array. But only use [0][i] part of dimensional array
        TextureRegion walkingRegions[][] = new TextureRegion[4][4];
        for(int j = 0; j < 4; j++) {
            //split textures into texture regions
            TextureRegion[][] region = TextureRegion.split(walkingTextures[j], 32, 32);
            for (int i = 0; i < 4; i++) {
                //put split regions into actual walking regions
                walkingRegions[j][i] = region[0][i];
            }
        }

        //Finally pass the texture regions to the 4 animation objects
        for (int i = 0; i < 4; i++) {
            walkingAnimation[i] = new Animation(walkingRegions[i],.2f);
        }

        //create 4 textures to fit into an array of 4
        Texture OutlineTextures[] = new Texture[4];
        OutlineTextures[UP] = new Texture("outline_back.png");
        OutlineTextures[DOWN] = new Texture("outline_front.png");
        OutlineTextures[RIGHT] = new Texture("outline_right.png");
        OutlineTextures[LEFT] = new Texture("outline_left.png");

        //create 4 animations to hold 4 textureRegions each
        outlineAnimation = new Animation[4];
        //Create 16 TextureRegions because split returns a multidimensional array. But only use [0][i] part of dimensional array
        TextureRegion outlineRegions[][] = new TextureRegion[4][4];
        for(int j = 0; j < 4; j++) {
            //split textures into texture regions
            TextureRegion[][] region1 = TextureRegion.split(OutlineTextures[j], 32, 32);
            for (int i = 0; i < 4; i++) {
                //put split regions into actual walking regions
                outlineRegions[j][i] = region1[0][i];
            }
        }

        //Finally pass the texture regions to the 4 animation objects
        for (int i = 0; i < 4; i++) {
            outlineAnimation[i] = new Animation(outlineRegions[i],.2f);
        }

        npcList = list;
        startTime = System.currentTimeMillis();
        currentTime = startTime;
        this.collisionlayer = collisionlayer;


    }

    public void draw(Batch spritebatch) {
        update(Gdx.graphics.getDeltaTime());

        if(velocity.x==0&&velocity.y==0)
        {
            walkingAnimation[direction].stop();
        }
        else
        {
            walkingAnimation[direction].play();
            walkingAnimation[direction].setDelay((collisionlayer.getTileWidth()/velocity.len())/2);
            walkingAnimation[direction].update(Gdx.graphics.getDeltaTime());
        }

        spritebatch.draw(walkingAnimation[direction].getFrame(), getX(), getY(),collisionlayer.getTileWidth(),collisionlayer.getTileHeight());
    }

    public void drawoutline(Batch spritebatch) {
        if(walkingAnimation[direction].paused)
        {
            outlineAnimation[direction].pause();
        }

        outlineAnimation[direction].setCurrentFrame(walkingAnimation[direction].getCurrentFrameNum());

        spritebatch.draw(outlineAnimation[direction].getFrame(), getX(), getY(),collisionlayer.getTileWidth(),collisionlayer.getTileHeight());

    }

    public void update(float delta)
    {
        float oldX = getX(), oldY = getY(), tilewidth = collisionlayer.getTileWidth(), tileheight = collisionlayer.getTileHeight();
        boolean collisionX = false, collisionY = false, fight = false;



        if(velocity.x < 0)
        {
            //left
            if(!collisionX)
                collisionX = collisionlayer.getCell((int) (getX() / tilewidth), (int) ((getY() + getHeight() / 2) / tileheight)).getTile().getProperties().containsKey("blocked");
        }

        else if(velocity.x > 0)
        {
            //right
            if(!collisionX)
                collisionX = collisionlayer.getCell((int) ((getX() + getWidth()) / tilewidth), (int) ((getY() + getHeight() / 2) / tileheight)).getTile().getProperties().containsKey("blocked");
        }

        //react to x collision
        if(collisionX||getX()>(collisionlayer.getWidth()-1)*collisionlayer.getTileWidth()||getX()<0)
        {
            setX(oldX);

            if(collisionX)
            {
                velocity.x=0;

            }

            if(direction == LEFT && getX() < 0)
            {
                velocity.x=0;
            }

            if(direction == RIGHT && getX()>(collisionlayer.getWidth()-1)*collisionlayer.getTileWidth())
            {
                velocity.x=0;
            }
        }

        if(velocity.y < 0)
        {
            //down
            if(!collisionY)
                collisionY = collisionlayer.getCell((int) ((getX() + getWidth() / 2) / tilewidth), (int) (getY() / tileheight)).getTile().getProperties().containsKey("blocked");
        }

        else if(velocity.y > 0)
        {
            //top middle
            if(!collisionY)
                collisionY = collisionlayer.getCell((int) ((getX() + getWidth() / 2) / tilewidth), (int) ((getY() + getHeight() / 2) / tileheight)).getTile().getProperties().containsKey("blocked");
        }

        //react to y collision
        if(collisionY || getY()>(collisionlayer.getHeight()-1)*collisionlayer.getTileHeight()||getY()<0)
        {
            setY(oldY);
            if(collisionY)
            {
                velocity.y = 0;
            }
            if(direction == DOWN && getY() < 0)
            {
                velocity.y = 0;
            }

            if(direction == UP && getY()>(collisionlayer.getHeight()-1)*collisionlayer.getTileHeight())
            {
                velocity.y = 0;
            }
        }

        if(stopped == true && (Math.abs(velocity.x) > 0 || Math.abs(velocity.y) > 0) && stopping == false) {
            stopping = true;
            if ( direction == RIGHT) {
                tileID = (int) (getX() / tilewidth);
                tileID = tileID + 1; //Sets tile to aim towards
            }
            if (direction == LEFT) {
                tileID = (int) (getX() / tilewidth);
                //tileID = tileID; //Sets tile to aim towards
            }
            if (direction == UP) {
                tileID = (int) (getY() / tileheight);
                tileID = tileID + 1; //Sets tile to aim towards
            }
            if (direction == DOWN) {
                tileID = (int) (getY() / tileheight);
            }
        }

        //Once stopping sequence initiated
        if(stopping) {
            if (direction == RIGHT) {
                lastpoint = (int) getX();
                if (lastpoint >= Math.abs(tileID * tilewidth)) {
                    stopMovement();
                    stopping = false;
                    //Wait until past this point
                }
            }
            if (direction == LEFT) {
                lastpoint = (int) getX();
                if (lastpoint <= Math.abs(tileID * tilewidth)) {
                    stopMovement();
                    stopping = false;
                    //Wait until past this point
                }
            }
            if (direction == UP) {
                lastpoint = (int) getY();
                if (lastpoint >= Math.abs(tileID * tileheight)) {
                    stopMovement();
                    stopping = false;
                    //Wait until past this point
                }
            }
            if (direction == DOWN) {
                lastpoint = (int) getY();
                if (lastpoint <= Math.abs(tileID * tileheight)) {
                    stopMovement();
                    stopping = false;
                    //Wait until past this point
                }

            }

        }
        else stopped = false;

        withinOneOfNpc = false;
        for (int i = 0; i < 4; i++) {
            freeDirections[i] = true;
        }
        for (int i = 0; i < npcList.size(); i++) {
            NPC npc = npcList.get(i);
            int npcTileX = (int) ((npc.getX() + getWidth() / 2) / tilewidth);
            int npcTileY = (int)((npc.getY() + getHeight() / 2)/ tileheight);
            int playerTileX = (int) ((getX() + getWidth() / 2)/ tilewidth);
            int playerTileY = (int) ((getY() + getHeight() / 2)/ tileheight);
            int differenceX = npcTileX - playerTileX;
            int differenceY = npcTileY - playerTileY;

            if(differenceY == 0 && Math.abs(differenceX) == 1) {
                float distance = Math.abs(npc.getX() - getX());
                if (differenceX == 1 && distance <= 32) {
                    freeDirections[RIGHT] = false;
                }
                if (differenceX == -1  && distance <= 32) {
                    freeDirections[LEFT] = false;
                }
                withinOneOfNpc = true;
            }
            if (differenceX == 0 && Math.abs(differenceY) == 1) {
                float distance = Math.abs(npc.getY() - getY());
                if (differenceY == 1  && distance <= 32) {
                    freeDirections[UP] = false;
                }
                if (differenceY == -1  && distance <= 32) {
                    freeDirections[DOWN] = false;
                }
                withinOneOfNpc = true;
            }
        }

        if(!freeDirections[direction]) {
            if (direction == LEFT)
            {
                velocity.x = 0;
                velocity.y = 0;
            }
            else if (direction == RIGHT)
            {
                velocity.x = 0;
                velocity.y = 0;
            }
            else if (direction == DOWN)
            {
                velocity.x = 0;
                velocity.y = 0;
            }
            else
            {
                velocity.x = 0;
                velocity.y = 0;
            }
        }

        //System.out.println("Up = " + freeDirections[UP] + "  Down = " + freeDirections[DOWN] + " Right = " + freeDirections[RIGHT] + " Left = " + freeDirections[LEFT]);

        setX(getX() + velocity.x*delta);
        setY(getY() + velocity.y*delta);
    }

    public TiledMapTileLayer getCollisionLayer()
    {
        return collisionlayer;
    }

    private void moveUp()
    {
        if(freeDirections[UP]) {
            velocity.y = speed;
            velocity.x = 0;
        }
    }

    private void moveDown()
    {
        if(freeDirections[DOWN]) {
            velocity.y = -speed;
            velocity.x = 0;
        }
    }

    private void moveLeft()
    {
        if(freeDirections[LEFT]) {
            velocity.x = -speed;
            velocity.y = 0;
        }
    }

    private void moveRight()
    {
        if(freeDirections[RIGHT]) {
            velocity.x = speed;
            velocity.y = 0;
        }
    }

    private void stopMovement()
    {
        velocity.y = 0;
        velocity.x = 0;
    }

    public void touchDown(int screenX, int screenY, int width, int height)
    {
        float xForCalculation = ((screenX-(width/2))/(float)width);
        float yForCalculation = ((-(screenY-(height/2)))/(float)height);
        getDirection(xForCalculation, yForCalculation);

        withinOneOfNpc = false;
        for (int i = 0; i < 4; i++) {
            freeDirections[i] = true;
        }
        for (int i = 0; i < npcList.size(); i++) {
            NPC npc = npcList.get(i);
            int npcTileX = (int) ((npc.getX() +getWidth() / 2) / collisionlayer.getTileWidth());
            int npcTileY = (int)((npc.getY() + getHeight() / 2)/ collisionlayer.getTileHeight());
            int playerTileX = (int) ((getX() + getWidth() / 2)/ collisionlayer.getTileWidth());
            int playerTileY = (int) ((getY() + getHeight() / 2)/ collisionlayer.getTileHeight());
            int differenceX = npcTileX - playerTileX;
            int differenceY = npcTileY - playerTileY;

            if(differenceY == 0 && Math.abs(differenceX) == 1) {
                if (differenceX == 1) {
                    freeDirections[RIGHT] = false;
                }
                if (differenceX == -1) {
                    freeDirections[LEFT] = false;
                }
                withinOneOfNpc = true;
            }
            if (differenceX == 0 && Math.abs(differenceY) == 1) {
                if (differenceY == 1) {
                    freeDirections[UP] = false;
                }
                if (differenceY == -1) {
                    freeDirections[DOWN] = false;
                }
                withinOneOfNpc = true;
            }
        }

        /**
         * Kris
         * Added the outermost if statement to stop the character moving if you
         * click on either the inventory or menu buttons
         */
        if(!(screenY < height/5 && (screenX > width * 10/12)))
        {
            if (direction == LEFT)
            {
                moveLeft();
            }
            else if (direction == RIGHT)
            {
                moveRight();
            }
            else if (direction == DOWN)
            {
                moveDown();
            }
            else
            {
                moveUp();
            }
        }
        return;
    }

    public boolean touchUp(int screenX, int screenY, int width, int height)
    {
        // TODO Auto-generated method stub
        stopped = true;
        return false;
    }

    public boolean touchDragged(int screenX, int screenY, int width, int height)
    {
        return true;
    }

    private void getDirection(float x, float y)
    {
        if(Math.abs(y)/Math.abs(x)>1)
        {
            if(y>0)
            {
                direction=UP;
            }
            else
            {
                direction=DOWN;
            }
        }
        else
        {
            if(x>0)
            {
                direction=RIGHT;
            }
            else
            {
                direction=LEFT;
            }
        }
    }
}