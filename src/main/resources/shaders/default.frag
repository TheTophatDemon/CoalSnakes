#version 330

//uniform float uTime;
uniform vec3 uAmbientLight;
uniform vec3 uSunDirection;

in vec3 vColor;
in vec3 vNormal;

out vec4 fragColor;

void main() {
    vec3 normal = normalize(vNormal);
    float lighting = (dot(normal, uSunDirection) + 1.0) / 2.0;
    fragColor = vec4(vColor * mix(uAmbientLight, vec3(1.0), lighting), 1.0);
}