package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import java.util.*;

public class analyser_v29 implements Analyser<double[], LoadState[]> {

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] result = new LoadState[n];
        if (n == 0) {
            return result;
        }

        double meanSlack = mean(metrics);
        double lambda = (meanSlack > 1e-9) ? 1.0 / meanSlack : 1.0;
        int overCount = 0, underCount = 0;
        for (int i = 0; i < n; i++) {
            double clamped = Math.max(0.0, metrics[i]);
            double survivalProb = Math.exp(-lambda * clamped);
            if (survivalProb < 0.15) { result[i] = LoadState.OVERLOADED; overCount++; }
            else if (survivalProb > 0.75) { result[i] = LoadState.UNDERLOADED; underCount++; }
            else result[i] = LoadState.BALANCED;
        }
        Log.printlnConcat(now, ": [analyser_v29] classified ", n, " cloudlets via exponential survival-probability hazard model (lambda=", lambda, ", over=", overCount, ", under=", underCount, ").");
        return result;
    }

    private double mean(double[] data) {
        double sum = 0.0;
        for (double v : data) sum += v;
        return (data.length > 0) ? sum / data.length : 0.0;
    }

    @Override
    public String inputSemantic() {
        return "cloudlet-estimated-slack-time-seconds";
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-load-state-exponential-survival-hazard";
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
