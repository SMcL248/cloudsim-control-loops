package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;

import java.util.List;

/**
 * Cloudlet-level monitor.
 *
 * Approach: intrinsic progress tracking. Reports each active cloudlet's
 * remaining work as a fraction of its total work (remainingLength /
 * totalLength), independent of which VM it is running on or how fast that
 * VM is currently going. This is a pure "how far from done is this unit
 * of work" signal - useful for spotting cloudlets that are still near
 * their starting line late into a simulation (a proxy for starvation or
 * chronic under-provisioning) without needing to reason about VM or host
 * state at all.
 */
public class monitor_v5 implements Monitor<double[]> {

    private static final double EPSILON = 1e-9;
    private static final String SEMANTIC = "cloudlet-remainingWorkFraction-progressRisk";
    private static final int GUID = 1400;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        double[] result = new double[cloudlets.size()];

        double sum = 0.0;
        for (int i = 0; i < cloudlets.size(); i++) {
            Cloudlet cl = cloudlets.get(i);

            long remaining = readSpace.getRemainingLength(cl);
            long total = readSpace.getTotalLength(cl);

            double fraction = (double) remaining / ((double) total + EPSILON);
            if (fraction < 0.0) {
                fraction = 0.0;
            } else if (fraction > 1.0) {
                fraction = 1.0;
            }

            result[i] = fraction;
            sum += fraction;
        }

        double mean = cloudlets.isEmpty() ? 0.0 : sum / cloudlets.size();
        String message = "cloudlets=" + cloudlets.size() + " meanRemainingWorkFraction=" + mean;
        Log.printlnConcat(readSpace.getNow(), ": [monitor_v5] ", message);

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
