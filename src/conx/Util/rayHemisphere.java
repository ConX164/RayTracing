package conx.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.lang.Math.*;

public class rayHemisphere {
    static float[][][] randomPoints;
    static int variations = 64;
    static Random rand = new Random();
    public static float radius = 0.8F;
    static int sections = 6;

    static {
        randomPoints = new float[variations][(sections - 1)*(4 * sections) + 1][3];
        float zSpace = radius / sections;
        float thetaSpace = (float) ((PI) / (2F * sections));
        int count;
        for(float[][] pointList : randomPoints){
            count = 1;
            pointList[0] = new float[]{0, 0, -radius};
            for(int i = 1; i < (sections); i++){
                float zBase = zSpace*i;
                for(int j = 0; j < 4 * sections; j++){
                    float z = zBase + rand.nextFloat()*zSpace;
                    float r = (float) sqrt(radius*radius - z*z);
                    float theta = thetaSpace*j + rand.nextFloat()*thetaSpace;
                    pointList[count] = new float[]{(float) (r*cos(theta)), (float) (r*sin(theta)), -z};
                    count++;
                }

            }
        }
    }

    public static float[][] randomHemisphere(){
        return randomPoints[rand.nextInt(0, variations)];
    }
}
