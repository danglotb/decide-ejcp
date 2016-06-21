import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by bdanglot on 20/06/16.
 */
public class Model {

    /**
     * input
     */
    private int numPoints;
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
        this.numPoints = this.input.getInt("NUMPOINTS");
        this.parameters = this.input.getJSONObject("PARAMETERS");
        this.points = new double[numPoints][2];
        this.PUV = new boolean[numPoints];
        this.LCM = new String[numPoints][numPoints];
        JSONArray arrayPoints = this.input.getJSONArray("points");
        JSONArray arrayPUV = this.input.getJSONArray("PUV");
        JSONObject arrayLCM = this.input.getJSONObject("LCM");
        for (int i = 0; i < numPoints; i++) {
            this.points[i][0] = arrayPoints.getJSONArray(i).getDouble(0);
            this.points[i][1] = arrayPoints.getJSONArray(i).getDouble(1);
            this.PUV[i] = arrayPUV.getBoolean(i);
            JSONArray arrayILCM = arrayLCM.getJSONArray(String.valueOf(i));
            for (int j = 0; j < numPoints; j++)
                this.LCM[i][j] = String.valueOf(arrayILCM.get(i));
        }
        this.computeCMV();
    }

    private void computeCMV() {
        this.CMV = new boolean[15];
        //0
        double LENGTH1 = this.parameters.getDouble("LENGTH1");
        for (int index = 0; index < this.numPoints - 1; index++) {
            if (LENGTH1 < computeDistance(this.points[index][0], this.points[index][1], this.points[index + 1][0], this.points[index + 1][1])) {
                this.CMV[0] = true;
                break;
            }
        }
        //1
        double RADIUS1 = this.parameters.getDouble("RADIUS1");
        for (int index = 0; index < this.numPoints - 2; index++) {
            if (RADIUS1 < computeDistance(this.points[index][0], this.points[index][1], this.points[index + 1][0], this.points[index + 1][1]) &&
                    RADIUS1 < computeDistance(this.points[index + 1][0], this.points[index + 1][1], this.points[index + 2][0], this.points[index + 2][1]) &&
                    RADIUS1 < computeDistance(this.points[index][0], this.points[index][1], this.points[index + 2][0], this.points[index + 2][1])) {
                this.CMV[1] = true;
                break;
            }
        }
        //2
        double EPSILON = this.parameters.getDouble("EPSILON");
        for (int index = 0; index < this.numPoints - 2; index++) {
            double a = computeDistance(this.points[index][0], this.points[index][1], this.points[index + 1][0], this.points[index + 1][1]);
            double b = computeDistance(this.points[index][0], this.points[index][1], this.points[index + 2][0], this.points[index + 2][1]);
            double c = computeDistance(this.points[index + 1][0], this.points[index + 1][1], this.points[index + 2][0], this.points[index + 2][1]);
            double angle = Math.cos(Math.pow(a, 2) + Math.pow(b, 2) - Math.pow(c, 2)) / (2 * a * b);
            if (angle < Math.PI - EPSILON || angle > Math.PI + EPSILON) {
                this.CMV[2] = true;
                break;
            }
        }
        //3
        double AREA1 = this.parameters.getDouble("AREA1");
        for (int index = 0; index < this.numPoints - 2; index++) {
            double a = computeDistance(this.points[index][0], this.points[index][1], this.points[index + 1][0], this.points[index + 1][1]);
            double b = computeDistance(this.points[index][0], this.points[index][1], this.points[index + 2][0], this.points[index + 2][1]);
            double c = computeDistance(this.points[index + 1][0], this.points[index + 1][1], this.points[index + 2][0], this.points[index + 2][1]);
            double s = (a + b + c) / 2.0D;
            double area = Math.sqrt(s * (s - a) * (s - b) * (s - c));
            if (area > AREA1) {
                this.CMV[3] = true;
                break;
            }
        }
        //4
        int Q_PTS = this.parameters.getInt("Q_PTS");

    }

    private double computeDistance(double x1, double x2, double y1, double y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
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
