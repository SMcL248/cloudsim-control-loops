package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

/**
 * Host-level monitor. For each host, measures how unevenly its remaining
 * headroom is spread across MIPS, RAM and Bandwidth. A host can look
 * "free" on average while one resource is nearly exhausted; this metric
 * surfaces that imbalance as the standard deviation of the three
 * per-resource headroom fractions. Low value = evenly balanced headroom
 * (safe for arbitrary future placements). High value = fragmented
 * headroom (host is close to becoming a bottleneck on one dimension
 * even though it looks fine overall).
 */
public class monitor_v1 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] result = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);

            double totalMips = readSpace.getHostTotalMips(host);
            double totalRam = readSpace.getHostTotalRam(host);
            double totalBw = readSpace.getHostTotalBw(host);

            double fMips = totalMips > 0 ? readSpace.getHostAvailableMips(host) / totalMips : 0.0;
            double fRam = totalRam > 0 ? readSpace.getHostAvailableRam(host) / totalRam : 0.0;
            double fBw = totalBw > 0 ? readSpace.getHostAvailableBw(host) / totalBw : 0.0;

            double mean = (fMips + fRam + fBw) / 3.0;
            double variance = (Math.pow(fMips - mean, 2) + Math.pow(fRam - mean, 2) + Math.pow(fBw - mean, 2)) / 3.0;

            result[i] = Math.sqrt(variance);
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v1] ", "computed headroom-imbalance score for ", hosts.size(), " hosts");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "host-resource-headroom-imbalance-mips-ram-bw-stddev";
    }

    @Override
    public int outputGuid() {
        return 1200;
    }
}
