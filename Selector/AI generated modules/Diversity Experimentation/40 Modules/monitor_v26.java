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

public class monitor_v26 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        java.util.List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];
        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);
            int dimsWithRoom = 0;
            if (readSpace.getNextMipsTier(vm) >= 0) dimsWithRoom++;
            if (readSpace.getNextRamTier(vm) >= 0) dimsWithRoom++;
            if (readSpace.getNextBwTier(vm) >= 0) dimsWithRoom++;
            result[i] = dimsWithRoom / 3.0;
        }
        Log.printlnConcat(readSpace.getNow(), ": [monitor_v26] computed elasticity headroom fraction for ", vms.size(), " VMs");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "vm-elasticity-headroom-fraction-dimensions-with-room-to-scale-over-three";
    }

    @Override
    public int outputGuid() {
        return 1300;
    }
}
