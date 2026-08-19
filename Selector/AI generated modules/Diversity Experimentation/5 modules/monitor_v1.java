package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

import java.util.List;

/**
 * Host-level monitor.
 *
 * Approach: power-normalised load. Reports each host's instantaneous power
 * draw as a fraction of that host's own maximum power draw. Because power
 * curves are non-linear and host-specific (idle floor, slope, ceiling),
 * two hosts at the same CPU utilisation can sit at very different points
 * on their own power curve. This metric surfaces "closeness to power
 * ceiling" directly, which raw utilisation cannot.
 */
public class monitor_v1 implements Monitor<double[]> {

    private static final double EPSILON = 1e-9;
    private static final String SEMANTIC = "host-powerStressRatio-instant";
    private static final int GUID = 1200;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];

        double sum = 0.0;
        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);

            double maxPower = readSpace.getHostMaxPower(host);
            double currentPower = readSpace.getHostPower(host);

            double ratio = currentPower / (maxPower + EPSILON);
            if (ratio < 0.0) {
                ratio = 0.0;
            }

            result[i] = ratio;
            sum += ratio;
        }

        double mean = hosts.isEmpty() ? 0.0 : sum / hosts.size();
        String message = "hosts=" + hosts.size() + " meanPowerStressRatio=" + mean;
        Log.printlnConcat(readSpace.getNow(), ": [monitor_v1] ", message);

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
