package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import java.util.*;

public class analyser_v39 implements Analyser<double[], LoadState[]> {

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] result = new LoadState[n];
        if (n == 0) {
            return result;
        }

        double[] logVals = new double[n];
        for (int i = 0; i < n; i++) logVals[i] = Math.log1p(Math.max(0.0, metrics[i]));
        double minL = logVals[0], maxL = logVals[0];
        for (double v : logVals) { if (v < minL) minL = v; if (v > maxL) maxL = v; }
        double range = maxL - minL;
        int overCount = 0, underCount = 0;
        for (int i = 0; i < n; i++) {
            double norm = (range > 1e-9) ? (logVals[i] - minL) / range : 0.5;
            if (norm > 0.70) { result[i] = LoadState.OVERLOADED; overCount++; }
            else if (norm < 0.30) { result[i] = LoadState.UNDERLOADED; underCount++; }
            else result[i] = LoadState.BALANCED;
        }
        Log.printlnConcat(now, ": [analyser_v39] classified ", n, " cloudlets via log-scale min-max normalized magnitude (over=", overCount, ", under=", underCount, ").");
        return result;
    }

    @Override
    public String inputSemantic() {
        return "cloudlet-total-length-mi";
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-load-state-log-minmax-normalized";
    }

    @Override
    public int inputGuid() {
        return 1400;
    }

    @Override
    public int outputGuid() {
        return 2400;
    }
}
