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

public class monitor_v28 implements Monitor<double[]> {

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
            double hostMipsPerPe = readSpace.getHostMipsPerPe(host);
            int numPes = readSpace.getVmNumberOfPes(vm);
            double sumPerPe = 0.0;
            for (Double perPe : readSpace.getVmMipsPerPe(vm)) {
                sumPerPe += perPe;
            }
            double avgPerPeReq = numPes > 0 ? (sumPerPe / numPes) : 0.0;
            result[i] = hostMipsPerPe > 1e-6 ? (avgPerPeReq / hostMipsPerPe) : 0.0;
        }
        Log.printlnConcat(readSpace.getNow(), ": [monitor_v28] computed per-PE saturation ratio for ", vms.size(), " VMs");
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
        return "vm-per-pe-saturation-ratio-avg-requested-mips-per-pe-over-host-mips-per-pe";
    }

    @Override
    public int outputGuid() {
        return 1300;
    }
}
