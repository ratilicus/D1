in vec2 texCoord;
uniform sampler2D m_DiffuseMap;
out vec4 FragColor;


void main(){
// gl_FragColor=vec4(1.0,0.0,1.0,0.5);
 FragColor=texture2D(m_DiffuseMap, texCoord);
}
