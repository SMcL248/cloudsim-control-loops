package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * Monitor v5 - Per-host VM CPU Utilisation Variance
 *
 * Metric: variance of per-VM CPU utilisation ratios across all VMs on a host.
 * Each VM's utilisation is expressed as a fraction of host total MIPS,
 * then population variance is computed.
 * A low value means VMs are evenly loaded; a high value signals imbalance
 * within the host (some VMs hot, others idle).
 * Output range: [0, ...); dimensionless (squared util fractions).
 * GUID: host-cpu-variance
 */
public class monitor_v5 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] metrics = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);
            List<GuestEntity> vms = host.getGuestList();
            int vmCount = vms.size();

            if (vmCount == 0) {
                metrics[i] = 0.0;
                Log.printlnConcat(now, ": [monitor_v5] Host ", host.getId(),
                        " cpu-util-variance=0.0 (no VMs)");
                continue;
            }

            double totalMips = host.getTotalMips();

            // First pass: compute mean VM utilisation ratio
            double sum = 0.0;
            for (GuestEntity vm : vms) {
                double util = (totalMips > 0.0)
                        ? vm.getTotalUtilizationOfCpuMips(now) / totalMips
                        : 0.0;
                sum += util;
            }
            double mean = sum / vmCount;

            // Second pass: compute population variance
            double variance = 0.0;
            for (GuestEntity vm : vms) {
                double util = (totalMips > 0.0)
                        ? vm.getTotalUtilizationOfCpuMips(now) / totalMips
                        : 0.0;
                double diff = util - mean;
                variance += diff * diff;
            }
            variance /= vmCount;

            metrics[i] = variance;
            Log.printlnConcat(now, ": [monitor_v5] Host ", host.getId(),
                    " cpu-util-variance=", metrics[i]);
        }

        return metrics;
    }

    @Override
    public String outputGuid() {
        return "host-cpu-variance";
    }
}
    