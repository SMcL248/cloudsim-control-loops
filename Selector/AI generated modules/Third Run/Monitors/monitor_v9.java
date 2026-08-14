package org.cloudbus.cloudsim.examples;

import java.util.List;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;

// Cloudlet-level progress fraction: (total length - remaining length) / total length.
// A direct per-cloudlet throughput signal, useful for prioritising nearly-finished
// work (protect it from disruption) versus early-stage work (safer to reschedule
// or migrate elsewhere).
public class monitor_v9 implements Monitor<double[]> {

    private static final double EPSILON = 1e-6;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        double[] result = new double[cloudlets.size()];

        for (int i = 0; i < cloudlets.size(); i++) {
            Cloudlet cl = cloudlets.get(i);
            long total = readSpace.getTotalLength(cl);
            long remaining = readSpace.getRemainingLength(cl);

            if (total > EPSILON) {
                result[i] = 1.0 - ((double) remaining / (double) total);
            } else {
                result[i] = 0.0;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v9] ", "computed progress fraction for ", cloudlets.size(), " cloudlets");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-progressFraction-ratio";
    }

    @Override
    public int outputGuid() {
        return 1400;
    }
}
