package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * Monitor v3 - VM Count
 *
 * Observes the number of VMs currently deployed on each host. This is a raw
 * placement count and does not reflect load; it is useful as a consolidation
 * or spreading signal independent of MIPS consumption.
 *
 * Output: double[] indexed by host position in readSpace.getAllHosts().
 * Each entry is a non-negative integer cast to double (e.g. 3.0 for 3 VMs).
 *
 * Output GUID: host-vm-count
 */
public class monitor_v3 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] metrics = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);
            double vmCount = host.getGuestList().size();
            metrics[i] = vmCount;
            Log.printlnConcat(now, ": [monitor_v3] Host ", host.getId(),
                    " VM count = ", (int) vmCount);
        }

        return metrics;
    }

    @Override
    public String outputGuid() {
        return "host-vm-count";
    }
}
