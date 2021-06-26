#version 330

layout (location=0) in vec3 position;
layout (location=1) in vec3 color;
layout (location=2) in vec3 normal;

uniform mat4 uModel;
uniform mat4 uView;
uniform mat4 uProjection;

out vec3 vColor;
out vec3 vNormal;

void main() {
    vColor = color;
    vNormal = (mat4(uModel[0], uModel[1], uModel[2], vec4(0.0, 0.0, 0.0, 1.0)) * vec4(normal, 1.0)).xyz;
    gl_Position = uProjection * uView * uModel * vec4(position, 1.0);
}