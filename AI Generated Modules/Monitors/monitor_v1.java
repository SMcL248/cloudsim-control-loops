package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * Monitor v1 - CPU Utilisation
 *
 * Observes the fraction of each host's total MIPS capacity that is currently
 * in use, computed as (totalMips - availableMips) / totalMips.
 *
 * Output: double[] indexed by host position in readSpace.getAllHosts().
 * Each entry is in [0.0, 1.0], where 1.0 represents full utilisation.
 *
 * Output GUID: host-cpu-util
 */
public class monitor_v1 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] metrics = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);
            double total = host.getTotalMips();
            double usedMips = 0.0;
            for (GuestEntity vm : host.getGuestList()) {
                usedMips += vm.getTotalUtilizationOfCpuMips(readSpace.getNow());
            }
            double util = (total > 0.0) ? (total - usedMips) / total : 0.0;
            metrics[i] = util;
            Log.printlnConcat(now, ": [monitor_v1] Host ", host.getId(),
                    " CPU util = ", util);
        }

        return metrics;
    }

    @Override
    public String outputGuid() {
        return "host-cpu-util";
    }
}
