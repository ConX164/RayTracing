package conx;

import conx.Util.*;
import conx.Util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Math.*;

/**
 * This class is responsible for creating threads that calculate data for each pixel
 */
class renderThread extends Thread{
    final Camera activeCamera;

    public renderThread(Camera activeCamera){
        this.activeCamera = activeCamera;
    }

    public void run(){
        try{
            long startTime = System.nanoTime();
            int count = 0;
            int[] samplingColor;
            int[] colorMix;
            int sampleSpots = activeCamera.sampling * activeCamera.sampling;
            int index;
            HashMap<Plane,int[]> hitPlanes;
            for (int x = 0; x < activeCamera.pixelsX; x++) {
                for (int y = 0; y < activeCamera.pixelsY; y++) {
                    index = x + y * activeCamera.pixelsX;
                    if (activeCamera.chunkCompletion.putIfAbsent(index, true) == null) {
                        colorMix = new int[]{0, 0, 0}; // Color data for antialiasing
                        hitPlanes = new HashMap<>(); // Tracking planes to prevent excess sampling
                        for (int i = 0; i < activeCamera.sampling; i++) {
                            for (int j = 0; j < activeCamera.sampling; j++) {
                                samplingColor = activeCamera.advancedRaytrace(      // perform raytrace
                                        x + (float) i / activeCamera.sampling, y + (float) j / activeCamera.sampling, activeCamera.visibleBodies, activeCamera.lightInstances, activeCamera.globalBrightness, hitPlanes);
                                colorMix[0] += samplingColor[0]; // red
                                colorMix[1] += samplingColor[1]; // green
                                colorMix[2] += samplingColor[2]; // blue
                            }
                        }
                        activeCamera.canvas[x][y] = new int[]{colorMix[0] / sampleSpots, colorMix[1] / sampleSpots, colorMix[2] / sampleSpots}; // store data
                        count++;
                    }
                }
            }
            long estimatedTime = System.nanoTime() - startTime;
            System.out.println("Thread " + this.threadId() + " : "+ estimatedTime / 1000000000F + " : " + count);
            //System.out.println("End:" + this.threadId());

        }
        catch (Exception e){
            System.out.println("Code 2: " + this.threadId());
            throw e;
        }
    }
}

public class Camera {
    public Vector origin, focus;
    Vector frameOrigin, targetVector, heightVector, widthVector;
    int pixelsX, pixelsY;
    static final Vector verticalVector = new Vector(0,0,1);
    Body[] visibleBodies;
    Light[] lightInstances;
    float globalBrightness, xMult, yMult;
    float iorLevel = 0.5F /  (float) (10*PI);
    int[][][] canvas;
    ConcurrentHashMap<Integer, Boolean> chunkCompletion = new ConcurrentHashMap<>();
    ConcurrentHashMap<Plane, Boolean> planeCompletion = new ConcurrentHashMap<>();
    List<Plane>[][] raster;
    int sampling = 4;

    // Constructor
    public Camera(Vector origin, Vector focus, int pixelsX, int pixelsY, int focalLength){
        this.origin = origin;
        this.focus = focus;
        this.pixelsX = pixelsX;
        this.pixelsY = pixelsY;
        this.xMult =  (2F / (pixelsX - 1));
        this.yMult =  (2F / (pixelsY - 1));
        this.canvas = new int[pixelsX][pixelsY][3];
        this.targetVector = new Vector((focus.x - origin.x), (focus.y - origin.y), (focus.z - origin.z)).unit();
        this.widthVector = Vector.cross(targetVector, verticalVector).unit();
        this.heightVector = Vector.cross(targetVector, this.widthVector).unit();
        this.frameOrigin =  Vector.multiply(targetVector,((float)focalLength) / 18F).add(origin);
        this.raster  = new ArrayList[this.pixelsY][this.pixelsX];
        for(int i = 0; i < this.pixelsY; i++){
            for(int j = 0; j < this.pixelsX; j++){
                this.raster[i][j] = new ArrayList<>();
            }
        }
        System.out.println(Arrays.toString(targetVector.toArray()));
        System.out.println(Arrays.toString(widthVector.toArray()));
        System.out.println(Arrays.toString(heightVector.toArray()));
        System.out.println(Arrays.toString(frameOrigin.toArray()));
    }
    public void move(float x, float y, float z){
        this.origin.add(new Vector(x,y,z));
        this.targetVector = new Vector((focus.x - origin.x), (focus.y - origin.y), (focus.z - origin.z)).unit();
        this.widthVector = Vector.cross(targetVector, verticalVector).multiply(-1).unit();
        this.heightVector = Vector.cross(targetVector, this.widthVector).multiply(-1).unit();
        this.frameOrigin = Vector.add(origin, Vector.subtract(this.targetVector, Vector.add(this.widthVector, this.heightVector)));
    }

