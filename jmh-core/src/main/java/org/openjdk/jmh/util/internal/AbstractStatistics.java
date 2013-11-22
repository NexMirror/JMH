/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.jmh.util.internal;

public abstract class AbstractStatistics implements Statistics {
    private static final double[][] STUDENT_T = {
            {3.078, 6.314, 12.706, 31.821, 63.657, 318.313},
            {1.886, 2.920, 4.303, 6.965, 9.925, 22.327},
            {1.638, 2.353, 3.182, 4.541, 5.841, 10.215},
            {1.533, 2.132, 2.776, 3.747, 4.604, 7.173},
            {1.476, 2.015, 2.571, 3.365, 4.032, 5.893},
            {1.440, 1.943, 2.447, 3.143, 3.707, 5.208},
            {1.415, 1.895, 2.365, 2.998, 3.499, 4.782},
            {1.397, 1.860, 2.306, 2.896, 3.355, 4.499},
            {1.383, 1.833, 2.262, 2.821, 3.250, 4.296},
            {1.372, 1.812, 2.228, 2.764, 3.169, 4.143},
            {1.363, 1.796, 2.201, 2.718, 3.106, 4.024},
            {1.356, 1.782, 2.179, 2.681, 3.055, 3.929},
            {1.350, 1.771, 2.160, 2.650, 3.012, 3.852},
            {1.345, 1.761, 2.145, 2.624, 2.977, 3.787},
            {1.341, 1.753, 2.131, 2.602, 2.947, 3.733},
            {1.337, 1.746, 2.120, 2.583, 2.921, 3.686},
            {1.333, 1.740, 2.110, 2.567, 2.898, 3.646},
            {1.330, 1.734, 2.101, 2.552, 2.878, 3.610},
            {1.328, 1.729, 2.093, 2.539, 2.861, 3.579},
            {1.325, 1.725, 2.086, 2.528, 2.845, 3.552},
            {1.323, 1.721, 2.080, 2.518, 2.831, 3.527},
            {1.321, 1.717, 2.074, 2.508, 2.819, 3.505},
            {1.319, 1.714, 2.069, 2.500, 2.807, 3.485},
            {1.318, 1.711, 2.064, 2.492, 2.797, 3.467},
            {1.316, 1.708, 2.060, 2.485, 2.787, 3.450},
            {1.315, 1.706, 2.056, 2.479, 2.779, 3.435},
            {1.314, 1.703, 2.052, 2.473, 2.771, 3.421},
            {1.313, 1.701, 2.048, 2.467, 2.763, 3.408},
            {1.311, 1.699, 2.045, 2.462, 2.756, 3.396},
            {1.310, 1.697, 2.042, 2.457, 2.750, 3.385},
            {1.309, 1.696, 2.040, 2.453, 2.744, 3.375},
            {1.309, 1.694, 2.037, 2.449, 2.738, 3.365},
            {1.308, 1.692, 2.035, 2.445, 2.733, 3.356},
            {1.307, 1.691, 2.032, 2.441, 2.728, 3.348},
            {1.306, 1.690, 2.030, 2.438, 2.724, 3.340},
            {1.306, 1.688, 2.028, 2.434, 2.719, 3.333},
            {1.305, 1.687, 2.026, 2.431, 2.715, 3.326},
            {1.304, 1.686, 2.024, 2.429, 2.712, 3.319},
            {1.304, 1.685, 2.023, 2.426, 2.708, 3.313},
            {1.303, 1.684, 2.021, 2.423, 2.704, 3.307},
            {1.303, 1.683, 2.020, 2.421, 2.701, 3.301},
            {1.302, 1.682, 2.018, 2.418, 2.698, 3.296},
            {1.302, 1.681, 2.017, 2.416, 2.695, 3.291},
            {1.301, 1.680, 2.015, 2.414, 2.692, 3.286},
            {1.301, 1.679, 2.014, 2.412, 2.690, 3.281},
            {1.300, 1.679, 2.013, 2.410, 2.687, 3.277},
            {1.300, 1.678, 2.012, 2.408, 2.685, 3.273},
            {1.299, 1.677, 2.011, 2.407, 2.682, 3.269},
            {1.299, 1.677, 2.010, 2.405, 2.680, 3.265},
            {1.299, 1.676, 2.009, 2.403, 2.678, 3.261},
            {1.298, 1.675, 2.008, 2.402, 2.676, 3.258},
            {1.298, 1.675, 2.007, 2.400, 2.674, 3.255},
            {1.298, 1.674, 2.006, 2.399, 2.672, 3.251},
            {1.297, 1.674, 2.005, 2.397, 2.670, 3.248},
            {1.297, 1.673, 2.004, 2.396, 2.668, 3.245},
            {1.297, 1.673, 2.003, 2.395, 2.667, 3.242},
            {1.297, 1.672, 2.002, 2.394, 2.665, 3.239},
            {1.296, 1.672, 2.002, 2.392, 2.663, 3.237},
            {1.296, 1.671, 2.001, 2.391, 2.662, 3.234},
            {1.296, 1.671, 2.000, 2.390, 2.660, 3.232},
            {1.296, 1.670, 2.000, 2.389, 2.659, 3.229},
            {1.295, 1.670, 1.999, 2.388, 2.657, 3.227},
            {1.295, 1.669, 1.998, 2.387, 2.656, 3.225},
            {1.295, 1.669, 1.998, 2.386, 2.655, 3.223},
            {1.295, 1.669, 1.997, 2.385, 2.654, 3.220},
            {1.295, 1.668, 1.997, 2.384, 2.652, 3.218},
            {1.294, 1.668, 1.996, 2.383, 2.651, 3.216},
            {1.294, 1.668, 1.995, 2.382, 2.650, 3.214},
            {1.294, 1.667, 1.995, 2.382, 2.649, 3.213},
            {1.294, 1.667, 1.994, 2.381, 2.648, 3.211},
            {1.294, 1.667, 1.994, 2.380, 2.647, 3.209},
            {1.293, 1.666, 1.993, 2.379, 2.646, 3.207},
            {1.293, 1.666, 1.993, 2.379, 2.645, 3.206},
            {1.293, 1.666, 1.993, 2.378, 2.644, 3.204},
            {1.293, 1.665, 1.992, 2.377, 2.643, 3.202},
            {1.293, 1.665, 1.992, 2.376, 2.642, 3.201},
            {1.293, 1.665, 1.991, 2.376, 2.641, 3.199},
            {1.292, 1.665, 1.991, 2.375, 2.640, 3.198},
            {1.292, 1.664, 1.990, 2.374, 2.640, 3.197},
            {1.292, 1.664, 1.990, 2.374, 2.639, 3.195},
            {1.292, 1.664, 1.990, 2.373, 2.638, 3.194},
            {1.292, 1.664, 1.989, 2.373, 2.637, 3.193},
            {1.292, 1.663, 1.989, 2.372, 2.636, 3.191},
            {1.292, 1.663, 1.989, 2.372, 2.636, 3.190},
            {1.292, 1.663, 1.988, 2.371, 2.635, 3.189},
            {1.291, 1.663, 1.988, 2.370, 2.634, 3.188},
            {1.291, 1.663, 1.988, 2.370, 2.634, 3.187},
            {1.291, 1.662, 1.987, 2.369, 2.633, 3.185},
            {1.291, 1.662, 1.987, 2.369, 2.632, 3.184},
            {1.291, 1.662, 1.987, 2.368, 2.632, 3.183},
            {1.291, 1.662, 1.986, 2.368, 2.631, 3.182},
            {1.291, 1.662, 1.986, 2.368, 2.630, 3.181},
            {1.291, 1.661, 1.986, 2.367, 2.630, 3.180},
            {1.291, 1.661, 1.986, 2.367, 2.629, 3.179},
            {1.291, 1.661, 1.985, 2.366, 2.629, 3.178},
            {1.290, 1.661, 1.985, 2.366, 2.628, 3.177},
            {1.290, 1.661, 1.985, 2.365, 2.627, 3.176},
            {1.290, 1.661, 1.984, 2.365, 2.627, 3.175},
            {1.290, 1.660, 1.984, 2.365, 2.626, 3.175},
            {1.290, 1.660, 1.984, 2.364, 2.626, 3.174},
            {1.282, 1.645, 1.960, 2.326, 2.576, 3.090}
    };

