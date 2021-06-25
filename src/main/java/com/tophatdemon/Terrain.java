package com.tophatdemon;

import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import org.joml.SimplexNoise;
import org.joml.Vector3f;
import org.joml.Vector3i;

public class Terrain {
    protected Mesh mesh = new Mesh();
    protected int gridCols;
    protected int gridRows;
    protected int gridLayers;
    protected float gridSpacing;
    protected float gridWeights[][][];
    protected boolean meshRegen = true;
    protected float isoLevel = 0.5f;

    public Terrain(int cols, int rows, int layers, float spacing) {
        gridCols = cols;
        gridRows = rows;
        gridLayers = layers;
        gridSpacing = spacing;
        gridWeights = new float[cols+1][layers+1][rows+1];
    }

    public Mesh GetMesh() {
        if (meshRegen) {
            generateMesh();
            meshRegen = false;
        }
        return mesh;
    }

    // private int getFlatVertexIndex(int x, int y, int z) {
    //     return z + y * (gridRows + 1) + x * (gridRows + 1) * (gridLayers + 1);
    // }

    public void SetRandomWeights() {
        Random random = new Random(System.nanoTime());
        for (int z = 0; z < gridRows + 1; ++z) {
            for (int y = 0; y < gridLayers + 1; ++y) {
                for (int x = 0; x < gridCols + 1; ++x) {
                    gridWeights[x][y][z] = random.nextFloat();
                }
            }
        }
        meshRegen = true;
    }

    public void SetPerlinNoiseWeights(float scale) {
        Random random = new Random(System.nanoTime());
        float offset = random.nextFloat() * 100.0f;
        for (int z = 0; z < gridRows + 1; ++z) {
            for (int y = 0; y < gridLayers + 1; ++y) {
                for (int x = 0; x < gridCols + 1; ++x) {
                    gridWeights[x][y][z] = (SimplexNoise.noise((x + offset) * scale, (y + offset) * scale, (z + offset) * scale) + 1.0f) / 2.0f;
                }
            }
        }
        meshRegen = true;
    }

    public void SetIsoLevel(float isoLevel) {
        this.isoLevel = isoLevel;
        meshRegen = true;
    }

    private static class Vertex {
        Vector3f position;
        Vector3f color;
        public Vertex(Vector3f pos, Vector3f col) {
            position = pos; color = col;
        }
    }

    private void generateMesh() {
        Vector3f offset = new Vector3f(
            -(gridCols * gridSpacing) / 2.0f,
            -(gridLayers * gridSpacing) / 2.0f,
            -(gridRows * gridSpacing) / 2.0f
        );

        List<Vertex> verts = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        for (int z = 0; z < gridRows; ++z) {
            for (int y = 0; y < gridLayers; ++y) {
                for (int x = 0; x < gridCols; ++x) {
                    int mask = 0;
                    if (gridWeights[x+0][y+0][z+1] < isoLevel) mask |= 1 << 0;
                    if (gridWeights[x+1][y+0][z+1] < isoLevel) mask |= 1 << 1;
                    if (gridWeights[x+1][y+0][z+0] < isoLevel) mask |= 1 << 2;
                    if (gridWeights[x+0][y+0][z+0] < isoLevel) mask |= 1 << 3;
                    if (gridWeights[x+0][y+1][z+1] < isoLevel) mask |= 1 << 4;
                    if (gridWeights[x+1][y+1][z+1] < isoLevel) mask |= 1 << 5;
                    if (gridWeights[x+1][y+1][z+0] < isoLevel) mask |= 1 << 6;
                    if (gridWeights[x+0][y+1][z+0] < isoLevel) mask |= 1 << 7;

                    float colorGrade = gridWeights[x][y][z];
                    Vector3f color = new Vector3f(colorGrade / 2.0f, colorGrade / 3.0f, colorGrade / 8.0f);

                    //TODO: Prevent the algorithm from making duplicate vertices
                    for (MarchingCubes.Edge e : MarchingCubes.SHAPES[mask]) {
                        float isoVal0 = gridWeights[x+e.offset.x][y+e.offset.y][z+e.offset.z];
                        float isoVal1 = gridWeights[x+e.offset.x+e.direction.x][y+e.offset.y+e.direction.y][z+e.offset.z+e.direction.z];
                        Vector3f pos = new Vector3f(x, y, z).add(e.GetInterpolatedPosition(isoLevel, isoVal0, isoVal1));
                        indices.add(verts.size());
                        verts.add(new Vertex(pos.mul(gridSpacing).sub(offset), color));
                    }
                }
            }
        }

        //Convert the data into primitive arrays to be passed into OpenGL
        float[] pData = new float[verts.size() * 3];
        int pNext = 0;
        float[] cData = new float[verts.size() * 3];
        int cNext = 0;
        int[] iData = new int[verts.size() * 36];
        for (int i = 0; i < verts.size(); ++i){
            Vertex v = verts.get(i);
            pData[pNext++] = v.position.x;
            pData[pNext++] = v.position.y;
            pData[pNext++] = v.position.z;
            cData[cNext++] = v.color.x;
            cData[cNext++] = v.color.y;
            cData[cNext++] = v.color.z;
        }

        mesh.setVertexPositions(pData);
        mesh.setColors(cData);
        mesh.setIndices(iData);
    }
}
