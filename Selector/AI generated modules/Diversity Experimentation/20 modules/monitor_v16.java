package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;

import java.util.List;

// Cloudlet level - fraction of a cloudlet's total instruction length already processed.
public class monitor_v16 implements Monitor<double[]> {

    private static final int OUTPUT_GUID = 1400;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        double[] result = new double[cloudlets.size()];

        for (int i = 0; i < cloudlets.size(); i++) {
            Cloudlet cl = cloudlets.get(i);
            long total = readSpace.getTotalLength(cl);
            long remaining = readSpace.getRemainingLength(cl);
            result[i] = (total > 0L) ? ((total - remaining) / (double) total) : 1.0;
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v16] computed cloudlet-progress-ratio for ", cloudlets.size(), " cloudlets.");

        return result;
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-progress-ratio: fraction of this cloudlet's total instruction length already processed, "
                + "computed as (totalLength - remainingLength) / totalLength, range 0.0 (not started) to 1.0 "
                + "(complete). Index i corresponds to getActiveCloudlets().get(i).";
    }

    @Override
    public int outputGuid() {
        return OUTPUT_GUID;
    }
}
