#version 330

layout (location=0) in vec3 position;
layout (location=1) in vec3 color;

uniform mat4 uModelView;
uniform mat4 uProjection;

varying vec3 vColor;

void main() {
    vColor = color;
    gl_Position = uProjection * uModelView * vec4(position, 1.0);
}