package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

/**
 * VM-level monitor. Compares what a VM was provisioned to deliver
 * (requested MIPS, its static capacity) against what it is actually
 * delivering right now (effective throughput). The normalised gap
 * reveals VMs that are silently under-delivering relative to their own
 * nominal capacity, for example due to host-side contention, even
 * though nothing about the VM's own configuration looks unhealthy.
 * 0.0 means the VM is delivering its full requested capacity; values
 * approaching 1.0 mean it is delivering almost none of it.
 */
public class monitor_v8 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);

            double requested = readSpace.getVmRequestedMips(vm);
            double effective = readSpace.getVmEffectiveThroughput(vm);

            if (requested <= 0) {
                result[i] = 0.0;
            } else {
                double gap = (requested - effective) / requested;
                result[i] = Math.max(0.0, gap);
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v8] ", "computed throughput efficiency gap for ", vms.size(), " vms");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "vm-throughput-efficiency-gap-requested-vs-effective";
    }

    @Override
    public int outputGuid() {
        return 1300;
    }
}
