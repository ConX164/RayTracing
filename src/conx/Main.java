package conx;

import conx.Util.Vector;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;

public class Main {
    static List<Body> bodyWorld = new ArrayList<Body>();
    static List<Light> lightWorld = new ArrayList<Light>();
    static final int size = 1000;



    static class ShapeDrawing extends JComponent {

        public void paint(Graphics g)
        {
            long startTime = System.nanoTime();
            System.out.println("start");
            int sizeX = size;
            int sizeY = size;
            Camera mainCamera = new Camera(new Vector(-2, -1F, 2.5F), new Vector(0, 0, 0), sizeX, sizeY);
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
        bodyWorld.add(Body.box(new Vector(0F,0F,0.5F), 1,1,1, new int[]{255,100,5}));
        bodyWorld.add(Body.sphere(new Vector(0,1,0.5F), 0.5F, 16, 8, new int[]{255,78,200}));
        //bodyWorld.add(Body.cylinder(new Vector(-0.5F,-1.2F,0.4F), 0.4F, 0.8F, 16, new int[]{200,78,255}));
        //bodyWorld.add(Body.cone(new Vector(0.5F,-0.3F,0.8F), 0.5F, 1, 16, new int[]{100,87,255},20,15,0));
        bodyWorld.add(Body.box(new Vector(-0.45F,-0.95F,0.25F), 0.5F, 0.5F, 0.5F, new int[]{95,50,5}));
        bodyWorld.add(Body.plane(new Vector(0,0,0), 4F, 4F, new int[]{5,255,255}));
        lightWorld.add(new Light(new Vector(-2F,0.2F,5), 10, lightColor, 0.2F));
        lightWorld.add(new Light(new Vector(-1F,-2F,3), 3, lightColor, 0.2F));
        JFrame frame = new JFrame("My first JFrame");
        frame.setSize(size+16, size+39);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(new ShapeDrawing ());
        frame.setVisible(true);
    }
}

