/*
Height Map fragment Shader
By: Adam Dybczak
Based On: JME Basic Game Project
*/


package dybczak;
 
import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import static com.jme3.math.FastMath.abs;
import static com.jme3.math.FastMath.sign;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.filters.LightScatteringFilter;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.control.CameraControl.ControlDirection;
import com.jme3.scene.shape.Sphere;
import com.jme3.shadow.CompareMode;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.ui.Picture;
import static java.lang.Float.min;
 
public class Main extends SimpleApplication implements  AnalogListener, ActionListener  {
    private int AZ=0, AX=0, AY=0, RZ=0;
    private float MX=0, MY=0;
    private final Vector3f camDir = new Vector3f(0, 0, 1f);
    private final Vector3f camLeft = new Vector3f(1f, 0, 0);
    private final Vector3f camUp = new Vector3f(0, 1f, 0);
    private final Vector3f vel = new Vector3f(0, 0, 0);
    float qa=0;
    Quaternion q = new Quaternion(0.1f, 0.9f, 0f, 1f);
    Node planetNode, shipNode;
    Material sphereMat;
//    float timer;

    
    Picture staticCursor = new Picture("StaticCursor");
    Picture cursor = new Picture("Cursor");
    private Vector2f cursorPos = new Vector2f(0f, 0f);
    
    
    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }
 
    private void addMapping(String map, int key) {
        inputManager.addMapping(map , new KeyTrigger(key));
        inputManager.addListener(this, map);
    }


    private void setUpKeys() {
        addMapping("U ", KeyInput.KEY_W);    // Forward
        addMapping("D ", KeyInput.KEY_S);    // Back
        addMapping("L ", KeyInput.KEY_A);    // Strafe Left
        addMapping("R ", KeyInput.KEY_D);    // Strafe Right
        addMapping("F ", KeyInput.KEY_SPACE);    // Strafe Up
        addMapping("B ", KeyInput.KEY_LCONTROL); // Strafe Down
        addMapping("P ", KeyInput.KEY_Q);    // Roll Left (Port)
        addMapping("S ", KeyInput.KEY_E);    // Roll Right (Starboard)

   
    
       inputManager.addMapping("ML", new MouseAxisTrigger(MouseInput.AXIS_X, true));
       inputManager.addMapping("MR", new MouseAxisTrigger(MouseInput.AXIS_X, false));
       inputManager.addMapping("MU", new MouseAxisTrigger(MouseInput.AXIS_Y, false));
       inputManager.addMapping("MD", new MouseAxisTrigger(MouseInput.AXIS_Y, true));
       inputManager.addListener(this, "ML");
       inputManager.addListener(this, "MR");
       inputManager.addListener(this, "MU");
       inputManager.addListener(this, "MD");
    
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

    public void onAnalog(String binding, float value, float tpf) {
        //System.out.println(binding +"|"+ value+"|"+tpf);
        switch (binding.charAt(1)) {
            case 'L':
                MX = -value*tpf *10000;
                break;
            case 'R':
                MX = value*tpf *10000;
                break;
            case 'U':
                MY = value*tpf *10000;
                break;
            case 'D':
                MY = -value*tpf *10000;
                break;
        }
    }

    Geometry addPlanet(ColorRGBA color, float heightMultiplier) {
        /* PLANET */
        Sphere sphereMesh = new Sphere(16, 16, 2f);
        //sphereMesh.setTextureMode(Sphere.TextureMode.Projected); // better quality on spheres
        //TangentBinormalGenerator.generate(sphereMesh);           // for lighting effect

        sphereMat = new Material(assetManager, "MatDefs/HM.j3md");
        sphereMat.setTexture("DiffuseMap", assetManager.loadTexture("Textures/c3.png"));
        sphereMat.setFloat("SphereRadius", 2f);
        sphereMat.setFloat("HeightMultiplier", heightMultiplier);
        //sphereMat.setBoolean("UseMaterialColors",true);    
        sphereMat.setColor("Ambient", color);
        sphereMat.setColor("Diffuse", color);
        sphereMat.setColor("Specular", color.mult(.5f));
        sphereMat.setFloat("Shininess", 116f);  // [0,128]
        sphereMat.setReceivesShadows(true);
        //sphereMat.getAdditionalRenderState().setWireframe(true);

        Geometry sphereGeo = new Geometry("Planet", sphereMesh);
        sphereGeo.getMesh().setMode(Mesh.Mode.Patch);
        sphereGeo.getMesh().setPatchVertexCount(3);
        sphereGeo.setMaterial(sphereMat);
        sphereGeo.setShadowMode(ShadowMode.CastAndReceive);
        sphereGeo.setQueueBucket(RenderQueue.Bucket.Opaque);
        // TODO: need a way to pass timer
        
        return sphereGeo;
    }

    Geometry addAsteroid(ColorRGBA color, float heightMultiplier) {
        /* PLANET */
        Sphere sphereMesh = new Sphere(16, 16, 2f);
        //sphereMesh.setTextureMode(Sphere.TextureMode.Projected); // better quality on spheres
        //TangentBinormalGenerator.generate(sphereMesh);           // for lighting effect

        sphereMat = new Material(assetManager, "MatDefs/Asteroid.j3md");
        sphereMat.setTexture("DiffuseMap", assetManager.loadTexture("Textures/c4.png"));
        sphereMat.setFloat("SphereRadius", 2f);
        sphereMat.setFloat("HeightMultiplier", heightMultiplier);
        //sphereMat.setBoolean("UseMaterialColors",true);    
        sphereMat.setColor("Ambient", color);
        sphereMat.setColor("Diffuse", color);
        sphereMat.setColor("Specular", color.mult(.5f));
        sphereMat.setFloat("Shininess", 116f);  // [0,128]
        sphereMat.setReceivesShadows(true);
        //sphereMat.getAdditionalRenderState().setWireframe(true);

        Geometry sphereGeo = new Geometry("Asteroid", sphereMesh);
        sphereGeo.getMesh().setMode(Mesh.Mode.Patch);
        sphereGeo.getMesh().setPatchVertexCount(3);
        sphereGeo.setMaterial(sphereMat);
        sphereGeo.setShadowMode(ShadowMode.CastAndReceive);
        sphereGeo.setQueueBucket(RenderQueue.Bucket.Opaque);
        // TODO: need a way to pass timer
        
        return sphereGeo;
    }

    @Override
    public void simpleInitApp() {
        Geometry sphereGeo;

        planetNode = new Node("PlanetSystem");
        rootNode.attachChild(planetNode);
        
        // SUN
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

        // PLANET
        sphereGeo = addPlanet(new ColorRGBA(.7f, .7f, 1f, 1f), 0.1f);
        sphereGeo.rotate(1.6f, 0 , 0);          // Rotate it a bit
        sphereGeo.setLocalScale(16.0f);
        planetNode.attachChild(sphereGeo);

        // MOON
        sphereGeo = addAsteroid(new ColorRGBA(.7f, .7f, 0.7f, 1f), -0.1f);
        sphereGeo.setLocalTranslation(128,0,0); // Move it a bit
        sphereGeo.rotate(1.6f, 0, 0);          // Rotate it a bit
        sphereGeo.setLocalScale(4f);
        planetNode.attachChild(sphereGeo);
        
        // MOON
        sphereGeo = addAsteroid(new ColorRGBA(.3f, .3f, 0.3f, 1f), -0.2f);
        sphereGeo.setLocalTranslation(128,0,30); // Move it a bit
        sphereGeo.rotate(1.6f, 0, 0);          // Rotate it a bit
        sphereGeo.setLocalScale(2f);
        planetNode.attachChild(sphereGeo);
        
        
        /* LIGHTS AND EFFECTS */
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(.15f));
        rootNode.addLight(al);
        
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-1,0,0).normalizeLocal());
        sun.setColor(ColorRGBA.White);
        rootNode.addLight(sun);
        viewPort.setBackgroundColor(new ColorRGBA(.1f, .1f, .1f, 0.1f));
        //flyCam.setMoveSpeed(100);
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        
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

        
        // SHIP + CAM
        Geometry ship = new Geometry("Ship", new Sphere(4, 4, 2f));
        ship.setLocalScale(.3f, .2f, .6f);
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setColor("Diffuse", ColorRGBA.Blue);
        mat.getAdditionalRenderState().setDepthTest(false);
        //mat.getAdditionalRenderState().setWireframe(true);
        ship.setMaterial(mat);
        //rootNode.attachChild(ship);
        
