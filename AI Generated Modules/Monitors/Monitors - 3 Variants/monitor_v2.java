package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * Monitor v2 - CPU Demand Pressure
 *
 * Observes the ratio of total MIPS requested by all VMs on a host to the
 * host's total MIPS capacity, computed as sum(vm.getCurrentRequestedTotalMips())
 * / host.getTotalMips().
 *
 * Unlike utilisation, demand pressure can exceed 1.0 when VMs collectively
 * request more MIPS than the host can supply, making it useful for detecting
 * resource contention before it manifests as throttling.
 *
 * Output: double[] indexed by host position in readSpace.getAllHosts().
 * Values >= 1.0 indicate over-subscription.
 *
 * Output GUID: host-cpu-demand
 */
public class monitor_v2 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] metrics = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);
            double total = host.getTotalMips();
            double requestedTotal = 0.0;

            for (GuestEntity vm : host.getGuestList()) {
                requestedTotal += vm.getCurrentRequestedTotalMips();
            }

            double demand = (total > 0.0) ? requestedTotal / total : 0.0;
            metrics[i] = demand;
            Log.printlnConcat(now, ": [monitor_v2] Host ", host.getId(),
                    " CPU demand pressure = ", demand);
        }

        return metrics;
    }

    @Override
    public String outputGuid() {
        return "host-cpu-demand";
    }
}
