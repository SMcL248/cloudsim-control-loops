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

public class monitor_v19 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        java.util.List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];
        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);
            double next = readSpace.getNextMipsTier(vm);
            double current = readSpace.getVmMips(vm);
            result[i] = next < 0 ? -1.0 : (next - current);
        }
        Log.printlnConcat(readSpace.getNow(), ": [monitor_v19] computed MIPS scale-up headroom for ", vms.size(), " VMs");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "vm-mips-scale-up-headroom-next-tier-minus-current-or-neg1-if-maxed";
    }

    @Override
    public int outputGuid() {
        return 1300;
    }
}
