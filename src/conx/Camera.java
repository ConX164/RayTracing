package conx;

import conx.Util.Plane;
import conx.Util.Vector;
import conx.Util.rayHemisphere;

import java.util.*;

import static java.lang.Math.*;

// Threading
class ThreadHandler extends Thread{
    final Camera activeCamera;

    public ThreadHandler(Camera activeCamera){
        this.activeCamera = activeCamera;
    }
    public void run(){
        try{
            long startTime = System.nanoTime();
            int count = 0;
            int[] samplingColor;
            int[] colorMix;
            int sampleSpots = activeCamera.sampling * activeCamera.sampling;
            HashMap<Plane,int[]> hitPlanes;
            for(int x = 0; x < activeCamera.pixelsX; x++){
                for(int y = 0; y < activeCamera.pixelsY; y++) {
                    if (!activeCamera.chunkCompletion[x][y]) {
                        activeCamera.chunkCompletion[x][y] = true;
                        colorMix = new int[]{0, 0, 0};
                        hitPlanes = new HashMap<>();
                        for(int i = 0; i < activeCamera.sampling; i ++) {
                            for(int j = 0; j < activeCamera.sampling; j++) {
                                samplingColor = activeCamera.advancedRaytrace(x + (float) i /activeCamera.sampling, y + (float) j /activeCamera.sampling, activeCamera.visibleBodies, activeCamera.lightInstances, activeCamera.globalBrightness, hitPlanes);
                                colorMix[0] += samplingColor[0];
                                colorMix[1] += samplingColor[1];
                                colorMix[2] += samplingColor[2];
                            }
                        }
                        activeCamera.canvas[x][y] = new int[]{colorMix[0] / sampleSpots,colorMix[1] / sampleSpots,colorMix[2] / sampleSpots};
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
    static Random rand = new Random();
    Body[] visibleBodies;
    Light[] lightInstances;
    float globalBrightness, xMult, yMult;
    int[][][] canvas;
    boolean[][] chunkCompletion;
    int sampling = 4;
    // Constructor
    public Camera(Vector origin, Vector focus, int pixelsX, int pixelsY){
        this.origin = origin;
        this.focus = focus;
        this.pixelsX = pixelsX;
        this.pixelsY = pixelsY;
        this.xMult =  (2F / (pixelsX - 1));
        this.yMult =  (2F / (pixelsY - 1));
        this.canvas = new int[pixelsX][pixelsY][3];
        this.chunkCompletion = new boolean[pixelsX][pixelsY];
        this.targetVector = new Vector((focus.x - origin.x), (focus.y - origin.y), (focus.z - origin.z)).unit();
        this.widthVector = Vector.cross(targetVector, verticalVector).multiply(-1).unit();
        this.heightVector = Vector.cross(targetVector, this.widthVector).multiply(-1).unit();
        this.frameOrigin = Vector.add(origin, Vector.subtract(this.targetVector, Vector.add(this.widthVector, this.heightVector)));
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
    private int[] simpleRaytrace(float pixelX, float pixelY, Body[] bodyList){
        float tx = pixelX * this.xMult;
        float ty = pixelY * this.yMult;
        Vector rayVector = Vector.subtract(Vector.add(Vector.add(Vector.multiply(widthVector, tx), Vector.multiply(heightVector,ty)), this.frameOrigin), this.origin).unit().multiply(1000);
        float lowestT = -1F;
        Plane hitPlane = null;
        int[] pixelColor = {70,70,70};

        float intersectT;
        for(Body body : bodyList){
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
                float cosBetween = abs(Vector.dot(this.targetVector, hitPlane.norm) / (this.targetVector.magnitude() * hitPlane.norm.magnitude())) * 255F;
                hitPlane.simpleColor = new int[]{round(hitPlane.color[0] * cosBetween),round(hitPlane.color[1] * cosBetween),round(hitPlane.color[2] * cosBetween)};
            }
            pixelColor = hitPlane.simpleColor;
        }
        return pixelColor;
    }

    public int[] advancedRaytrace(float pixelX, float pixelY, Body[] bodyList, Light[] lightList, float minimumBrightness, HashMap<Plane, int[]> hitPlanes){
        float tx = pixelX * xMult;
        float ty = pixelY * yMult;
        Vector rayVector = Vector.subtract(Vector.add(Vector.add(Vector.multiply(widthVector, tx), Vector.multiply(heightVector,ty)), this.frameOrigin), this.origin).unit().multiply(1000);
        float lowestT = -1F;
        Plane hitPlane = null;
        float[] pixelColor = {minimumBrightness, minimumBrightness, minimumBrightness};
        int[] newColor;

        float intersectT;
        for(Body body : bodyList){
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
            int[] preColor = hitPlanes.get(hitPlane);
            if(preColor != null){
                return preColor;
            }
            float[] totalColor = new float[3];
            List<float[]> colorCasts = new ArrayList<>();
            Vector hitPoint = Vector.add(this.origin, Vector.multiply(rayVector, lowestT));
            float occlusionEstimate = ambientOcclusionDetect(hitPoint,hitPlane,bodyList,rayVector);
            float occlusionMult = minimumBrightness;
            if(occlusionEstimate < 1F) {
                occlusionMult = ambientOcclusionRandom(hitPoint, hitPlane, bodyList, rayVector) * minimumBrightness;
            }
            for(Light light : lightList){
                float multiplier = light.illumination(hitPoint,hitPlane,bodyList,rayVector);
                colorCasts.add(new float[]{hitPlane.color[0] * multiplier * light.color[0], hitPlane.color[1] * multiplier * light.color[1], hitPlane.color[2] * multiplier * light.color[2]});
            }
            colorCasts.add(new float[]{hitPlane.color[0] * occlusionMult, hitPlane.color[1] * occlusionMult, hitPlane.color[2] * occlusionMult});
            for(float[] color : colorCasts){
                totalColor[0] += color[0];
                totalColor[1] += color[1];
                totalColor[2] += color[2];
            }
            totalColor[0] = (float) pow(1.0F - exp(totalColor[0] * -1.4F), 0.8333333333333333F);
            totalColor[1] = (float) pow(1.0F - exp(totalColor[1] * -1.4F), 0.8333333333333333F);
            totalColor[2] = (float) pow(1.0F - exp(totalColor[2] * -1.4F), 0.8333333333333333F);
            if(totalColor[0] > 1.0F) {
                totalColor[0] = 1.0F;
            }
            if(totalColor[1] > 1.0F) {
                totalColor[1] = 1.0F;
            }
            if(totalColor[2] > 1.0F) {
                totalColor[2] = 1.0F;
            }
            newColor = new int[]{round(totalColor[0]*255), round(totalColor[1]*255), round(totalColor[2]*255)};
            hitPlanes.put(hitPlane, newColor);
        }
        else {
            newColor = new int[]{round(pixelColor[0] * 255), round(pixelColor[1] * 255), round(pixelColor[2] * 255)};
        }
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
        int threadCount = 6;
        ThreadHandler[] threads = new ThreadHandler[threadCount];
        this.visibleBodies = visibleBodies;
        this.lightInstances = lightInstances;
        this.globalBrightness = globalBrightness;

        for(int i = 0; i < threadCount; i++){
            threads[i] = new ThreadHandler(this);
            threads[i].start();
        }
        for(int i = 0; i < threadCount; i++){try {threads[i].join();} catch (InterruptedException ignored) {}}
        return canvas;
    }

    private static float ambientOcclusionDetect(Vector point, Plane parentPlane, Body[] bodyList, Vector cameraRay){
        List<float[]> hemiPoints = rayHemisphere.hemiPoints;
        int visiblility = hemiPoints.size();
        Vector vi = Vector.subtract(parentPlane.p1,parentPlane.p2).unit();
        Vector vk = parentPlane.correctedNormal(cameraRay).unit();
        Vector vj = Vector.cross(vk,vi).unit();

        outerLoop:
        for(float[] source : hemiPoints){
            Vector occlusionRay = Vector.add(Vector.multiply(vi, source[0]), Vector.multiply(vj, source[1])).add(Vector.multiply(vk, source[2]));
            for(Body body : bodyList){
                if (Vector.shortDistance(occlusionRay, point, body.origin) <= body.boundingRadius) {
                    for (Plane plane : body.surfaces) {
                        if (plane != parentPlane) {
                            if (plane.linearIntersect(point, occlusionRay) >= 0.000001F) {
                                visiblility--;
                                continue outerLoop;
                            }
                        }
                    }
                }
            }
        }
        return (float) visiblility / (float) hemiPoints.size();
    }

    private static float ambientOcclusionRandom(Vector point, Plane parentPlane, Body[] bodyList, Vector cameraRay){
        List<float[]> hemiPoints = rayHemisphere.adjustedRandom();
        int visiblility = hemiPoints.size();
        float strength = 1.2F;
        Vector vi = Vector.subtract(parentPlane.p1,parentPlane.p2).unit();
        Vector vk = parentPlane.correctedNormal(cameraRay).unit();
        Vector vj = Vector.cross(vk,vi).unit();

        outerLoop:
        for(float[] source : hemiPoints){
            Vector occlusionRay = Vector.add(Vector.multiply(vi, source[0]), Vector.multiply(vj, source[1])).add(Vector.multiply(vk, source[2]));
            for(Body body : bodyList){
                if (Vector.shortDistance(occlusionRay, point, body.origin) <= body.boundingRadius) {
                    for (Plane plane : body.surfaces) {
                        if (plane != parentPlane) {
                            if (plane.linearIntersect(point, occlusionRay) >= 0.000001F) {
                                visiblility--;
                                continue outerLoop;
                            }
                        }
                    }
                }
            }
        }
        float occlusion = (float) visiblility / (float) hemiPoints.size();
        return (float) pow(occlusion, strength);
    }
}
