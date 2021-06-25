package com.tophatdemon;

import java.util.Map;
import java.util.HashMap;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL33.*;

public class Shader implements AutoCloseable {
    private static String ERR_SHADER_VERT = """
        #version 330

        layout (location=0) in vec3 position;

        uniform mat4 uModelView;
        uniform mat4 uProjection;

        void main() {
            gl_Position = uProjection * uModelView * vec4(position, 1.0);
        }
    """;
    private static String ERR_SHADER_FRAG = """
        #version 330
        
        out vec4 fragColor;
        
        void main() {
            fragColor = vec4(1.0, 0.0, 1.0, 1.0);
        }
    """;

    public static enum Uniform {
        MODEL_MATRIX("uModel"),
        VIEW_MATRIX("uView"),
        MODELVIEW_MATRIX("uModelView"),
        PROJECTION_MATRIX("uProjection"),
        TIME("uTime"),
        AMBIENT_LIGHT("uAmbientLight"),
        SUN_DIRECTION("uSunDirection"),
        ;
        
        private String key;
        private Uniform(String key) {
            this.key = key;
        }
        @Override
        public String toString(){
            return key;
        }
    }

    private Map<Uniform, Integer> uniformLocations = new HashMap<Uniform, Integer>();
    
    private int program;
    private int vertexShader;
    private int fragmentShader;

    public static Shader LoadFromFile(String vertexPath, String fragmentPath) {
        String vertexSource = ERR_SHADER_VERT;
        String fragmentSource = ERR_SHADER_FRAG;

        try {
            vertexSource = Assets.getFileContents("shaders/default.vert");
            fragmentSource = Assets.getFileContents("shaders/default.frag");
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }

        return new Shader(vertexSource, fragmentSource);
    }

    public Shader(String vertexSource, String fragmentSource) {
        vertexShader = glCreateShader(GL_VERTEX_SHADER);
        compile(vertexShader, vertexSource);
        fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        compile(fragmentShader, fragmentSource);

        program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glLinkProgram(program);

        if (glGetProgrami(program, GL_LINK_STATUS) != GL_TRUE) {
            System.err.format("%s\n", glGetProgramInfoLog(program));
            throw new IllegalStateException("Failed to link program");
        }

        if (vertexShader != 0) glDetachShader(program, vertexShader);
        if (fragmentShader != 0) glDetachShader(program, fragmentShader);

        glValidateProgram(program);
        if (glGetProgrami(program, GL_VALIDATE_STATUS) == GL_FALSE) {
            System.err.println("Potentially invalid shader code: " + glGetProgramInfoLog(program, 1024));
        }
    }

    private void compile(int shader, String source) {
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            System.err.format("%s\n", glGetProgramInfoLog(shader));
            //throw new IllegalStateException("Failed to compile shader");
        }
    }

    public void bind() {
        glUseProgram(program);
    }

    private int getUniformLoc(Uniform key) throws Exception {
        if (uniformLocations.containsKey(key)) {
            return uniformLocations.get(key);
        }
        int loc = glGetUniformLocation(program, key.toString());
        if (loc < 0) {
            throw new Exception("Could not find uniform for: " + key);
        }
        uniformLocations.put(key, loc);
        return loc;
    }

    public void setUniform(Uniform key, Integer val) throws Exception {
        glUniform1i(getUniformLoc(key), val);
    }
    public void setUniform(Uniform key, Float val) throws Exception {
        glUniform1f(getUniformLoc(key), val);
    }
    public void setUniform(Uniform key, Matrix4f val) throws Exception {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer matf = stack.mallocFloat(16);
            val.get(matf);
            glUniformMatrix4fv(getUniformLoc(key), false, matf);
        }
    }
    public void setUniform(Uniform key, Vector3f val) throws Exception {
        glUniform3f(getUniformLoc(key), val.x, val.y, val.z);
    }

    @Override
    public void close() {
        if (vertexShader > 0) {
            glDeleteShader(vertexShader);
        }
        if (fragmentShader > 0) {
            glDeleteShader(fragmentShader);
        }
        if (program > 0) {
            glDeleteProgram(program);
        }
    }
}
