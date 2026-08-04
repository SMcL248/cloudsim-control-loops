package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

// VM-level monitor: reports spare MIPS headroom per VM (max attainable MIPS minus
// currently requested MIPS). Supports the availability goal of preserving VM capacity
// for incoming work, distinct from host-level capacity.
public class monitor_v8 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        double[] metrics = new double[vms.size()];

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);
            double headroom = readSpace.getVmMaxMips(vm) - readSpace.getVmRequestedMips(vm);
            metrics[i] = headroom > 0.0 ? headroom : 0.0;
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v8] computed spare MIPS headroom for ", vms.size(), " VMs");
        return metrics;
    }

    @Override
    public String outputSemantic() {
        return "vm-spare-mips-headroom-max-minus-requested-per-vm";
    }

    @Override
    public int outputGuid() {
        return 1302;
    }

}
