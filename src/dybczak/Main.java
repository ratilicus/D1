/*
Height Map fragment Shader
By: Adam Dybczak
Based On: JME Basic Game Project
*/


package dybczak;
 
import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import static com.jme3.math.FastMath.sin;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.filters.LightScatteringFilter;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Sphere;
import com.jme3.shadow.CompareMode;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.util.TangentBinormalGenerator;
 
public class Main extends SimpleApplication implements ActionListener  {
    private int AZ=0, AX=0, AY=0, RZ=0;
    private final Vector3f camDir = new Vector3f();
    private final Vector3f camLeft = new Vector3f();
    private final Vector3f camUp = new Vector3f();
    private final Vector3f vel = new Vector3f();
    float qa=0;
    Quaternion q = new Quaternion(0.1f, 0.9f, 0f, 1f);
    Geometry sphereGeo,  sphereGeo2;
    Node planetNode;
    Material sphereMat;
    float timer;
    
    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }
 
    private void addMapping(String map, int key) {
        inputManager.addMapping(map , new KeyTrigger(key));
        inputManager.addListener(this, map);
    }
    
    private void setUpKeys() {
        addMapping("U", KeyInput.KEY_W);    // Forward
        addMapping("D", KeyInput.KEY_S);    // Back
        addMapping("L", KeyInput.KEY_A);    // Strafe Left
        addMapping("R", KeyInput.KEY_D);    // Strafe Right
        addMapping("F", KeyInput.KEY_SPACE);    // Strafe Up
        addMapping("B", KeyInput.KEY_LCONTROL); // Strafe Down
        addMapping("P", KeyInput.KEY_Q);    // Roll Left (Port)
        addMapping("S", KeyInput.KEY_E);    // Roll Right (Starboard)
    }

    @Override
    public void onAction(String binding, boolean isPressed, float tpf) {
        switch (binding.charAt(0)) {
            case 'F':
                AZ = isPressed ? 1: 0;
                break;
            case 'B':
                AZ = isPressed ? -1: 0;
                break;
            case 'L':
                AX = isPressed ? 1: 0;
                break;
            case 'R':
                AX = isPressed ? -1: 0;
                break;
            case 'U':
                AY = isPressed ? 1: 0;
                break;
            case 'D':
                AY = isPressed ? -1: 0;
                break;
            case 'P':
                RZ = isPressed ? 1: 0;
                break;
            case 'S':
                RZ = isPressed ? -1: 0;
                break;
        }

    }
    
    @Override
    public void simpleInitApp() {

        Geometry sun_sphere = new Geometry("Sun", new Sphere(8, 8, 15f));
        Material sun_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        sun_mat.setColor("Color", ColorRGBA.White);
        sun_mat.setReceivesShadows(false);
        sun_mat.getAdditionalRenderState().setDepthWrite(false);
        //sun_mat.getAdditionalRenderState().setDepthTest(false);
        sun_sphere.setMaterial(sun_mat);        
        rootNode.attachChild(sun_sphere);
        sun_sphere.setShadowMode(ShadowMode.Off);
        sun_sphere.setQueueBucket(RenderQueue.Bucket.Sky);
        sun_sphere.setLocalTranslation(1000,0,0); // Move it a bit
        
        
        Sphere sphereMesh = new Sphere(32,32, 2f);
        sphereMesh.setTextureMode(Sphere.TextureMode.Projected); // better quality on spheres
        TangentBinormalGenerator.generate(sphereMesh);           // for lighting effect

        Sphere sphereMesh2 = new Sphere(16,16, 2f);
        sphereMesh2.setTextureMode(Sphere.TextureMode.Projected); // better quality on spheres
        TangentBinormalGenerator.generate(sphereMesh2);           // for lighting effect

//        Material sphereMat = new Material(assetManager, "MatDefs/simple.j3md");
//        sphereMat.getAdditionalRenderState().setWireframe(true);
        
        //Material sphereMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        sphereMat = new Material(assetManager, "MatDefs/HM.j3md");
        sphereMat.setTexture("DiffuseMap", assetManager.loadTexture("Textures/c3.png"));
        //sphereMat.setTexture("NormalMap", assetManager.loadTexture("Textures/c4.png"));
sphereMat.setFloat("SphereRadius", 3f);

        ColorRGBA color = new ColorRGBA(.7f, .7f, 1f, 1f);
        sphereMat.setBoolean("UseMaterialColors",true);    
        sphereMat.setColor("Ambient", color);
        sphereMat.setColor("Diffuse", color);
        sphereMat.setColor("Specular", color.mult(.5f));
        sphereMat.setFloat("Shininess", 116f);  // [0,128]
        sphereMat.setReceivesShadows(true);
//sphereMat.getAdditionalRenderState().setWireframe(true);
        
        Material sphereMat2 = sphereMat.clone();
        color = new ColorRGBA(.7f, .7f, .7f, 1f);
        sphereMat2.setColor("Ambient", color);
        sphereMat2.setColor("Diffuse", color);
        sphereMat2.setColor("Specular", color.mult(.1f));
sphereMat2.setFloat("SphereRadius", 1.0f);
        
        planetNode = new Node("PlanetSystem");
        rootNode.attachChild(planetNode);
        
        sphereGeo = new Geometry("Earth", sphereMesh);
sphereGeo.getMesh().setMode(Mesh.Mode.Patch);
sphereGeo.getMesh().setPatchVertexCount(3);

        sphereGeo.setMaterial(sphereMat);
        sphereGeo.setLocalTranslation(0,0,0); // Move it a bit
        sphereGeo.rotate(1.6f, 0 , 0);          // Rotate it a bit
        sphereGeo.setLocalScale(16.0f);
        sphereGeo.setShadowMode(ShadowMode.CastAndReceive);
        sphereGeo.setQueueBucket(RenderQueue.Bucket.Opaque);
        planetNode.attachChild(sphereGeo);

        sphereGeo2 = new Geometry("Moon", sphereMesh2);
        sphereGeo2.setMaterial(sphereMat2);
        sphereGeo2.setLocalTranslation(128,0,0); // Move it a bit
        sphereGeo2.rotate(1.6f, 0, 0);          // Rotate it a bit
        sphereGeo2.setLocalScale(4f);
        sphereGeo2.setShadowMode(ShadowMode.CastAndReceive);
        sphereGeo2.setQueueBucket(RenderQueue.Bucket.Opaque);
sphereGeo2.getMesh().setMode(Mesh.Mode.Patch);
sphereGeo2.getMesh().setPatchVertexCount(3);
               
        planetNode.attachChild(sphereGeo2);

        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(.15f));
        rootNode.addLight(al);
        
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-1,0,0).normalizeLocal());
        sun.setColor(ColorRGBA.White);
        rootNode.addLight(sun);
        viewPort.setBackgroundColor(new ColorRGBA(.1f, .1f, .1f, 0.1f));
        //flyCam.setMoveSpeed(100);
        setUpKeys();
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);

        cam.setLocation(new Vector3f(60, 0, 0));
        
        LightScatteringFilter lsFilter = new LightScatteringFilter(sun.getDirection().mult(-1000f));
        //lsFilter.setLightDensity(0.9f);
        lsFilter.setBlurStart(0.1f);
        lsFilter.setNbSamples(64);
        lsFilter.setLightPosition(new Vector3f(500, 0, 0));
        fpp.addFilter(lsFilter);

        /*
        FogFilter fog=new FogFilter();
        fog.setFogColor(new ColorRGBA(1f,1f,.7f, 1.0f));
        //fog.setFogDistance(200);
        fog.setFogDensity(0.5f);
        fpp.addFilter(fog);
        */
        
        DirectionalLightShadowFilter dlsf = new DirectionalLightShadowFilter(assetManager, 1024, 3);
        dlsf.setLight(sun);
        dlsf.setEdgeFilteringMode(EdgeFilteringMode.PCF4);
        dlsf.setShadowCompareMode(CompareMode.Hardware);
        dlsf.setEnabled(true);
        dlsf.setShadowIntensity(0.4f);
        dlsf.setFlushQueues(true);
        fpp.addFilter(dlsf);
        
        BloomFilter bFilter = new BloomFilter();
        fpp.addFilter(bFilter);
        
        this.viewPort.addProcessor(fpp); 
    }
  

    @Override
    public void simpleUpdate(float tpf) {
        timer += tpf;
        sphereMat.setFloat("Timer", timer);

        camDir.set(cam.getDirection()).multLocal(tpf);
        camLeft.set(cam.getLeft()).multLocal(tpf);
        camUp.set(cam.getUp()).multLocal(tpf);
        //vel.set(0, 0, 0);
        vel.multLocal(0.995f);
        // TODO: add roll
        // FIXME: camera gets goofy if flipped upside down
        if (AZ!=0) {
            vel.addLocal(camDir.mult(AZ*.3f));
        }
        if (AX!=0) {
            vel.addLocal(camLeft.mult(AX*.3f));
        }
        if (AY!=0) {
            vel.addLocal(camUp.mult(AY*.3f));
        }
        cam.setLocation(cam.getLocation().add(vel));
//        planetNode.rotate(0f, 0.125f*tpf, 0);
    }
}
