package conx.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.lang.Math.*;

public class rayHemisphere {
    public static List<float[]> hemiPoints;
    static Random rand = new Random();
    static float PI2 = (float) (2 * PI);
    static int divisions = 6;
    static float zSpacing = 1F / divisions;
    static float oSpacing = PI2 / (4 * divisions);
    static float[] zList = new float[divisions];
    static float[] oList = new float[divisions * 4];
    static int divisionsLQ = 6;
    static float zSpacingLQ = 1F / divisionsLQ;
    static float oSpacingLQ = PI2 / (4 * divisionsLQ);
    static float[] zListLQ = new float[divisionsLQ];
    static float[] oListLQ = new float[divisionsLQ * 4];

    static {
        zList[0] = zSpacing / 1000F;
        for(int i = 1; i < divisions; i++){
            zList[i] = zSpacing * i;
        }
        for(int i = 0; i < divisions*4; i++){
            oList[i] = oSpacing * i;
        }

        zListLQ[0] = zSpacingLQ / 1000F;
        for(int i = 1; i < divisionsLQ; i++){
            zListLQ[i] = zSpacingLQ * i;
        }
        for(int i = 0; i < divisionsLQ*4; i++){
            oListLQ[i] = oSpacingLQ * i;
        }

        float size = 0.8F;
        hemiPoints = new ArrayList<>();
        for(int i = 0; i < divisionsLQ; i++){
            for(int j = 0; j < divisionsLQ * 4; j++){
                float z = zListLQ[i];
                float r = size * (float) sqrt(1 - z*z);
                float o = oListLQ[j];
                float x = (float) cos(o);
                float y = (float) (o>PI ? -sqrt(1 - x * x) : sqrt(1 - x * x));
                hemiPoints.add(new float[]{r * x, r * y, size * z});
            }
        }
    }

    public static List<float[]> adjustedRandom(){
        float size = 0.8F;
        List<float[]> newList = new ArrayList<>();
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
