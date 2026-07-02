package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * Monitor v1 - Per-host CPU Utilisation Ratio
 *
 * Metric: sum of actual VM CPU usage (MIPS) across all VMs on a host,
 * divided by the host's total MIPS capacity.
 * Output range: [0, 1] under normal load; may slightly exceed 1 if overloaded.
 * GUID: host-cpu-util
 */
public class monitor_v1 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] metrics = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);
            double totalMips = host.getTotalMips();
            double usedMips = 0.0;

            for (GuestEntity vm : host.getGuestList()) {
                usedMips += vm.getTotalUtilizationOfCpuMips(now);
            }

            metrics[i] = (totalMips > 0.0) ? usedMips / totalMips : 0.0;
            Log.printlnConcat(now, ": [monitor_v1] Host ", host.getId(),
                    " cpu-util=", metrics[i]);
        }

        return metrics;
    }

    @Override
    public String outputGuid() {
        return "host-cpu-util";
    }
}
