package conx.Util;
import java.lang.Math;

public class Vector {
    public float x, y, z;
    private float mag = -1;
    private Vector unit;
    // Constructor
    public Vector(float x, float y, float z){
        this.x = x;
        this.y = y;
        this.z = z;
    }
    // Object methods
    public Vector add(Vector v2){
        this.x += v2.x;
        this.y += v2.y;
        this.z += v2.z;
        this.mag = -1;
        this.unit = null;
        return this;
    }
    public Vector subtract(Vector v2){
        this.x -= v2.x;
        this.y -= v2.y;
        this.z -= v2.z;
        this.mag = -1;
        this.unit = null;
        return this;
    }
    public Vector multiply(float scalar){
        this.x *= scalar;
        this.y *= scalar;
        this.z *= scalar;
        this.mag = -1;
        this.unit = null;
        return this;
    }
    public Vector divide(float scalar){
        this.x /= scalar;
        this.y /= scalar;
        this.z /= scalar;
        this.mag = -1;
        this.unit = null;
        return this;
    }
    public float magnitude(){
        if(this.mag == -1){
            this.mag = (float) Math.sqrt(this.x*this.x + this.y*this.y + this.z*this.z);
        }
        return this.mag;
    }
    public Vector unit(){
        if(this.unit == null){
            if(this.magnitude() == 0){
                this.unit = new Vector(0,0,0);
            }else {
                this.unit = new Vector(this.x / this.magnitude(), this.y / this.magnitude(), this.z / this.magnitude());
            }
        }
        return this.unit;
    }
    public float[] toArray(){
        return new float[]{this.x, this.y, this.z};
    }
    public Matrix toMatrix(){
        return new Matrix(new float[][]{{this.x}, {this.y}, {this.z}});
    }
    public Vector rotate(float roll, float pitch, float yaw){
        return Matrix.rotation(yaw, pitch, roll).multiply(this.toMatrix()).toVector();
    }
    // Static methods
    public static Vector add(Vector v1, Vector v2){
        return new Vector(v1.x + v2.x, v1.y + v2.y, v1.z + v2.z);
    }
    public static Vector subtract(Vector v1, Vector v2){
        return new Vector(v1.x - v2.x, v1.y - v2.y, v1.z - v2.z);
    }
    public static Vector multiply(Vector v1, float scalar){
        return new Vector(v1.x * scalar, v1.y * scalar, v1.z * scalar);
    }
    public static Vector divide(Vector v1, float scalar){
        return new Vector(v1.x / scalar, v1.y / scalar, v1.z / scalar);
    }
    public static float dot(Vector v1, Vector v2){
        return (v1.x*v2.x + v1.y*v2.y + v1.z*v2.z);
    }
    public static Vector cross(Vector v1, Vector v2){
        return new Vector(v1.y*v2.z - v1.z*v2.y, -(v1.x*v2.z - v1.z*v2.x), v1.x*v2.y - v1.y*v2.x);
    }
    public static float proj(Vector a, Vector b){
        return Vector.dot(a , b) / b.magnitude();
    }
    public static Vector extend(Vector origin, Vector normal, float t){
        return new Vector(origin.x + normal.x*t,origin.y + normal.y*t,origin.z + normal.z*t);
    }
    public static float shortDistance(Vector ray, Vector origin, Vector point){
        return  Vector.cross(ray, Vector.subtract(point, origin)).magnitude() / ray.magnitude();
    }
}
