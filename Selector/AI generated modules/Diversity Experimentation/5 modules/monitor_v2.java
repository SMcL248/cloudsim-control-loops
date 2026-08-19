package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

import java.util.List;

/**
 * Host-level monitor.
 *
 * Approach: multi-dimensional resource balance. Reports the spread
 * (max - min) across a host's normalised CPU, RAM and Bandwidth headroom
 * fractions. A host can look "healthy" on aggregate MIPS while one
 * dimension (e.g. RAM) is nearly exhausted - that host is functionally
 * unable to accept further guests even though its CPU headroom looks
 * fine. This metric exposes that fragmentation risk directly, which a
 * single-dimension load reading cannot.
 */
public class monitor_v2 implements Monitor<double[]> {

    private static final double EPSILON = 1e-9;
    private static final String SEMANTIC = "host-resourceBalanceSkew-cpuRamBwSpread";
    private static final int GUID = 1200;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];

        double sum = 0.0;
        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);

            double cpuFrac = readSpace.getHostAvailableMips(host) / (readSpace.getHostTotalMips(host) + EPSILON);
            double ramFrac = readSpace.getHostAvailableRam(host) / (readSpace.getHostTotalRam(host) + EPSILON);
            double bwFrac = readSpace.getHostAvailableBw(host) / (readSpace.getHostTotalBw(host) + EPSILON);

            double max = Math.max(cpuFrac, Math.max(ramFrac, bwFrac));
            double min = Math.min(cpuFrac, Math.min(ramFrac, bwFrac));
            double skew = max - min;

            result[i] = skew;
            sum += skew;
        }

        double mean = hosts.isEmpty() ? 0.0 : sum / hosts.size();
        String message = "hosts=" + hosts.size() + " meanResourceBalanceSkew=" + mean;
        Log.printlnConcat(readSpace.getNow(), ": [monitor_v2] ", message);

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
