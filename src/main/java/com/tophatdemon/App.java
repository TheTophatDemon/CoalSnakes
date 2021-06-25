package com.tophatdemon;

import java.util.Map;
import java.util.HashMap;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.*;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Quaternionf;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

import static java.lang.Math.*;


public class App 
{
    private static final int SCREEN_WIDTH = 1280;
    private static final int SCREEN_HEIGHT = 720;
    private static final float ASPECT_RATIO = SCREEN_WIDTH / (float) SCREEN_HEIGHT;
    private static final float FOV = 70.0f * (float)PI / 180.0f;
    private static final float CAMERA_NEAR = 0.1f;
    private static final float CAMERA_FAR = 1000.0f;

    private final Map<Integer, Boolean> keyDown = new HashMap<>();
    private final Map<Integer, Boolean> keyPress = new HashMap<>();

    private float globalTime;
    private long window;

    private Vector3f cameraPosition = new Vector3f();
    private Vector3f cameraRotation = new Vector3f();

    public static void main(String[] args) {
        new App().run(args);
    }

    public boolean IsKeyDown(int keyCode) {
        return keyDown.getOrDefault(keyCode, false);
    }

    public boolean IsKeyPressed(int keyCode) {
        return keyPress.getOrDefault(keyCode, false);
    }

    public void run( String[] args )
    {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Unable to initialize GLFW");
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        window = glfwCreateWindow(1280, 720, "Здравствуйте", NULL, NULL);
        if (window == NULL) throw new RuntimeException("Failed to create GLFW window");

        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true);
            } else {
                if (action == GLFW_PRESS) {
                    keyPress.put(key, true);
                    keyDown.put(key, true);
                } else if (action == GLFW_RELEASE) {
                    keyDown.put(key, false);
                }
            }
        });

        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);

            glfwGetWindowSize(window, pWidth, pHeight);
            
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            glfwSetWindowPos(
                window,
                (vidmode.width() - pWidth.get(0)) / 2,
                (vidmode.height() - pHeight.get(0)) / 2
            );
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);

        //GL init
        GL.createCapabilities();
        //glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);

        Shader testShader = Shader.LoadFromFile("/res/shaders/default.vert", "/res/shaders/default.frag");
        Mesh testMesh = new Mesh();
        testMesh.setVertexPositions(
            -1.0f, -1.0f, 0.0f,
            +1.0f, -1.0f, 0.0f,
            +1.0f, +1.0f, 0.0f,
            -1.0f, +1.0f, 0.0f
        );
        testMesh.setColors(
            1.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 1.0f
        );
        testMesh.setIndices(
            0, 1, 2, 0, 2, 3
        );
        Terrain terrain = new Terrain(64, 64, 64, 4.0f);
        terrain.SetPerlinNoiseWeights(0.1f);
        
        Matrix4f projection = new Matrix4f().setPerspective(FOV, ASPECT_RATIO, CAMERA_NEAR, CAMERA_FAR);
        
        glClearColor(0.0f, 0.0f, 0.5f, 0.0f);
        
        long lastTime = System.nanoTime();
        //Loop
        while (!glfwWindowShouldClose(window)) {
            long now = System.nanoTime();
            float deltaTime = (now - lastTime) / 1_000_000_000.0f;
            lastTime = now;

            globalTime += deltaTime;

            Vector3f rotDelta = new Vector3f(0.0f, 0.0f, 0.0f);
            if (IsKeyDown(GLFW_KEY_LEFT)) {
                rotDelta.y = 1.0f;
            } else if (IsKeyDown(GLFW_KEY_RIGHT)) {
                rotDelta.y = -1.0f;
            }
            if (IsKeyDown(GLFW_KEY_UP)) {
                rotDelta.x = 1.0f;
            } else if (IsKeyDown(GLFW_KEY_DOWN)) {
                rotDelta.x = -1.0f;
            }
            cameraRotation.add(rotDelta.mul(deltaTime * 3.0f));
            Matrix4f rotMatrix = new Matrix4f()
            .rotateY(cameraRotation.y)
            .rotateX(cameraRotation.x)
            ;

            Vector3f movement = new Vector3f(0.0f, 0.0f, 0.0f);
            if (IsKeyDown(GLFW_KEY_W)) {
                movement.z = -1.0f;
            } else if (IsKeyDown(GLFW_KEY_S)) {
                movement.z = 1.0f;
            }
            if (IsKeyDown(GLFW_KEY_A)) {
                movement.x = -1.0f;
            } else if (IsKeyDown(GLFW_KEY_D)) {
                movement.x = 1.0f;
            }
            if (IsKeyDown(GLFW_KEY_SPACE)) {
                movement.y = 1.0f;
            } else if (IsKeyDown(GLFW_KEY_LEFT_CONTROL)) {
                movement.y = -1.0f;
            }
            if (movement.lengthSquared() != 0.0f) {
                cameraPosition.add(movement.normalize(deltaTime * 10.0f).mulPosition(rotMatrix));
            }
            //Render

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            Matrix4f view = new Matrix4f().translate(cameraPosition).rotate(rotMatrix.getNormalizedRotation(new Quaternionf())).invert();

            Matrix4f modelView = new Matrix4f()
                .mul(view)
                .translate(0.0f, 0.0f, -5.0f)
                .rotate(globalTime, 0.0f, 1.0f, 0.0f);

            testShader.bind();
            try {
                testShader.setUniform(Shader.Uniform.MODELVIEW_MATRIX, modelView);
                testShader.setUniform(Shader.Uniform.PROJECTION_MATRIX, projection);
                //testShader.setUniform(Shader.Uniform.TIME, globalTime);
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }

            testMesh.bind();
            testMesh.draw();
            testMesh.unbind();

            modelView = new Matrix4f()
                .mul(view)
                .translate(0.0f, 0.0f, 0.0f)
                .rotate(globalTime, 0.0f, 1.0f, 0.0f);
                // .translate(0.0f, 0.0f, -100.0f)
                // .rotate((float)PI / 2.0f, 1.0f, 0.0f, 0.0f);

            try {
                testShader.setUniform(Shader.Uniform.MODELVIEW_MATRIX, modelView);
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }

            Mesh terrainMesh = terrain.GetMesh();
            terrainMesh.bind();
            terrainMesh.draw();
            terrainMesh.unbind();

            glfwSwapBuffers(window);
            keyPress.clear();
            glfwPollEvents();
        }

        testMesh.close();
        testShader.close();

        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }
}
