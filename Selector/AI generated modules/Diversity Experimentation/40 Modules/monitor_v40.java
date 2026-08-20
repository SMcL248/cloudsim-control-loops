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

public class monitor_v40 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        java.util.Map<Integer, GuestEntity> owners = buildOwnerMap(readSpace);
        java.util.List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        double[] result = new double[cloudlets.size()];
        for (int i = 0; i < cloudlets.size(); i++) {
            Cloudlet cl = cloudlets.get(i);
            GuestEntity vm = owners.get(readSpace.getId(cl));
            if (vm == null) {
                result[i] = 0.0;
                continue;
            }
            int siblingCount = readSpace.getVmCloudletList(vm).size() - 1;
            result[i] = Math.max(0.0, (double) siblingCount);
        }
        Log.printlnConcat(readSpace.getNow(), ": [monitor_v40] computed contention sibling count for ", cloudlets.size(), " cloudlets");
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
        return "cloudlet-contention-sibling-count-co-located-cloudlets-on-same-vm-excluding-self";
    }

    @Override
    public int outputGuid() {
        return 1400;
    }
}
