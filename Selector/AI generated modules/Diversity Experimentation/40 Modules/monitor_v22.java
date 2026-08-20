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

public class monitor_v22 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        java.util.List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];
        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);
            HostEntity host = findHostForVm(readSpace, vm);
            if (host == null) {
                result[i] = 0.0;
                continue;
            }
            double power = readSpace.getHostPower(host);
            int guestCount = readSpace.getVmListForHost(host).size();
            result[i] = guestCount > 0 ? (power / guestCount) : power;
        }
        Log.printlnConcat(readSpace.getNow(), ": [monitor_v22] attributed host power share for ", vms.size(), " VMs");
        return result;
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
        return "vm-attributed-host-power-share-watts-equal-split-across-co-located-guests";
    }

    @Override
    public int outputGuid() {
        return 1300;
    }
}
