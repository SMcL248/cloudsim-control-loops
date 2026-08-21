package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

import java.util.List;

// Variant 7: availability-adjusted VM throughput.
// A VM mid-migration or still instantiating is not really contributing
// stable throughput right now, even if its last known effective
// throughput reads high. Zeroing these out separates real underperformance
// from a transitional stall that will resolve on its own.
public class monitor_v7 implements Monitor<double[]> {

    private static final String MODULE_NAME = "monitor_v7";

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];
        int stalledCount = 0;

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);

            boolean stalled = readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm);

            if (stalled) {
                result[i] = 0.0;
                stalledCount++;
            } else {
                result[i] = readSpace.getVmEffectiveThroughput(vm);
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] ",
                "availability-adjusted throughput computed, ", stalledCount, " VMs zeroed as transitional");

        return result;
    }

    @Override
    public String outputSemantic() {
        return "vm-availability-adjusted-throughput";
    }

    @Override
    public int outputGuid() {
        return 1300;
    }
}