    /**
     * Returns the interval c1, c2 of which there's an 1-alpha
     * probability of the mean being within the interval.
     *
     * @param confidence level
     * @return the confidence interval
     */
    @Override
    public double[] getConfidenceInterval(double confidence) {
        double[] interval = new double[2];

        double ip = getStudentT(1 - (1 - confidence)/2, getN() - 1);
        interval[0] = getMean() - ip * (getStandardDeviation() / Math.sqrt(getN()));
        interval[1] = getMean() + ip * (getStandardDeviation() / Math.sqrt(getN()));

        return interval;
    }

    protected double getStudentT(double confidence, int n) {
        if (n <= 1) throw new IllegalStateException();

        double[] indices = {0.90, 0.95, 0.975, 0.99, 0.995, 0.999};

        int index = indices.length - 1;
        for (int i = 0; i < indices.length - 1; i++) {
            if (indices[i] <= confidence && confidence < indices[i + 1]) {
                index = i;
                break;
            }
        }

        if (n > STUDENT_T.length) {
            n = STUDENT_T.length;
        }

        return STUDENT_T[n - 1][index];
    }

    @Override
    public double getMeanError(double confidence) {
        if (getN() < 2) return Double.NaN;
        double ip = getStudentT(1 - (1 - confidence)/2, getN() - 1);
        return ip * (getStandardDeviation() / Math.sqrt(getN()));
    }

    @Override
    public String toString() {
        return "N:" + getN() + " Mean: " + getMean()
                + " Min: " + getMin() + " Max: " + getMax()
                + " StdDev: " + getStandardDeviation();
    }

    @Override
    public double getMean() {
        if (getN() > 0) {
            return getSum() / getN();
        } else {
            return Double.NaN;
        }
    }

    @Override
    public double getStandardDeviation() {
        return Math.sqrt(getVariance());
    }

}
