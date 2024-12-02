package conx.Util;


import static java.lang.Math.max;
import static java.lang.Math.round;

public class Plane {
    public Vector p0, p1, p2, l1, l2, norm;
    public float[] color;
    public int[] simpleColor;
    // Constructor
    public Plane(Vector p0, Vector p1, Vector p2){
        this.color = new float[]{0.4F,0.4F,0.4F};
        this.p0 = p0;
        this.p1 = p1;
        this.p2 = p2;
        this.l1 = Vector.subtract(p1, p0);
        this.l2 = Vector.subtract(p2, p0);
        this.norm = Vector.cross(l1, l2);
    }
    public Plane setColor(int[] inColor){
        this.color = new float[]{inColor[0] / 255F, inColor[1] / 255F, inColor[2] / 255F};
        return this;
    }
    public Vector correctedNormal(Vector cameraRay){
        if(Vector.dot(cameraRay, norm) / (cameraRay.magnitude() * norm.magnitude()) > 0F){
            return Vector.multiply(norm,-1);
        }
        return norm;
    }

    public float linearIntersect(Vector origin, Vector lineVector){
        if(Vector.dot(Vector.multiply(lineVector, -1), Vector.cross(l1, l2)) != 0){
            Vector top = Vector.subtract(origin, p0);
            float bottom = Vector.dot(Vector.multiply(lineVector, -1), this.norm);
            float t = Vector.dot(this.norm, top) / bottom;
            float u = Vector.dot(Vector.cross(l2, Vector.multiply(lineVector, -1)), top) / bottom;
            float v = Vector.dot(Vector.cross(Vector.multiply(lineVector, -1), l1), top) / bottom;
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
        return new Plane(p0.rotate(roll, pitch, yaw), p1.rotate(roll, pitch, yaw), p2.rotate(roll, pitch, yaw));
    }

}
