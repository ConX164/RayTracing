package conx.Util;

import static java.lang.Math.*;

public class Matrix {
    int rows, columns;
    float[][] matrix;
    public Matrix(float[][] input){
        this.rows = input.length;
        this.columns = input[0].length;
        this.matrix = input.clone();
    }

    public Matrix multiply(Matrix other){
        float[][] newMatrix = new float[this.rows][other.columns];
        for(int row = 0; row < this.rows; row++){
            for(int column = 0; column < other.columns; column++){
                for(int i = 0; i < this.columns; i++){
                    newMatrix[row][column] += this.matrix[row][i]*other.matrix[i][column];
                }
            }
        }
        return new Matrix(newMatrix);
    }

    public Vector toVector(){
        return new Vector(this.matrix[0][0], this.matrix[1][0], this.matrix[2][0]);
    }

    public static Matrix rotation(float yaw, float pitch, float roll){
        yaw *= (float) PI /180F;
        pitch *= (float) PI /180F;
        roll *= (float) PI /180F;
        return new Matrix(new float[][]{
                {(float)(cos(yaw)*cos(pitch)), (float)(cos(yaw)*sin(pitch)*sin(roll)-sin(yaw)*cos(roll)), (float)(cos(yaw)*sin(pitch)*cos(roll)+sin(yaw)*sin(roll))},
                {(float)(sin(yaw)*cos(pitch)), (float)(sin(yaw)*sin(pitch)*sin(roll)+cos(yaw)*cos(roll)), (float)(sin(yaw)*sin(pitch)*cos(roll)-cos(yaw)*sin(roll))},
                {(float)(-sin(pitch)), (float)(cos(pitch)*sin(roll)), (float)(cos(pitch)*cos(roll))}
        });
    }




}
