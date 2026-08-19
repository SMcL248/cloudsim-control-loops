package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

// VM-level monitor: mean projected cloudlet finish delay.
// For each VM, averages (estimated finish time - now) across every cloudlet
// currently assigned to it. VMs mid-migration or still being instantiated
// report a sentinel, since finish-time estimates are unreliable during those
// transitions. VMs with no assigned cloudlets report zero delay (idle, no
// outstanding risk).
public class monitor_v5 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];
        double now = readSpace.getNow();

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);

            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                result[i] = -1.0;
                continue;
            }

            List<Cloudlet> cloudlets = readSpace.getVmCloudletList(vm);
            if (cloudlets.isEmpty()) {
                result[i] = 0.0;
                continue;
            }

            double sumDelay = 0.0;
            for (Cloudlet cl : cloudlets) {
                sumDelay += (readSpace.getCloudletEstimatedFinishTime(vm, cl) - now);
            }

            result[i] = sumDelay / cloudlets.size();
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v5] computed vm mean cloudlet finish delay for ",
                vms.size(), " vms");

        return result;
    }

    @Override
    public String outputSemantic() {
        return "vm-mean-cloudlet-estimated-finish-delay-seconds";
    }

    @Override
    public int outputGuid() {
        return 1300;
    }
}
