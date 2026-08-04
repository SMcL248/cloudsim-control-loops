package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import java.util.List;

// Cloudlet-level monitor: reports completion fraction (processed length / total length)
// per active cloudlet. Supports the service-quality goal (cloudlet completion rate) with
// direct per-cloudlet progress rather than an aggregate resource proxy.
public class monitor_v10 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        double[] metrics = new double[cloudlets.size()];

        for (int i = 0; i < cloudlets.size(); i++) {
            Cloudlet cl = cloudlets.get(i);
            long total = readSpace.getTotalLength(cl);
            long remaining = readSpace.getRemainingLength(cl);

            if (total > 0L) {
                metrics[i] = (double) (total - remaining) / (double) total;
            } else {
                metrics[i] = 0.0;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v10] computed completion fraction for ", cloudlets.size(), " active cloudlets");
        return metrics;
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-completion-fraction-processed-over-total-length-per-cloudlet";
    }

    @Override
    public int outputGuid() {
        return 1400;
    }

}
