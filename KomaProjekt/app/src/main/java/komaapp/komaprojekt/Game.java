package komaapp.komaprojekt;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.util.Log;

import com.badlogic.gdx.math.Vector2;

import org.andengine.audio.music.Music;
import org.andengine.audio.music.MusicFactory;
import org.andengine.audio.sound.Sound;
import org.andengine.audio.sound.SoundFactory;
import org.andengine.engine.LimitedFPSEngine;
import org.andengine.engine.camera.Camera;
import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.FillResolutionPolicy;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.IOnSceneTouchListener;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.text.Text;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.font.Font;
import org.andengine.opengl.font.FontFactory;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.ITiledTextureRegion;
import org.andengine.ui.activity.SimpleBaseGameActivity;
import org.andengine.util.color.Color;

import java.io.IOException;

import komaapp.komaprojekt.GameLogic.Collision.CollisionManager;
import komaapp.komaprojekt.GameLogic.EnemyManager;
import komaapp.komaprojekt.GameLogic.Explosion;
import komaapp.komaprojekt.GameLogic.HealthBar;
import komaapp.komaprojekt.GameLogic.MovingBackground;
import komaapp.komaprojekt.GameLogic.Player;
import komaapp.komaprojekt.GameLogic.ShotManager;

public class Game extends SimpleBaseGameActivity implements IOnSceneTouchListener {

    //PRE-GAME LOADING SCREEN
    private BitmapTextureAtlas splashTextureAtlas;
    private ITextureRegion splashTextureRegion;
    private Sprite splash;

    private Camera camera;
    public static final int CAMERA_WIDTH = 1200;
    public static final int CAMERA_HEIGHT = 1824; //1920 - 96 (nav. bar height)

    private Sprite pauseButton, resumeButton, restartButton, quitButton, gameOverText;

    private Rectangle preStartRect;

    public static AnimatedSprite shootBtn;
    private Font mFont;
    private Font cashFont;

    private ITiledTextureRegion rocketBtnTex;

    //TEXTURES
    private BitmapTextureAtlas texAtlas;

    private ITextureRegion player_tex, pause_tex;
    private ITextureRegion resume_tex, restart_tex, quit_tex, game_over_tex;

    private HealthBar playerHealth;

    // BACKGROUNDS
    private MovingBackground bg1, bg2;
    private MovingBackground fg_level1_sun;
    private MovingBackground fg_level2_1, fg_level2_2, fg_level2_3;

    private Player player;
    private ShotManager playerShotManager;

    public static Context ctx;

    //ENEMIES
    private EnemyManager enemyManager;
    private ShotManager enemyShotManager;

    //DATABASE
    public static Database database = new Database();
    private int engineLvl;
    private int gunsLvl;
    private int shieldLvl;

    //SOUNDS
    public static Sound player_explosion, player_damage, player_laser, player_missile;
    public static Sound enemy_explosion, enemy_laser;
    public static Sound boss_explosion;
    public static Music gameMusic;

    //EXPLOSIONS
    private ITiledTextureRegion explosionTex;
    private boolean hasPlayedDeathExplosion = false;

    private ITextureRegion loadITextureRegion(String filename, int width, int height)
    {
        //GRAPHICS
        BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");
        texAtlas = new BitmapTextureAtlas(getTextureManager(), width, height, TextureOptions.DEFAULT);
        ITextureRegion tex = BitmapTextureAtlasTextureRegionFactory.createFromAsset(texAtlas, this, filename, 0, 0);

        texAtlas.load();

        return tex;
    }

