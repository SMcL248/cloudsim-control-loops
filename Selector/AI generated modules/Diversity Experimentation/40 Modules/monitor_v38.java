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

public class monitor_v38 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        java.util.Map<Integer, GuestEntity> owners = buildOwnerMap(readSpace);
        java.util.List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        double[] result = new double[cloudlets.size()];
        for (int i = 0; i < cloudlets.size(); i++) {
            Cloudlet cl = cloudlets.get(i);
            GuestEntity vm = owners.get(readSpace.getId(cl));
            HostEntity host = vm != null ? findHostForVm(readSpace, vm) : null;
            double component = 0.0;
            if (host != null) {
                if (readSpace.isHostFailed(host) || readSpace.isHostPermanentlyDead(host)) component += 0.5;
                if (!readSpace.hostHasFreePe(host)) component += 0.5;
            }
            result[i] = component;
        }
        Log.printlnConcat(readSpace.getNow(), ": [monitor_v38] computed host-risk composite for ", cloudlets.size(), " cloudlets");
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

    // Locate the host currently carrying the input VM, by scanning host guest lists.
    // Returns null if the VM is unallocated (e.g. still instantiating).
    private HostEntity findHostForVm(ReadSpace readSpace, GuestEntity vm) {
        int vmId = readSpace.getId(vm);
        for (HostEntity host : readSpace.getAllHosts()) {
            for (GuestEntity g : readSpace.getVmListForHost(host)) {
                if (readSpace.getId(g) == vmId) {
                    return host;
                }
            }
        }
        return null;
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-host-risk-composite-0.5-failure-plus-0.5-pe-saturation-of-owning-host";
    }

    @Override
    public int outputGuid() {
        return 1400;
    }
}
