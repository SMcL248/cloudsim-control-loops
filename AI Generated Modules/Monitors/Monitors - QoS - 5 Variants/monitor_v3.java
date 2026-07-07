package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * Monitor v3 - Per-host VM Count
 *
 * Metric: number of VMs currently deployed on each host.
 * A raw count; does not account for VM size or resource usage.
 * Useful for detecting load imbalance based purely on VM density.
 * GUID: host-vm-count
 */
public class monitor_v3 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] metrics = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);
            metrics[i] = host.getGuestList().size();
            Log.printlnConcat(now, ": [monitor_v3] Host ", host.getId(),
                    " vm-count=", (int) metrics[i]);
        }

        return metrics;
    }

    @Override
    public String outputGuid() {
        return "host-vm-count";
    }
}