    private void loadBackgrounds()
    {
        //Randomize starting Y offset : (backY1 lies in range [0, 1920]
        float backX = 0, spawnY_1 = (float)Math.random()*CAMERA_HEIGHT ,spawnY_2 = spawnY_1-CAMERA_HEIGHT, spawnY_3 = spawnY_2-CAMERA_HEIGHT;

        //Randomize order of planets
        int randSeed = (int)Math.ceil(Math.random()*6);

        float fg1_pos = spawnY_1, fg2_pos = spawnY_2, fg3_pos = spawnY_3;

        switch (randSeed)
        {
            case 1:
                fg1_pos = spawnY_1;
                fg2_pos = spawnY_2;
                fg3_pos = spawnY_3;
                break;
            case 2:
                fg2_pos = spawnY_1;
                fg3_pos = spawnY_2;
                fg1_pos = spawnY_3;
                break;
            case 3:
                fg3_pos = spawnY_1;
                fg1_pos = spawnY_2;
                fg2_pos = spawnY_3;
                break;
            case 4:
                fg1_pos = spawnY_1;
                fg3_pos = spawnY_2;
                fg2_pos = spawnY_3;
                break;
            case 5:
                fg2_pos = spawnY_1;
                fg1_pos = spawnY_2;
                fg3_pos = spawnY_3;
                break;
            case 6:
                fg3_pos = spawnY_1;
                fg2_pos = spawnY_2;
                fg1_pos = spawnY_3;
                break;
        }

        Log.d("DaseLog", "spawnY_1 = " + spawnY_1 + ", spawnY_2 = " + spawnY_2 + ", spawnY_3 = " + spawnY_3);
        Log.d("DaseLog", "fg1_pos = " + fg1_pos + ", fg2_pos = " + fg2_pos + ", fg3_pos = " + fg3_pos);

        //Far background
        bg1 = new MovingBackground(backX, spawnY_1, loadITextureRegion("bg/space_bg1.jpg", 1200, 1920), getVertexBufferObjectManager());
        bg2 = new MovingBackground(backX, spawnY_2, loadITextureRegion("bg/space_bg2.jpg", 1200, 1920), getVertexBufferObjectManager());

        //Middle background
        fg_level1_sun = new MovingBackground(backX, fg1_pos, loadITextureRegion("bg/fg_level1_sun.png", 1200, 1920), getVertexBufferObjectManager());

        //Foreground
        fg_level2_1 = new MovingBackground(backX, fg1_pos, loadITextureRegion("bg/fg_level2_1.png", 1200, 1920), getVertexBufferObjectManager());
        fg_level2_2 = new MovingBackground(backX, fg2_pos, loadITextureRegion("bg/fg_level2_2.png", 1200, 1920), getVertexBufferObjectManager());
        fg_level2_3 = new MovingBackground(backX, fg3_pos, loadITextureRegion("bg/fg_level2_3.png", 1200, 1920), getVertexBufferObjectManager());
    }

    @Override
    public EngineOptions onCreateEngineOptions()
    {
        /*final DisplayMetrics displayMetrics = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        CAMERA_HEIGHT= displayMetrics.heightPixels;
        CAMERA_WIDTH= displayMetrics.widthPixels;*/

        camera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
        EngineOptions engineOptions = new EngineOptions(true, ScreenOrientation.PORTRAIT_FIXED, new FillResolutionPolicy(), camera);
        engineOptions.getTouchOptions().setNeedsMultiTouch(true);

        //ENABLE SOUND ENGINE
        engineOptions.getAudioOptions().setNeedsSound(true);
        engineOptions.getAudioOptions().setNeedsMusic(true);
        engineOptions.getAudioOptions().getSoundOptions().setMaxSimultaneousStreams(5000);

        LimitedFPSEngine engine = new LimitedFPSEngine(engineOptions, 60);
        return engine.getEngineOptions();
    }

