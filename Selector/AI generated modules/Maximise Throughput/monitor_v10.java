package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

import java.util.List;

// Variant 10: cloudlet PE-demand pressure.
// Ratio of a cloudlet's requested PEs to its hosting VM's total PE count.
// A cloudlet claiming a large share of its VM's PEs leaves little room for
// co-located cloudlets under the time-shared model, flagging intra-VM
// contention pressure at the individual cloudlet level rather than at the
// VM or host level.
public class monitor_v10 implements Monitor<double[]> {

    private static final String MODULE_NAME = "monitor_v10";
    private static final double UNKNOWN_SENTINEL = -1.0;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        double[] result = new double[cloudlets.size()];
        int unresolvedCount = 0;

        for (int i = 0; i < cloudlets.size(); i++) {
            Cloudlet cloudlet = cloudlets.get(i);
            GuestEntity hostingVm = findHostingVm(readSpace, cloudlet);

            if (hostingVm == null) {
                result[i] = UNKNOWN_SENTINEL;
                unresolvedCount++;
                continue;
            }

            int vmPes = readSpace.getVmNumberOfPes(hostingVm);
            int cloudletPes = readSpace.getCloudletNumberOfPes(cloudlet);
            result[i] = vmPes > 0 ? (double) cloudletPes / (double) vmPes : 0.0;
        }

        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] ",
                "PE demand pressure computed for ", cloudlets.size(), " active cloudlets, ",
                unresolvedCount, " unresolved");

        return result;
    }

    // Cloudlets are only reachable through their owning VM's cloudlet list;
    // this walks all VMs to find the one currently holding this cloudlet.
    private GuestEntity findHostingVm(ReadSpace readSpace, Cloudlet cloudlet) {
        int targetId = readSpace.getId(cloudlet);

        for (GuestEntity vm : readSpace.getVmList()) {
            for (Cloudlet candidate : readSpace.getVmCloudletList(vm)) {
                if (readSpace.getId(candidate) == targetId) {
                    return vm;
                }
            }
        }

        return null;
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-pe-demand-pressure-ratio";
    }

    @Override
    public int outputGuid() {
        return 1400;
    }
}
