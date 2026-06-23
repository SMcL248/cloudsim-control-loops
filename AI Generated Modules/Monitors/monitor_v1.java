package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * Monitor v1 — Host-level CPU utilisation monitor.
 *
 * Observes all hosts in the datacenter and returns a double[] where each
 * element i is the fractional CPU utilisation of host i (0.0 – 1.0),
 * computed as the sum of MIPS currently used by resident VMs divided by
 * the host's total MIPS capacity.
 *
 * Output GUID: "host-cpu"
 */
public class monitor_v1 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] cpuUtilisation = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);
            double totalMips = host.getTotalMips();
            double usedMips = 0.0;

            for (GuestEntity vm : host.getGuestList()) {
                usedMips += vm.getTotalUtilizationOfCpuMips(now);
            }

            cpuUtilisation[i] = (totalMips > 0.0) ? usedMips / totalMips : 0.0;

            Log.printlnConcat(now, ": [Monitor] Host ", i,
                    " CPU utilisation = ", cpuUtilisation[i],
                    " (used=", usedMips, " MIPS, total=", totalMips, " MIPS)");
        }

        return cpuUtilisation;
    }

    @Override
    public String outputGuid() {
        return "host-cpu";
    }
}
