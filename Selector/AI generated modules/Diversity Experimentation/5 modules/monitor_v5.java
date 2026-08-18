package org.cloudbus.cloudsim.examples;// always include

// Import whats needed
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Variant 5: Cloudlet-level estimated time-to-completion.
// ReadSpace exposes cloudlet ownership only in the vm-to-cloudlet direction
// (getVmCloudletList), so a cloudletId -> owning-vm lookup is built first by
// walking every VM's assigned cloudlet list. That owner is then used with
// getCloudletEstimatedFinishTime to derive remaining seconds until finish.
// This is a scheduling-urgency signal (how soon does this cloudlet clear),
// distinct from a raw progress-percentage metric, since two cloudlets with
// identical progress can have very different times-to-completion depending
// on the throughput of the VM they are running on.
public class monitor_v5 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        double[] eta = new double[cloudlets.size()];

        Map<Integer, GuestEntity> ownerByCloudletId = new HashMap<Integer, GuestEntity>();
        List<GuestEntity> vms = readSpace.getVmList();
        for (int v = 0; v < vms.size(); v++) {
            GuestEntity vm = vms.get(v);
            List<Cloudlet> owned = readSpace.getVmCloudletList(vm);
            for (int c = 0; c < owned.size(); c++) {
                ownerByCloudletId.put(readSpace.getId(owned.get(c)), vm);
            }
        }

        double now = readSpace.getNow();

        for (int i = 0; i < cloudlets.size(); i++) {
            Cloudlet cl = cloudlets.get(i);
            GuestEntity owner = ownerByCloudletId.get(readSpace.getId(cl));

            if (owner == null) {
                eta[i] = -1.0;
            } else {
                double finish = readSpace.getCloudletEstimatedFinishTime(owner, cl);
                double remaining = finish - now;
                eta[i] = remaining >= 0.0 ? remaining : -1.0;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v5] cloudlet estimated time-to-completion computed for ", cloudlets.size(), " active cloudlets (index-aligned with getActiveCloudlets).");
        return eta;
    }

    @Override
    public String outputSemantic() {
        return "4-cloudletEstimatedTimeToCompletion-secondsUntilFinish_negOneIfOwnerVmUnknownOrAlreadyDue";
    }

    @Override
    public int outputGuid() {
        return 1400;
    }

}
