package conx.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.lang.Math.*;

public class rayHemisphere {
    public static List<float[]> hemiPoints = new ArrayList<float[]>();
    public static int amount;
    static Random rand = new Random();
    static float PI2 = (float) (2 * PI);
    static int divisions = 4;
    static float zSpacing = 1F / divisions;
    static float oSpacing = PI2 / (4 * divisions);
    static float[] zList = new float[divisions];
    static float[] oList = new float[divisions * 4];

    static {
        float size = 0.6F;
        float spacing = 0.1F;
        int amount1 = (int) ((PI * size) / (2 * spacing));
        float interval1 = (float) (PI / (2 * amount1));
        for(int i = 1; i < amount1;i++) {
            float z = (float) (size * sin(i * interval1));
            float r = (float) (size * cos(i * interval1));

            int amount2 = (int) ((2 * PI * r) / spacing);
            float interval2 = (float) (2*PI / amount2);
            for(int j = 0; j < amount2;j++){
                float x = (float) (r * cos(interval2*j));
                float y = (float) (r * sin(interval2*j));
                hemiPoints.add(new float[] {x,y,z});
            }
        }
        hemiPoints.add(new float[] {0,0,size});
        amount = hemiPoints.size();
        float zSpacing = 1F / divisions;
        zList[0] = zSpacing / 1000F;
        for(int i = 1; i < divisions; i++){
            zList[i] = zSpacing * i;
        }
        for(int i = 0; i < divisions*4; i++){
            oList[i] = oSpacing * i;
        }
    }

    public static List<float[]> random(){
        int amount = 400;
        float size = 0.8F;
        List<float[]> newList = new ArrayList<float[]>();
        for(int i = 0; i < amount; i++) {
            float z =  0.99F * rand.nextFloat() + 0.01F;
            float r =  size * (float) sqrt(1 - z*z);
            float o = PI2 * rand.nextFloat();
            float x = (float) cos(o);
            float y = (float) (o>PI ? -sqrt(1 - x * x) : sqrt(1 - x * x));
            newList.add(new float[]{r * x, r * y, size * z});
        }
        return newList;
    }

    public static List<float[]> adjustedRandom(){
        float size = 0.8F;
        List<float[]> newList = new ArrayList<float[]>();
        for(int i = 0; i < divisions; i++){
            for(int j = 0; j < divisions * 4; j++){
                float z = zList[i] + zSpacing*rand.nextFloat();
                float r = size * (float) sqrt(1 - z*z);
                float o = oList[j] + oSpacing*rand.nextFloat();
                float x = (float) cos(o);
                float y = (float) (o>PI ? -sqrt(1 - x * x) : sqrt(1 - x * x));
                newList.add(new float[]{r * x, r * y, size * z});
            }
        }
        return newList;
    }
}
