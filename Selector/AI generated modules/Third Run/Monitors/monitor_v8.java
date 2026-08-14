package org.cloudbus.cloudsim.examples;

import java.util.List;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// VM-level MIPS demand pressure: requested MIPS divided by currently allocated MIPS.
// A ratio above 1 means the VM is demanding more compute than it currently holds
// (throughput bottleneck, scale-up candidate); a ratio well below 1 means the VM is
// over-provisioned relative to demand (scale-down / consolidation candidate for
// power savings). VMs with zero current allocation are flagged with a large
// sentinel value when they have outstanding demand.
public class monitor_v8 implements Monitor<double[]> {

    private static final double EPSILON = 1e-6;
    private static final double UNBOUNDED_DEMAND_SENTINEL = Double.MAX_VALUE;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);
            double requested = readSpace.getVmRequestedMips(vm);
            double current = readSpace.getVmMips(vm);

            if (current > EPSILON) {
                result[i] = requested / current;
            } else if (requested > EPSILON) {
                result[i] = UNBOUNDED_DEMAND_SENTINEL;
            } else {
                result[i] = 0.0;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v8] ", "computed MIPS demand pressure for ", vms.size(), " VMs");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "vm-mipsDemandPressure-ratio";
    }

    @Override
    public int outputGuid() {
        return 1300;
    }
}
