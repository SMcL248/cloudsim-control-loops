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

public class monitor_v31 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        java.util.Map<Integer, GuestEntity> owners = buildOwnerMap(readSpace);
        java.util.List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        double[] result = new double[cloudlets.size()];
        double now = readSpace.getNow();
        for (int i = 0; i < cloudlets.size(); i++) {
            Cloudlet cl = cloudlets.get(i);
            GuestEntity vm = owners.get(readSpace.getId(cl));
            if (vm == null) {
                result[i] = -1.0;
                continue;
            }
            double finish = readSpace.getCloudletEstimatedFinishTime(vm, cl);
            result[i] = Math.max(0.0, finish - now);
        }
        Log.printlnConcat(readSpace.getNow(), ": [monitor_v31] estimated relative time remaining for ", cloudlets.size(), " cloudlets");
        return result;
    }

    // Build a Cloudlet-id -> owning-VM lookup by scanning every VM's assigned cloudlet list.
    private java.util.Map<Integer, GuestEntity> buildOwnerMap(ReadSpace readSpace) {
        java.util.Map<Integer, GuestEntity> owners = new java.util.HashMap<Integer, GuestEntity>();
        for (GuestEntity vm : readSpace.getVmList()) {
            for (Cloudlet cl : readSpace.getVmCloudletList(vm)) {
                owners.put(readSpace.getId(cl), vm);
            }
        }
        return owners;
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-estimated-time-remaining-finish-minus-now-clamped-nonneg";
    }

    @Override
    public int outputGuid() {
        return 1400;
    }
}
