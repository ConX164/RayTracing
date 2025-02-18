package conx;

import conx.Util.Matrix;
import conx.Util.Plane;
import conx.Util.Vector;
import conx.Util.rayHemisphere;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

import static java.lang.Math.*;

public class Body {
    public List<Plane> surfaces = new ArrayList<> ();
    public int[] color;
    public boolean smooth = false;
    public float boundingRadius = 0;
    public Vector origin;
    public HashMap<Plane[], float[]> planeChunks;
    // Constructor
    public Body(Vector origin, Plane[] planeList, int[] color, float roll, float pitch, float yaw){
        this.color = color.clone();
        this.origin = origin;
        for (Plane plane : planeList) {
            Plane adjustedPlane = plane.rotate(roll, pitch, yaw).shift(origin).setColor(color).finish();
            adjustedPlane.parent = this;
            surfaces.add(adjustedPlane);
            float planeRadius = adjustedPlane.distanceFrom(origin);
            if (planeRadius > this.boundingRadius) {
                this.boundingRadius = planeRadius;
            }
        }
        this.fragment();
    }
    public Body setSmooth(){
        if(surfaces.getFirst().n0Hold != null){
            for (Plane plane : surfaces) {
                if(plane.n0Hold != null) {
                    plane.n0 = plane.n0Hold;
                    plane.n1 = plane.n1Hold;
                    plane.n2 = plane.n2Hold;
                    plane.nAvg = Vector.add(plane.n0, plane.n1).add(plane.n2).divide(3F).unit();
                }
            }
        }else {
            for (Plane plane : surfaces) {
                plane.centerNormals(this.origin);
            }
        }
        return this;
    }
    public Body setRoughness(float roughness){
        float specular = 4F / (float) pow(roughness, 2.3);
        int modifier = 0;//(int)((/*-0.62996*cbrt(roughness - 0.5) + 0.5*/0) * rayHemisphere.divisions) * (rayHemisphere.divisions * 4);
        for(Plane plane : this.surfaces){
            plane.specular = specular;
            plane.occlusionModifier = modifier;
            plane.roughness = roughness;
        }
        return this;
    }
    public Body fragment(int size) {
        List<List<Plane>> fragmentList = new ArrayList<>();
        fragmentList.add(this.surfaces);
        List<List<Plane>> temporaryList = new ArrayList<>();
        while(Plane.childrenGreater(fragmentList, size)){
            temporaryList.clear();
            for(List<Plane> planeList : fragmentList){
                if(planeList.size() <= size){
                    temporaryList.add(planeList);
                }else{
                    List<Plane> list1 = new ArrayList<>();
                    List<Plane> list2 = new ArrayList<>();
                    Vector center = Plane.findCenter(planeList);
                    float radius = Plane.findRadius(planeList, center);
                    Vector line = Matrix.linreg(planeList).multiply(radius);
                    Vector p1 = Vector.add(center, line);
                    Vector p2 = Vector.subtract(center, line);
                    List<Plane> checklist = new ArrayList<>(planeList);
                    Plane goodPlane;
                    for(int i = 0; i < planeList.size(); i++){
                        if(i % 2 == 0){
                            goodPlane = Plane.findLeastFar(checklist, p1);
                            list1.add(goodPlane);
                            checklist.remove(goodPlane);
                        }else{
                            goodPlane = Plane.findLeastFar(checklist, p2);
                            list2.add(goodPlane);
                            checklist.remove(goodPlane);
                        }
                    }
                    temporaryList.add(list1);
                    temporaryList.add(list2);
                }
            }
            fragmentList = new ArrayList<>(temporaryList);
        }
        this.planeChunks = new HashMap<>();
        for(List<Plane> planeList : fragmentList){
            Vector center = Plane.findCenter(planeList);
            float radius = Plane.findRadius(planeList, center);
            this.planeChunks.put(planeList.toArray(new Plane[0]), new float[]{center.x, center.y, center.z, radius});
        }
        return this;
    }
    private Body fragment(){
        return this.fragment(15);//15
    }
    // Static methods
    public static Body importModel(String fileName, Vector origin, int[] color, float scale , float roll, float pitch, float yaw) {
        List<Plane> faces = new ArrayList<>();
        try {
            File modelFile = new File(fileName);
            Scanner modelScanner = new Scanner(modelFile);
            String[] line;
            List<Vector> points = new ArrayList<>();
            List<Vector> normals = new ArrayList<>();
            while (modelScanner.hasNextLine()) {
                line = modelScanner.nextLine().split("\\s");
                if (Objects.equals(line[0], "v")) {
                    points.add(new Vector(Float.parseFloat(line[3]) * scale, Float.parseFloat(line[1]) * scale, Float.parseFloat(line[2]) * scale));
                } else if (Objects.equals(line[0], "vn")) {
                    normals.add(new Vector(Float.parseFloat(line[3]), Float.parseFloat(line[1]), Float.parseFloat(line[2])));
                } else if (Objects.equals(line[0], "f")) {
                    String[] p0Data = line[1].split("/");
                    String[] p1Data = line[2].split("/");
                    String[] p2Data = line[3].split("/");
                    faces.add(new Plane(points.get(Integer.parseInt(p0Data[0]) - 1), points.get(Integer.parseInt(p1Data[0]) - 1), points.get(Integer.parseInt(p2Data[0]) - 1)).setNormals(normals.get(Integer.parseInt(p0Data[2]) - 1), normals.get(Integer.parseInt(p1Data[2]) - 1), normals.get(Integer.parseInt(p2Data[2]) - 1)));
                }
            }
            modelScanner.close();
        } catch (FileNotFoundException ignored) {
            System.out.println("ERROR: Model file not found");
        }
        return new Body(origin, faces.toArray(new Plane[0]), color, roll, pitch, yaw);
    }
    public static Body plane(Vector origin, float scaleX, float scaleY, int[] color,float roll, float pitch, float yaw){
        Vector xy = new Vector(scaleX*0.5F,scaleY*0.5F,0);
        Vector x_y = new Vector(-scaleX*0.5F,scaleY*0.5F,0);
        Vector xy_ = new Vector(scaleX*0.5F,-scaleY*0.5F,0);
        Vector x_y_ = new Vector(-scaleX*0.5F,-scaleY*0.5F,0);
        Plane[] squarePlanes = {
                new Plane(xy, x_y, xy_),
                new Plane(x_y_, x_y, xy_)
        };
        return new Body(origin, squarePlanes, color, roll, pitch, yaw);
    }
    public static Body box(Vector origin, float scaleX, float scaleY, float scaleZ, int[] color, float roll, float pitch, float yaw){
        Vector xyz = new Vector(scaleX*0.5F, scaleY*0.5F, scaleZ*0.5F);
        Vector x_yz = new Vector(-scaleX*0.5F, scaleY*0.5F, scaleZ*0.5F);
        Vector xy_z = new Vector(scaleX*0.5F, -scaleY*0.5F, scaleZ*0.5F);
        Vector x_y_z = new Vector(-scaleX*0.5F, -scaleY*0.5F, scaleZ*0.5F);
        Vector xyz_ = new Vector(scaleX*0.5F, scaleY*0.5F, -scaleZ*0.5F);
        Vector x_yz_ = new Vector(-scaleX*0.5F, scaleY*0.5F, -scaleZ*0.5F);
        Vector xy_z_ = new Vector(scaleX*0.5F, -scaleY*0.5F, -scaleZ*0.5F);
        Vector x_y_z_ = new Vector(-scaleX*0.5F, -scaleY*0.5F, -scaleZ*0.5F);
        Plane[] boxPlanes = {
                //Top
                new Plane(xyz, x_yz, xy_z),
                new Plane(x_y_z, x_yz, xy_z),
                //Bottom
                new Plane(xyz_, x_yz_, xy_z_),
                new Plane(x_y_z_, x_yz_, xy_z_),
                //Front
                new Plane(xyz, xy_z, xyz_),
                new Plane(xy_z_, xy_z, xyz_),
                //Back
                new Plane(x_yz, x_y_z, x_yz_),
                new Plane(x_y_z_, x_y_z, x_yz_),
                //Right
                new Plane(xyz, x_yz, xyz_),
                new Plane(x_yz_, x_yz, xyz_),
                //Left
                new Plane(xy_z, x_y_z, xy_z_),
                new Plane(x_y_z_, x_y_z, xy_z_)
        };
        return new Body(origin, boxPlanes, color, roll, pitch, yaw);
    }
    public static Body sphere(Vector origin, float radius, int segments, int rings, int[] color, float roll, float pitch, float yaw){
        Plane[] spherePlanes = new Plane[2 * segments * (rings - 1)];
        Vector[][] spherePoints = new Vector[rings - 1][segments];
        float thetaOffset = (float) (2 * PI / segments);
        float phiOffset = (float) (PI / rings);
        for(int layer = 0; layer < rings - 1; layer++){
            float z = (float) (radius*cos(phiOffset * (layer + 1)));
            float r = (float) (radius*sin(phiOffset * (layer + 1)));
            for(int step = 0; step < segments; step++){
                spherePoints[layer][step] = new Vector((float) (r*cos(thetaOffset*step)), (float) (r*sin(thetaOffset*step)), z);
            }
        }
        int indexCount = 0;
        for(int ring = 1; ring < (rings - 1); ring++){
            for(int segment = 0; segment < (segments - 1); segment++){
                spherePlanes[indexCount] = new Plane(spherePoints[ring - 1][segment], spherePoints[ring - 1][segment + 1], spherePoints[ring][segment]);
                spherePlanes[indexCount + 1] = new Plane(spherePoints[ring][segment + 1], spherePoints[ring - 1][segment + 1], spherePoints[ring][segment]);
                indexCount += 2;
            }
            spherePlanes[indexCount] = new Plane(spherePoints[ring - 1][segments - 1], spherePoints[ring - 1][0], spherePoints[ring][segments - 1]);
            spherePlanes[indexCount + 1] = new Plane(spherePoints[ring][0], spherePoints[ring - 1][0], spherePoints[ring][segments - 1]);
            indexCount += 2;
        }
        Vector top = new Vector(0,0,radius);
        Vector bottom = new Vector(0,0,-radius);
        for(int segment = 0; segment < (segments - 1); segment++){
            spherePlanes[indexCount] = new Plane(top, spherePoints[0][segment], spherePoints[0][segment + 1]);
            spherePlanes[indexCount + 1] = new Plane(bottom, spherePoints[rings - 2][segment], spherePoints[rings - 2][segment + 1]);
            indexCount += 2;
        }
        spherePlanes[indexCount] = new Plane(top, spherePoints[0][segments - 1], spherePoints[0][0]);
        spherePlanes[indexCount + 1] = new Plane(bottom, spherePoints[rings - 2][segments - 1], spherePoints[rings - 2][0]);

        return new Body(origin, spherePlanes, color, roll, pitch, yaw);
    }
    public static Body cylinder(Vector origin, float radius, float height, int segments, int[] color, float roll, float pitch, float yaw){
        Plane[] cylinderPlanes = new Plane[4*(segments - 1)];
        Vector[][] circlePoints = new Vector[2][segments + 1];
        float thetaOffset = (float) (2*PI / segments);
        for(int i = 0; i < segments + 1; i++){
            circlePoints[0][i] = new Vector((float) (radius*cos(i*thetaOffset)), (float) (radius*sin(i*thetaOffset)),height * 0.5F);
            circlePoints[1][i] = new Vector((float) (radius*cos(i*thetaOffset)), (float) (radius*sin(i*thetaOffset)),height * -0.5F);
        }
        int indexCount = 0;
        for(int i = 0; i < segments; i++){
            cylinderPlanes[indexCount] = new Plane(circlePoints[0][i], circlePoints[0][i+1], circlePoints[1][i]).setNormals(new Vector(circlePoints[0][i].x, circlePoints[0][i].y, 0), new Vector(circlePoints[0][i+1].x, circlePoints[0][i+1].y, 0), new Vector(circlePoints[1][i].x, circlePoints[1][i].y, 0));
            cylinderPlanes[indexCount + 1] = new Plane(circlePoints[1][i+1], circlePoints[0][i+1], circlePoints[1][i]).setNormals(new Vector(circlePoints[1][i+1].x, circlePoints[1][i+1].y, 0), new Vector(circlePoints[0][i+1].x, circlePoints[0][i+1].y, 0), new Vector(circlePoints[1][i].x, circlePoints[1][i].y, 0));
            indexCount += 2;
        }
        for(int i = 1; i < segments - 1; i++){
            cylinderPlanes[indexCount] = new Plane(circlePoints[0][0], circlePoints[0][i], circlePoints[0][i+1]);
            cylinderPlanes[indexCount + 1] = new Plane(circlePoints[1][0], circlePoints[1][i], circlePoints[1][i+1]);
            indexCount += 2;
        }
        return new Body(origin, cylinderPlanes, color, roll, pitch, yaw);
    }
    public static Body cone(Vector origin, float radius, float height, int segments, int[] color, float roll, float pitch, float yaw){
        Plane[] conePlanes = new Plane[2*segments - 2];
        Vector[] circlePoints = new Vector[segments + 1];
        Vector tip = new Vector(0,0,height*0.5F);
        float thetaOffset = (float) (2*PI / segments);
        float normalHeight = radius * radius / height;
        for(int i = 0; i < segments + 1; i++){
            circlePoints[i] = new Vector((float) (radius*cos(i*thetaOffset)), (float) (radius*sin(i*thetaOffset)),height * -0.5F);
        }
        int indexCount = 0;
        Vector n1, n2, n3;
        n3 = new Vector(0F,0F,0F);
        for(int i = 0; i < segments; i++, indexCount++){
            n1 = new Vector(circlePoints[i].x, circlePoints[i].y, normalHeight);
            n2 = new Vector(circlePoints[i+1].x, circlePoints[i+1].y, normalHeight);
            conePlanes[indexCount] = new Plane(circlePoints[i], circlePoints[i+1], tip).setNormals(n1.unit(), n2.unit(), n3);
        }
        for(int i = 1; i < segments - 1; i++, indexCount++){
            conePlanes[indexCount] = new Plane(circlePoints[0], circlePoints[i], circlePoints[i+1]);
        }
        return new Body(origin, conePlanes, color, roll, pitch, yaw);
    }
    public static Body torus(Vector origin, float majorRadius, float minorRadius, int majorSegments, int minorSegments, int[] color, float roll, float pitch, float yaw){
        float minorTheta = (float) (2*PI/minorSegments);
        float majorTheta = (float) (2*PI/majorSegments);
        Vector[][] points = new Vector[majorSegments + 1][minorSegments + 1];
        Vector[][] normals = new Vector[majorSegments + 1][minorSegments + 1];
        Plane[] torusPlanes = new Plane[2 * minorSegments * majorSegments];
        Vector newPoint;
        for(int i = 0; i <= minorSegments; i++){
            newPoint = new Vector(0, (float) (minorRadius*cos(i*minorTheta)), (float) (minorRadius*sin(i*minorTheta)));
            for(int j = 0; j <= majorSegments; j++){
                points[j][i] = Vector.add(newPoint, new Vector(0, majorRadius, 0)).rotate(0,0,(j*majorTheta) * (180F/(float)PI));
                normals[j][i] = newPoint.rotate(0,0,(j*majorTheta) * (180F/(float)PI)).unit();
            }
        }

        int indexcount = 0;
        for(int i = 0; i < minorSegments; i++){
            for(int j = 0; j < majorSegments; j++){
                torusPlanes[indexcount] = new Plane(points[j][i], points[j+1][i], points[j][i+1]).setNormals(normals[j][i], normals[j+1][i], normals[j][i+1]);
                torusPlanes[indexcount + 1] = new Plane(points[j+1][i+1], points[j+1][i], points[j][i+1]).setNormals(normals[j+1][i+1], normals[j+1][i], normals[j][i+1]);
                indexcount += 2;
            }
        }
        return new Body(origin, torusPlanes, color, roll, pitch, yaw);
    }
    // Overloading
    public static Body importModel(String fileName, Vector origin, int[] color){
        return importModel(fileName, origin, color, 1, 0, 0, 0);
    }
    public static Body importModel(String fileName, Vector origin, int[] color, float scale){
        return importModel(fileName, origin, color, scale, 0, 0, 0);
    }
    public static Body importModel(String fileName, Vector origin, int[] color, float roll, float pitch, float yaw){
        return importModel(fileName, origin, color,1, roll, pitch, yaw);
    }
    public static Body plane(Vector origin, float scaleX, float scaleY, int[] color){
        return plane(origin, scaleX, scaleY, color, 0, 0, 0);
    }
    public static Body box(Vector origin, float scaleX, float scaleY, float scaleZ, int[] color){
        return box(origin, scaleX, scaleY, scaleZ, color,0F, 0F, 0F);
    }
    public static Body sphere(Vector origin, float radius, int segments, int rings, int[] color){
        return sphere(origin, radius, segments, rings, color,0, 0, 0);
    }

    public static Body cylinder(Vector origin, float radius, float height, int segments, int[] color) {
        return cylinder(origin, radius, height, segments, color, 0, 0, 0);
    }
    public static Body cone(Vector origin, float radius, float height, int segments, int[] color){
        return cone(origin, radius, height, segments, color, 0, 0, 0);
    }
    public static Body torus(Vector origin, float majorRadius, float minorRadius, int majorSegments, int minorSegments, int[] color){
        return torus(origin, majorRadius, minorRadius, majorSegments, minorSegments, color, 0, 0, 0);
    }
}
