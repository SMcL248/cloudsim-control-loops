package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;

/**
 * Monitor variant 2: VM Remaining Workload Length Monitor.
 *
 * For each VM, reports the total remaining cloudlet length (in MI)
 * summed across every cloudlet in its execution list. This captures
 * the raw amount of work still outstanding on the VM, independent of
 * its processing speed.
 */
public class monitor_v2 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<GuestEntity> vms = readSpace.getVmList();
        double[] metrics = new double[vms.size()];

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);
            List<Cloudlet> execList = vm.getCloudletScheduler().getCloudletExecList();

            double totalRemainingLength = 0.0;
            for (Cloudlet cloudlet : execList) {
                totalRemainingLength += cloudlet.getRemainingCloudletLength();
            }

            metrics[i] = totalRemainingLength;
            Log.printlnConcat(now, ": monitor_v2 - VM #", vm.getId(), " total remaining length = ", totalRemainingLength);
        }

        return metrics;
    }

    @Override
    public String outputGuid() {
        return "vm-length";
    }

}
