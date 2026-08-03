package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import java.util.List;

public class monitor_v10 implements Monitor<double[]> {

    private static final String SEMANTIC = "cloudlet-progress-ratio";
    private static final int GUID = 1400;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        double[] result = new double[cloudlets.size()];

        for (int i = 0; i < cloudlets.size(); i++) {
            Cloudlet cl = cloudlets.get(i);
            long total = readSpace.getTotalLength(cl);
            long remaining = readSpace.getRemainingLength(cl);

            if (total <= 0) {
                result[i] = 0.0;
            } else {
                result[i] = (double) (total - remaining) / (double) total;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v10] cloudlet progress ratio computed for ", cloudlets.size(), " active cloudlets");
        return result;
    }

    @Override
    public String outputSemantic() {
        return SEMANTIC;
    }

    @Override
    public int outputGuid() {
        return GUID;
    }
}
