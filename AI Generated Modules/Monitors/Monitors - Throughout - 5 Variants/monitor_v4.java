package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;

/**
 * Monitor variant 4: VM Processing Demand Monitor.
 *
 * For each VM, reports the estimated time (in seconds) required to
 * clear its current backlog at its own processing rate:
 *     totalRemainingCloudletLength / vm.getMips()
 *
 * Unlike monitor_v2 (raw remaining length), this metric normalises
 * workload by VM capacity, so two VMs holding identical backlogs but
 * running at different MIPS ratings will report different demand.
 * VMs with a non-positive MIPS rating are reported as 0.0 (undefined
 * capacity, treated as no measurable demand).
 */
public class monitor_v4 implements Monitor<double[]> {

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

            double mips = vm.getMips();
            double demand = (mips > 0.0) ? (totalRemainingLength / mips) : 0.0;

            metrics[i] = demand;
            Log.printlnConcat(now, ": monitor_v4 - VM #", vm.getId(), " processing demand = ", demand);
        }

        return metrics;
    }

    @Override
    public String outputGuid() {
        return "vm-demand";
    }

}
