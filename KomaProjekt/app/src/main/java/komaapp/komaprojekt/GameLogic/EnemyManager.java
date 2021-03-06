package komaapp.komaprojekt.GameLogic;

import android.util.Log;

import org.andengine.entity.scene.Scene;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Random;

import komaapp.komaprojekt.Game;

/**
 * Created by benjamin on 14-11-23.
 *
 * Handler class for all enemies
 */
public class EnemyManager {
    private float internalTimer;

    // Enemy texture library
    Hashtable<String, ITextureRegion> enemyTextures;

    // Random number generator
    Random randGen = new Random();

    // Reference to the active scene, for attaching enemies to the scene
    private Scene scene;
    private VertexBufferObjectManager VBOmanager;
    private ShotManager shotManager;

    // List of currently active enemies
    private boolean shouldSpawnEnemies;
    private ArrayList<BaseEnemy> enemies;

    // Timer stuffsies
    private float spawnInterval;
    private int enemyCounter;

    //Hard-coded shoot frequency
    public static final float shootInterval = 0.75f;

    //BOSS SHIT
    private boolean bossBattleHasStarted = false;
    public boolean getBossBattleHasStarted() { return bossBattleHasStarted; }
    private float bossTimer = 25f;
    private Boss boss;
    public Boss getBoss() { return boss; }
    public static final float bossShootInterval = 0.2f;

    public EnemyManager(Scene scene, VertexBufferObjectManager VBOmanager, ShotManager shotManager)
    {
        this.scene = scene;
        this.VBOmanager = VBOmanager;
        this.shotManager = shotManager;
        enemies = new ArrayList<BaseEnemy>(10);

        enemyTextures = new Hashtable<String, ITextureRegion>();

        spawnInterval = 2.5f;
        enemyCounter = 0;

        shouldSpawnEnemies = false;

        internalTimer = 0.0f;
    }

    public void update(float currentTime, float dt)
    {
        // CHECK IF BOSS BATTLE SHOULD COMMENCE
        if (internalTimer >= bossTimer && !bossBattleHasStarted)
        {
            bossTimer = 0;

            shouldSpawnEnemies = false;
            boss = new Boss(0, 0, enemyTextures.get("bossTex"), VBOmanager, shotManager, enemyCounter, 10f);

            scene.attachChild(boss);

            Log.d("BossLog", "Boss spawned!");

            bossBattleHasStarted = true;

            HealthBar bossHealthBar = new HealthBar(boss.getCenterX(), boss.getCenterY()+boss.getHeight()/2+10f, boss.getWidth(), 15f, VBOmanager, boss.getHealth());
            boss.setHealthBar(bossHealthBar);
        }

        if(bossBattleHasStarted)
        {
            if(boss.getHealth() <= 0){
                Log.d("BossLog", "Boss killed!");
                bossTimer = internalTimer + 25f;
                bossBattleHasStarted = false;
                shouldSpawnEnemies = true;
            }
            Log.d("BossLog", "Boss health: " + boss.getHealth());
        }

        if (bossBattleHasStarted) boss.update(dt);

        ArrayList<BaseEnemy> enemiesToRemove = new ArrayList<BaseEnemy>();

        //Update each enemy
        for (BaseEnemy enemy : enemies)
        {
            //Update the enemy
            enemy.update(dt);

            //Check if out of screen
            if (enemy.getY() - enemy.getHeight()/2 >= Game.CAMERA_HEIGHT)
            {
                enemiesToRemove.add(enemy);
                // dispose the memory of enemy
                enemy.destroy();

            }
        }
        enemies.removeAll(enemiesToRemove); // Remove all enemies to far down from the arrayList

        if (shouldSpawnEnemies)
        {
            internalTimer += dt;

            //Try to spawn enemy
            if (internalTimer >= spawnInterval*enemyCounter)
            {
                float spawnX = randGen.nextFloat() * Game.CAMERA_WIDTH;
                float spawnY = -150f;
                float speed = 150 + (randGen.nextFloat()-0.5f)*100;

                // Usually this can be a field rather than a method variable
                Random rand = new Random();

                // nextInt is normally exclusive of the top value,
                // so add 1 to make it inclusive
                int enemyToSpawn = rand.nextInt((2 - 1) + 1) + 1;

                if(enemyToSpawn == 1) {
                    addEnemy("Enemy1", spawnX, spawnY, speed);
                }
                else{
                    addEnemy("Enemy2", spawnX, spawnY, speed);
                }

                enemyCounter++;
            }
        }
    }

    // texName is the key in the HashTable enemyTextures for the wanted ITextureRegion
    public void addEnemy(String texName, float spawnX, float spawnY, float speed)
    {
        BaseEnemy newEnemy;

        if (enemyCounter%4 == 0) {
            newEnemy = new WavingEnemy(spawnX, spawnY, enemyTextures.get(texName), VBOmanager, shotManager, enemyCounter, (randGen.nextFloat()+0.5f)*speed, 0.02f, 100.0f);
        }
        else newEnemy = new SimpleEnemy(spawnX, spawnY, enemyTextures.get(texName), VBOmanager, shotManager, enemyCounter, (randGen.nextFloat()+0.5f)*speed);

        this.enemies.add(newEnemy);
        this.scene.attachChild(newEnemy);

        // Log.d("EnemyLog", "Enemy added: textureName=" + texName + " spawnX=" + spawnX + " spawnY=" + spawnY + " ID=" + newEnemy.getID() );
    }

    public void removeEnemies(ArrayList<BaseEnemy> enemiesToRemove)
    {
        // Throw explosions
        for (BaseEnemy enemy : enemiesToRemove)
        {
            float explosionX = enemy.getCenterX() - Explosion.getTextureWidth()/2;
            float explosionY = enemy.getCenterY() - Explosion.getTextureHeight()/2;

            final AnimatedSprite explosionAnimation = new AnimatedSprite(explosionX, explosionY, Explosion.getExplosionTex(), VBOmanager);
            scene.attachChild(explosionAnimation);
            explosionAnimation.setScaleCenter(explosionAnimation.getWidth()/2, explosionAnimation.getHeight()/2);
            explosionAnimation.setScale((randGen.nextFloat()+2.3f)*1.5f);
            explosionAnimation.animate(1000/60, false, new AnimatedSprite.IAnimationListener() {
                @Override
                public void onAnimationStarted(AnimatedSprite animatedSprite, int i){}

                @Override
                public void onAnimationFrameChanged(AnimatedSprite animatedSprite, int i, int i2) {}

                @Override
                public void onAnimationLoopFinished(AnimatedSprite animatedSprite, int i, int i2) {}

                @Override
                public void onAnimationFinished(AnimatedSprite animatedSprite) {
                    explosionAnimation.setVisible(false);

                }
            });
        }
        this.enemies.removeAll(enemiesToRemove);
    }

    // Set the rate of enemy spawns
    public void setSpawnInterval(float dt)
    {
        this.spawnInterval = dt;
    }

    // Method to add a texture to the "library" of enemy textures
    public void addEnemyTexture(ITextureRegion tex, String textureName)
    {
        enemyTextures.put(textureName, tex);
    }

    public final ArrayList<BaseEnemy> getEnemies()
    {
        return enemies;
    }

    public void setShouldSpawnEnemies(boolean shouldSpawnEnemies) { this.shouldSpawnEnemies = shouldSpawnEnemies; }
}
