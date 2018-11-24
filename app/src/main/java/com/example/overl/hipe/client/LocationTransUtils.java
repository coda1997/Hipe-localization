package com.example.overl.hipe.client;

public class LocationTransUtils {
    public static double[] LB2xy(double[] LB) {
        //LB[0] latitude;
        //LB[1] longitude;
        double[] xy = new double[2];
        //long axis of the ellipsoid
        double a = 6378137;
        //送花ort axis of the ellipsoid
        double b = 6356752.3141;
        //oblateness of the ellipsoid
        double Alfa = 1.0 / 298.257222101;
        //longitude of the central meridian
        double radl0 = Math.toRadians(114);
        //calculate the second eccentricity of the ellipsoid
        double e1 = Math.sqrt((a / b) * (a / b) - 1);
        //calculate the first eccentricity of the ellipsoid
        double e = Math.sqrt(1 - (b / a) * (b / a));
        //obtain the coor : L B
        double dmslat = LB[0];
        double dmslon = LB[1];
        double radlat = Math.toRadians(dmslat);
        double radlon = Math.toRadians(dmslon);
        double l = radlon - radl0;

        double sb = Math.sin(radlat);
        double cb = Math.cos(radlat);
        double t = sb / cb;
        double ita = e1 * cb;

        //calculate the a0 a2 a4 a6 a8
        double[] a02468 = a02468_Cal(a, e);

        double X = a02468[0] * radlat - sb * cb * ((a02468[1] - a02468[2] + a02468[3]) + (2.0 * a02468[2] - 16.0 * a02468[3] / 3.0) * sb * sb + (16.0 / 3.0) * a02468[3] * Math.pow(sb, 4));
        double c = a * a / b;
        double v = Math.sqrt(1.0 + e1 * e1 * cb * cb);
        double N = c / v;

        xy[0] = X + N * sb * cb * l * l / 2.0 + N * sb * Math.pow(cb, 3) * (5.0 - t * t + 9.0 * ita * ita + 4.0 * Math.pow(ita, 4)) * Math.pow(l, 4) / 24.0 + N * sb * Math.pow(cb, 5) * (61.0 - 58.0 * t * t + Math.pow(t, 4)) * Math.pow(l, 6) / 720.0;
        xy[1] = N * cb * l + N * Math.pow(cb, 3) * (1 - t * t + ita * ita) * Math.pow(l, 3) / 6.0 + N * Math.pow(cb, 5) * (5.0 - 18.0 * t * t + Math.pow(t, 4) + 14.0 * ita * ita - 58.0 * ita * ita * t * t) * Math.pow(l, 5) / 120.0;

        return xy;
    }

    private static double[] a02468_Cal(double a, double e) {
        double[] a02468 = new double[5];
        double m0 = a * (1.0 - e * e);
        double m2 = 3.0 * e * e * m0 / 2.0;
        double m4 = 5.0 * e * e * m2 / 4.0;
        double m6 = 7.0 * e * e * m4 / 6.0;
        double m8 = 9.0 * e * e * m6 / 8.0;
        a02468[0] = m0 + m2 / 2.0 + 3.0 * m4 / 8.0 + 5.0 * m6 / 16.0 + 35.0 * m8 / 128.0;
        a02468[1] = m2 / 2.0 + m4 / 2.0 + 15.0 * m6 / 32.0 + 7.0 * m8 / 16.0;
        a02468[2] = m4 / 8.0 + 3.0 * m6 / 16.0 + 7.0 * m8 / 32.0;
        a02468[3] = m6 / 32.0 + m8 / 16.0;
        a02468[4] = m8 / 128.0;
        return a02468;
    }
    public static double[] randomTrainsfer(double a, double b) {
        double[] s1Old = new double[]{3372329.9306, 550544.2590};
        double[] s2Old = new double[]{3372329.9246, 550551.4651};
        double[] s3Old = new double[]{3372325.7055, 550551.4580};

        //may reset s1, s2 s3 values
        double[] s1New = new double[]{3.43, -0.165};
        double[] s2New = new double[]{-0.441, -5.56};
        double[] s3New = new double[]{-3.6, -3.17};

        double[] mx = new double[]{s2Old[0] - s3Old[0], s2Old[1] - s3Old[1]};
        double[] my = new double[]{s2Old[0] - s1Old[0], s2Old[1] - s1Old[1]};
        double[] nx = new double[]{s2New[0] - s3New[0], s2New[1] - s3New[1]};
        double[] ny = new double[]{s2New[0] - s1New[0], s2New[1] - s1New[1]};

        double lenMx = Math.sqrt(mx[0] * mx[0] + mx[1] * mx[1]);
        double lenNx = Math.sqrt(nx[0] * nx[0] + nx[1] * nx[1]);

        double cosTheta = Math.abs((mx[0] * nx[0] + mx[1] * nx[1]) / (lenMx * lenNx));
        double sinTheta = Math.sqrt(1 - cosTheta * cosTheta);

        double x = 0.9 * cosTheta * (a - s2Old[0]) - 0.9 * sinTheta * (b - s2Old[1]) + s2New[0];

        double y = -0.8 * sinTheta * (a - s2Old[0]) - 0.8 * cosTheta * (b - s2Old[1]) + s2New[1];

        return new double[]{x, y};
    }

    public static double[] bigTransfer(double[] xy){
        double[] res = LB2xy(xy);
        return randomTrainsfer(res[0],res[1]+500000);
    }
}
