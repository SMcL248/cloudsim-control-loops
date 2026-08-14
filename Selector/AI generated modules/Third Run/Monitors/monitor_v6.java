package org.cloudbus.cloudsim.examples;

import java.util.List;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;

// VM-level estimated backlog drain time: total remaining cloudlet length assigned to
// the VM divided by its effective throughput. Estimates how long the VM needs to clear
// its current queue at its current rate - a direct throughput-risk signal.
// VMs with remaining work but zero measured throughput are flagged with a large
// sentinel value (backlog time is effectively unbounded at the current rate).
public class monitor_v6 implements Monitor<double[]> {

    private static final double EPSILON = 1e-6;
    private static final double STALLED_SENTINEL = Double.MAX_VALUE;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);
            List<Cloudlet> cloudlets = readSpace.getVmCloudletList(vm);

            long remaining = 0;
            for (Cloudlet cl : cloudlets) {
                remaining += readSpace.getRemainingLength(cl);
            }

            double throughput = readSpace.getVmEffectiveThroughput(vm);

            if (remaining <= 0) {
                result[i] = 0.0;
            } else if (throughput > EPSILON) {
                result[i] = remaining / throughput;
            } else {
                result[i] = STALLED_SENTINEL;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v6] ", "computed backlog drain time for ", vms.size(), " VMs");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "vm-estimatedBacklogDrainTime-seconds";
    }

    @Override
    public int outputGuid() {
        return 1300;
    }
}
