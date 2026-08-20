package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import java.util.*;

public class analyser_v36 implements Analyser<double[], LoadState[]> {

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] result = new LoadState[n];
        if (n == 0) {
            return result;
        }

        if (n < 4) {
            for (int i = 0; i < n; i++) result[i] = LoadState.BALANCED;
        } else {
            double globalSplit = entropyThreshold(metrics, 8);
            List<Double> lowerList = new ArrayList<>();
            List<Double> upperList = new ArrayList<>();
            for (double v : metrics) { if (v <= globalSplit) lowerList.add(v); else upperList.add(v); }
            double lowerBoundary = globalSplit;
            double upperBoundary = globalSplit;
            if (lowerList.size() >= 4) {
                double[] lowArr = new double[lowerList.size()];
                for (int k = 0; k < lowArr.length; k++) lowArr[k] = lowerList.get(k);
                lowerBoundary = entropyThreshold(lowArr, 6);
            }
            if (upperList.size() >= 4) {
                double[] upArr = new double[upperList.size()];
                for (int k = 0; k < upArr.length; k++) upArr[k] = upperList.get(k);
                upperBoundary = entropyThreshold(upArr, 6);
            }
            int overCount = 0, underCount = 0;
            for (int i = 0; i < n; i++) {
                if (metrics[i] > globalSplit && metrics[i] > upperBoundary) { result[i] = LoadState.OVERLOADED; overCount++; }
                else if (metrics[i] <= globalSplit && metrics[i] < lowerBoundary) { result[i] = LoadState.UNDERLOADED; underCount++; }
                else result[i] = LoadState.BALANCED;
            }
            Log.printlnConcat(now, ": [analyser_v36] classified ", n, " cloudlets via entropy-minimizing recursive histogram split (over=", overCount, ", under=", underCount, ").");
        }
        return result;
    }

    private double entropyThreshold(double[] data, int bins) {
        if (data.length < 2) return data.length > 0 ? data[0] : 0.0;
        double minV = data[0], maxV = data[0];
        for (double v : data) { if (v < minV) minV = v; if (v > maxV) maxV = v; }
        double range = maxV - minV;
        if (range < 1e-9) return minV;
        int[] counts = new int[bins];
        for (double v : data) {
            int idx = (int) ((v - minV) / range * bins);
            if (idx >= bins) idx = bins - 1;
            if (idx < 0) idx = 0;
            counts[idx]++;
        }
        int total = data.length;
        double bestEntropy = Double.MAX_VALUE;
        int bestSplit = bins / 2;
        for (int split = 1; split < bins; split++) {
            int leftCount = 0;
            for (int i = 0; i < split; i++) leftCount += counts[i];
            int rightCount = total - leftCount;
            double leftEntropy = binEntropy(counts, 0, split, leftCount);
            double rightEntropy = binEntropy(counts, split, bins, rightCount);
            double weighted = ((double) leftCount / total) * leftEntropy
                    + ((double) rightCount / total) * rightEntropy;
            if (weighted < bestEntropy) {
                bestEntropy = weighted;
                bestSplit = split;
            }
        }
        return minV + ((double) bestSplit / bins) * range;
    }

    private double binEntropy(int[] counts, int from, int to, int groupTotal) {
        if (groupTotal <= 0) return 0.0;
        double entropy = 0.0;
        for (int i = from; i < to; i++) {
            if (counts[i] == 0) continue;
            double p = (double) counts[i] / groupTotal;
            entropy -= p * (Math.log(p) / Math.log(2));
        }
        return entropy;
    }

    @Override
    public String inputSemantic() {
        return "cloudlet-remaining-length-fraction";
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-load-state-entropy-recursive-split";
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
