package org.cloudbus.cloudsim.examples;// always include

import java.util.List;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Strategy: cloudlet slack time = estimated finish time minus now. Unlike
// the plain progress ratio, this accounts for the owning VM's current
// processing rate, so two cloudlets at the same completion fraction can
// carry very different urgency if their VMs run at different effective
// speeds.
public class monitor_v9 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[cloudlets.size()];
        double now = readSpace.getNow();

        for (int i = 0; i < cloudlets.size(); i++) {
            Cloudlet cl = cloudlets.get(i);
            GuestEntity owner = findOwner(readSpace, vms, cl);

            if (owner == null) {
                // no resolvable owner: cannot estimate a finish time
                result[i] = -1.0;
            } else {
                double finishTime = readSpace.getCloudletEstimatedFinishTime(owner, cl);
                result[i] = finishTime - now;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v9] cloudlet slack time computed for ", cloudlets.size(), " cloudlets");
        return result;
    }

    private GuestEntity findOwner(ReadSpace readSpace, List<GuestEntity> vms, Cloudlet target) {
        int targetId = readSpace.getId(target);

        for (GuestEntity vm : vms) {
            List<Cloudlet> owned = readSpace.getVmCloudletList(vm);
            for (Cloudlet cl : owned) {
                if (readSpace.getId(cl) == targetId) {
                    return vm;
                }
            }
        }
        return null;
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-slackTime-estimatedFinishMinusNow";
    }

    @Override
    public int outputGuid() {
        return 1402;
    }
}
