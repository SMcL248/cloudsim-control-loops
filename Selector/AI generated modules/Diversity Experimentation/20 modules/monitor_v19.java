package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.core.PowerGuestEntity;
import org.cloudbus.cloudsim.core.PowerHostEntity;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVm;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class monitor_v19 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        double[] result = new double[cloudlets.size()];

        // Build a cloudlet-id -> owning-VM lookup since ReadSpace does not
        // expose a direct reverse mapping from Cloudlet to GuestEntity.
        Map<Integer, GuestEntity> owner = new HashMap<Integer, GuestEntity>();
        for (GuestEntity vm : vms) {
            for (Cloudlet cl : readSpace.getVmCloudletList(vm)) {
                owner.put(readSpace.getId(cl), vm);
            }
        }

        for (int i = 0; i < cloudlets.size(); i++) {
            Cloudlet cl = cloudlets.get(i);
            GuestEntity vm = owner.get(readSpace.getId(cl));
            if (vm != null) {
                result[i] = readSpace.getCloudletEstimatedFinishTime(vm, cl) - readSpace.getNow();
            } else {
                result[i] = -1.0;
            }
        }
        Log.printlnConcat(readSpace.getNow(), ": [monitor_v19] estimated time remaining for ", cloudlets.size(), " cloudlets");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-estimated-time-remaining-seconds";
    }

    @Override
    public int outputGuid() {
        return 1400;
    }
}
