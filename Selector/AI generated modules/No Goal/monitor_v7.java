package org.cloudbus.cloudsim.examples;// always include

import java.util.List;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Strategy: queue-drain ETA per VM = total remaining work (MI) assigned to
// the VM, divided by its current effective throughput (MIPS). A temporal
// forecast metric rather than an instantaneous load snapshot -- estimates
// how long the VM's existing backlog will take to clear at its current
// rate.
public class monitor_v7 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);
            List<Cloudlet> assigned = readSpace.getVmCloudletList(vm);

            long backlog = 0L;
            for (Cloudlet cl : assigned) {
                backlog += readSpace.getRemainingLength(cl);
            }

            double throughput = readSpace.getVmEffectiveThroughput(vm);

            if (backlog <= 0L) {
                result[i] = 0.0;
            } else if (throughput <= 0.0) {
                // backlog exists but nothing is draining it: flag as stalled
                result[i] = -1.0;
            } else {
                result[i] = backlog / throughput;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v7] VM queue drain ETA computed for ", vms.size(), " VMs");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "vm-queueDrainEta-secondsToClearBacklog";
    }

    @Override
    public int outputGuid() {
        return 1303;
    }
}
