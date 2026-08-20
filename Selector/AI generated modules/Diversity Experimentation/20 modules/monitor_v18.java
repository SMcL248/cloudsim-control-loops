package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;

import java.util.List;

// Cloudlet level - relative size deviation of this cloudlet vs the mean of all active cloudlets.
public class monitor_v18 implements Monitor<double[]> {

    private static final int OUTPUT_GUID = 1400;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        double[] result = new double[cloudlets.size()];

        double sum = 0.0;
        for (Cloudlet cl : cloudlets) {
            sum += readSpace.getTotalLength(cl);
        }
        double mean = (cloudlets.size() > 0) ? (sum / cloudlets.size()) : 0.0;

        for (int i = 0; i < cloudlets.size(); i++) {
            Cloudlet cl = cloudlets.get(i);
            long length = readSpace.getTotalLength(cl);
            result[i] = (mean > 0.0) ? ((length - mean) / mean) : 0.0;
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v18] computed cloudlet-size-outlier-score for ", cloudlets.size(), " cloudlets.");

        return result;
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-size-outlier-score: this cloudlet's total length relative deviation from the mean total "
                + "length of all currently active cloudlets, computed as (thisLength - meanLength) / meanLength. "
                + "Positive values indicate an above-average (heavier) cloudlet, negative indicates below-average, "
                + "0.0 if the swarm mean is zero. Index i corresponds to getActiveCloudlets().get(i).";
    }

    @Override
    public int outputGuid() {
        return OUTPUT_GUID;
    }
}
