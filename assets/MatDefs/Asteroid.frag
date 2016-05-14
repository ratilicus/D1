/*
Height Map fragment Shader
By: Adam Dybczak
Base File: Common.MatDefs.Light.Lighting.frag
*/


#import "Common/ShaderLib/BlinnPhongLighting.glsllib"
#import "Common/ShaderLib/Lighting.glsllib"
#import "Common/ShaderLib/Instancing.glsllib"

uniform float m_Timer;
uniform float m_HeightMultiplier;

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
in vec3 pw;
in vec2 tu;
in vec2 tv;
in vec2 tw;

uniform float m_AlphaDiscardThreshold;
uniform float m_Shininess;

// TODO: turn these into uniform + abstract the height layers
vec3 deep_water = vec3(0.0, 0.0, 0.2);
vec3 water = vec3(0.2);
vec3 sand = vec3(0.86);
vec3 grass = vec3(0.72);
vec3 dark_grass = vec3(0.18);
float b_dwater = 0.5;
float b_water = 0.55;
float b_sand = 0.58;
 

vec4 getHCR(sampler2D tex, vec2 tc, int loops) {
    vec4 c = texture2D(tex, tc);
    float m = 10.0;
    float d = 0.1;
    for (int i=1; i <= loops; i++) {
        vec3 sc = (texture2D(tex, fract(tc * m)).rgb-0.5) * d;
        c.rgb += vec3(sc[int(mod(i, 3))], sc[int(mod(i+1, 3))],sc[int(mod(i+2, 3))]);
        m *= 10.0;
        d /= 3.0;
    }
    return c;
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

    } else if (h < b_water) {
      // water
      float s = (h - b_dwater)*10.0;
      color.rgb = mix(water, sand, s)*c;

    } else if (h < b_sand) {
      // sand -> grass
      color.rgb = mix(sand, grass, c)*r;
      color.a = 1.3;

    } else {
      // grass
      color.rgb = mix(grass, dark_grass, c)*r;
    color.a = 1.5;
    }

    return color;
}

float getHeight(float h) {
    return 1.0+m_HeightMultiplier*h;
}

void main(){
    // quantizing the level for display purposes to clearly see borders
    int o = int(clamp(0.326666 * gl_FragCoord.z / gl_FragCoord.w / 2, 1.0, 5.0));
    float of = o / 5.0;

    vec3 hcr = getHCR(m_DiffuseMap, texCoord, 6-o).xyz;
    vec4 color = getColor(hcr);

    vec4 diffuseColor = mix(color, vec4(0.8), of*0.2);

    vec4 specularColor = vec4(1.0)*color.a*(1.0-of*0.7);


    // get height at nearby coords and figure out normal 
    // TODO: fix issue with borders
    float s = mix(250.0, 500.0, 1.0 - of);
    vec3 vNormal;

    float h = getHeight(hcr.x);

    float tuh = getHeight(getHCR(m_DiffuseMap, texCoord+tu/s, 6-o).x);
    float tvh = getHeight(getHCR(m_DiffuseMap, texCoord+tv/s, 6-o).x);
    float twh = getHeight(getHCR(m_DiffuseMap, texCoord+tw/s, 6-o).x);

    vec3 up = p*h;
    vNormal = normalize(TransformNormal(normalize(
                cross((p + pu/s) * tuh - up, (p + pv/s) * tvh - up)+
                cross((p + pv/s) * tvh - up, (p + pw/s) * twh - up)+
                cross((p + pw/s) * twh - up, (p + pu/s) * tuh - up)
        )));

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

    vec2   light = computeLighting(vNormal, viewDir, lightDir.xyz, lightDir.w * spotFallOff, m_Shininess);

    FragColor.rgb =  AmbientSum     * diffuseColor.rgb  +
                     DiffuseSum.rgb * diffuseColor.rgb  * vec3(light.x) +
                     SpecularSum    * specularColor.rgb * vec3(light.y);
    FragColor.a = alpha;
}
