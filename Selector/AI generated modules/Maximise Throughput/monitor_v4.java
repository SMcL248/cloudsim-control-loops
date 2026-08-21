package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

import java.util.List;

// Variant 4: VM throughput deficit.
// Gap between what a VM is provisioned to push (requested MIPS) and what
// it is actually pushing (effective throughput). A large, persistent gap
// flags contention or starvation at the VM level that raw utilisation
// numbers alone would not surface.
public class monitor_v4 implements Monitor<double[]> {

    private static final String MODULE_NAME = "monitor_v4";

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);
            double requested = readSpace.getVmRequestedMips(vm);
            double effective = readSpace.getVmEffectiveThroughput(vm);
            result[i] = Math.max(0.0, requested - effective);
        }

        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] ",
                "throughput deficit computed for ", vms.size(), " VMs");

        return result;
    }

    @Override
    public String outputSemantic() {
        return "vm-throughput-deficit-mips";
    }

    @Override
    public int outputGuid() {
        return 1300;
    }
}
