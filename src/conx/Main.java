/*
 * 3D Ray Tracing Engine
 * Author: Connor Ard
 * https://github.com/ConX164
 *
 * Current features:
 *     Main frame rasterization
 *     Ray traced diffuse specular lighting (phong shading)
 *     Ray traced shadows
 *     Soft lighting/shadows from point sources
 *     Ray traced ambient occlusion (hemisphere model)
 *     Smooth shading using vertex normals (phong shading)
 *     (Rough) automatic vertex normals calculations for any models
 *     Colored lightning
 *     Basic roughness and first level ray traced reflections
 *     3-deep k-d tree optimization
 *     Optimized supersampling antialiasing (SSAA)
 *     Customizable primitive shapes (with accurate vertex normals)
 *     Triangulated model importing of .obj files
 *     Full rotation and scaling support
 *     Camera focus position and focal length
 *     Mutithreaded rendering
 */

package conx;

import conx.Util.Vector;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;

public class Main {
    static List<Body> bodyWorld = new ArrayList<>();
    static List<Light> lightWorld = new ArrayList<>();
    static final int size = 1000; // Window size adjustment. Currently, resolution is 1:1 with size

    /*
     * This section of a code is (admittedly) a horrible
     * implementation of rendering the pixel data as a window
     */
    static class ShapeDrawing extends JComponent {

        public void paint(Graphics g)
        {
            long startTime = System.nanoTime();
            System.out.println("start");
            int sizeX = size;
            int sizeY = size;
            //Camera mainCamera = new Camera(new Vector(-13F, -7F, 10F), new Vector(0, 0, 0), sizeX, sizeY, 26);
            Camera mainCamera = new Camera(new Vector(-2.6F, -1.4F, 2.9F), new Vector(0, 0, 0), sizeX, sizeY, 26); // Creating the camera object
            //int[][][] imageCapture = mainCamera.simpleCapture(bodyWorld.toArray(new Body[0]));
            int[][][] imageCapture = mainCamera.advancedCapture(bodyWorld.toArray(new Body[0]), lightWorld.toArray(new Light[0]), 0.2F); // Calling render method
            Graphics2D g2 = (Graphics2D) g;
            for (int xCount = 0; xCount < sizeX; xCount++) {
                for (int yCount = 0; yCount < sizeY; yCount++) {
                    g2.setColor(new Color(imageCapture[xCount][yCount][0], imageCapture[xCount][yCount][1], imageCapture[xCount][yCount][2]));
                    g2.drawLine(xCount, yCount, xCount, yCount);
                }
            }
            long estimatedTime = System.nanoTime() - startTime;
            System.out.println(estimatedTime / 1000000000F);
        }
    }
    public static void main(String[] args) {
        /*
         * The following code defines a sample scene.
         * Body objects are created and added to the global collection, which is in turn fed into the render function of the camera.
         * Light objects are also defined and added to their own collection.
         * WARNING: Depending on complexity, renders can take anywhere from 10 seconds to half an hour.
         * Most renders using ~10 primitives often take a minute or less.
         * Renders containing simple imports can take around two minutes or more.
         */
        int[] lightColor = new int[]{255,255,255};
        bodyWorld.add(Body.plane(new Vector(0,0,0), 4F, 4F, new int[]{5,255,255}).setRoughness(0.1F));
        //bodyWorld.add(Body.box(new Vector(0F,0F,0.5F), 1,1,1, new int[]{255,100,5}).setRoughness(0.001F));
        bodyWorld.add(Body.sphere(new Vector(0,1,0.5F), 0.5F, 20, 10, new int[]{255,78,200}).setSmooth().setRoughness(0.1F));
        //bodyWorld.add(Body.cylinder(new Vector(-0.6F,-1.3F,0.4F), 0.4F, 0.8F, 20, new int[]{200,78,255}).setSmooth().setRoughness(0.5F));
        //bodyWorld.add(Body.cone(new Vector(-1F,1F,0.5F), 0.5F, 1F, 20, new int[]{100,87,255}).setSmooth().setRoughness(0.1F));
        bodyWorld.add(Body.torus(new Vector(-1.4F,-0.2F,0.2F), 0.28F, 0.18F,  24, 10, new int[]{140,255,19}).setSmooth().setRoughness(0.1F));
        //bodyWorld.add(Body.importModel("monkey1.obj", new Vector(1.1F, -1.1F, 0.4F), new int[]{120,120,200}, 0.6F, 0, -35, 165).setSmooth().setRoughness(0.1F));
        //bodyWorld.add(Body.importModel("HousePlan.obj", new Vector(0, 0, 0), new int[]{120,120,200}, 90, 0, 180).setRoughness(1F));
        //bodyWorld.add(Body.plane(new Vector(0,0,0), 4F, 4F, new int[]{5,255,255}));
        //bodyWorld.add(Body.box(new Vector(0F,0F,0.5F), 1,1,1, new int[]{255,100,5}));
        //bodyWorld.add(Body.sphere(new Vector(0,1,0.5F), 0.5F, 20, 10, new int[]{255,78,200}).setSmooth());
        //bodyWorld.add(Body.cylinder(new Vector(-0.5F,-1.2F,0.4F), 0.4F, 0.8F, 20, new int[]{200,78,255}).setSmooth());
        //bodyWorld.add(Body.cone(new Vector(-1F,1F,0.5F), 0.5F, 1F, 20, new int[]{100,87,255}).setSmooth());
        //bodyWorld.add(Body.torus(new Vector(-1.4F,-0.2F,0.2F), 0.28F, 0.18F,  24, 10, new int[]{140,255,19}).setSmooth());
        //bodyWorld.add(Body.importModel("monkey1.obj", new Vector(0, 0, 0.5F), new int[]{120,120,200}, 45, 0, -50).setSmooth());

        lightWorld.add(new Light(new Vector(-2F,0.2F,5), 10, lightColor, 0.2F));
        lightWorld.add(new Light(new Vector(-1,-2F,3), 3, lightColor, 0.2F));
        //lightWorld.add(new Light(new Vector(-20F,-9F,30F), 500, lightColor, 0F));
        //lightWorld.add(new Light(new Vector(-2F,1F,5), 11, lightColor, 0.1F));

        JFrame frame = new JFrame("My first JFrame"); // All code below is necessary for setting up the window
        frame.setSize(size+16, size+39);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(new ShapeDrawing ());
        frame.setVisible(true);
    }
}

