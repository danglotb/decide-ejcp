import com.sun.org.apache.xerces.internal.impl.dv.xs.BooleanDV;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by bdanglot on 20/06/16.
 */
public class Model {

    private static final int NUMBER_CLAUSE = 15;

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
        this.PUV = new boolean[NUMBER_CLAUSE];
        this.LCM = new String[NUMBER_CLAUSE][NUMBER_CLAUSE];

        JSONArray arrayPoints = this.input.getJSONArray("points");
        JSONArray arrayPUV = this.input.getJSONArray("PUV");
        JSONObject arrayLCM = this.input.getJSONObject("LCM");

        for (int i = 0; i < NUMBER_CLAUSE; i++) {
            JSONArray arrayILCM = arrayLCM.getJSONArray(String.valueOf(i));
            for (int j = 0; j < NUMBER_CLAUSE; j++)
                this.LCM[i][j] = arrayILCM.getString(j);
            this.PUV[i] = arrayPUV.getBoolean(i);
        }

        for (int i = 0; i < NUMPOINTS; i++) {
            this.points[i][0] = arrayPoints.getJSONArray(i).getDouble(0);
            this.points[i][1] = arrayPoints.getJSONArray(i).getDouble(1);
        }

    }

    private String decide() {
        this.computeCMV();
        this.computePUM();
        this.computeFUV();
        for (boolean b : this.FUV) {
            if (!b)
                return "No";
        }
        return "Yes";
    }

    private void computeFUV() {
        this.FUV = new boolean[NUMBER_CLAUSE];
        for (int i = 0; i < NUMBER_CLAUSE; i++) {
            if (!this.PUV[i]) {
                this.FUV[i] = true;
            } else {
                this.FUV[i] = true;
                for (int j = 0; j < NUMBER_CLAUSE; j++) {
                    if (j != i)
                        this.FUV[i] &= this.PUM[i][j];
                }
            }
        }
    }

    private void computePUM() {
        this.PUM = new boolean[NUMBER_CLAUSE][NUMBER_CLAUSE];
        for (int i = 0; i < this.LCM.length; i++) {
            for (int j = 0; j < this.LCM[i].length; j++) {
                switch (this.LCM[i][j]) {
                    case "NOTUSED":
                        this.PUM[i][j] = true;
                        break;
                    case "ANDD":
                        this.PUM[i][j] = this.CMV[i] && this.CMV[j];
                        break;
                    case "ORR":
                        this.PUM[i][j] = this.CMV[i] || this.CMV[j];
                        break;
                }
            }
        }
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
        int QUADS = this.parameters.getInt("QUADS");
        for (int index = 0; index < this.NUMPOINTS - (Q_PTS - 1); index++) {
            int lieCounter = 0;
            int currentQuad = 1;
            for (int ndx = index; ndx < (index + Q_PTS); ndx++) {
                if (getQuadranNumber(this.points[ndx]) != currentQuad) {
                    lieCounter++;
                    if (lieCounter > QUADS) {
                        this.CMV[4] = true;
                        break;
                    }
                }

                currentQuad++;
                if (currentQuad > 4) {
                    currentQuad = 1;
                }
            }

            if (lieCounter > 3) {
                break;
            }
        }

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
        if (NUMPOINTS >= 3) {
            for (int i = 0; i < NUMPOINTS - N_PTS; i++) {
                if (Arrays.equals(points[i], points[i + N_PTS])) {
                    for (int j = i + 1; j < NUMPOINTS - N_PTS - 1; j++) {
                        double d = computeDistancePointToPoint(points[i], points[j]);
                        if (d > DIST) {
                            this.CMV[6] = true;
                            break;
                        }
                    }
                } else {
                    for (int j = i + 1; j < NUMPOINTS - N_PTS - 1; j++) {
                        if (computeDistancePointToLine(points[j], computeEquationLine(points[i], points[i + N_PTS])) > DIST) {
                            this.CMV[6] = true;
                            break;
                        }
                    }
                }
                if (this.CMV[6])
                    break;
            }
        }
        //7
        int K_PTS = this.parameters.getInt("K_PTS");
        if (NUMPOINTS >= 3) {
            for (int i = 0; i < NUMPOINTS - K_PTS; i++) {
                if (computeDistancePointToPoint(points[i], points[i + K_PTS]) > LENGTH1) {
                    this.CMV[7] = true;
                    break;
                }
            }
        }
        //8
        int A_PTS = this.parameters.getInt("A_PTS");
        int B_PTS = this.parameters.getInt("B_PTS");
        if (NUMPOINTS >= 5) {
            for (int i = 0; i < NUMPOINTS - (A_PTS + B_PTS); i++) {
                double d1 = computeDistancePointToPoint(points[i], points[i + A_PTS]);
                double d2 = computeDistancePointToPoint(points[i + A_PTS], points[i + B_PTS]);
                if (d1 > RADIUS1 && d2 > RADIUS1) {
                    this.CMV[8] = true;
                    break;
                }
            }
        }
        //9
        int C_PTS = this.parameters.getInt("C_PTS");
        int D_PTS = this.parameters.getInt("D_PTS");
        if (NUMPOINTS >= 5) {
            for (int index = 0; index < this.NUMPOINTS - (C_PTS + D_PTS); index++) {
                if (Arrays.equals(this.points[index], this.points[index + C_PTS]) ||
                        Arrays.equals(this.points[index + C_PTS], this.points[index + D_PTS]) ||
                        Arrays.equals(this.points[index], this.points[index + D_PTS]))
                    continue;
                double a = computeDistancePointToPoint(this.points[index], this.points[index + C_PTS]);
                double b = computeDistancePointToPoint(this.points[index + C_PTS], this.points[index + C_PTS]);
                double c = computeDistancePointToPoint(this.points[index], this.points[index + C_PTS]);
                double angle = Math.cos(Math.pow(a, 2) + Math.pow(b, 2) - Math.pow(c, 2)) / (2 * a * b);
                if (angle < Math.PI - EPSILON || angle > Math.PI + EPSILON) {
                    this.CMV[9] = true;
                    break;
                }
            }
        }
        //10
        int E_PTS = this.parameters.getInt("E_PTS");
        int F_PTS = this.parameters.getInt("F_PTS");
        if (NUMPOINTS >= 5) {
            for (int index = 0; index < this.NUMPOINTS - (E_PTS + F_PTS); index++) {
                double a = computeDistancePointToPoint(this.points[index], this.points[index + E_PTS]);
                double b = computeDistancePointToPoint(this.points[index + E_PTS], this.points[index + F_PTS]);
                double c = computeDistancePointToPoint(this.points[index], this.points[index + F_PTS]);
                double s = (a + b + c) / 2.0D;
                double area = Math.sqrt(s * (s - a) * (s - b) * (s - c));
                if (area > AREA1) {
                    this.CMV[10] = true;
                    break;
                }
            }
        }
        //11
        int G_PTS = this.parameters.getInt("G_PTS");
        if (NUMPOINTS >= 3) {
            for (int index = 0; index < this.NUMPOINTS - G_PTS; index++) {
                if (this.points[index + G_PTS][0] - this.points[index][0] < 0) {
                    this.CMV[11] = true;
                    break;
                }
            }
        }
        //12
        int LENGTH2 = this.parameters.getInt("LENGTH2");
        if (NUMPOINTS >= 3) {
            for (int index = 0; index < this.NUMPOINTS - K_PTS; index++) {
                double d1 = computeDistancePointToPoint(this.points[index], this.points[index + K_PTS]);
                if (d1 > LENGTH1) {
                    for (int index1 = 0; index1 < this.NUMPOINTS - K_PTS; index1++) {
                        double d2 = computeDistancePointToPoint(this.points[index1], this.points[index1 + K_PTS]);
                        if (d2 < LENGTH2) {
                            this.CMV[12] = true;
                            break;
                        }
                    }
                }
                if (this.CMV[12])
                    break;
            }
        }
        //13
        double RADIUS2 = this.parameters.getDouble("RADIUS2");
        if (NUMPOINTS >= 5) {
            for (int i = 0; i < NUMPOINTS - (A_PTS + B_PTS); i++) {
                double d1 = computeDistancePointToPoint(this.points[i], this.points[i + A_PTS]);
                double d2 = computeDistancePointToPoint(this.points[i + A_PTS], this.points[i + B_PTS]);
                if (d1 > RADIUS1 && d2 > RADIUS1) {
                    for (int i1 = 0; i1 < NUMPOINTS - (A_PTS + B_PTS); i1++) {
                        double d3 = computeDistancePointToPoint(this.points[i1], this.points[i1 + A_PTS]);
                        double d4 = computeDistancePointToPoint(this.points[i1 + A_PTS], this.points[i1 + B_PTS]);
                        if (d3 <= RADIUS2 && d4 <= RADIUS2) {
                            this.CMV[13] = true;
                            break;
                        }
                    }
                }
                if (this.CMV[13])
                    break;
            }
        }
        //14
        double AREA2 = this.parameters.getDouble("AREA2");
        if (NUMPOINTS >= 5) {
            for (int index = 0; index < this.NUMPOINTS - (E_PTS + F_PTS); index++) {
                double a = computeDistancePointToPoint(this.points[index], this.points[index + E_PTS]);
                double b = computeDistancePointToPoint(this.points[index + E_PTS], this.points[index + F_PTS]);
                double c = computeDistancePointToPoint(this.points[index], this.points[index + F_PTS]);
                double s = (a + b + c) / 2.0D;
                double area = Math.sqrt(s * (s - a) * (s - b) * (s - c));
                if (area > AREA1) {
                    for (int index1 = 0; index1 < this.NUMPOINTS - (E_PTS + F_PTS); index1++) {
                        double a1 = computeDistancePointToPoint(this.points[index1], this.points[index1 + E_PTS]);
                        double b1 = computeDistancePointToPoint(this.points[index1 + E_PTS], this.points[index1 + F_PTS]);
                        double c1 = computeDistancePointToPoint(this.points[index1], this.points[index1 + F_PTS]);
                        double s1 = (a1 + b1 + c1) / 2.0D;
                        double area2 = Math.sqrt(s1 * (s1 - a1) * (s1 - b1) * (s1 - c1));
                        if (area2 > AREA2) {
                            this.CMV[14] = true;
                            break;
                        }
                    }
                }
                if (this.CMV[14])
                    break;
            }
        }
    }

    /**
     * Compute the equation of a line formed by the two given points.
     *
     * @return the equation in an array of double
     */
    private double[] computeEquationLine(double[] p1, double[] p2) {
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

    /**
     * Return the quadran number for a given point
     */
    private int getQuadranNumber(double[] p) {
        double x = p[0];
        double y = p[1];

        if (x >= 0 && y >= 0) {
            return 1;
        } else if (x < 0 && y >= 0) {
            return 2;
        } else if (x < 0 && y < 0) {
            return 3;
        } else {
            return 4;
        }
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
        if (args.length > 0) {
            Model m = new Model(args[0]);
            System.out.println(m.decide());
        }
        else {
            File folder = new File("input");
            File[] listOfFiles = folder.listFiles();

            for (File f : listOfFiles) {
                Model m = new Model(f.getPath());
                System.out.println(f.getName() + ": " + m.decide());
            }
        }

    }

}