//        flyCam.setMoveSpeed(40);
//        flyCam.setEnabled(false);
//        inputManager.setCursorVisible(false);
        
        shipNode = new Node();
        shipNode.attachChild(ship);
        rootNode.attachChild(shipNode);
        shipNode.setLocalTranslation(90,0,0); // Move it a bit

        CameraNode cameraNode = new CameraNode("Camera Node", cam);
        cameraNode.setControlDir(ControlDirection.SpatialToCamera);
        shipNode.attachChild(cameraNode);
        cameraNode.setLocalTranslation(0,1,-3); // Move it a bit
        
//        cam.setLocation(new Vector3f(60, 0, 0));

        setUpKeys();
        

        
        staticCursor.move(0,0,-1);
        staticCursor.setPosition(settings.getWidth()/2-25, settings.getHeight()/2-25);
        staticCursor.setWidth(50);
        staticCursor.setHeight(50);
        staticCursor.setImage(assetManager, "Textures/cur.png", true);
        guiNode.attachChild(staticCursor);

        
        cursor.move(0,0,-1);
        cursor.setWidth(30);
        cursor.setHeight(30);
        cursor.setImage(assetManager, "Textures/cur.png", true);
        guiNode.attachChild(cursor);
        updateCursor();
        
        
    }

    void updateCursor() {
        float x = settings.getWidth()/2-15 + cursorPos.x * 50;
        float y = settings.getHeight()/2-15 + cursorPos.y * 50;
        cursor.setPosition(x,y);
        
    }
    
    @Override
    public void simpleUpdate(float tpf) {
        //timer += tpf;
//        sphereMat.setFloat("Timer", timer);

       
/*        camDir.set(cam.getDirection()).multLocal(tpf);
        camLeft.set(cam.getLeft()).multLocal(tpf);
        camUp.set(cam.getUp()).multLocal(tpf);*/
        
        //camDir = new Vector3f(0f,0f,1f);

        Vector3f lvel = new Vector3f(0f, 0f, 0f);
        
        //vel.set(0, 0, 0);
        // TODO: add roll
        // FIXME: camera gets goofy if flipped upside down
        if (AZ!=0) {
            lvel.addLocal(camDir.mult(AZ*.1f*tpf));
        }
        if (AX!=0) {
            lvel.addLocal(camLeft.mult(AX*.1f*tpf));
        }
        if (AY!=0) {
            lvel.addLocal(camUp.mult(AY*.1f*tpf));
        }

        vel.multLocal(1.0f - 0.5f * tpf);
        vel.addLocal(shipNode.getLocalRotation().mult(lvel));

        //cursorPos.multLocal(1.0f - 2f * tpf);
        if (MX != 0f || MY != 0f) {
            cursorPos.x+=MX;
            cursorPos.y+=MY;
            updateCursor();
        }
        
        shipNode.move(vel);
        if (abs(cursorPos.x)>0.2 || abs(cursorPos.y)>0.2 || RZ != 0.0) {
            shipNode.rotate(-sign(cursorPos.y) * min(abs(cursorPos.y)/10f, 100f)*tpf, -sign(cursorPos.x)*min(abs(cursorPos.x)/10f, 100f)*tpf, -RZ*0.01f);
        }

        MX = 0;
        MY = 0;
    }
}
