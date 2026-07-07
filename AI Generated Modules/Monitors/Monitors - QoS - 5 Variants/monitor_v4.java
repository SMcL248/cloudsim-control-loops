package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * Monitor v4 - Per-host Peak VM CPU Utilisation Share
 *
 * Metric: the single highest-utilising VM's CPU usage as a fraction of
 * the host's total MIPS capacity. Identifies hotspot concentration:
 * a high value means one VM dominates host load regardless of overall util.
 * Output range: [0, 1].
 * GUID: host-cpu-util
 */
public class monitor_v4 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] metrics = new double[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            HostEntity host = hosts.get(i);
            double totalMips = host.getTotalMips();
            double peakVmMips = 0.0;

            for (GuestEntity vm : host.getGuestList()) {
                double vmMips = vm.getTotalUtilizationOfCpuMips(now);
                if (vmMips > peakVmMips) {
                    peakVmMips = vmMips;
                }
            }

            metrics[i] = (totalMips > 0.0) ? peakVmMips / totalMips : 0.0;
            Log.printlnConcat(now, ": [monitor_v4] Host ", host.getId(),
                    " peak-vm-util-share=", metrics[i]);
        }

        return metrics;
    }

    @Override
    public String outputGuid() {
        return "host-cpu-util";
    }
}
