package conx;

import conx.Util.Plane;
import conx.Util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.lang.Math.*;


public class Light {
    public Vector origin;
    public float strength, radius;
    public float[] color;
    static Random rand = new Random();
    final float spacing = 0.025F;
    List<float[]> lightPoints = new ArrayList<>();
    private int amount;
    //Constructor
    public Light(Vector origin, float strength, int[] color, float radius){
        this.origin = origin;
        this.strength = strength;
        this.color = new float[]{color[0] / 255F, color[1] / 255F, color[2] / 255F};
        this.radius = radius;
        float r2 = (this.radius*this.radius) / (spacing*spacing);
        int xAmount = (int) (radius / spacing);
        for(int x = -xAmount; x <= xAmount;x++) {
            int yAmount = (int) (sqrt(r2 - x*x));
            for (int y = -yAmount; y <= yAmount; y++) {
                this.lightPoints.add(new float[] {spacing * x, spacing * y});
            }
        }
        this.amount = this.lightPoints.size();
    }
    public float illumination(Vector point, Plane parentPlane, Body[] bodyList, Vector cameraRay){
        float visibility = 0F;
        Vector mainRay = Vector.subtract(this.origin, point);
        Vector v1 = Vector.cross(mainRay, cameraRay).unit();
        Vector v2 = Vector.cross(mainRay, v1).unit();
        outerLoop:
        for(float[] source : this.lightPoints){
            Vector circlePoint = Vector.add(origin,Vector.add(Vector.multiply(v1,source[0]), Vector.multiply(v2,source[1])));
            Vector lightRay = Vector.subtract(point, circlePoint);
            for(Body body : bodyList){
                if (Vector.shortDistance(lightRay, this.origin, body.origin) <= body.boundingRadius + 0.000001F) {
                   /*if(body.planeChunks == null) {
                        for (Plane plane : body.surfaces) {
                            if (plane != parentPlane) {
                                if ((body == parentPlane.parent) && (plane.n0 != null) && (-Vector.dot(plane.nAvg, lightRay) > 0)) {
                                    continue;
                                }
                                if (plane.linearIntersect(point, lightRay) >= 0.000001F) {
                                    continue outerLoop;
                                }
                            }
                        }
                    }else{*/
                       for(Plane[] planeList : body.planeChunks.keySet()){
                           float[] data = body.planeChunks.get(planeList);
                           Vector center = new Vector(data[0], data[1], data[2]);
                           if (Vector.shortDistance(lightRay, this.origin, center) <= data[3] + 0.000001F) {
                               for (Plane plane : planeList) {
                                   if (plane != parentPlane) {
                                       if ((body == parentPlane.parent) && (plane.n0 != null) && (-Vector.dot(plane.nAvg, lightRay) > 0)) {
                                           continue;
                                       }
                                       if (plane.linearIntersect(point, lightRay) >= 0.000001F) {
                                           continue outerLoop;
                                       }
                                   }
                               }
                           }
                       }
                    //}
                }
            }
            visibility += -Vector.dot(lightRay.unit(), parentPlane.correctedNormal(cameraRay, point).unit());
        }
        return (this.strength * visibility) / (mainRay.magnitude() * mainRay.magnitude() * this.amount);
    }

    public float illuminationRandom(Vector point, Plane parentPlane, Body[] bodyList, Vector cameraRay){
        int randomNumber = 32;
        List<float[]> randomPoints = new ArrayList<>();
        for(int i = 0; i < randomNumber; i++){
            float r = radius*rand.nextFloat();
            float theta =  (float) (2*PI*rand.nextFloat());
            randomPoints.add(new float[] {(float) (r*cos(theta)), (float) (r*sin(theta))});
        }
        float visibility = 0F;
        Vector mainRay = Vector.subtract(this.origin, point);
        Vector v1 = Vector.cross(mainRay, cameraRay).unit();
        Vector v2 = Vector.cross(mainRay, v1).unit();
        outerLoop:
        for(float[] source : randomPoints){
            Vector circlePoint = Vector.add(origin,Vector.add(Vector.multiply(v1,source[0]), Vector.multiply(v2,source[1])));
            Vector lightRay = Vector.subtract(circlePoint, point);
            for(Body body : bodyList){
                for(Plane plane : body.surfaces){
                    if(plane != parentPlane){
                        if(plane.linearIntersect(point, lightRay) >= 0){
                            continue outerLoop;
                        }
                    }
                }
            }
            visibility += Vector.dot(lightRay.unit(), parentPlane.correctedNormal(cameraRay).unit());
        }
        return (this.strength * visibility) / (mainRay.magnitude() * mainRay.magnitude() * randomNumber);
    }
}
