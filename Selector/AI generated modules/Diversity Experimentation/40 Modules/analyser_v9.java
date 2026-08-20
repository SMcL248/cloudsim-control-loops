package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import java.util.*;

public class analyser_v9 implements Analyser<double[], LoadState[]> {

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] result = new LoadState[n];
        if (n == 0) {
            return result;
        }

        int overCount = 0, underCount = 0;
        for (int i = 0; i < n; i++) {
            int rank = 0;
            for (int j = 0; j < n; j++) if (metrics[j] < metrics[i]) rank++;
            double percentileRank = (n > 1) ? (double) rank / (n - 1) : 0.5;
            if (percentileRank >= 0.90) { result[i] = LoadState.OVERLOADED; overCount++; }
            else if (percentileRank <= 0.10) { result[i] = LoadState.UNDERLOADED; underCount++; }
            else result[i] = LoadState.BALANCED;
        }
        Log.printlnConcat(now, ": [analyser_v9] classified ", n, " cloudlets via pairwise-count rank percentile (over=", overCount, ", under=", underCount, ").");
        return result;
    }

    @Override
    public String inputSemantic() {
        return "cloudlet-remaining-length-fraction";
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-load-state-rank-percentile";
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
