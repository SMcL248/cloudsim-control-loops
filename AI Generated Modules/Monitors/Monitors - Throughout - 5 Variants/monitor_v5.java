package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;

/**
 * Monitor variant 5: VM Average Cloudlet Length Monitor.
 *
 * For each VM, reports the average remaining cloudlet length across
 * its execution list:
 *     totalRemainingCloudletLength / cloudletCount
 *
 * This differs from monitor_v2 (total backlog) and monitor_v3 (task
 * count) by capturing workload granularity -- whether a VM's backlog
 * is made up of a few large cloudlets or many small ones. VMs with an
 * empty execution list are reported as 0.0.
 */
public class monitor_v5 implements Monitor<double[]> {

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

            int cloudletCount = execList.size();
            double avgLength = (cloudletCount > 0) ? (totalRemainingLength / cloudletCount) : 0.0;

            metrics[i] = avgLength;
            Log.printlnConcat(now, ": monitor_v5 - VM #", vm.getId(), " avg cloudlet length = ", avgLength);
        }

        return metrics;
    }

    @Override
    public String outputGuid() {
        return "vm-avglength";
    }

}
