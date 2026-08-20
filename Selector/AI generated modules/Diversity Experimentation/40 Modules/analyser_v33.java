package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import java.util.*;

public class analyser_v33 implements Analyser<double[], LoadState[]> {

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] result = new LoadState[n];
        if (n == 0) {
            return result;
        }

        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        double[] peDemand = new double[n];
        double maxPe = 1.0;
        for (int i = 0; i < n; i++) {
            peDemand[i] = readSpace.getCloudletNumberOfPes(cloudlets.get(i));
            if (peDemand[i] > maxPe) maxPe = peDemand[i];
        }
        double[] composite = new double[n];
        for (int i = 0; i < n; i++) composite[i] = 0.7 * metrics[i] + 0.3 * (peDemand[i] / maxPe);
        double[] sortedComposite = composite.clone();
        Arrays.sort(sortedComposite);
        double q1 = percentile(sortedComposite, 25.0);
        double q3 = percentile(sortedComposite, 75.0);
        int overCount = 0, underCount = 0;
        for (int i = 0; i < n; i++) {
            if (composite[i] > q3) { result[i] = LoadState.OVERLOADED; overCount++; }
            else if (composite[i] < q1) { result[i] = LoadState.UNDERLOADED; underCount++; }
            else result[i] = LoadState.BALANCED;
        }
        Log.printlnConcat(now, ": [analyser_v33] classified ", n, " cloudlets via weighted composite of remaining-fraction and PE-demand (over=", overCount, ", under=", underCount, ").");
        return result;
    }

    private double percentile(double[] sortedData, double p) {
        int m = sortedData.length;
        if (m == 0) return 0.0;
        if (m == 1) return sortedData[0];
        double rank = (p / 100.0) * (m - 1);
        int lowIdx = (int) Math.floor(rank);
        int highIdx = (int) Math.ceil(rank);
        if (lowIdx == highIdx) return sortedData[lowIdx];
        double frac = rank - lowIdx;
        return sortedData[lowIdx] * (1.0 - frac) + sortedData[highIdx] * frac;
    }

    @Override
    public String inputSemantic() {
        return "cloudlet-remaining-length-fraction";
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-load-state-composite-remaining-pe-weighted";
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
