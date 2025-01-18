package conx.Util;

import java.util.List;

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

    public Matrix invert3(){
        float[][] intermediaryData = new float[][]{
                {this.matrix[1][1]*this.matrix[2][2]-this.matrix[1][2]*this.matrix[2][1], this.matrix[0][2]*this.matrix[2][1]-this.matrix[0][1]*this.matrix[2][2], this.matrix[0][1]*this.matrix[1][2]-this.matrix[0][2]*this.matrix[1][1]},
                {this.matrix[1][2]*this.matrix[2][0]-this.matrix[1][0]*this.matrix[2][2], this.matrix[0][0]*this.matrix[2][2]-this.matrix[0][2]*this.matrix[2][0], this.matrix[0][2]*this.matrix[1][0]-this.matrix[0][0]*this.matrix[1][2]},
                {this.matrix[1][0]*this.matrix[2][1]-this.matrix[1][1]*this.matrix[2][0], this.matrix[0][1]*this.matrix[2][0]-this.matrix[0][0]*this.matrix[2][1], this.matrix[0][0]*this.matrix[1][1]-this.matrix[0][1]*this.matrix[1][0]}
        };
        float determinant = this.matrix[0][0]*this.matrix[1][1]*this.matrix[2][2] + this.matrix[0][1]*this.matrix[1][2]*this.matrix[2][0] + this.matrix[0][2]*this.matrix[1][0]*this.matrix[2][1] - this.matrix[0][2]*this.matrix[1][1]*this.matrix[2][0] - this.matrix[0][1]*this.matrix[1][0]*this.matrix[2][2] - this.matrix[0][0]*this.matrix[1][2]*this.matrix[2][1];
        for(int i = 0; i < 3; i++){
            for(int j = 0; j < 3; j++){
                intermediaryData[i][j] /= determinant;
            }
        }
        return new Matrix(intermediaryData);
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

    public static Vector linreg(Vector[] vectorList){
        float[][] xData = new float[vectorList.length][3];
        float[][] xtData = new float[3][vectorList.length];
        float[][] yData = new float[vectorList.length][1];
        for(int i = 0; i < vectorList.length; i++){
            xData[i][0] = 1F;
            xData[i][1] = vectorList[i].x;
            xData[i][2] = vectorList[i].y;
            xtData[0][i] = 1F;
            xtData[1][i] = vectorList[i].x;
            xtData[2][i] = vectorList[i].y;
            yData[i][0] = vectorList[i].z;
        }
        Matrix x = new Matrix(xData);
        Matrix xt = new Matrix(xtData);
        Matrix y = new Matrix(yData);
        return xt.multiply(x).invert3().multiply(xt.multiply(y)).toVector().unit();
    }

    public static Vector linreg(List<Plane> vectorList){
        float[][] xData = new float[vectorList.size()][3];
        float[][] xtData = new float[3][vectorList.size()];
        float[][] yData = new float[vectorList.size()][1];
        for(int i = 0; i < vectorList.size(); i++){
            xData[i][0] = 1F;
            xData[i][1] = vectorList.get(i).center.x;
            xData[i][2] = vectorList.get(i).center.y;
            xtData[0][i] = 1F;
            xtData[1][i] = vectorList.get(i).center.x;
            xtData[2][i] = vectorList.get(i).center.y;
            yData[i][0] = vectorList.get(i).center.z;
        }
        Matrix x = new Matrix(xData);
        Matrix xt = new Matrix(xtData);
        Matrix y = new Matrix(yData);
        return xt.multiply(x).invert3().multiply(xt.multiply(y)).toVector().unit();
    }
}
