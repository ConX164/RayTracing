package conx.Util;


import conx.Body;

import java.util.List;

import static java.lang.Math.*;

public class Plane {
    public Vector p0, p1, p2, l1, l2, norm, center, n0, n1, n2, nAvg, base0, base1, n0Hold, n1Hold, n2Hold;
    public float radius, parArea;
    public float iorTotal = 0;
    public float specular = 4F;
    public int occlusionModifier = 0;
    public float[] color;
    public int[] simpleColor;
    public Body parent;
    private float[][] bounds;

    // Constructor
    public Plane(Vector p0, Vector p1, Vector p2) {
        this.color = new float[]{0.4F, 0.4F, 0.4F};
        this.p0 = p0;
        this.p1 = p1;
        this.p2 = p2;
    }

    public Plane setColor(int[] inColor) {
        this.color = new float[]{inColor[0] / 255F, inColor[1] / 255F, inColor[2] / 255F};
        return this;
    }
    public Plane setNormals(Vector n0, Vector n1, Vector n2){
        this.n0Hold = n0;
        this.n1Hold = n1;
        this.n2Hold = n2;
        //this.nAvg = Vector.add(n0, n1).add(n2).divide(3F).unit();
        return this;
    }
    public Plane centerNormals(Vector origin){
        n0 = Vector.subtract(this.p0 , origin).unit();
        n1 = Vector.subtract(this.p1 , origin).unit();
        n2 = Vector.subtract(this.p2 , origin).unit();
        nAvg = Vector.add(n0, n1).add(n2).divide(3F).unit();
        return this;
    }
    public Vector correctedNormal(Vector cameraRay, Vector point){
        if(point == null || this.n0 == null) {
            if (Vector.dot(cameraRay, norm) / (cameraRay.magnitude() * norm.magnitude()) < 0F) {
                return Vector.multiply(norm, -1);
            }
            return norm;
        }else{
            Vector toCenter = Vector.subtract(point,this.p2);
            float b0 = Vector.cross(toCenter, base0).magnitude() / parArea;
            float b1 = Vector.cross(toCenter, base1).magnitude() / parArea;
            return Vector.multiply(this.n0, b0).add(Vector.multiply(this.n1, b1)).add(Vector.multiply(this.n2, 1F - b0 - b1)).unit();
        }
    }

    public Vector correctedNormal(Vector cameraRay){
        return correctedNormal(cameraRay, null);
    }

    public float[][] bounds(){
        if(this.bounds == null){
            this.bounds = new float[][]{{min(p0.x ,min(p1.x, p2.x)), max(p0.x ,max(p1.x, p2.x))}, {min(p0.y ,min(p1.y, p2.y)), max(p0.y ,max(p1.y, p2.y))}, {min(p0.z ,min(p1.z, p2.z)), max(p0.z ,max(p1.z, p2.z))}};
        }
        return this.bounds;
    }

    public float iorTotal(float iorLevel){
        if(iorTotal == 0) {
            this.iorTotal = iorLevel * (this.specular + 2);
        }
        return this.iorTotal;
    }

    public static float[][] bounds(List<Plane> inPlanes){
        float[][] boundHold = inPlanes.getFirst().bounds();
        float[][] boundTest;
        for(Plane plane : inPlanes){
            boundTest = plane.bounds();
            boundHold = new float[][]{{min(boundTest[0][0], boundHold[0][0]), max(boundTest[0][1], boundHold[0][1])}, {min(boundTest[1][0], boundHold[1][0]), max(boundTest[1][1], boundHold[1][1])}, {min(boundTest[2][0], boundHold[2][0]), max(boundTest[2][1], boundHold[2][1])}};
        }
        return boundHold;
    }

    public float linearIntersect(Vector origin, Vector lineVector){
        if(Vector.dot(lineVector, norm) != 0){
            Vector top = Vector.subtract(origin, p0);
            float bottom = Vector.dot(lineVector, this.norm);
            float t = Vector.dot(this.norm, top) / bottom;
            float u = Vector.dot(Vector.cross(l2, lineVector), top) / bottom;
            float v = Vector.dot(Vector.cross(lineVector, l1), top) / bottom;
            if((t >= 0 && t <= 1) && (u >= 0 && u <= 1) && (v >= 0 && v <= 1) && (u + v <= 1)){
                return t;
            }else{
                return -1F;
            }
        }
        return -1F;
    }

    public float distanceFrom(Vector origin){
        float d0 = Vector.subtract(this.p0, origin).magnitude();
        float d1 = Vector.subtract(this.p1, origin).magnitude();
        float d2 = Vector.subtract(this.p2, origin).magnitude();
        return max(d0, max(d1, d2));
    }

    public Plane shift(Vector offset){
        p0 = Vector.add(p0,offset);
        p1 = Vector.add(p1,offset);
        p2 = Vector.add(p2,offset);
        return this;
    }

    public Plane rotate(float roll, float pitch, float yaw){
        Plane newPlane = new Plane(p0.rotate(roll, pitch, yaw), p1.rotate(roll, pitch, yaw), p2.rotate(roll, pitch, yaw));
        if(n0 != null) {
            newPlane.n0 = this.n0.rotate(roll, pitch, yaw);
            newPlane.n1 = this.n1.rotate(roll, pitch, yaw);
            newPlane.n2 = this.n2.rotate(roll, pitch, yaw);
        }
        if(n0Hold != null) {
            newPlane.n0Hold = this.n0Hold.rotate(roll, pitch, yaw);
            newPlane.n1Hold = this.n1Hold.rotate(roll, pitch, yaw);
            newPlane.n2Hold = this.n2Hold.rotate(roll, pitch, yaw);
        }
        return newPlane;
    }

    public Plane finish(){
        this.l1 = Vector.subtract(p1, p0);
        this.l2 = Vector.subtract(p2, p0);
        this.norm = Vector.cross(l1, l2);
        this.center = Vector.add(p0, Vector.add(p1, p2)).divide(3F);
        this.radius = this.distanceFrom(this.center);
        this.base0 = Vector.subtract(this.p1, this.p2);
        this.base1 = Vector.subtract(this.p0, this.p2);
        this.parArea = Vector.cross(base0, base1).magnitude();
        return this;
    }

}
