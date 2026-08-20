package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import java.util.*;

public class analyser_v18 implements Analyser<double[], LoadState[]> {

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] result = new LoadState[n];
        if (n == 0) {
            return result;
        }

        double meanVal = mean(metrics);
        double stdVal = stddev(metrics, meanVal);
        int overCount = 0, underCount = 0;
        for (int i = 0; i < n; i++) {
            double z = (stdVal > 1e-9) ? (metrics[i] - meanVal) / stdVal : 0.0;
            double score = Math.tanh(z);
            if (score > 0.4) { result[i] = LoadState.OVERLOADED; overCount++; }
            else if (score < -0.4) { result[i] = LoadState.UNDERLOADED; underCount++; }
            else result[i] = LoadState.BALANCED;
        }
        Log.printlnConcat(now, ": [analyser_v18] classified ", n, " cloudlets via tanh-squashed soft z-score (over=", overCount, ", under=", underCount, ").");
        return result;
    }

    private double mean(double[] data) {
        double sum = 0.0;
        for (double v : data) sum += v;
        return (data.length > 0) ? sum / data.length : 0.0;
    }
    private double stddev(double[] data, double meanVal) {
        double sq = 0.0;
        for (double v : data) sq += (v - meanVal) * (v - meanVal);
        return (data.length > 0) ? Math.sqrt(sq / data.length) : 0.0;
    }

    @Override
    public String inputSemantic() {
        return "cloudlet-remaining-length-fraction";
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-load-state-tanh-soft-score";
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