    private void loadGameResources()
    {
        BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");

        //FONTS
        this.mFont = FontFactory.create(this.getFontManager(), this.getTextureManager(), 256, 256, TextureOptions.BILINEAR, Typeface.create(Typeface.DEFAULT, Typeface.BOLD), 48);
        this.mFont.load();

        this.cashFont = FontFactory.create(this.getFontManager(), this.getTextureManager(), 256, 256, TextureOptions.BILINEAR, Typeface.create(Typeface.DEFAULT, Typeface.BOLD), 48, Color.WHITE.hashCode());
        this.cashFont.load();

        resume_tex = loadITextureRegion("btn_resume.png",632, 306 );
        restart_tex = loadITextureRegion("btn_restart.png",632, 306 );
        quit_tex = loadITextureRegion("btn_quit.png",632, 306 );
        pause_tex = loadITextureRegion("btn_pause.png", 146, 146);

        game_over_tex = loadITextureRegion("text_game_over.png", 879, 102);
        player_tex = loadITextureRegion("player_ship.png", 200, 200);

        // EXPLOSIONS

        BitmapTextureAtlas explosionTexAtlas = new BitmapTextureAtlas(this.getTextureManager(), 1024, 1024, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
        explosionTex = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(explosionTexAtlas, this.getAssets(), "kp_explosion_sheet1.png", 0, 0, 8, 8);
        explosionTexAtlas.load();

        Explosion.setExplosionTex(explosionTex);

        BitmapTextureAtlas rocketBtnTexAtlas = new BitmapTextureAtlas(getTextureManager(), 2048, 2048, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
        rocketBtnTex = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(rocketBtnTexAtlas, getAssets(), "btnRocketTiledTexture16x16.png", 0, 0, 16, 16);
        rocketBtnTexAtlas.load();

        loadBackgrounds();
    }

    private void loadSounds()
    {
        try
        {
            ////PLAYER
            player_explosion = SoundFactory.createSoundFromAsset(mEngine.getSoundManager(), this, "mfx/player_explosion.ogg");
            player_explosion.setVolume((float)database.getSoundVolume()/100);

            player_damage = SoundFactory.createSoundFromAsset(mEngine.getSoundManager(), this, "mfx/player_damage.ogg");
            player_damage.setVolume((float)database.getSoundVolume()/100);

            player_laser = SoundFactory.createSoundFromAsset(mEngine.getSoundManager(), this, "mfx/player_laser.ogg");
            player_laser.setVolume((float)database.getSoundVolume()/100);

            player_missile = SoundFactory.createSoundFromAsset(mEngine.getSoundManager(), this, "mfx/player_missile.ogg");
            player_missile.setVolume((float)database.getSoundVolume()/100);

            ////ENEMY
            enemy_explosion = SoundFactory.createSoundFromAsset(mEngine.getSoundManager(), this, "mfx/enemy_explosion.ogg");
            enemy_explosion.setVolume((float)database.getSoundVolume()/100);

            enemy_laser = SoundFactory.createSoundFromAsset(mEngine.getSoundManager(), this, "mfx/enemy_laser.ogg");
            enemy_laser.setVolume((float)database.getSoundVolume()/100);

            ////BOSS
            boss_explosion = SoundFactory.createSoundFromAsset(mEngine.getSoundManager(), this, "mfx/boss_explosion.ogg");
            boss_explosion.setVolume((float)database.getSoundVolume()/100);

            ////MUSIC
            gameMusic = MusicFactory.createMusicFromAsset(mEngine.getMusicManager(), this, "mfx/game_music.ogg");
            gameMusic.setVolume((float)database.getMusicVolume()/100);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void onCreateResources()
    {
        BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");
        splashTextureAtlas = new BitmapTextureAtlas(this.getTextureManager(), 480, 60, TextureOptions.DEFAULT);
        splashTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(splashTextureAtlas, this,"splash.png", 0, 0);
        splashTextureAtlas.load();
        loadSounds();
    }

    private Scene createGameScene()
    {
        //Create pause background rectangle
        final Rectangle pauseRectangle = new Rectangle (0, 0 , CAMERA_WIDTH, CAMERA_HEIGHT, this.getVertexBufferObjectManager());
        pauseRectangle.setColor(org.andengine.util.color.Color.BLACK);
        pauseRectangle.setAlpha(0.8f);

        //Create the scene
        final Scene scene = new Scene();
        scene.setOnSceneTouchListener(this);
        scene.setVisible(false); // NO-SHOW until everything loaded

        ctx = getBaseContext();

        try {
            database.readFile(ctx);
        } catch (IOException e) {
            e.printStackTrace();
        }

        gunsLvl = database.getLvl("guns");
        engineLvl = database.getLvl("engine");
        shieldLvl = database.getLvl("shield");

        //BACKGROUNDS
        bg1.setBackgroundSpeed(15f);
        bg2.setBackgroundSpeed(15f);
        scene.attachChild(bg1);
        scene.attachChild(bg2);

        fg_level1_sun.setBackgroundSpeed(22f);
        scene.attachChild(fg_level1_sun);

        fg_level2_1.setBackgroundSpeed(30f); fg_level2_1.setLayersInLevel(3);
        fg_level2_2.setBackgroundSpeed(30f); fg_level2_2.setLayersInLevel(3);
        fg_level2_3.setBackgroundSpeed(30f); fg_level2_3.setLayersInLevel(3);
        scene.attachChild(fg_level2_1);
        scene.attachChild(fg_level2_2);
        scene.attachChild(fg_level2_3);


        //ENEMY MANAGEMENT
        this.enemyShotManager = new ShotManager( scene, this.getVertexBufferObjectManager() , loadITextureRegion("laser_enemy.png",20,65), loadITextureRegion("missile_sprite.png", 36, 128));
        this.enemyManager = new EnemyManager( scene, this.getVertexBufferObjectManager(), this.enemyShotManager);
        enemyManager.addEnemyTexture(loadITextureRegion("Enemy/Enemy1.png", 200, 200), "Enemy1");
        enemyManager.addEnemyTexture(loadITextureRegion("Enemy/Enemy2.png", 200, 200), "Enemy2");
        enemyManager.addEnemyTexture(loadITextureRegion("boss_sprite_large.png", 320, 454), "bossTex");


        //SHOT MANAGEMENT
        this.playerShotManager = new ShotManager( scene, this.getVertexBufferObjectManager(), loadITextureRegion("laser_player.png",20,65), loadITextureRegion("missile_sprite.png", 36, 128));

        //Instantiate the player object
        player = new Player(camera.getWidth()/2, 1000, player_tex, this.getVertexBufferObjectManager(), playerShotManager, gunsLvl, engineLvl, shieldLvl)
        {
            @Override
            public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float X, float Y)
            {
                if (pSceneTouchEvent.isActionDown())
                {
                    Log.d("TextLog", "Player sprite touched.");
                }
                return true;
            }
        };
        scene.attachChild(player);

        //HEALTH BAR
        float healthBarWidth = 200f;

        playerHealth = new HealthBar( (player.getWidth()-healthBarWidth)/2, -player.getHeight()*0.8f, healthBarWidth, 40f, getVertexBufferObjectManager(), (float) Player.getHealth());
        player.setHealthBar(playerHealth);

        //CASH TEXT
        final Text cashText = new Text(10, 10, cashFont, "Cash", 10, this.getVertexBufferObjectManager());
        cashText.setPosition(CAMERA_WIDTH - 200, 10);
        cashText.setColor(0.435f, 0.8f, 0.867f); //CYAN BLUE

        final Text cashVal = new Text( 10, 10, cashFont, "" + database.getCash(), 100, this.getVertexBufferObjectManager());
        cashVal.setPosition(CAMERA_WIDTH  - 200, 70);
        cashVal.setColor(0.435f, 0.8f, 0.867f); //CYAN BLUE

        scene.attachChild(cashText);
        scene.attachChild(cashVal);

        //MISSILE BUTTON
        shootBtn = new AnimatedSprite(24, CAMERA_HEIGHT-216, rocketBtnTex, getVertexBufferObjectManager())
        {
            public boolean onAreaTouched(TouchEvent touchEvent, float X, float Y)
            {
                if (touchEvent.isActionDown() && shootBtn.getCurrentTileIndex() == shootBtn.getTileCount()-1)
                {
                    //SHOOT
                    Log.d("ShotLog", "Player tried to shoot!");
                    player.shootMissile();
                    player_missile.play();
                }
                return true;
            }
        };
        shootBtn.stopAnimation(shootBtn.getTileCount()-1);
        shootBtn.setZIndex(-1);
        shootBtn.setScaleCenter(0, 0);
        shootBtn.setScale(1.5f);

        scene.attachChild(shootBtn);

        scene.registerUpdateHandler(new IUpdateHandler() {
            float currentTime = 0;

            @Override
            public void onUpdate(float v) {
                double playerShieldBeforeHit = Player.shield;
                currentTime += v;

                player.update(v);
                playerShotManager.update(v);

                bg1.updatePosition(v);
                bg2.updatePosition(v);
                fg_level1_sun.updatePosition(v);
                fg_level2_1.updatePosition(v);
                fg_level2_2.updatePosition(v);
                fg_level2_3.updatePosition(v);

                enemyManager.update(currentTime, v);
                enemyShotManager.update(v);

                cashVal.setText("" + database.getCash());

                /*** COLLISION DETECTION ***/

                if (enemyManager.getBossBattleHasStarted())
                {
                    CollisionManager.collidePlayerWithBoss(player, enemyManager);
                    CollisionManager.collideBossWithShots(playerShotManager, enemyManager);
                }
                // 1. Check for collisions between player and enemies
                CollisionManager.collidePlayerWithEnemies(player, enemyManager);

                // 2. Check for collision between player and enemy shots
                CollisionManager.collidePlayerWithShots(player, enemyShotManager);

                // 3. Check for collision between enemies and player shots
                CollisionManager.collideEnemiesWithShots(enemyManager, playerShotManager);

                playerHealth.update(v);
                if (enemyManager.getBossBattleHasStarted()) enemyManager.getBoss().getHealthBar().update(v);

                //DO SOMETHING IF PLAYER DIES
                // TODO Make this not run several times
                if (Player.shield <= 0 && !hasPlayedDeathExplosion) {
                    hasPlayedDeathExplosion = true;

                    try {
                        database.writeFile(ctx);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    scene.detachChild(pauseButton);
                    scene.unregisterTouchArea(pauseButton);

                    scene.detachChild(shootBtn);
                    scene.unregisterTouchArea(shootBtn);

                    float explosionCenterX = player.getCenterX();
                    float explosionCenterY = player.getCenterY();

                    player.detachSelf();
                    player.dispose();

                    final AnimatedSprite explAnim = new AnimatedSprite(explosionCenterX, explosionCenterY, Explosion.getExplosionTex(), getVertexBufferObjectManager());
                    explAnim.setScaleCenter(explAnim.getScaleCenterX()+explAnim.getWidthScaled()/2, explAnim.getScaleCenterY()+explAnim.getHeightScaled()/2);
                    explAnim.setScale(10f);
                    scene.attachChild(explAnim);
                    explAnim.setPosition(explAnim.getX()+500, explAnim.getY()+200);
                    explAnim.animate(1000 / 60, false, new AnimatedSprite.IAnimationListener() {
                        @Override
                        public void onAnimationStarted(AnimatedSprite animatedSprite, int i) {
                        }

                        @Override
                        public void onAnimationFrameChanged(AnimatedSprite animatedSprite, int i, int i2) {
                        }

                        @Override
                        public void onAnimationLoopFinished(AnimatedSprite animatedSprite, int i, int i2) {
                        }

                        @Override
                        public void onAnimationFinished(AnimatedSprite animatedSprite) {
                            scene.setIgnoreUpdate(true);
                            scene.attachChild(pauseRectangle);
                            scene.attachChild(gameOverText);
                            scene.attachChild(restartButton);
                            scene.registerTouchArea(restartButton);
                            scene.attachChild(quitButton);
                            scene.registerTouchArea(quitButton);
                        }
                    });
                }
            }

            @Override
            public void reset() {}
        });

        resumeButton = new Sprite (((CAMERA_WIDTH/2)-316), ((CAMERA_HEIGHT/2)-600), this.resume_tex, this.getVertexBufferObjectManager())
        {
            public boolean onAreaTouched(TouchEvent touchEvent, float X, float Y)
            {
                if (touchEvent.isActionDown())
                {
                    RunOnStart.soundHandler.buttonSound();

                    scene.detachChild(resumeButton);
                    scene.unregisterTouchArea(resumeButton);

                    scene.detachChild(restartButton);
                    scene.unregisterTouchArea(restartButton);

                    scene.detachChild(quitButton);
                    scene.unregisterTouchArea(quitButton);

                    scene.detachChild(pauseRectangle);

                    scene.attachChild(shootBtn);
                    scene.registerTouchArea(shootBtn);

                    scene.attachChild(pauseButton);
                    scene.registerTouchArea(pauseButton);

                    scene.setIgnoreUpdate(false);
                }
                return true;
            }
        };

        gameOverText = new Sprite (((CAMERA_WIDTH/2)-439),((CAMERA_HEIGHT/2)-600), this.game_over_tex, this.getVertexBufferObjectManager()){ };

        //Button to restart the game, calls startActivity once again
        restartButton = new Sprite (((CAMERA_WIDTH/2)-316),((CAMERA_HEIGHT/2)-153), this.restart_tex, this.getVertexBufferObjectManager())
        {
            public boolean onAreaTouched(TouchEvent touchEvent, float X, float Y)
            {
                if (touchEvent.isActionDown())
                {
                    RunOnStart.soundHandler.buttonSound();
                    startActivity(new Intent(getApplicationContext(), Game.class));
                }
                return true;
            }
        };

        //Button to quit game and go to main menu
        quitButton = new Sprite (((CAMERA_WIDTH/2)-316), ((CAMERA_HEIGHT/2)+294), this.quit_tex, this.getVertexBufferObjectManager())
        {
            public boolean onAreaTouched(TouchEvent touchEvent, float X, float Y)
            {
                if (touchEvent.isActionDown())
                {
                    RunOnStart.soundHandler.buttonSound();
                    gameMusic.stop();
                    startActivity(new Intent(getApplicationContext(), MainMenu.class));
                }
                return true;
            }
        };

        pauseButton = new Sprite(0, 0, this.pause_tex, this.getVertexBufferObjectManager())
        {
            public boolean onAreaTouched(TouchEvent touchEvent, float X, float Y)
            {
                if (touchEvent.isActionDown())
                {
                    RunOnStart.soundHandler.buttonSound();

                    scene.detachChild(pauseButton);
                    scene.unregisterTouchArea(pauseButton);

                    scene.detachChild(shootBtn);
                    scene.unregisterTouchArea(shootBtn);

                    scene.setIgnoreUpdate(true);
                    scene.attachChild(pauseRectangle);
                    scene.attachChild(resumeButton);
                    scene.registerTouchArea(resumeButton);
                    scene.attachChild(restartButton);
                    scene.registerTouchArea(restartButton);
                    scene.attachChild(quitButton);
                    scene.registerTouchArea(quitButton);
                }
                return true;
            }
        };
        scene.registerTouchArea(pauseButton);
        scene.attachChild(pauseButton);

        // PRE-START OVERLAY
        final Sprite preStartText = new Sprite(0, 0, loadITextureRegion("touchToStart.png", 800, 120), getVertexBufferObjectManager());
        preStartText.setPosition( (CAMERA_WIDTH-preStartText.getWidth())/2, CAMERA_HEIGHT/4 );
        preStartRect = new Rectangle(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT, getVertexBufferObjectManager())
        {
            public boolean onAreaTouched(TouchEvent touchEvent, float X, float Y)
            {
                if ( touchEvent.isActionDown() )
                {
                    // START GAME
                    scene.detachChild(preStartRect);
                    scene.unregisterTouchArea(preStartRect);
                    enemyManager.setShouldSpawnEnemies(true);
                    player.setGameHasStarted(true);
                    scene.detachChild(preStartText);

                    scene.registerTouchArea(shootBtn);

                    playerHealth.setAlpha(1.0f);

                    preStartText.dispose();
                    preStartRect.dispose();

                    player.setTargetPosition(X, Y);
                }
                return true;
            }
        };

        preStartRect.setColor(0, 0, 0, 0.45f);
        preStartRect.setZIndex(-100);

        scene.attachChild(preStartRect);
        scene.attachChild(preStartText);
        scene.registerTouchArea(preStartRect);

        scene.setVisible(true);
        return scene;
    }

    @Override
    protected Scene onCreateScene()
    {
        Scene splashScene = new Scene();
        splash = new Sprite(0, 0, splashTextureRegion, mEngine.getVertexBufferObjectManager());
        splash.setPosition((CAMERA_WIDTH - splash.getWidth()) * 0.5f, (CAMERA_HEIGHT - splash.getHeight()) * 0.5f);

        splashScene.attachChild(splash);

        gameMusic.play();

        splashScene.registerUpdateHandler(new IUpdateHandler()
        {
            @Override
            public void onUpdate(float v)
            {
                loadGameResources();
                Scene mainScene = createGameScene();
                splash.detachSelf();
                splash.dispose();
                mEngine.setScene(mainScene);
            }

            @Override
            public void reset() {}
        });

        return splashScene;
    }

    @Override
    public boolean onSceneTouchEvent(Scene scene, TouchEvent touchEvent) {
        //if (!touch in HUD)
        if (touchEvent.isActionDown() || touchEvent.isActionMove())
        {
            //Execute touch event
            player.setTargetPosition(new Vector2(touchEvent.getX(), touchEvent.getY()));
            player.setTouchActive(true);

            //sound.setCenterPosition(touchEvent.getX(),touchEvent.getY());
        } else if (touchEvent.isActionUp() || touchEvent.isActionOutside())
        {
            player.setTouchActive(false);
        }
        return false;
    }

    //Disable the back button
    public void onBackPressed() {
    }
}