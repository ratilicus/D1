/*
Height Map fragment Shader
By: Adam Dybczak
Base File: Common.MatDefs.Light.Lighting.frag
*/


#import "Common/ShaderLib/BlinnPhongLighting.glsllib"
#import "Common/ShaderLib/Lighting.glsllib"

uniform float m_Timer;

uniform vec4 g_LightDirection;
uniform sampler2D m_DiffuseMap;

in vec2 texCoord;
in vec3 AmbientSum;
in vec4 DiffuseSum;
in vec3 SpecularSum;

in vec3 vPosition;
in vec3 vViewDir;
in vec4 vLightDir;
in vec3 lightVec;

in vec3 vNormal;
out vec4 FragColor;

uniform float m_AlphaDiscardThreshold;
uniform float m_Shininess;


vec4 getColor(sampler2D tex, vec2 tc) {
    vec2 t1 = vec2(mod(tc[1] * 10.0, 1.0), fract(tc[0] * 10.0));
    vec2 t2 = vec2(mod(tc[0] * 100.0, 1.0), fract(tc[1] * 100.0));
    vec2 t3 = vec2(mod(tc[0] * 1000.0, 1.0), fract(tc[1] * 1000.0));

    vec4 c1 = texture2D(tex, tc);
    vec4 c2 = texture2D(tex, t1)-0.5;
    vec4 c3 = texture2D(tex, t2)-0.5;
    vec4 c4 = texture2D(tex, t3)-0.5;
    float h = 0.9*c1.r + 0.25*c2.g + 0.05*c3.b + 0.02*c4.r;
    float c = 0.9*c1.g + 0.25*c2.b + 0.3*c3.r + 0.2*c4.g;
    float r = 0.9*c1.b + 0.25*c2.r + 0.3*c3.g + 0.6*c4.b;

    // TODO: turn these into uniform + abstract the height layers
    vec3 deep_water = vec3(0.0, 0.0, 0.2);
    vec3 water = vec3(0.2, 0.2, 0.51);
    vec3 sand = vec3(0.86, 0.82, 0.71);
    vec3 grass = vec3(0.72, 0.76, 0.45);
    vec3 dark_grass = vec3(0.18, 0.27, 0.07);
    vec3 ice = vec3(0.9, 0.9, 0.9);
    float b_dwater = 0.5;
    float b_water = 0.55;
    float b_sand = 0.58;

    vec4 color;
    color.a = 1.0;
    if (h < b_dwater) {
      // deep water
      color.rgb = mix(deep_water, water, (h+ 0.1*c3.b)/b_dwater)*c;
      color.a = 1.5 + 0.15* sin(10f*m_Timer*c);

    } else if (h < b_water) {
      // water
      float s = (h - b_dwater)*10.0;
      color.rgb = mix(water, sand, s)*c;
      color.a = 1.5 + 0.15* sin(8f*m_Timer*c);

    } else if (h < b_sand) {
      // sand -> grass
      color.rgb = mix(sand, grass, c)*r;
      color.a = c;

    } else {
      // grass
      color.rgb = mix(grass, dark_grass, c)*r;
      color.a = c;
    }

    float o = clamp(0.026666 * gl_FragCoord.z / gl_FragCoord.w,0.0, 0.8);

    vec4 final_color = vec4(o)*o + color*(1.0 - o);
    final_color.a = color.a;

    return final_color;
}


void main(){
    vec4 diffuseColor = getColor(m_DiffuseMap, texCoord);
    vec4 specularColor = vec4(1.0)*diffuseColor.a;
    diffuseColor.a = 1.0;

    float alpha = DiffuseSum.a * diffuseColor.a;

    vec4 lightDir = vLightDir;
    lightDir.xyz = normalize(lightDir.xyz);
    vec3 viewDir = normalize(vViewDir);
    float spotFallOff = 1.0;

    // allow use of control flow
    if(g_LightDirection.w != 0.0){
        spotFallOff =  computeSpotFalloff(g_LightDirection, lightVec);
        if(spotFallOff <= 0.0){
            FragColor.rgb = AmbientSum * diffuseColor.rgb;
            FragColor.a   = alpha;
            return;
        }
    }

    vec2   light = computeLighting(vNormal, viewDir, lightDir.xyz, lightDir.w * spotFallOff, m_Shininess) ;

    FragColor.rgb =  AmbientSum       * diffuseColor.rgb  +
                     DiffuseSum.rgb   * diffuseColor.rgb  * vec3(light.x) +
                     SpecularSum * specularColor.rgb * vec3(light.y);
    FragColor.a = alpha;
}
