package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;

/**
 * Monitor variant 1: VM Estimated-Time-to-Completion (ETC) Monitor.
 *
 * For each VM, reports the maximum estimated remaining duration across
 * all cloudlets currently in its execution list:
 *     max( getEstimatedFinishTime(cloudlet, now) - now )
 *
 * A VM with an empty execution list is reported as idle (0.0).
 */
public class monitor_v1 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<GuestEntity> vms = readSpace.getVmList();
        double[] metrics = new double[vms.size()];

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);
            List<Cloudlet> execList = vm.getCloudletScheduler().getCloudletExecList();

            double maxRemaining = 0.0;
            for (Cloudlet cloudlet : execList) {
                double remaining = vm.getCloudletScheduler().getEstimatedFinishTime(cloudlet, now) - now;
                if (remaining > maxRemaining) {
                    maxRemaining = remaining;
                }
            }

            metrics[i] = maxRemaining;
            Log.printlnConcat(now, ": monitor_v1 - VM #", vm.getId(), " max ETC = ", maxRemaining);
        }

        return metrics;
    }

    @Override
    public String outputGuid() {
        return "vm-etc";
    }

}
