package org.cloudbus.cloudsim.examples;// always include

import java.util.List;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;

// Strategy: cloudlet progress ratio = fraction of total length already
// executed. The baseline temporal-progress signal at cloudlet level.
public class monitor_v8 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        double[] result = new double[cloudlets.size()];

        for (int i = 0; i < cloudlets.size(); i++) {
            Cloudlet cl = cloudlets.get(i);
            long total = readSpace.getTotalLength(cl);
            long remaining = readSpace.getRemainingLength(cl);

            if (total <= 0L) {
                result[i] = 0.0;
            } else {
                result[i] = 1.0 - ((double) remaining / (double) total);
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v8] cloudlet progress ratio computed for ", cloudlets.size(), " cloudlets");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-progressRatio-fractionLengthCompleted";
    }

    @Override
    public int outputGuid() {
        return 1401;
    }
}
