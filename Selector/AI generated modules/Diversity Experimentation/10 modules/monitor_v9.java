package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import java.util.List;

/**
 * Cloudlet-level monitor. Combines how far a cloudlet is from finishing
 * with how much PE footprint it occupies, on the basis that a
 * barely-started cloudlet holding many PEs is a bigger prioritisation
 * concern than an equally unfinished single-PE cloudlet. Urgency is the
 * fraction of work remaining multiplied by the number of PEs requested.
 * This is a prioritisation-weight metric, not a timing estimate.
 */
public class monitor_v9 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        double[] result = new double[cloudlets.size()];

        for (int i = 0; i < cloudlets.size(); i++) {
            Cloudlet cl = cloudlets.get(i);

            long total = readSpace.getTotalLength(cl);
            long remaining = readSpace.getRemainingLength(cl);
            int pes = readSpace.getCloudletNumberOfPes(cl);

            double remainingFraction = total > 0 ? (double) remaining / (double) total : 0.0;
            result[i] = remainingFraction * pes;
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v9] ", "computed resource-weighted urgency for ", cloudlets.size(), " cloudlets");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-resource-weighted-urgency-progress-and-pe-footprint";
    }

    @Override
    public int outputGuid() {
        return 1400;
    }
}
