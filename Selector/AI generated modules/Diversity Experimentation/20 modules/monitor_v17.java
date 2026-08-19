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

public class monitor_v17 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];
        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);
            int headroomTiers = 0;
            if (readSpace.getNextMipsTier(vm) != -1) {
                headroomTiers++;
            }
            if (readSpace.getNextRamTier(vm) != -1) {
                headroomTiers++;
            }
            if (readSpace.getNextBwTier(vm) != -1) {
                headroomTiers++;
            }
            result[i] = headroomTiers;
        }
        Log.printlnConcat(readSpace.getNow(), ": [monitor_v17] counted scale-up tier headroom for ", vms.size(), " VMs");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "vm-scale-up-headroom-tier-count";
    }

    @Override
    public int outputGuid() {
        return 1300;
    }
}
