package org.cloudbus.cloudsim.examples;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;

// Cloudlet-level estimated finish time (absolute simulation time). Built by mapping
// each active cloudlet to its owning VM (via each VM's cloudlet list) and asking
// ReadSpace for the estimated finish time on that VM. Directly supports throughput
// goals by surfacing which cloudlets are projected to finish late.
// Cloudlets whose owning VM cannot be resolved (e.g. transient scheduling state)
// are flagged with a negative sentinel value.
public class monitor_v10 implements Monitor<double[]> {

    private static final double UNRESOLVED_SENTINEL = -1.0;

    @Override
    public double[] observe(ReadSpace readSpace) {
        Map<Integer, GuestEntity> ownerByCloudletId = new HashMap<Integer, GuestEntity>();
        for (GuestEntity vm : readSpace.getVmList()) {
            for (Cloudlet cl : readSpace.getVmCloudletList(vm)) {
                ownerByCloudletId.put(readSpace.getId(cl), vm);
            }
        }

        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        double[] result = new double[cloudlets.size()];

        for (int i = 0; i < cloudlets.size(); i++) {
            Cloudlet cl = cloudlets.get(i);
            GuestEntity owner = ownerByCloudletId.get(readSpace.getId(cl));

            if (owner != null) {
                result[i] = readSpace.getCloudletEstimatedFinishTime(owner, cl);
            } else {
                result[i] = UNRESOLVED_SENTINEL;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v10] ", "computed estimated finish time for ", cloudlets.size(), " cloudlets");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-estimatedFinishTime-simTime";
    }

    @Override
    public int outputGuid() {
        return 1400;
    }
}
