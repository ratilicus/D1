/*
Height Map Vertex Shader
By: Adam Dybczak
Base File: Common.MatDefs.Light.Lighting.vert
*/


#import "Common/ShaderLib/Instancing.glsllib"
#import "Common/ShaderLib/Lighting.glsllib"

uniform vec4 m_Ambient;
uniform vec4 m_Diffuse;
uniform vec4 m_Specular;
uniform float m_Shininess;

uniform vec4 g_LightColor;
uniform vec4 g_LightPosition;
uniform vec4 g_AmbientLightColor;

varying vec2 texCoord;
varying vec3 AmbientSum;
varying vec4 DiffuseSum;
varying vec3 SpecularSum;

attribute vec3 inPosition;
attribute vec2 inTexCoord;

varying vec3 lightVec;

uniform sampler2D m_DiffuseMap;

varying vec4 worldPos;
varying vec3 vViewDir;
varying vec4 vLightDir;

void main(){
    vec4 modelSpacePos = vec4(inPosition, 1.0);
    worldPos = TransformWorldViewProjection(modelSpacePos);
    gl_Position = vec4(inPosition, 1.0);

    texCoord = inTexCoord;

    vec3 wvPosition = TransformWorldView(modelSpacePos).xyz;
    vViewDir = normalize(-wvPosition);

    vec4 wvLightPos = (g_ViewMatrix * vec4(g_LightPosition.xyz,clamp(g_LightColor.w,0.0,1.0)));
    wvLightPos.w = g_LightPosition.w;
    vec4 lightColor = g_LightColor;

    lightComputeDir(wvPosition, lightColor.w, wvLightPos, vLightDir, lightVec);

    AmbientSum  = (m_Ambient  * g_AmbientLightColor).rgb;
    DiffuseSum  =  m_Diffuse  * vec4(lightColor.rgb, 1.0);
    SpecularSum = (m_Specular * lightColor).rgb;

}