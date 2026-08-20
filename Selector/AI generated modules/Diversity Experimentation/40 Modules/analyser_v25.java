package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import java.util.*;

public class analyser_v25 implements Analyser<double[], LoadState[]> {

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] result = new LoadState[n];
        if (n == 0) {
            return result;
        }

        final double LATE_THRESHOLD = 0.0;
        final double AMPLE_SLACK_THRESHOLD = 100.0;
        int overCount = 0, underCount = 0;
        for (int i = 0; i < n; i++) {
            if (metrics[i] < LATE_THRESHOLD) { result[i] = LoadState.OVERLOADED; overCount++; }
            else if (metrics[i] > AMPLE_SLACK_THRESHOLD) { result[i] = LoadState.UNDERLOADED; underCount++; }
            else result[i] = LoadState.BALANCED;
        }
        Log.printlnConcat(now, ": [analyser_v25] classified ", n, " cloudlets via fixed slack-time thresholds (over=", overCount, ", under=", underCount, ").");
        return result;
    }

    @Override
    public String inputSemantic() {
        return "cloudlet-estimated-slack-time-seconds";
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-load-state-slack-fixed-threshold";
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
