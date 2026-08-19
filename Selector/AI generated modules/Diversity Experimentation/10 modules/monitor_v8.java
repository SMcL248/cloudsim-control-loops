package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import java.util.List;

// Cloudlet-level monitor: progress fraction.
// For each active cloudlet, reports the fraction of its total length that
// has already been processed: 1 - (remaining / total). A value near 1.0
// means the cloudlet is nearly done; a value near 0.0 means it has barely
// started.
public class monitor_v8 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        double[] result = new double[cloudlets.size()];

        for (int i = 0; i < cloudlets.size(); i++) {
            Cloudlet cl = cloudlets.get(i);

            long total = readSpace.getTotalLength(cl);
            long remaining = readSpace.getRemainingLength(cl);

            if (total <= 0) {
                result[i] = -1.0;
                continue;
            }

            result[i] = 1.0 - ((double) remaining / (double) total);
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v8] computed cloudlet progress fraction for ",
                cloudlets.size(), " cloudlets");

        return result;
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-progress-fraction-completed-length-over-total";
    }

    @Override
    public int outputGuid() {
        return 1400;
    }
}
