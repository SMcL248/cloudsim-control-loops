package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Cloudlet-level monitor: estimated time to finish.
// For each active cloudlet, reports (estimated finish time - now) in
// seconds. Since getCloudletEstimatedFinishTime requires the owning VM, this
// monitor first builds a cloudlet-id -> VM lookup by walking every VM's
// assigned cloudlet list. Any active cloudlet that cannot be matched to an
// owning VM through that lookup reports the sentinel.
public class monitor_v9 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        double[] result = new double[cloudlets.size()];
        double now = readSpace.getNow();

        Map<Integer, GuestEntity> cloudletOwner = new HashMap<Integer, GuestEntity>();
        for (GuestEntity vm : readSpace.getVmList()) {
            for (Cloudlet cl : readSpace.getVmCloudletList(vm)) {
                cloudletOwner.put(readSpace.getId(cl), vm);
            }
        }

        for (int i = 0; i < cloudlets.size(); i++) {
            Cloudlet cl = cloudlets.get(i);
            GuestEntity owner = cloudletOwner.get(readSpace.getId(cl));

            if (owner == null) {
                result[i] = -1.0;
                continue;
            }

            result[i] = readSpace.getCloudletEstimatedFinishTime(owner, cl) - now;
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v9] computed cloudlet estimated time to finish for ",
                cloudlets.size(), " cloudlets");

        return result;
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-estimated-time-to-finish-seconds-from-now";
    }

    @Override
    public int outputGuid() {
        return 1400;
    }
}
