package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * Monitor v2 - Per-host CPU Demand Pressure
 *
 * Metric: sum of MIPS currently requested by all VMs on a host,
 * divided by the host's total MIPS capacity.
 * Unlike utilisation, this reflects what VMs want, not what they are getting.
 * Output range: [0, ...); values > 1 indicate demand exceeds capacity.
 * GUID: host-cpu-demand
 */
public class monitor_v2 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] metrics = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);
            double totalMips = host.getTotalMips();
            double requestedMips = 0.0;

            for (GuestEntity vm : host.getGuestList()) {
                requestedMips += vm.getCurrentRequestedTotalMips();
            }

            metrics[i] = (totalMips > 0.0) ? requestedMips / totalMips : 0.0;
            Log.printlnConcat(now, ": [monitor_v2] Host ", host.getId(),
                    " cpu-demand=", metrics[i]);
        }

        return metrics;
    }

    @Override
    public String outputGuid() {
        return "host-cpu-demand";
    }
}