    /**
     * This function provides a basic raytrace based on a plane's angle from the view.
     * Does not include AO, lighting, or reflections
     * @param pixelX Horizontal pixel index
     * @param pixelY Vertical Pixel index
     * @param bodyList List of bodies to be rendered
     * @return Returns color data as a list in [R,G,B] format for a specific pixel (0-255)
     */
    private int[] simpleRaytrace(float pixelX, float pixelY, Body[] bodyList){
        float tx = pixelX * this.xMult;
        float ty = pixelY * this.yMult;
        Vector rayVector = Vector.subtract(Vector.add(Vector.add(Vector.multiply(widthVector, tx), Vector.multiply(heightVector,ty)), this.frameOrigin), this.origin).unit().multiply(1000);
        float lowestT = -1F;
        Plane hitPlane = null;
        int[] pixelColor = {70,70,70};

        float intersectT;
        for(Body body : bodyList){ // Start intersection tests
            if (Vector.shortDistance(rayVector, this.origin, body.origin) <= body.boundingRadius) {
                for (Plane plane : body.surfaces) {
                    intersectT = plane.linearIntersect(origin, rayVector);
                    if (intersectT >= 0F) {
                        if (intersectT < lowestT || lowestT < 0F) {
                            lowestT = intersectT;
                            hitPlane = plane;
                        }
                    }
                }
            }
        }
        if(hitPlane != null){
            if(hitPlane.simpleColor == null){
                // Calculate shading and color
                float cosBetween = abs(Vector.dot(this.targetVector, hitPlane.norm) / (this.targetVector.magnitude() * hitPlane.norm.magnitude())) * 255F;
                hitPlane.simpleColor = new int[]{round(hitPlane.color[0] * cosBetween),round(hitPlane.color[1] * cosBetween),round(hitPlane.color[2] * cosBetween)};
            }
            pixelColor = hitPlane.simpleColor;
        }
        return pixelColor;
    }

