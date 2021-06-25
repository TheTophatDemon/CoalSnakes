#version 330

//uniform float uTime;
uniform vec3 uAmbientLight;
uniform vec3 uSunDirection;

in vec3 vColor;
in vec3 vNormal;

out vec4 fragColor;

void main() {
    vec3 normal = normalize(vNormal);
    float lighting = max(0.0, dot(normal, uSunDirection));
    fragColor = vec4(vColor * mix(uAmbientLight, vec3(1.0), lighting), 1.0);
}