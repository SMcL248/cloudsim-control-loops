package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;

/**
 * Monitor variant 3: VM Cloudlet Count Monitor.
 *
 * For each VM, reports the number of cloudlets currently present in
 * its execution list. This is a coarse-grained load indicator based
 * purely on task multiplicity rather than task size or duration.
 */
public class monitor_v3 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<GuestEntity> vms = readSpace.getVmList();
        double[] metrics = new double[vms.size()];

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);
            List<Cloudlet> execList = vm.getCloudletScheduler().getCloudletExecList();

            double cloudletCount = execList.size();

            metrics[i] = cloudletCount;
            Log.printlnConcat(now, ": monitor_v3 - VM #", vm.getId(), " cloudlet count = ", cloudletCount);
        }

        return metrics;
    }

    @Override
    public String outputGuid() {
        return "vm-count";
    }

}