    /**
     * This function provides a complex raytrace with all features.
     * @param pixelX Horizontal pixel index
     * @param pixelY Vertical pixel index
     * @param bodyList List of bodies to be rendered
     * @param lightList List of light sources to be rendered
     * @param minimumBrightness Value for global lighting (0-1)
     * @param hitPlanes Plane map to prevent excess supersampling
     * @return Returns color data as a list in [R,G,B] format for a specific pixel (0-255)
     */
    public int[] advancedRaytrace(float pixelX, float pixelY, Body[] bodyList, Light[] lightList, float minimumBrightness, HashMap<Plane, int[]> hitPlanes){
        float tx = (pixelX - (this.pixelsX)/2F) * xMult; // Converting pixel index to vector scale factor
        float ty = (pixelY - (this.pixelsY)/2F) * yMult;
        Vector rayVector = Vector.subtract(Vector.add(Vector.add(Vector.multiply(widthVector, tx), Vector.multiply(heightVector,ty)), this.frameOrigin), this.origin).unit().multiply(-1000); // Creating main ray
        float lowestT = -1F;
        Plane hitPlane = null;
        float[] pixelColor = {minimumBrightness, minimumBrightness, minimumBrightness};
        int[] newColor;

        // Start intersection test from precalculated raster
        float intersectT;
        for(Plane plane : this.raster[(int) pixelY][(int) pixelX]){
            intersectT = plane.linearIntersect(origin, rayVector);
            if (intersectT >= 0F) {
                if (intersectT < lowestT || lowestT < 0F) {
                    lowestT = intersectT;
                    hitPlane = plane;
                }
            }
        }
        // Deprecated intersection test without rasterization
        /*for(Body body : bodyList){
            if (Vector.shortDistance(rayVector, this.origin, body.origin) <= body.boundingRadius) {
                for (Plane plane : body.surfaces) {
                    if (Vector.shortDistance(rayVector, this.origin, plane.center) <= plane.radius) {
                        intersectT = plane.linearIntersect(origin, rayVector);
                        if (intersectT >= 0F) {
                            if (intersectT < lowestT || lowestT < 0F) {
                                lowestT = intersectT;
                                hitPlane = plane;
                            }
                        }
                    }
                }
            }
        }*/
        if(hitPlanes.get(hitPlane) != null){
            return hitPlanes.get(hitPlane);
        }
        if(hitPlane != null){ // Begin color calculations
            float[] totalColor = new float[3];
            List<float[]> colorCasts = new ArrayList<>();
            Vector hitPoint = Vector.add(this.origin, Vector.multiply(rayVector, -lowestT));
            Vector planeNorm = hitPlane.correctedNormal(rayVector, hitPoint).unit();
            //float occlusionEstimate = 0;//ambientOcclusionDetect(hitPoint,hitPlane,bodyList,planeNorm); Deprecated AO optimization
            float occlusionMult = minimumBrightness;
            //if(occlusionEstimate == 0) {
            occlusionMult = ambientOcclusionRandom(hitPoint, hitPlane, bodyList, planeNorm) * minimumBrightness; // Determine AO amount
            //}
            float iorMult = hitPlane.iorTotal(this.iorLevel); // Temporary variable call for gloss properties
            for(Light light : lightList){ // Start light/shadow detection
                float multiplier = light.illumination(hitPoint,hitPlane,bodyList,rayVector); // Detect lighting
                Vector reflection = Vector.add(rayVector.unit(), (Vector.multiply(planeNorm, -2F*Vector.dot(planeNorm, rayVector.unit())))); // Create reflection ray
                float specular = iorMult * multiplier * (float) pow(max(Vector.dot(reflection, Vector.subtract(hitPoint, light.origin).unit()), 0), hitPlane.specular); // Calculate specular
                colorCasts.add(new float[]{light.color[0]*(multiplier*hitPlane.color[0] + specular), light.color[1]*(multiplier*hitPlane.color[1] + specular), light.color[2]*(multiplier*hitPlane.color[2] + specular)}); // Combine lighting
            }
            colorCasts.add(new float[]{hitPlane.color[0] * occlusionMult, hitPlane.color[1] * occlusionMult, hitPlane.color[2] * occlusionMult}); // Add AO
            for(float[] color : colorCasts){ // Combine all effects
                totalColor[0] += color[0];
                totalColor[1] += color[1];
                totalColor[2] += color[2];
            }
            if(hitPlane.roughness < 0.5F){ // Reflections
                float[] relectColor = reflect(bodyList, lightList, minimumBrightness, rayVector, hitPoint, hitPlane); // Perform reflection detection
                float reflectionWeight = 0.15F - hitPlane.roughness/3.3333F; // Scaling clarity to roughness
                float baseWeight = 1 - reflectionWeight;
                totalColor[0] = totalColor[0] * baseWeight + relectColor[0] * reflectionWeight;
                totalColor[1] = totalColor[1] * baseWeight + relectColor[1] * reflectionWeight;
                totalColor[2] = totalColor[2] * baseWeight + relectColor[2] * reflectionWeight;
            }
            // Gamma correction
            totalColor[0] = (float) pow(1.0F - exp(totalColor[0] * -1.4F), 0.8333333333333333F);
            totalColor[1] = (float) pow(1.0F - exp(totalColor[1] * -1.4F), 0.8333333333333333F);
            totalColor[2] = (float) pow(1.0F - exp(totalColor[2] * -1.4F), 0.8333333333333333F);
            // Clamping values
            if(totalColor[0] > 1.0F) {
                totalColor[0] = 1.0F;
            }
            if(totalColor[1] > 1.0F) {
                totalColor[1] = 1.0F;
            }
            if(totalColor[2] > 1.0F) {
                totalColor[2] = 1.0F;
            }
            newColor = new int[]{round(totalColor[0]*255), round(totalColor[1]*255), round(totalColor[2]*255)}; // return color
        }
        else {
            newColor = new int[]{round(pixelColor[0] * 255), round(pixelColor[1] * 255), round(pixelColor[2] * 255)}; // return color (nothing hit)
        }
        hitPlanes.put(hitPlane, newColor);
        return newColor;
    }
    
