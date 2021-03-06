package com.gamestudio64.martianrun.stages;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.gamestudio64.martianrun.actors.Enemy;
import com.gamestudio64.martianrun.actors.Ground;
import com.gamestudio64.martianrun.actors.Runner;
import com.gamestudio64.martianrun.utils.WorldUtils;
import com.gamestudio64.martianrun.utils.BodyUtils;


/**
 * A stage is set in a scene. You can have multiple stages in a scene.
 * For ex. One for UI and one for the actual game objects (enemies, player, other moving things)
 */
public class GameStage extends Stage implements ContactListener {

    //Rectangle member
    private Rectangle screenRightSide;
    private Rectangle screenLeftSide;

    //This will be our viewport measurements while working with the debug renderer
    private static final int VIEWPORT_WIDTH = 20;
    private static final int VIEWPORT_HEIGHT = 13;

    //This stage contains the game's world and the ground
    private World world;
    private Ground ground;
    private Runner runner;

    //Touch point vector member
    private Vector3 touchPoint;

    //The time step for the game, sort of like FPS
    private final float TIME_STEP = 1 / 300f;
    private float accumulator = 0f;

    //Camera and Debug renderer objects
    private OrthographicCamera camera;
    private Box2DDebugRenderer renderer;

    public GameStage() {
        setupWorld();
        setupCamera();
        setupTouchControlAreas();
        renderer = new Box2DDebugRenderer();
    }

    private void setupTouchControlAreas(){
        touchPoint = new Vector3();
        screenLeftSide = new Rectangle(0, 0, getCamera().viewportWidth / 2, getCamera().viewportHeight);
        screenRightSide = new Rectangle(getCamera().viewportWidth / 2, 0, getCamera().viewportWidth / 2, getCamera().viewportHeight);
        Gdx.input.setInputProcessor(this);
    }

    @Override
    public boolean touchDown(int x, int y, int pointer, int button){
        //Need to get the actual coordinates
        translateScreenToWorldCoordinates(x, y);

        if(rightSideTouched(touchPoint.x, touchPoint.y)){
            runner.jump();
        } else if(leftSideTouched(touchPoint.x, touchPoint.y)){
            runner.dodge();
        }

        return super.touchDown(x, y, pointer, button);
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button){
        if(runner.isDodging()){
            runner.stopDodge();;
        }
        return super.touchUp(screenX, screenY, pointer, button);
    }


    private boolean rightSideTouched(float x, float y){
        return screenRightSide.contains(x, y);
    }

    private boolean leftSideTouched(float x, float y){
        return screenLeftSide.contains(x, y);
    }

    private void translateScreenToWorldCoordinates(int x, int y){
        getCamera().unproject(touchPoint.set(x, y, 0));
    }

    private void setupWorld(){
        world = WorldUtils.createWorld();
        world.setContactListener(this);
        setUpGround();
        setUpRunner();
        createEnemy();
    }

    private void setUpGround(){
        ground = new Ground(WorldUtils.createGround(world));
        addActor(ground);
    }

    private void setUpRunner(){
        runner = new Runner(WorldUtils.createRunner(world));
        addActor(runner);
    }

    private void setupCamera(){
        camera = new OrthographicCamera(VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
        camera.position.set(camera.viewportWidth / 2, camera.viewportHeight / 2, 0f);
        camera.update();
    }

    @Override
    public void act(float delta){

        //Calls the act method on all the actors on the stage
        super.act(delta);

        Array<Body> bodies = new Array<Body>(world.getBodyCount());
        world.getBodies(bodies);

        for(Body body : bodies){
            update(body);
        }

        // Fixed timestamp
        accumulator += delta;

        while(accumulator >= delta){
            world.step(TIME_STEP, 6, 2);
            accumulator -= TIME_STEP;
        }

        //TODO: implement interpolation
    }

    private void update(Body body){
        if(!BodyUtils.bodyInBounds(body)){
            if(BodyUtils.bodyIsEnemy(body) && !runner.isHit()){
                createEnemy();
            }
            world.destroyBody(body);
        }
    }

    private void createEnemy(){
        Enemy enemy  = new Enemy(WorldUtils.createEnemy(world));
        addActor(enemy);
    }



    @Override
    public void draw(){
        super.draw();
        renderer.render(world, camera.combined);
    }

    @Override
    public void beginContact(Contact contact){
        Body a = contact.getFixtureA().getBody();
        Body b = contact.getFixtureB().getBody();

        if((BodyUtils.bodyIsRunner(a) && BodyUtils.bodyIsEnemy(b)) ||
                (BodyUtils.bodyIsEnemy(a) && BodyUtils.bodyIsRunner(b))){
            runner.hit();
        }
        else if((BodyUtils.bodyIsRunner(a)  && BodyUtils.bodyIsGround(b)) ||
                (BodyUtils.bodyIsGround(a) && BodyUtils.bodyIsRunner(b))){
            runner.landed();
        }
    }

    @Override
    public void endContact(Contact contact) {

    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {

    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {

    }

}
