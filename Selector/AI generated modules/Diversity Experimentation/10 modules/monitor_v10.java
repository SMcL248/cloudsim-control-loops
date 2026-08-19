package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Cloudlet-level monitor. Looks up each active cloudlet's owning VM by
 * scanning every VM's own cloudlet list once (ReadSpace has no direct
 * cloudlet-to-VM accessor), then asks the simulation for that
 * cloudlet's estimated finish time. The result is the slack between
 * that estimate and the current simulation clock: an absolute
 * time-to-completion projection, rather than a progress ratio or a
 * resource-weighted priority score.
 * Sentinel: -1.0 means the owning VM for the cloudlet could not be
 * resolved.
 */
public class monitor_v10 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        Map<Integer, GuestEntity> owner = new HashMap<Integer, GuestEntity>();
        for (GuestEntity vm : readSpace.getVmList()) {
            List<Cloudlet> owned = readSpace.getVmCloudletList(vm);
            if (owned == null) {
                continue;
            }
            for (Cloudlet cl : owned) {
                owner.put(readSpace.getId(cl), vm);
            }
        }

        List<Cloudlet> active = readSpace.getActiveCloudlets();
        double[] result = new double[active.size()];

        for (int i = 0; i < active.size(); i++) {
            Cloudlet cl = active.get(i);
            GuestEntity vm = owner.get(readSpace.getId(cl));

            if (vm == null) {
                result[i] = -1.0;
            } else {
                double finishTime = readSpace.getCloudletEstimatedFinishTime(vm, cl);
                result[i] = finishTime - readSpace.getNow();
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v10] ", "computed finish-time slack for ", active.size(), " cloudlets");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-estimated-finish-time-slack-from-now";
    }

    @Override
    public int outputGuid() {
        return 1400;
    }
}
