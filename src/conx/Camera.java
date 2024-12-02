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
            //System.out.println("Start:" + this.threadId());
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
    float globalBrightness;
    int[][][] canvas;
    boolean[][] chunkCompletion;
    int sampling = 4;
    // Constructor
    public Camera(Vector origin, Vector focus, int pixelsX, int pixelsY){
        this.origin = origin;
        this.focus = focus;
        this.pixelsX = pixelsX;
        this.pixelsY = pixelsY;
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
        float tx = pixelX * (2F / (this.pixelsX -1));
        float ty = pixelY * (2F / (this.pixelsY - 1));
        Vector rayVector = Vector.subtract(Vector.add(Vector.add(Vector.multiply(widthVector, tx), Vector.multiply(heightVector,ty)), this.frameOrigin), this.origin).unit().multiply(1000);
        float lowestT = -1F;
        Plane hitPlane = null;
        int[] pixelColor = {70,70,70};

        float intersectT;
        for(Body body : bodyList){
            for(Plane plane : body.surfaces){
                intersectT = plane.linearIntersect(origin, rayVector);
                if((intersectT >= 0F) && (lowestT >= 0F)){
                    if(intersectT < lowestT){
                        lowestT = intersectT;
                        hitPlane = plane;
                    }
                } else if (intersectT >= 0F) {
                    lowestT = intersectT;
                    hitPlane = plane;
                }
            }
        }
        if(hitPlane != null){
            if(hitPlane.simpleColor == null){
                float cosBetween = abs(Vector.dot(this.targetVector, hitPlane.norm) / (this.targetVector.magnitude() * hitPlane.norm.magnitude()));
                //cosBetween *= (cosBetween / 2) + 0.5F;
                hitPlane.simpleColor = new int[]{round(hitPlane.color[0] * cosBetween),round(hitPlane.color[1] * cosBetween),round(hitPlane.color[2] * cosBetween)};
            }
            pixelColor = hitPlane.simpleColor;
        }
        return pixelColor;
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
    public int[] advancedRaytrace(float pixelX, float pixelY, Body[] bodyList, Light[] lightList, float minimumBrightness, HashMap<Plane, int[]> hitPlanes){
        float tx = pixelX * (2F / (this.pixelsX - 1));
        float ty = pixelY * (2F / (this.pixelsY - 1));
        Vector rayVector = Vector.subtract(Vector.add(Vector.add(Vector.multiply(widthVector, tx), Vector.multiply(heightVector,ty)), this.frameOrigin), this.origin).unit().multiply(1000);
        float lowestT = -1F;
        Plane hitPlane = null;
        float[] pixelColor = {minimumBrightness, minimumBrightness, minimumBrightness};
        int[] newColor;

        float intersectT;
        for(Body body : bodyList){
            if (Vector.shortDistance(rayVector, this.origin, body.origin) <= body.boundingRadius) {
                for (Plane plane : body.surfaces) {
                    if(plane.boundingRadius > 0) {
                        if (Vector.shortDistance(rayVector, this.origin, plane.center) <= plane.boundingRadius) {
                            intersectT = plane.linearIntersect(origin, rayVector);
                            if ((intersectT >= 0F) && (lowestT >= 0F)) {
                                if (intersectT < lowestT) {
                                    lowestT = intersectT;
                                    hitPlane = plane;
                                }
                            } else if (intersectT >= 0F) {
                                lowestT = intersectT;
                                hitPlane = plane;
                            }
                        }
                    }else{
                        intersectT = plane.linearIntersect(origin, rayVector);
                        if ((intersectT >= 0F) && (lowestT >= 0F)) {
                            if (intersectT < lowestT) {
                                lowestT = intersectT;
                                hitPlane = plane;
                            }
                        } else if (intersectT >= 0F) {
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
            float[] totalColor = new float[]{0,0,0};
            List<float[]> colorCasts = new ArrayList<>();
            Vector hitPoint = Vector.add(this.origin, Vector.multiply(rayVector, lowestT));
            float occlusionMult = ambientOcclusionRandom(hitPoint,hitPlane,bodyList,rayVector);
            for(Light light : lightList){
                float[] tempColor = new float[3];
                float multiplier = light.illumination(hitPoint,hitPlane,bodyList,rayVector);
                tempColor[0] = hitPlane.color[0] * multiplier * light.color[0];
                tempColor[1] = hitPlane.color[1] * multiplier * light.color[1];
                tempColor[2] = hitPlane.color[2] * multiplier * light.color[2];
                colorCasts.add(new float[]{tempColor[0], tempColor[1], tempColor[2]});
            }
            // Temporary
            float[] tempColor = new float[3];
            tempColor[0] = hitPlane.color[0] * minimumBrightness * occlusionMult;
            tempColor[1] = hitPlane.color[1] * minimumBrightness * occlusionMult;
            tempColor[2] = hitPlane.color[2] * minimumBrightness * occlusionMult;
            colorCasts.add(new float[]{tempColor[0], tempColor[1], tempColor[2]});
            //
            for(float[] color : colorCasts){
                totalColor[0] += color[0];
                totalColor[1] += color[1];
                totalColor[2] += color[2];
            }
            for(int i = 0; i < 3; i++){
                totalColor[i] = (float) pow(1.0F - exp(-totalColor[i] * 1.4F), (1.0F / 1.2F));
                if(totalColor[i] > 1.0F) {
                    totalColor[i] = 1.0F;
                }
            }
            pixelColor = totalColor.clone();
            newColor = new int[]{round(pixelColor[0]*255), round(pixelColor[1]*255), round(pixelColor[2]*255)};
            hitPlanes.put(hitPlane, newColor);
        }
        else {
            newColor = new int[]{round(pixelColor[0] * 255), round(pixelColor[1] * 255), round(pixelColor[2] * 255)};
        }
        return newColor;
    }
    public int[][][] advancedCapture(Body[] visibleBodies, Light[] lightInstances, float globalBrightness){
        int threadCount = 10;
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

    private static float ambientOcclusion(Vector point, Plane parentPlane, Body[] bodyList, Vector cameraRay){
        int visiblility = rayHemisphere.amount;
        float strength = 1.25F;
        Vector vi = Vector.subtract(parentPlane.p1,parentPlane.p2).unit();
        Vector vk = parentPlane.correctedNormal(cameraRay).unit();
        Vector vj = Vector.cross(vk,vi).unit();

        outerLoop:
        for(float[] source : rayHemisphere.hemiPoints){
            Vector occlusionRay = Vector.add(Vector.multiply(vi, source[0]), Vector.multiply(vj, source[1])).add(Vector.multiply(vk, source[2]));
            for(Body body : bodyList){
                for(Plane plane : body.surfaces){
                    if(plane != parentPlane){
                        if(plane.linearIntersect(point, occlusionRay) >= 0){
                            visiblility--;
                            continue outerLoop;
                        }
                    }
                }
            }
        }
        float occlusion = (float) visiblility / (float) rayHemisphere.amount;
        return (float) pow(occlusion, strength);
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
                for(Plane plane : body.surfaces){
                    if(plane != parentPlane){
                        if(plane.linearIntersect(point, occlusionRay) >= 0){
                            visiblility--;
                            continue outerLoop;
                        }
                    }
                }
            }
        }
        float occlusion = (float) visiblility / (float) hemiPoints.size();
        return (float) pow(occlusion, strength);
    }

    private static float ambientOcclusionSmoothRandom(Vector point, Plane parentPlane, Body[] bodyList, Vector cameraRay){
        float variance = 0.1F;
        int visiblility = rayHemisphere.amount;
        float strength = 1.25F;
        Vector vi = Vector.subtract(parentPlane.p1,parentPlane.p2).unit();
        Vector vk = parentPlane.correctedNormal(cameraRay).unit();
        Vector vj = Vector.cross(vk,vi).unit();

        outerLoop:
        for(float[] source : rayHemisphere.hemiPoints){
            float[] newPoint = new float[]{source[0] + variance*(rand.nextFloat() - 0.5F), source[1] + variance*(rand.nextFloat() - 0.5F), source[2] + variance*(rand.nextFloat() - 0.5F)};
            Vector occlusionRay = Vector.add(Vector.multiply(vi, newPoint[0]), Vector.multiply(vj, newPoint[1])).add(Vector.multiply(vk, newPoint[2]));
            for(Body body : bodyList){
                for(Plane plane : body.surfaces){
                    if(plane != parentPlane){
                        if(plane.linearIntersect(point, occlusionRay) >= 0){
                            visiblility--;
                            continue outerLoop;
                        }
                    }
                }
            }
        }
        float occlusion = (float) visiblility / (float) rayHemisphere.amount;
        return (float) pow(occlusion, strength);
    }
}
