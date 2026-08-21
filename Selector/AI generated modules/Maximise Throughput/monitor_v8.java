package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

import java.util.List;

// Variant 8: cloudlet estimated time remaining.
// Uses the simulator's own finish-time estimate rather than a simple
// remaining/total ratio, so it reflects the cloudlet's actual current
// processing rate on its VM (which can change with contention or scaling),
// not just how much work is nominally left.
public class monitor_v8 implements Monitor<double[]> {

    private static final String MODULE_NAME = "monitor_v8";
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

            double finishTime = readSpace.getCloudletEstimatedFinishTime(hostingVm, cloudlet);
            result[i] = finishTime - readSpace.getNow();
        }

        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] ",
                "estimated time remaining computed for ", cloudlets.size(), " active cloudlets, ",
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
        return "cloudlet-estimated-time-remaining";
    }

    @Override
    public int outputGuid() {
        return 1400;
    }
}
