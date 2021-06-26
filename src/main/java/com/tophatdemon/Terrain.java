package com.tophatdemon;

import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.EnumMap;
import org.joml.SimplexNoise;
import org.joml.Vector3f;
import org.joml.Vector3i;

import com.tophatdemon.MarchingCubes.Edge;

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

    public void SetPerlinNoiseWeights(float scale, boolean border) {
        Random random = new Random(System.nanoTime());
        float offset = random.nextFloat() * 100.0f;
        for (int z = 0; z < gridRows + 1; ++z) {
            for (int y = 0; y < gridLayers + 1; ++y) {
                for (int x = 0; x < gridCols + 1; ++x) {
                    if (border && (z == 0 || x == 0 || y == 0 || z == gridRows || y == gridLayers || x == gridCols)) {
                        gridWeights[x][y][z] = 1.0f;
                    } else {
                        gridWeights[x][y][z] = (SimplexNoise.noise((x + offset) * scale, (y + offset) * scale, (z + offset) * scale) + 1.0f) / 2.0f;
                    }
                }
            }
        }
        meshRegen = true;
    }

    public void SetDebugWeights() {
        for (int z = 0; z < gridRows + 1; ++z) {
            for (int y = 0; y < gridLayers + 1; ++y) {
                for (int x = 0; x < gridCols + 1; ++x) {
                    gridWeights[x][y][z] = (x % 2 == 0 && y % 2 == 0 && z % 2 == 0) ? 1.0f : 0.0f;
                }
            }
        }
    }

    public void SetIsoLevel(float isoLevel) {
        this.isoLevel = Math.max(0.0f, Math.min(1.0f, isoLevel));
        meshRegen = true;
    }

    private static class Vertex {
        Vector3i gridPosition;
        Vector3f position;
        Vector3f color;
        Vector3f normal;
        int multiplicity = 1; //Number of triangles that contain this vertex
        public Vertex(Vector3i gridPos, Vector3f pos, Vector3f col, Vector3f norm) {
            gridPosition = gridPos; position = pos; color = col; normal = norm;
        }
        public Vertex(Vector3i gridPos, Vector3f pos, Vector3f col) {
            this(gridPos, pos, col, new Vector3f(0.0f, 0.0f, 0.0f));
        }
    }

    private void generateMesh() {
        long timerStart = System.currentTimeMillis();

        Vector3f offset = new Vector3f(
            -((gridCols + 1) * gridSpacing) / 2.0f,
            -((gridLayers + 1) * gridSpacing) / 2.0f,
            -((gridRows + 1) * gridSpacing) / 2.0f
        );

        List<Vertex> verts = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        //TODO: Make smoother normals

        Random random = new Random(System.currentTimeMillis());

        Map<Edge, Integer> edgeIndices = new EnumMap<>(Edge.class);
        for (int z = 0; z < gridRows; ++z) {
            for (int y = 0; y < gridLayers; ++y) {
                for (int x = 0; x < gridCols; ++x) {
                    int mask = 0;
                    if (gridWeights[x+0][y+0][z+0] < isoLevel) mask |= 1 << 0;
                    if (gridWeights[x+1][y+0][z+0] < isoLevel) mask |= 1 << 1;
                    if (gridWeights[x+1][y+0][z+1] < isoLevel) mask |= 1 << 2;
                    if (gridWeights[x+0][y+0][z+1] < isoLevel) mask |= 1 << 3;
                    if (gridWeights[x+0][y+1][z+0] < isoLevel) mask |= 1 << 4;
                    if (gridWeights[x+1][y+1][z+0] < isoLevel) mask |= 1 << 5;
                    if (gridWeights[x+1][y+1][z+1] < isoLevel) mask |= 1 << 6;
                    if (gridWeights[x+0][y+1][z+1] < isoLevel) mask |= 1 << 7;

                    edgeIndices.clear();
                    for (int i = 0; i < MarchingCubes.SHAPES[mask].length; ++i) {
                        Edge e = MarchingCubes.SHAPES[mask][i];
                        if (!edgeIndices.containsKey(e)) {
                            float isoVal0 = gridWeights[x+e.offset.x][y+e.offset.y][z+e.offset.z];
                            float isoVal1 = gridWeights[x+e.offset.x+e.direction.x][y+e.offset.y+e.direction.y][z+e.offset.z+e.direction.z];
                            Vector3f interp_ofs = e.GetInterpolatedPosition(isoLevel, isoVal0, isoVal1);
                            Vector3f pos = new Vector3f(x, y, z).add(interp_ofs);
                            edgeIndices.put(e, verts.size()); //Associate this vertex's index to the edge type

                            Vector3f color = new Vector3f(0.5f, 0.25f, 0.1f);
                            verts.add(new Vertex(new Vector3i(x, y, z), pos.mul(gridSpacing).add(offset), color));
                        } else {
                            verts.get(edgeIndices.get(e)).multiplicity++;
                        }
                        indices.add(edgeIndices.get(e));

                        //Calculate per-triangle normals
                        if (i % 3 == 2) {
                            Vertex v0 = verts.get(edgeIndices.get(MarchingCubes.SHAPES[mask][i-2]));
                            Vertex v1 = verts.get(edgeIndices.get(MarchingCubes.SHAPES[mask][i-1]));
                            Vertex v2 = verts.get(edgeIndices.get(MarchingCubes.SHAPES[mask][i]));
                            Vector3f e0 = new Vector3f(v1.position).sub(v0.position);
                            Vector3f e1 = new Vector3f(v2.position).sub(v0.position);
                            Vector3f normal = e0.cross(e1);
                            v0.normal.add(new Vector3f(normal).div(v0.multiplicity));
                            v1.normal.add(new Vector3f(normal).div(v1.multiplicity));
                            v2.normal.add(new Vector3f(normal).div(v2.multiplicity));
                        }
                    }
                }
            }
        }


        //TODO: Replace this code with a more convenient set of methods in Mesh
        //Convert the data into primitive arrays to be passed into OpenGL
        float[] pData = new float[verts.size() * 3];
        int pNext = 0;
        float[] cData = new float[verts.size() * 3];
        int cNext = 0;
        float[] nData = new float[verts.size() * 3];
        int nNext = 0;
        for (int i = 0; i < verts.size(); ++i){
            Vertex v = verts.get(i);
            pData[pNext++] = v.position.x;
            pData[pNext++] = v.position.y;
            pData[pNext++] = v.position.z;
            cData[cNext++] = v.color.x;
            cData[cNext++] = v.color.y;
            cData[cNext++] = v.color.z;
            nData[nNext++] = v.normal.x;
            nData[nNext++] = v.normal.y;
            nData[nNext++] = v.normal.z;
        }
        int[] iData = new int[indices.size()];
        int iNext = 0;
        for (int i = 0; i < indices.size(); ++i) {
            iData[iNext++] = indices.get(i);
        }

        mesh.setVertexPositions(pData);
        mesh.setColors(cData);
        mesh.setNormals(nData);
        mesh.setIndices(iData);

        long methodTime = System.currentTimeMillis() - timerStart;
        System.out.printf("Chunk generation took %dms.", methodTime);
    }
}
