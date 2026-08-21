package org.cloudbus.cloudsim.examples;// always include

import java.util.List;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Strategy: throughput efficiency per VM = effective throughput over
// requested (provisioned) MIPS. Requested MIPS is static/provisioned
// capacity, effective throughput is what the VM actually achieves right
// now, so a low ratio flags contention/starvation even when nominal
// utilisation looks fine.
public class monitor_v6 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);
            double requested = readSpace.getVmRequestedMips(vm);
            double effective = readSpace.getVmEffectiveThroughput(vm);

            if (requested <= 0.0) {
                result[i] = 0.0;
            } else {
                result[i] = effective / requested;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v6] VM throughput efficiency computed for ", vms.size(), " VMs");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "vm-throughputEfficiency-effectiveOverRequestedMips";
    }

    @Override
    public int outputGuid() {
        return 1302;
    }
}
