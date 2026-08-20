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

public class monitor_v20 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        java.util.List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];
        for (int i = 0; i < vms.size(); i++) {
            result[i] = readSpace.isVmMigrating(vms.get(i)) ? 1.0 : 0.0;
        }
        Log.printlnConcat(readSpace.getNow(), ": [monitor_v20] computed migration-in-progress flag for ", vms.size(), " VMs");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "vm-migration-in-progress-binary-flag";
    }

    @Override
    public int outputGuid() {
        return 1300;
    }
}
