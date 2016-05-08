/*
Height Map fragment Shader
By: Adam Dybczak
Base File: Common.MatDefs.Light.Lighting.frag
*/


#import "Common/ShaderLib/BlinnPhongLighting.glsllib"
#import "Common/ShaderLib/Lighting.glsllib"
#import "Common/ShaderLib/Instancing.glsllib"

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

//in vec3 vNormal;
out vec4 FragColor;

in vec3 p;
in vec3 pu;
in vec3 pv;
in vec2 tu;
in vec2 tv;

uniform float m_AlphaDiscardThreshold;
uniform float m_Shininess;

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
 
vec3 getHCR(sampler2D tex, vec2 tc) {
    vec2 t1 = vec2(mod(tc[1] * 10.0, 1.0), fract(tc[0] * 10.0));
    vec2 t2 = vec2(mod(tc[0] * 100.0, 1.0), fract(tc[1] * 100.0));
    vec2 t3 = vec2(mod(tc[0] * 1000.0, 1.0), fract(tc[1] * 1000.0));

    vec4 c1 = texture2D(tex, tc);
    vec4 c2 = texture2D(tex, t1)-0.5;
    vec4 c3 = texture2D(tex, t2)-0.5;
    vec4 c4 = texture2D(tex, t3)-0.5;
    return vec3(
        0.9*c1.r + 0.1*c2.g + 0.05*c3.b + 0.02*c4.r,
        0.9*c1.g + 0.25*c2.b + 0.3*c3.r + 0.2*c4.g,
        0.9*c1.b + 0.25*c2.r + 0.3*c3.g + 0.6*c4.b);
}

vec4 getColor(vec3 hcr) {

    float h = hcr.x;
    float c = hcr.y;
    float r = hcr.z;

    vec4 color;
    color.a = 1.0;
    if (h < b_dwater) {
      // deep water
      color.rgb = mix(deep_water, water, (h+ 0.1*r)/b_dwater)*c;
      color.a = 1.5 + 0.15* sin(10f*m_Timer*c);

    } else if (h < b_water) {
      // water
      float s = (h - b_dwater)*10.0;
      color.rgb = mix(water, sand, s)*c;
      color.a = 1.5 + 0.15* sin(8f*m_Timer*c);

    } else if (h < b_sand) {
      // sand -> grass
      color.rgb = mix(sand, grass, c)*r;
      color.a = 0.25+c;

    } else {
      // grass
      color.rgb = mix(grass, dark_grass, c)*r;
      color.a = 0.25+c;
    }

    return color;
}

float getHeight(float h) {
    h = clamp(h, b_water, 1.0)-b_water;
    return 1.0+0.1*h;
}

void main(){
    vec3 hcr = getHCR(m_DiffuseMap, texCoord);
    vec4 color = getColor(hcr);

    // quantizing the level for display purposes to clearly see borders
    float o = int(clamp(0.326666 * gl_FragCoord.z / gl_FragCoord.w, 1.0, 9.0))/10.0;
    float o1 = 1.0 - o;

    vec4 diffuseColor = mix(color, vec4(0.8), o*0.8);

    vec4 specularColor = vec4(1.0)*color.a;
    diffuseColor.a = 1.0;


    // get height at nearby coords and figure out normal 
    // TODO: fix issue with borders
    float s = mix(5.0, 500.0, o1);
    vec3 vNormal;

    if (hcr.x < b_water) {
        vNormal = normalize(TransformNormal(p));
    } else {
        float h = getHeight(hcr.x);

        float tuh = getHeight(getHCR(m_DiffuseMap, texCoord+tu/s).x);
        float tvh = getHeight(getHCR(m_DiffuseMap, texCoord+tv/s).x);

        vec3 up = p*h;
        vNormal = normalize(TransformNormal(cross((p + pu/s) * tuh - up,
                             (p + pv/s) * tvh - up)));
    }

    float alpha = DiffuseSum.a * color.a;

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
