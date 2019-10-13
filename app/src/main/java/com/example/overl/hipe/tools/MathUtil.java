package com.example.overl.hipe.tools;

//import static whu.example.com.gnssins.util.Constants.D2R;
//import static whu.example.com.gnssins.util.Constants.Height;
//import static whu.example.com.gnssins.util.Constants.R2D;
//import static whu.example.com.gnssins.util.Constants.Rm;
//import static whu.example.com.gnssins.util.Constants.Rn;
//import static whu.example.com.gnssins.util.Constants.lat_wanda;
//import static whu.example.com.gnssins.util.Constants.lon_wanda;

/**
 * Created by CXG on 2017/10/23.
 *
 * 数学计算相关函数
 */

public class MathUtil {
    /*判断两个浮点数是否相等
   * */
    public static boolean isEqualFloat(float x, float y) {
        boolean temp = false;
        if (Math.abs(x - y) < 1e-4) {
            temp = true;
        } else {
            temp = false;
        }
        return temp;
    }

}
