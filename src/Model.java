import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by bdanglot on 20/06/16.
 */
public class Model {

    /**
     * input
     */
    private int NUMPOINTS;
    private double[][] points;
    private String[][] LCM;
    private boolean[] PUV;
    private JSONObject parameters;

    /**
     * intermediate results
     */
    private boolean[] CMV;
    private boolean[][] PUM;
    private boolean[] FUV;

    private JSONObject input;

    public Model(String path) {
        this.input = readJSON(path);
        this.NUMPOINTS = this.input.getInt("NUMPOINTS");
        this.parameters = this.input.getJSONObject("PARAMETERS");
        this.points = new double[NUMPOINTS][2];
        this.PUV = new boolean[NUMPOINTS];
        this.LCM = new String[NUMPOINTS][NUMPOINTS];
        JSONArray arrayPoints = this.input.getJSONArray("points");
        JSONArray arrayPUV = this.input.getJSONArray("PUV");
        JSONObject arrayLCM = this.input.getJSONObject("LCM");
        for (int i = 0; i < NUMPOINTS; i++) {
            this.points[i][0] = arrayPoints.getJSONArray(i).getDouble(0);
            this.points[i][1] = arrayPoints.getJSONArray(i).getDouble(1);
            this.PUV[i] = arrayPUV.getBoolean(i);
            JSONArray arrayILCM = arrayLCM.getJSONArray(String.valueOf(i));
            for (int j = 0; j < NUMPOINTS; j++)
                this.LCM[i][j] = String.valueOf(arrayILCM.get(i));
        }
        this.computeCMV();
    }

    private void computeCMV() {
        this.CMV = new boolean[15];
        //0
        double LENGTH1 = this.parameters.getDouble("LENGTH1");
        for (int index = 0; index < this.NUMPOINTS - 1; index++) {
            if (LENGTH1 < computeDistancePointToPoint(this.points[index], this.points[index + 1])) {
                this.CMV[0] = true;
                break;
            }
        }
        //1
        double RADIUS1 = this.parameters.getDouble("RADIUS1");
        for (int index = 0; index < this.NUMPOINTS - 2; index++) {
            double a = computeDistancePointToPoint(this.points[index], this.points[index + 1]);
            double b = computeDistancePointToPoint(this.points[index + 1], this.points[index + 2]);
            double c = computeDistancePointToPoint(this.points[index], this.points[index + 2]);
            if (RADIUS1 < a && RADIUS1 < b && RADIUS1 < c) {
                this.CMV[1] = true;
                break;
            }
        }
        //2
        double EPSILON = this.parameters.getDouble("EPSILON");
        for (int index = 0; index < this.NUMPOINTS - 2; index++) {
            double a = computeDistancePointToPoint(this.points[index], this.points[index + 1]);
            double b = computeDistancePointToPoint(this.points[index + 1], this.points[index + 2]);
            double c = computeDistancePointToPoint(this.points[index], this.points[index + 2]);
            double angle = Math.cos(Math.pow(a, 2) + Math.pow(b, 2) - Math.pow(c, 2)) / (2 * a * b);
            if (angle < Math.PI - EPSILON || angle > Math.PI + EPSILON) {
                this.CMV[2] = true;
                break;
            }
        }
        //3
        double AREA1 = this.parameters.getDouble("AREA1");
        for (int index = 0; index < this.NUMPOINTS - 2; index++) {
            double a = computeDistancePointToPoint(this.points[index], this.points[index + 1]);
            double b = computeDistancePointToPoint(this.points[index + 1], this.points[index + 2]);
            double c = computeDistancePointToPoint(this.points[index], this.points[index + 2]);
            double s = (a + b + c) / 2.0D;
            double area = Math.sqrt(s * (s - a) * (s - b) * (s - c));
            if (area > AREA1) {
                this.CMV[3] = true;
                break;
            }
        }
        //4
        int Q_PTS = this.parameters.getInt("Q_PTS");

        //TODO

        //5
        for (int index = 0; index < this.NUMPOINTS - 1; index++) {
            if (this.points[index + 1][0] - this.points[index][0] < 0) {
                this.CMV[5] = true;
                break;
            }
        }
        //6
        int N_PTS = this.parameters.getInt("N_PTS");
        double DIST = this.parameters.getDouble("DIST");
        if (N_PTS <= 3 && N_PTS <= NUMPOINTS && NUMPOINTS <= 13) {
            for (int i = 0; i < NUMPOINTS - N_PTS; i++) {
                if (Arrays.equals(points[i], points[i + N_PTS])) {

                } else {
                    for (int j = i + 1 ; j < NUMPOINTS - N_PTS - 1 ; j++) {
                        if (computeDistancePointToLine(points[j], computeEquationLine(points[i], points[i + N_PTS])) > DIST) {
                            this.CMV[6] = true;
                            break;
                        }
                    }
                }
            }
        }
    }

    private double[] computeEquationLine(double[] p1, double[] p2) {
        // ax + by + c = 0 : contains a, b and c
        double[] equation = new double[3];
        equation[0] = p1[1] - p2[1];
        equation[1] = p1[0] - p2[0];
        equation[2] = p1[0] * p2[1] - p2[0] * p1[1];
        return equation;
    }

    /**
     * compute the distance between one point p1 and one line defined by l1 and l2
     */
    private double computeDistancePointToLine(double[] p1, double[] equationLine) {
        return Math.abs(equationLine[0] * p1[0] + equationLine[1] * p1[1] + equationLine[2]) / Math.sqrt(Math.pow(equationLine[0], 2) + Math.pow(equationLine[1], 2));
    }

    /**
     * Compute the distance between two points
     */
    private double computeDistancePointToPoint(double[] p1, double[] p2) {
        return Math.sqrt(Math.pow(p1[0] - p2[0], 2) + Math.pow(p1[1] - p2[1], 2));
    }

    private JSONObject readJSON(String path) {
        try {
            BufferedReader buffer = new BufferedReader(new FileReader(path));
            String json = buffer.lines().reduce((acc, line) -> acc + line).get();
            return new JSONObject(json);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return null;
    }

    public static void main(String[] args) {
        new Model("input/input0.json");
    }

}