    public int[][][] simpleCapture(Body[] visibleBodies){
        int[][][] newCapture = new int[this.pixelsX][this.pixelsY][3];
        for(int x = 0; x < this.pixelsX; x++){
            for(int y = 0; y < this.pixelsY; y++){
                newCapture[x][y] = simpleRaytrace(x, y, visibleBodies);
            }
        }
        return newCapture;
    }

    public int[][][] advancedCapture(Body[] visibleBodies, Light[] lightInstances, float globalBrightness){
        int threadCount = 7;
        renderThread[] renderThreads = new renderThread[threadCount];
        this.visibleBodies = visibleBodies;
        this.lightInstances = lightInstances;
        this.globalBrightness = globalBrightness;

        for(Body body : visibleBodies){
            for(Plane plane : body.surfaces){
                this.planeCompletion.put(plane, false);
            }
        }

        rasterize();

        for(int i = 0; i < threadCount; i++){
            renderThreads[i] = new renderThread(this);
            renderThreads[i].start();
        }
        for(int i = 0; i < threadCount; i++){try {renderThreads[i].join();} catch (InterruptedException ignored) {}}
        return canvas;
    }

    /*private static int ambientOcclusionDetect(Vector point, Plane parentPlane, Body[] bodyList, Vector vk){
        float[][] hemiPoints = rayHemisphere.testPoints;
        Vector vTemp = Vector.subtract(parentPlane.p1,parentPlane.p2).unit();
        Vector vi = Vector.cross(vk,vTemp).unit();
        Vector vj = Vector.cross(vk,vi).unit();
        for(float[] source : hemiPoints){
            Vector occlusionRay = Vector.add(Vector.multiply(vi, source[0]), Vector.multiply(vj, source[1])).add(Vector.multiply(vk, source[2]));
            for(Body body : bodyList){
                if (Vector.shortDistance(occlusionRay, point, body.origin) <= body.boundingRadius) {
                    for(Plane[] planeList : body.planeChunks.keySet()){
                        float[] data = body.planeChunks.get(planeList);
                        Vector center = new Vector(data[0], data[1], data[2]);
                        if (Vector.shortDistance(occlusionRay, point, center) <= data[3] + 0.000001F) {
                            for (Plane plane : planeList) {
                                if (plane != parentPlane) {
                                    if ((body == parentPlane.parent) && (plane.n0 != null) && (Vector.dot(plane.nAvg, occlusionRay) < 0)) {
                                        continue;
                                    }
                                    if (Vector.shortDistance(occlusionRay, point, plane.center) <= plane.radius) {
                                        if (plane.linearIntersect(point, occlusionRay) >= 0.000001F) {
                                            return 0;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return 1;
    }*/

