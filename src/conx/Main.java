package conx;

import conx.Util.Matrix;
import conx.Util.Vector;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;

public class Main {
    static List<Body> bodyWorld = new ArrayList<>();
    static List<Light> lightWorld = new ArrayList<>();
    static final int size = 1000;



    static class ShapeDrawing extends JComponent {

        public void paint(Graphics g)
        {
            long startTime = System.nanoTime();
            System.out.println("start");
            int sizeX = size;
            int sizeY = size;
            Camera mainCamera = new Camera(new Vector(-13F, -7F, 15F), new Vector(0, 0, 0), sizeX, sizeY, 26);
            //Camera mainCamera = new Camera(new Vector(-2.6F, -1.4F, 2.9F), new Vector(0, 0, 0), sizeX, sizeY, 26);
            //int[][][] imageCapture = mainCamera.simpleCapture(bodyWorld.toArray(new Body[0]));
            int[][][] imageCapture = mainCamera.advancedCapture(bodyWorld.toArray(new Body[0]), lightWorld.toArray(new Light[0]), 0.2F);//.05F);
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
        int[] lightColor = new int[]{255,255,255};
        //bodyWorld.add(Body.plane(new Vector(0,0,0), 4F, 4F, new int[]{5,255,255}).setRoughness(0.1F));
        //bodyWorld.add(Body.box(new Vector(0F,0F,0.5F), 1,1,1, new int[]{255,100,5}).setRoughness(0.001F));
        //bodyWorld.add(Body.sphere(new Vector(0,1,0.5F), 0.5F, 20, 10, new int[]{255,78,200}).setSmooth().setRoughness(0.1F));
        //bodyWorld.add(Body.cylinder(new Vector(-0.5F,-1.2F,0.4F), 0.4F, 0.8F, 20, new int[]{200,78,255}).setSmooth().setRoughness(0.5F));
        //bodyWorld.add(Body.cone(new Vector(-1F,1F,0.5F), 0.5F, 1F, 20, new int[]{100,87,255}).setSmooth().setRoughness(0.1F));
        //bodyWorld.add(Body.torus(new Vector(-1.4F,-0.2F,0.2F), 0.28F, 0.18F,  24, 10, new int[]{140,255,19}).setSmooth().setRoughness(0.1F));
        //bodyWorld.add(Body.importModel("monkey1.obj", new Vector(0, 0, 0.5F), new int[]{120,120,200}, 45, 0, -50).setSmooth().setRoughness(0.1F));
        bodyWorld.add(Body.importModel("HousePlan.obj", new Vector(0, 0, 0), new int[]{120,120,200}, 90, 0, 180).setRoughness(1F));

        lightWorld.add(new Light(new Vector(-2F,0.2F,5), 10, lightColor, 0.2F));
        lightWorld.add(new Light(new Vector(-1,-2F,3), 3, lightColor, 0.2F));

        JFrame frame = new JFrame("My first JFrame");
        frame.setSize(size+16, size+39);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(new ShapeDrawing ());
        frame.setVisible(true);
    }
}

