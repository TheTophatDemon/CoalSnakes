package com.tophatdemon;

import org.lwjgl.system.MemoryUtil;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL33.*;

public class Mesh implements AutoCloseable {
    protected static final int ATTRIBUTE_INDEX_POS = 0;
    protected static final int ATTRIBUTE_INDEX_COLOR = 1;

    protected int vertexArrayObject;

    protected int positionBuffer;
    protected int colorBuffer;
    protected int indexBuffer;

    protected int numIndices;
    protected int numVertexBuffers; //Not counting index buffer

    public Mesh() {
        vertexArrayObject = glGenVertexArrays();
    }

    public void setVertexPositions(float ...pos) {
        if (positionBuffer != 0) {
            glDeleteBuffers(positionBuffer);
            --numVertexBuffers;
        }
        positionBuffer = generateFloatVBO(ATTRIBUTE_INDEX_POS, 3, pos);
    }
    
    public void setColors(float ...cols) {
        if (colorBuffer != 0) {
            glDeleteBuffers(colorBuffer);
            --numVertexBuffers;
        }
        colorBuffer = generateFloatVBO(ATTRIBUTE_INDEX_COLOR, 3, cols);
    }

    private int generateFloatVBO(int attrIdx, int attrSize, float ...data) {
        FloatBuffer buffer = MemoryUtil.memAllocFloat(data.length);
        buffer.put(data).flip();

        glBindVertexArray(vertexArrayObject);
        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
        
        glVertexAttribPointer(attrIdx, attrSize, GL_FLOAT, false, 0, 0);
        ++numVertexBuffers;
        MemoryUtil.memFree(buffer);

        return vbo;

    }

    public void setIndices(int ...inds) {
        numIndices = inds.length;
        
        IntBuffer data = MemoryUtil.memAllocInt(inds.length);
        data.put(inds).flip();

        if (indexBuffer != 0) {
            glDeleteBuffers(indexBuffer);
        }

        glBindVertexArray(vertexArrayObject);
        indexBuffer = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, data, GL_STATIC_DRAW);

        MemoryUtil.memFree(data);
    }

    public void bind() {
        glBindVertexArray(vertexArrayObject);
        for (int i = 0; i < numVertexBuffers; ++i) glEnableVertexAttribArray(i);
    }

    public void draw() {
        glDrawElements(GL_TRIANGLES, numIndices, GL_UNSIGNED_INT, 0);
    }

    public void unbind() {
        for (int i = 0; i < numVertexBuffers; ++i) glDisableVertexAttribArray(i);
    }

    @Override
    public void close() {
        if (vertexArrayObject > 0) glDeleteVertexArrays(vertexArrayObject);
        if (positionBuffer > 0) glDeleteBuffers(positionBuffer);
        if (colorBuffer > 0) glDeleteBuffers(colorBuffer);
        if (indexBuffer > 0) glDeleteBuffers(indexBuffer);
    }
}
