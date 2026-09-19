package com.mygame;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;

public class FootballGame extends ApplicationAdapter {

    ModelBatch batch;
    SpriteBatch spriteBatch;
    BitmapFont font;
    Environment environment;
    PerspectiveCamera camera;

    ModelInstance field;
    ModelInstance ball;
    ModelInstance player1, player2;
    ModelInstance goalL1, goalL2, goalR1, goalR2;

    Vector3 ballPos = new Vector3();
    Vector3 ballVel = new Vector3();
    float ballY, ballYVel;
    final float BALL_R = 0.5f;
    final float GRAVITY = -25f;
    final float FRICTION = 0.8f;

    static final float FW = 40f;
    static final float FL = 60f;
    static final float HW = FW / 2f;
    static final float HL = FL / 2f;
    static final float GOAL_W = 8f;
    static final float PLAYER_SPEED = 16f;

    int scoreLeft = 0, scoreRight = 0;
    boolean menuMode = true;
    boolean isHost = false;
    Networking net;
    float shootCooldown = 0f;

    float foulTimer = 0f;
    float foulFlashTimer = 0f;
    String refereeMessage = "";

    float joyCX, joyCY;

    @Override
    public void create() {
        batch = new ModelBatch();
        spriteBatch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(2.2f);

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.7f, 0.7f, 0.7f, 1f));
        environment.add(new DirectionalLight().set(1f, 1f, 1f, -1f, -1.5f, -1f));

        camera = new PerspectiveCamera(60, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0, 30, 45);
        camera.lookAt(0, 0, 0);
        camera.near = 1f; camera.far = 300f;
        camera.update();

        ModelBuilder mb = new ModelBuilder();
        int attrs = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;

        field = new ModelInstance(mb.createBox(FW, 0.5f, FL,
            new Material(ColorAttribute.createDiffuse(new Color(0.1f, 0.6f, 0.15f, 1f))), attrs));

        Model ballM = mb.createSphere(1f, 1f, 1f, 20, 20,
            new Material(ColorAttribute.createDiffuse(Color.WHITE)), attrs);
        ball = new ModelInstance(ballM);

        Model pRedM = mb.createBox(2f, 4f, 2f,
            new Material(ColorAttribute.createDiffuse(Color.RED)), attrs);
        player1 = new ModelInstance(pRedM);

        Model pBlueM = mb.createBox(2f, 4f, 2f,
            new Material(ColorAttribute.createDiffuse(new Color(0.1f, 0.3f, 1f, 1f))), attrs);
        player2 = new ModelInstance(pBlueM);

        Model postM = mb.createBox(0.5f, 6f, 0.5f,
            new Material(ColorAttribute.createDiffuse(Color.WHITE)), attrs);
        goalL1 = new ModelInstance(postM);
        goalL2 = new ModelInstance(postM);
        goalR1 = new ModelInstance(postM);
        goalR2 = new ModelInstance(postM);

        joyCX = Gdx.graphics.getWidth() * 0.15f;
        joyCY = Gdx.graphics.getHeight() * 0.35f;

        resetPositions();
    }

    void resetPositions() {
        ballPos.set(0, BALL_R, 0);
        ballVel.set(0, 0, 0);
        ballY = BALL_R;
        ballYVel = 0;
        player1.transform.setToTranslation(-6, 2f, 0);
        player2.transform.setToTranslation(6, 2f, 0);
        float zL = -HL + 1f, zR = HL - 1f;
        goalL1.transform.setToTranslation(-GOAL_W / 2f, 3f, zL);
        goalL2.transform.setToTranslation(GOAL_W / 2f, 3f, zL);
        goalR1.transform.setToTranslation(-GOAL_W / 2f, 3f, zR);
        goalR2.transform.setToTranslation(GOAL_W / 2f, 3f, zR);
        ball.transform.setToTranslation(0, BALL_R, 0).scale(BALL_R * 2, BALL_R * 2, BALL_R * 2);
    }

    @Override
    public void render() {
        float dt = Math.min(Gdx.graphics.getDeltaTime(), 0.05f);

        if (menuMode) {
            handleMenu();
        } else {
            handleGameInput(dt);
            updateBall(dt);
            checkFouls(dt);
            shootCooldown -= dt;
        }

        if (net != null) net.tick(dt);

        Vector3 bp = ball.transform.getTranslation(new Vector3());
        camera.position.set(bp.x * 0.3f, 30, 45 + bp.z * 0.2f);
        camera.lookAt(bp.x * 0.5f, 0, bp.z * 0.5f);
        camera.update();

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.5f, 0.8f, 1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        batch.begin(camera);
        batch.render(field, environment);
        batch.render(ball, environment);
        batch.render(player1, environment);
        batch.render(player2, environment);
        batch.render(goalL1, environment);
        batch.render(goalL2, environment);
        batch.render(goalR1, environment);
        batch.render(goalR2, environment);
        batch.end();

        drawUI(dt);
    }

    void handleMenu() {
        if (Gdx.input.justTouched()) {
            int x = Gdx.input.getX();
            if (x < Gdx.graphics.getWidth() / 2) {
                isHost = true;
                menuMode = false;
                net = new Networking(this, true);
            } else {
                isHost = false;
                menuMode = false;
                net = new Networking(this, false);
            }
        }
    }

    void handleGameInput(float dt) {
        ModelInstance myPlayer = isHost ? player1 : player2;
        Vector3 p = myPlayer.transform.getTranslation(new Vector3());
        float speed = PLAYER_SPEED * dt;

        boolean useJoy = false;
        float jx = 0, jy = 0;
        boolean shoot = false;

        for (int i = 0; i < 10; i++) {
            if (Gdx.input.isTouched(i)) {
                float tx = Gdx.input.getX(i);
                float ty = Gdx.graphics.getHeight() - Gdx.input.getY(i);
                if (tx < Gdx.graphics.getWidth() * 0.4f) {
                    useJoy = true;
                    jx = tx;
                    jy = ty;
                } else {
                    shoot = true;
                }
            }
        }

        if (useJoy) {
            float dx = jx - joyCX;
            float dy = jy - joyCY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist > 15) {
                p.x += (dx / dist) * speed;
                p.z -= (dy / dist) * speed;
            }
        }

        if (shoot && shootCooldown <= 0) {
            Vector3 bWorld = new Vector3(ballPos.x, ballY, ballPos.z);
            if (p.dst(bWorld) < 5f) {
                Vector3 dir = new Vector3(bWorld).sub(p).nor();
                ballVel.set(dir.x * 32f, 0, dir.z * 32f);
                ballYVel = 10f;
                shootCooldown = 0.4f;
            }
        }

        p.x = Math.max(-HW + 2, Math.min(HW - 2, p.x));
        p.z = Math.max(-HL + 2, Math.min(HL - 2, p.z));
        myPlayer.transform.setToTranslation(p.x, 2f, p.z);
    }

    void updateBall(float dt) {
        if (!isHost && net != null && net.isConnected()) {
            ball.transform.setToTranslation(ballPos.x, ballY, ballPos.z)
                .scale(BALL_R * 2, BALL_R * 2, BALL_R * 2);
            return;
        }

        ballPos.x += ballVel.x * dt;
        ballPos.z += ballVel.z * dt;

        ballY += ballYVel * dt;
        ballYVel += GRAVITY * dt;
        if (ballY < BALL_R) {
            ballY = BALL_R;
            ballYVel = -ballYVel * 0.6f;
            if (Math.abs(ballYVel) < 1f) ballYVel = 0;
        }

        float speed = ballVel.len();
        if (speed > 0.01f) {
            float ns = Math.max(0, speed - FRICTION * dt * speed);
            ballVel.scl(ns / speed);
        }

        checkBallCollision(player1);
        checkBallCollision(player2);

        if (ballPos.x < -HW + BALL_R) { ballPos.x = -HW + BALL_R; ballVel.x = -ballVel.x * 0.7f; }
        if (ballPos.x > HW - BALL_R) { ballPos.x = HW - BALL_R; ballVel.x = -ballVel.x * 0.7f; }

        if (ballPos.z < -HL + BALL_R) {
            if (Math.abs(ballPos.x) < GOAL_W / 2f) {
                scoreRight++;
                resetPositions();
                return;
            }
            ballPos.z = -HL + BALL_R;
            ballVel.z = -ballVel.z * 0.7f;
        }
        if (ballPos.z > HL - BALL_R) {
            if (Math.abs(ballPos.x) < GOAL_W / 2f) {
                scoreLeft++;
                resetPositions();
                return;
            }
            ballPos.z = HL - BALL_R;
            ballVel.z = -ballVel.z * 0.7f;
        }

        ball.transform.setToTranslation(ballPos.x, ballY, ballPos.z)
            .scale(BALL_R * 2, BALL_R * 2, BALL_R * 2);
    }

    void checkBallCollision(ModelInstance player) {
        Vector3 p = player.transform.getTranslation(new Vector3());
        float dx = ballPos.x - p.x;
        float dz = ballPos.z - p.z;
        float dist = (float) Math.sqrt(dx * dx + dz * dz);
        if (dist < 2.5f && dist > 0.01f) {
            float nx = dx / dist;
            float nz = dz / dist;
            ballPos.x = p.x + nx * 2.5f;
            ballPos.z = p.z + nz * 2.5f;
            ballVel.set(nx * 20f, 0, nz * 20f);
        }
    }

    void checkFouls(float dt) {
        Vector3 p1 = player1.transform.getTranslation(new Vector3());
        Vector3 p2 = player2.transform.getTranslation(new Vector3());
        if (p1.dst(p2) < 2.5f) {
            foulTimer += dt;
            if (foulTimer > 1.5f) {
                foulTimer = 0;
                refereeMessage = "FOUL!";
                foulFlashTimer = 1.5f;
                ballPos.set(0, BALL_R, 0);
                ballVel.set(0, 0, 0);
                ballYVel = 0;
                ballY = BALL_R;
            }
        } else {
            foulTimer = 0;
        }
    }

    void drawUI(float dt) {
        spriteBatch.begin();
        if (menuMode) {
            font.setColor(Color.WHITE);
            font.draw(spriteBatch, "3D FOOTBALL", Gdx.graphics.getWidth() / 2f - 110, Gdx.graphics.getHeight() - 80);
            font.draw(spriteBatch, "Tap LEFT to HOST", 40, Gdx.graphics.getHeight() / 2f);
            font.draw(spriteBatch, "Tap RIGHT to JOIN", Gdx.graphics.getWidth() / 2f + 20, Gdx.graphics.getHeight() / 2f);
        } else {
            font.setColor(Color.WHITE);
            font.draw(spriteBatch, "RED " + scoreLeft + " - " + scoreRight + " BLUE",
                Gdx.graphics.getWidth() / 2f - 140, Gdx.graphics.getHeight() - 30);

            font.setColor(Color.YELLOW);
            font.draw(spriteBatch, "MOVE", joyCX - 60, joyCY + 100);
            font.draw(spriteBatch, "SHOOT >", Gdx.graphics.getWidth() * 0.7f, Gdx.graphics.getHeight() / 2f);

            font.setColor(Color.GREEN);
            if (net != null && net.isConnected()) {
                font.draw(spriteBatch, "CONNECTED", 20, 40);
            } else {
                font.draw(spriteBatch, "WAITING...", 20, 40);
            }

            if (foulFlashTimer > 0) {
                font.setColor(Color.RED);
                font.draw(spriteBatch, refereeMessage, Gdx.graphics.getWidth() / 2f - 60, Gdx.graphics.getHeight() / 2f);
                foulFlashTimer -= dt;
            }
        }
        spriteBatch.end();
    }

    public void setRemotePlayer(float x, float z) {
        ModelInstance rp = isHost ? player2 : player1;
        rp.transform.setToTranslation(x, 2f, z);
    }

    public void setRemoteBall(float x, float y, float z) {
        ballPos.set(x, y, z);
        ballY = y;
        ball.transform.setToTranslation(x, y, z).scale(BALL_R * 2, BALL_R * 2, BALL_R * 2);
    }

    public Vector3 getLocalPlayerPos() {
        ModelInstance mp = isHost ? player1 : player2;
        return mp.transform.getTranslation(new Vector3());
    }

    public float getBallX() { return ballPos.x; }
    public float getBallY() { return ballY; }
    public float getBallZ() { return ballPos.z; }

    @Override
    public void dispose() {
        batch.dispose();
        spriteBatch.dispose();
        font.dispose();
        if (net != null) net.dispose();
    }
}
