package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;

import java.util.List;

// Variant 9: cloudlet remaining-length ratio.
// A pure progress-fraction view (0 = done, 1 = untouched) computed only
// from the cloudlet's own length fields, independent of VM assignment or
// processing rate. Deliberately simple and cheap - a baseline "how far
// along is this cloudlet" signal that variant 8's rate-based estimate does
// not directly provide.
public class monitor_v9 implements Monitor<double[]> {

    private static final String MODULE_NAME = "monitor_v9";

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        double[] result = new double[cloudlets.size()];

        for (int i = 0; i < cloudlets.size(); i++) {
            Cloudlet cloudlet = cloudlets.get(i);
            long remaining = readSpace.getRemainingLength(cloudlet);
            long total = readSpace.getTotalLength(cloudlet);

            result[i] = total > 0 ? (double) remaining / (double) total : 0.0;
        }

        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] ",
                "remaining-length ratio computed for ", cloudlets.size(), " active cloudlets");

        return result;
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-remaining-length-ratio";
    }

    @Override
    public int outputGuid() {
        return 1400;
    }
}
