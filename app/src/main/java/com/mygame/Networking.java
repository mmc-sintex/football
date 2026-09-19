package com.mygame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector3;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Networking {
    public static final int PORT = 25565;
    public static final String DEFAULT_HOST_IP = "192.168.43.1";

    FootballGame game;
    boolean isHost;
    Socket socket;
    DataOutputStream out;
    DataInputStream in;
    volatile boolean connected = false;
    float sendTimer = 0;

    public Networking(FootballGame g, boolean host) {
        this.game = g;
        this.isHost = host;
        if (host) startHost();
        else startClient(DEFAULT_HOST_IP);
    }

    void startHost() {
        new Thread(() -> {
            try {
                Gdx.app.log("Net", "Starting server on port " + PORT);
                ServerSocket ss = new ServerSocket(PORT);
                socket = ss.accept();
                out = new DataOutputStream(socket.getOutputStream());
                in = new DataInputStream(socket.getInputStream());
                connected = true;
                Gdx.app.log("Net", "Client connected!");
                listenLoop();
            } catch (Exception e) {
                Gdx.app.error("Net", "Host error: " + e.getMessage());
            }
        }).start();
    }

    void startClient(String ip) {
        new Thread(() -> {
            try {
                Gdx.app.log("Net", "Connecting to " + ip);
                socket = new Socket();
                socket.connect(new InetSocketAddress(ip, PORT), 5000);
                out = new DataOutputStream(socket.getOutputStream());
                in = new DataInputStream(socket.getInputStream());
                connected = true;
                Gdx.app.log("Net", "Connected!");
                listenLoop();
            } catch (Exception e) {
                Gdx.app.error("Net", "Client error: " + e.getMessage());
            }
        }).start();
    }

    void listenLoop() {
        try {
            while (connected) {
                float px = in.readFloat();
                float pz = in.readFloat();
                float bx = in.readFloat();
                float by = in.readFloat();
                float bz = in.readFloat();
                game.setRemotePlayer(px, pz);
                if (isHost) {
                    // host authoritative on ball
                } else {
                    game.setRemoteBall(bx, by, bz);
                }
            }
        } catch (Exception e) {
            Gdx.app.error("Net", "Listen error: " + e.getMessage());
            connected = false;
        }
    }

    public void tick(float dt) {
        if (!connected || out == null) return;
        sendTimer += dt;
        if (sendTimer < 0.05f) return;
        sendTimer = 0;
        try {
            Vector3 p = game.getLocalPlayerPos();
            Vector3 b = new Vector3(game.getBallX(), game.getBallY(), game.getBallZ());
            out.writeFloat(p.x);
            out.writeFloat(p.z);
            out.writeFloat(b.x);
            out.writeFloat(b.y);
            out.writeFloat(b.z);
            out.flush();
        } catch (Exception e) {
            connected = false;
        }
    }

    public boolean isConnected() { return connected; }

    public void dispose() {
        try {
            connected = false;
            if (socket != null) socket.close();
        } catch (Exception ignored) {}
    }
}
