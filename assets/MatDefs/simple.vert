#import "Common/ShaderLib/Instancing.glsllib"

attribute vec3 inPosition;

void main(){
   gl_Position = TransformWorldViewProjection(vec4(inPosition, 1.0));
}
