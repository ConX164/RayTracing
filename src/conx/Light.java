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
    float[][][] lightGrids;
    int amount;
    int variations = 16;
    //int sections = 9;//7
    //Constructor
    public Light(Vector origin, float strength, int[] color, float radius){
        this.origin = origin;
        this.strength = strength;
        this.color = new float[]{color[0] / 255F, color[1] / 255F, color[2] / 255F};
        this.radius = radius;
        int sections = round((float) (7.4d * pow(radius, 1/11d)));
        this.amount = (4*sections*sections) + 1;
        lightGrids = new float[variations][amount][2];
        float rSpace = radius / sections;
        float thetaSpace = (float) (2*PI / sections);
        for(float[][] grid : lightGrids){
            grid[0] = new float[]{0,0};
            int count = 1;
            for(int i = 0; i < sections; i++){
                float rBase = rSpace*i;
                //float r =  rSpace*i + rSpace*rand.nextFloat();
                for(int j = 0; j < 4*sections; j++){
                    float theta = thetaSpace*j + thetaSpace*rand.nextFloat();
                    float r = (float) sqrt(radius * (rBase + rSpace*rand.nextFloat()));
                    grid[count] = new float[]{(float) (r*cos(theta)), (float) (r*sin(theta))};
                    count++;
                }
            }

        }
    }
    public float illumination(Vector point, Plane parentPlane, Body[] bodyList, Vector cameraRay){
        float visibility = 0F;
        Vector mainRay = Vector.subtract(this.origin, point);
        Vector v1 = Vector.cross(mainRay, cameraRay).unit();
        Vector v2 = Vector.cross(mainRay, v1).unit();
        outerLoop:
        for(float[] source : this.lightGrids[rand.nextInt(0, variations)]){
            Vector circlePoint = Vector.add(origin,Vector.add(Vector.multiply(v1,source[0]), Vector.multiply(v2,source[1])));
            Vector lightRay = Vector.subtract(point, circlePoint);
            for(Body body : bodyList){
                if (Vector.shortDistance(lightRay, circlePoint, body.origin) <= body.boundingRadius + 0.000001F) {
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
                           if (Vector.shortDistance(lightRay, circlePoint, center) <= data[3] + 0.000001F) {
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
    public float[] illuminationComplex(Vector point, Plane parentPlane, Body[] bodyList, Vector cameraRay){
        float visibility = 0F;
        float clarity = 0F;
        Vector mainRay = Vector.subtract(this.origin, point);
        Vector v1 = Vector.cross(mainRay, cameraRay).unit();
        Vector v2 = Vector.cross(mainRay, v1).unit();
        outerLoop:
        for(float[] source : this.lightGrids[rand.nextInt(0, variations)]){
            Vector circlePoint = Vector.add(origin,Vector.add(Vector.multiply(v1,source[0]), Vector.multiply(v2,source[1])));
            Vector lightRay = Vector.subtract(point, circlePoint);
            for(Body body : bodyList){
                if (Vector.shortDistance(lightRay, circlePoint, body.origin) <= body.boundingRadius + 0.000001F) {
                    for(Plane[] planeList : body.planeChunks.keySet()){
                        float[] data = body.planeChunks.get(planeList);
                        Vector center = new Vector(data[0], data[1], data[2]);
                        if (Vector.shortDistance(lightRay, circlePoint, center) <= data[3] + 0.000001F) {
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
            clarity += 1F;
        }
        return new float[]{(this.strength * visibility) / (mainRay.magnitude() * mainRay.magnitude() * this.amount), clarity/this.amount};
    }
}
