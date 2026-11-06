
package com.ambulancerescue.game;

import android.content.Context;
import android.content.SharedPreferences;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GameRenderer implements GLSurfaceView.Renderer {

    private final Context context;
    private final SharedPreferences prefs;
    private final Random random = new Random();

    private int program;
    private int positionHandle;
    private int colorHandle;
    private int mvpHandle;

    private FloatBuffer cubeBuffer;

    private final float[] projection = new float[16];
    private final float[] view = new float[16];
    private final float[] model = new float[16];
    private final float[] mvp = new float[16];
    private final float[] temp = new float[16];

    private float carX = 0;
    private float carZ = 0;
    private float carYaw = 0;
    private float speed = 0;

    private float cameraX, cameraY, cameraZ;
    private int cameraMode = 0;
    private int vehicle = 0;

    private boolean steerLeft;
    private boolean steerRight;
    private boolean accelerate;
    private boolean brake;
    private boolean siren;

    private boolean emergency = false;
    private boolean patient = false;

    private float emergencyX = 40;
    private float emergencyZ = 70;

    private int money = 1000;
    private int score = 0;

    private long lastEmergency;
    private String status = "Pronto per il servizio";

    private final float[][] traffic = new float[12][3];

    private static final float[] CUBE = {
        -0.5f,-0.5f,-0.5f,  0.5f,-0.5f,-0.5f,
         0.5f, 0.5f,-0.5f, -0.5f, 0.5f,-0.5f,
        -0.5f,-0.5f, 0.5f,  0.5f,-0.5f, 0.5f,
         0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f
    };

    private static final String VERTEX =
        "uniform mat4 uMVP;" +
        "attribute vec3 aPosition;" +
        "void main(){" +
        " gl_Position=uMVP*vec4(aPosition,1.0);" +
        "}";

    private static final String FRAGMENT =
        "precision mediump float;" +
        "uniform vec4 uColor;" +
        "void main(){" +
        " gl_FragColor=uColor;" +
        "}";

    public GameRenderer(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences(
            "ambulance3d", Context.MODE_PRIVATE
        );

        money = prefs.getInt("money", 1000);
        score = prefs.getInt("score", 0);
        vehicle = prefs.getInt("vehicle", 0);

        for (int i = 0; i < traffic.length; i++) {
            traffic[i][0] = random.nextInt(180) - 90;
            traffic[i][1] = 0;
            traffic[i][2] = random.nextInt(300) - 150;
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.45f, 0.68f, 0.83f, 1f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        int vs = shader(GLES20.GL_VERTEX_SHADER, VERTEX);
        int fs = shader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT);

        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vs);
        GLES20.glAttachShader(program, fs);
        GLES20.glLinkProgram(program);

        positionHandle = GLES20.glGetAttribLocation(
            program, "aPosition"
        );
        colorHandle = GLES20.glGetUniformLocation(
            program, "uColor"
        );
        mvpHandle = GLES20.glGetUniformLocation(
            program, "uMVP"
        );

        ByteBuffer bb = ByteBuffer.allocateDirect(
            CUBE.length * 4
        );
        bb.order(ByteOrder.nativeOrder());
        cubeBuffer = bb.asFloatBuffer();
        cubeBuffer.put(CUBE);
        cubeBuffer.position(0);
    }

    private int shader(int type, String source) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, source);
        GLES20.glCompileShader(s);
        return s;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        float ratio = (float)width / Math.max(1, height);

        Matrix.perspectiveM(
            projection, 0, 65f, ratio, 0.1f, 700f
        );
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(
            GLES20.GL_COLOR_BUFFER_BIT
            | GLES20.GL_DEPTH_BUFFER_BIT
        );

        update();

        setCamera();

        GLES20.glUseProgram(program);

        drawCity();
        drawTraffic();
        drawEmergency();
        drawAmbulance();
    }

    private void update() {
        if (accelerate) speed += 0.012f;
        else speed *= 0.985f;

        if (brake) speed *= 0.94f;

        speed = Math.max(-0.06f, Math.min(0.34f, speed));

        if (steerLeft) carYaw += 1.8f * (0.3f + Math.abs(speed));
        if (steerRight) carYaw -= 1.8f * (0.3f + Math.abs(speed));

        double rad = Math.toRadians(carYaw);

        carX += (float)Math.sin(rad) * speed;
        carZ += (float)Math.cos(rad) * speed;

        carX = Math.max(-145, Math.min(145, carX));
        carZ = Math.max(-145, Math.min(145, carZ));

        for (float[] car : traffic) {
            car[2] -= 0.07f;
            if (car[2] < -180) car[2] = 180;
        }

        if (!emergency &&
            System.currentTimeMillis() - lastEmergency > 18000) {
            createEmergency();
        }

        if (emergency && !patient &&
            distance(carX, carZ, emergencyX, emergencyZ) < 5) {
            status = "Paziente raggiunto! Premi SOCCORSO";
        }
    }

    private void setCamera() {
        double rad = Math.toRadians(carYaw);

        float fx = (float)Math.sin(rad);
        float fz = (float)Math.cos(rad);

        if (cameraMode == 0) {
            cameraX = carX - fx * 9;
            cameraY = 5.2f;
            cameraZ = carZ - fz * 9;

            Matrix.setLookAtM(
                view, 0,
                cameraX, cameraY, cameraZ,
                carX, 1.2f, carZ,
                0, 1, 0
            );
        } else if (cameraMode == 1) {
            cameraX = carX + fx * 0.25f;
            cameraY = 2.0f;
            cameraZ = carZ + fz * 0.25f;

            Matrix.setLookAtM(
                view, 0,
                cameraX, cameraY, cameraZ,
                carX + fx * 20,
                1.5f,
                carZ + fz * 20,
                0, 1, 0
            );
        } else {
            cameraX = carX + 13;
            cameraY = 11;
            cameraZ = carZ + 13;

            Matrix.setLookAtM(
                view, 0,
                cameraX, cameraY, cameraZ,
                carX, 0, carZ,
                0, 1, 0
            );
        }
    }

    private void drawCity() {
        box(0, -0.6f, 0, 360, 1, 360,
            0.25f, 0.48f, 0.22f, 1);

        // Strade principali
        box(0, -0.05f, 0, 12, 0.12f, 360,
            0.16f, 0.17f, 0.18f, 1);

        box(0, -0.04f, 0, 360, 0.12f, 12,
            0.16f, 0.17f, 0.18f, 1);

        // Linee centrali
        for (int z = -170; z < 180; z += 12) {
            box(0, 0.03f, z, 0.25f, 0.03f, 5,
                0.95f, 0.85f, 0.3f, 1);

            box(z, 0.03f, 0, 5, 0.03f, 0.25f,
                0.95f, 0.85f, 0.3f, 1);
        }

        // Case e palazzi
        for (int x = -150; x <= 150; x += 30) {
            for (int z = -150; z <= 150; z += 30) {
                if (Math.abs(x) < 13 || Math.abs(z) < 13)
                    continue;

                float height = 5 + ((x * x + z * z) % 13);

                box(x, height / 2, z,
                    17, height, 17,
                    0.63f, 0.59f, 0.52f, 1);

                box(x, height + 0.1f, z,
                    17.5f, 0.3f, 17.5f,
                    0.3f, 0.25f, 0.22f, 1);

                box(x, height * 0.5f, z + 8.6f,
                    4, 2, 0.15f,
                    0.25f, 0.55f, 0.72f, 1);
            }
        }

        // Ospedale
        box(27, 3, 27, 20, 6, 17,
            0.86f, 0.88f, 0.88f, 1);

        box(27, 6.2f, 27, 20.5f, 0.5f, 17.5f,
            0.8f, 0.1f, 0.1f, 1);

        box(27, 4, 18.3f, 5, 2, 0.2f,
            0.8f, 0.1f, 0.1f, 1);

        box(27, 4, 18.1f, 1, 4, 0.3f,
            0.8f, 0.1f, 0.1f, 1);
    }

    private void drawTraffic() {
        for (int i = 0; i < traffic.length; i++) {
            float x = traffic[i][0];
            float z = traffic[i][2];

            box(x, 0.65f, z,
                2.1f, 1.2f, 4,
                0.1f + (i % 3) * 0.2f,
                0.25f,
                0.7f,
                1);

            box(x, 1.35f, z,
                1.5f, 0.5f, 1.8f,
                0.35f, 0.55f, 0.7f, 1);
        }
    }

    private void drawAmbulance() {
        double rad = Math.toRadians(carYaw);

        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, carX, 0, carZ);
        Matrix.rotateM(model, 0, carYaw, 0, 1, 0);

        // Corpo
        drawLocal(0, 1, 0, 2.5f, 2, 5,
            0.93f, 0.94f, 0.94f, 1);

        // Cabina
        drawLocal(0, 1.7f, 1.75f, 2.2f, 1.2f, 1.4f,
            0.18f, 0.48f, 0.65f, 1);

        // Finestrini
        drawLocal(0, 1.7f, 2.47f, 1.5f, 0.7f, 0.08f,
            0.12f, 0.3f, 0.42f, 1);

        // Croce rossa
        drawLocal(0, 1.1f, -0.4f, 0.25f, 1.1f, 0.08f,
            0.9f, 0.05f, 0.05f, 1);

        drawLocal(0, 1.1f, -0.4f, 0.9f, 0.25f, 0.08f,
            0.9f, 0.05f, 0.05f, 1);

        // Barre lampeggianti
        drawLocal(0, 2.15f, 0, 1.8f, 0.18f, 0.45f,
            0.08f, 0.1f, 0.15f, 1);

        float blink = (System.currentTimeMillis() / 250) % 2;

        drawLocal(-0.65f, 2.3f, 0, 0.45f, 0.18f, 0.4f,
            blink == 0 ? 1 : 0.1f,
            0.05f, 0.05f, 1);

        drawLocal(0.65f, 2.3f, 0, 0.45f, 0.18f, 0.4f,
            0.05f, 0.1f,
            blink == 0 ? 1 : 0.1f, 1);

        Matrix.setIdentityM(model, 0);
    }

    private void drawLocal(
        float x, float y, float z,
        float sx, float sy, float sz,
        float r, float g, float b, float a
    ) {
        float[] local = new float[16];

        Matrix.setIdentityM(local, 0);
        Matrix.translateM(local, 0, x, y, z);
        Matrix.scaleM(local, 0, sx, sy, sz);

        float[] result = new float[16];
        Matrix.multiplyMM(result, 0, model, 0, local, 0);

        drawMatrix(result, r, g, b, a);
    }

    private void drawEmergency() {
        if (!emergency) return;

        float pulse =
            1.0f + (float)Math.sin(
                System.currentTimeMillis() * 0.008
            ) * 0.2f;

        box(emergencyX, 1.2f, emergencyZ,
            1.5f * pulse, 2.4f * pulse, 1.5f * pulse,
            1, 0.05f, 0.05f, 1);
    }

    private void box(
        float x, float y, float z,
        float sx, float sy, float sz,
        float r, float g, float b, float a
    ) {
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, x, y, z);
        Matrix.scaleM(model, 0, sx, sy, sz);
        drawMatrix(model, r, g, b, a);
    }

    private void drawMatrix(
        float[] matrix,
        float r, float g, float b, float a
    ) {
        Matrix.multiplyMM(temp, 0, view, 0, matrix, 0);
        Matrix.multiplyMM(mvp, 0, projection, 0, temp, 0);

        GLES20.glUniformMatrix4fv(
            mvpHandle, 1, false, mvp, 0
        );

        GLES20.glUniform4f(colorHandle, r, g, b, a);

        cubeBuffer.position(0);

        GLES20.glEnableVertexAttribArray(positionHandle);

        GLES20.glVertexAttribPointer(
            positionHandle, 3,
            GLES20.GL_FLOAT, false,
            0, cubeBuffer
        );

        drawFaces();
    }

    private void drawFaces() {
        // Sei facce del cubo, ciascuna composta da due triangoli.
        int[] faces = {
            0,1,2, 0,2,3,
            4,6,5, 4,7,6,
            0,4,5, 0,5,1,
            3,2,6, 3,6,7,
            1,5,6, 1,6,2,
            0,3,7, 0,7,4
        };

        for (int i = 0; i < faces.length; i += 3) {
            float[] tri = new float[9];

            for (int j = 0; j < 3; j++) {
                int index = faces[i+j] * 3;
                tri[j*3] = CUBE[index];
                tri[j*3+1] = CUBE[index+1];
                tri[j*3+2] = CUBE[index+2];
            }

            FloatBuffer fb = ByteBuffer
                .allocateDirect(9 * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

            fb.put(tri);
            fb.position(0);

            GLES20.glVertexAttribPointer(
                positionHandle, 3,
                GLES20.GL_FLOAT, false, 0, fb
            );

            GLES20.glDrawArrays(
                GLES20.GL_TRIANGLES, 0, 3
            );
        }
    }

    private float distance(
        float x1, float z1, float x2, float z2
    ) {
        return (float)Math.hypot(x1-x2, z1-z2);
    }

    private void createEmergency() {
        emergency = true;
        patient = false;

        emergencyX = random.nextInt(220) - 110;
        emergencyZ = random.nextInt(220) - 110;

        lastEmergency = System.currentTimeMillis();
        status = "Nuova emergenza: raggiungi il segnale rosso";
    }

    public void tryMission() {
        if (!emergency) {
            status = "Nessuna emergenza attiva";
            return;
        }

        if (distance(carX, carZ, emergencyX, emergencyZ) < 9) {
            if (!patient) {
                patient = true;
                emergency = false;
                status = "Paziente caricato! Raggiungi l'ospedale";
            } else {
                patient = false;
                money += 250;
                score += 100;
                status = "Missione completata! +250 crediti";
                save();
            }
        } else {
            status = "Avvicinati al segnale rosso per soccorrere";
        }
    }

    public void setControl(int control, boolean pressed) {
        if (control == 0) steerLeft = pressed;
        if (control == 1) steerRight = pressed;
        if (control == 2) brake = pressed;
        if (control == 3) accelerate = pressed;
    }

    public void setSiren(boolean enabled) {
        siren = enabled;
    }

    public void nextCamera() {
        cameraMode = (cameraMode + 1) % 3;
    }

    public String cameraName() {
        if (cameraMode == 0) return "Terza persona";
        if (cameraMode == 1) return "Prima persona";
        return "Libera";
    }

    public void nextVehicle() {
        vehicle = (vehicle + 1) % 4;
        save();
    }

    public String vehicleName() {
        return new String[] {
            "Ambulanza standard",
            "Ambulanza rossa",
            "Ambulanza urbana",
            "Ambulanza speciale"
        }[vehicle];
    }

    public String getStatus() {
        return status;
    }

    public void save() {
        prefs.edit()
            .putInt("money", money)
            .putInt("score", score)
            .putInt("vehicle", vehicle)
            .apply();
    }
        }