    private static float ambientOcclusionRandom(Vector point, Plane parentPlane, Body[] bodyList, Vector vk){
        float[][] hemiPoints = rayHemisphere.randomHemisphere();
        int visiblility = hemiPoints.length;// - parentPlane.occlusionModifier;
        float strength = 1.2F;
        Vector vTemp = Vector.subtract(parentPlane.p1,parentPlane.p2).unit();
        Vector vi = Vector.cross(vk,vTemp).unit();
        Vector vj = Vector.cross(vk,vi).unit();
        //int i = 0;
        outerLoop:
        for(float[] source : hemiPoints){
            Vector occlusionRay = Vector.add(Vector.multiply(vi, source[0]), Vector.multiply(vj, source[1])).add(Vector.multiply(vk, source[2]));
            for(Body body : bodyList){
                if (Vector.shortDistance(occlusionRay, point, body.origin) <= body.boundingRadius && Vector.subtract(point, body.origin).magnitude() <= body.boundingRadius + rayHemisphere.radius) {
                    for(Plane[] planeList : body.planeChunks.keySet()){
                        float[] data = body.planeChunks.get(planeList);
                        Vector center = new Vector(data[0], data[1], data[2]);
                        if(Vector.shortDistance(occlusionRay, point, center) <= data[3] + 0.000001F && Vector.subtract(point, center).magnitude() <= data[3] + rayHemisphere.radius) {
                            for (Plane plane : planeList) {
                                if (plane != parentPlane) {
                                    if ((body == parentPlane.parent) && (plane.n0 != null) && (Vector.dot(plane.nAvg, occlusionRay) < 0)) {
                                        continue;
                                    }
                                    if (Vector.shortDistance(occlusionRay, point, plane.center) <= plane.radius) {
                                        if (plane.linearIntersect(point, occlusionRay) >= 0.000001F) {
                                            visiblility--;
                                            continue outerLoop;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        float occlusion = (float) visiblility / ((float)hemiPoints.length);// - parentPlane.occlusionModifier);
        return (float) pow(occlusion, strength);
    }

    private static float[] reflect(Body[] visibleBodies, Light[] lightInstances, float globalBrightness, Vector ray, Vector point, Plane parentPlane){
        Vector normal = parentPlane.correctedNormal(ray, point).unit();
        Vector rayVector = Vector.subtract(ray, Vector.multiply(normal, 2F*Vector.dot(ray, normal)));
        float lowestT = -1F;
        Plane hitPlane = null;
        float intersectT;
        for(Body body : visibleBodies){
            if (Vector.shortDistance(rayVector, point, body.origin) <= body.boundingRadius) {
                for(Plane[] planeList : body.planeChunks.keySet()){
                    float[] data = body.planeChunks.get(planeList);
                    Vector center = new Vector(data[0], data[1], data[2]);
                    if (Vector.shortDistance(rayVector, point, center) <= data[3] + 0.000001F) {
                        for (Plane plane : planeList) {
                            if (plane != parentPlane && Vector.shortDistance(rayVector, point, plane.center) <= plane.radius) {
                                intersectT = plane.linearIntersect(point, rayVector);
                                if (intersectT >= 0F) {
                                    if (intersectT < lowestT || lowestT < 0F) {
                                        lowestT = intersectT;
                                        hitPlane = plane;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        float[] totalColor;
        if(hitPlane != null){
            totalColor = new float[]{hitPlane.color[0] * globalBrightness, hitPlane.color[1] * globalBrightness, hitPlane.color[2] * globalBrightness};
            List<float[]> colorCasts = new ArrayList<>();
            Vector hitPoint = Vector.add(point, Vector.multiply(rayVector, -lowestT));
            Vector planeNorm = hitPlane.correctedNormal(rayVector, hitPoint).unit();
            float iorMult = hitPlane.iorTotal(0.5F /  (float) (10*PI));
            for(Light light : lightInstances){
                float multiplier = light.illumination(hitPoint,hitPlane,visibleBodies,rayVector);
                Vector reflection = Vector.add(rayVector.unit(), (Vector.multiply(planeNorm, -2F*Vector.dot(planeNorm, rayVector.unit()))));
                float specular = iorMult * multiplier * (float) pow(max(Vector.dot(reflection, Vector.subtract(hitPoint, light.origin).unit()), 0), hitPlane.specular);
                colorCasts.add(new float[]{light.color[0]*(multiplier*hitPlane.color[0] + specular), light.color[1]*(multiplier*hitPlane.color[1] + specular), light.color[2]*(multiplier*hitPlane.color[2] + specular)});
            }
            for(float[] color : colorCasts){
                totalColor[0] += color[0];
                totalColor[1] += color[1];
                totalColor[2] += color[2];
            }
        }
        else{
            totalColor = new float[]{globalBrightness, globalBrightness, globalBrightness};
        }
        return totalColor;
    }

    public void rasterize(){
        float[][] points;
        Vector frameVector = Vector.subtract(this.frameOrigin, this.origin);
        float xAdjust = (this.pixelsX)/2F;
        float yAdjust = (this.pixelsY)/2F;
        for(Plane plane : this.planeCompletion.keySet()){
            points = new float[3][2];
            Vector d0 = Vector.subtract(plane.p0, this.origin);
            d0.divide(Vector.proj(d0, frameVector)/frameVector.magnitude());
            points[0][0] = Vector.proj(d0, this.widthVector) / xMult + xAdjust;
            points[0][1] = Vector.proj(d0, this.heightVector) / yMult + yAdjust;

            Vector d1 = Vector.subtract(plane.p1, this.origin);
            d1.divide(Vector.proj(d1, frameVector)/frameVector.magnitude());
            points[1][0] = Vector.proj(d1, this.widthVector) / xMult + xAdjust;
            points[1][1] = Vector.proj(d1, this.heightVector) / yMult + yAdjust;

            Vector d2 = Vector.subtract(plane.p2, this.origin);
            d2.divide(Vector.proj(d2, frameVector)/frameVector.magnitude());
            points[2][0] = Vector.proj(d2, this.widthVector) / xMult + xAdjust;
            points[2][1] = Vector.proj(d2, this.heightVector) / yMult + yAdjust;

            //System.out.println(Arrays.deepToString(points));
            Arrays.sort(points, (a, b) -> Float.compare(a[0], b[0]));
            //System.out.println(Arrays.deepToString(points));
            float m1 = (points[1][1] - points[0][1]) / (points[1][0] - points[0][0]);
            float m2 = (points[2][1] - points[0][1]) / (points[2][0] - points[0][0]);
            float m3 = (points[2][1] - points[1][1]) / (points[2][0] - points[1][0]);
            float offset1 = 0, offset2 = 0, offset3 = 0;

            if(m1 >= m2){
                if(m1 > 0){
                    offset1 = 1;
                }
                if(m2 < 0){
                    offset2 = 1;
                }
                if(m3 > 0){
                    offset3 = 1;
                }
            }else{
                if(m1 < 0){
                    offset1 = 1;
                }
                if(m2 > 0){
                    offset2 = 1;
                }
                if(m3 < 0){
                    offset3 = 1;
                }
            }
            for(int u = max(0, (int)points[0][0]); u < min(this.pixelsX, (int)points[1][0]); u++){
                int v1 = (int)(m1 * (u - points[0][0] + offset1) + points[0][1]);
                int v2 = (int)(m2 * (u - points[0][0] + offset2) + points[0][1]);
                for(int v = max(0, min(v1, v2)); v <= min(this.pixelsY - 1, max(v1,v2)); v++){
                    raster[v][u].add(plane);
                }
            }
            if((int)points[2][0] == (int)points[1][0] && (int)points[1][0] < this.pixelsX){
                for (int v = max(0, (int)min(points[1][1], points[2][1])); v <= min(this.pixelsY - 1, (int)max(points[1][1], points[2][1])); v++) {
                    if(v >= this.pixelsY){
                        break;
                    }
                    raster[v][(int)points[1][0]].add(plane);
                }
            }else {
                for (int u = max(0, (int)points[1][0]); u <= min(this.pixelsX - 1, (int)points[2][0]+1); u++) {
                    int v1 = (int) (m3 * (u - points[1][0] + offset3) + points[1][1]);
                    int v2 = (int) (m2 * (u - points[0][0] + offset2) + points[0][1]);
                    for (int v = max(0, min(v1, v2)); v <= min(this.pixelsY - 1, max(v1, v2)); v++) {
                        raster[v][u].add(plane);
                    }
                }
            }
       }
    }
}
