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

public class monitor_v17 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        java.util.List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];
        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);
            double mips = readSpace.getVmMips(vm);
            double meanMips = readSpace.getVmUtilizationMean(vm) * mips;
            double madMips = readSpace.getVmUtilizationMad(vm) * mips;
            result[i] = meanMips > 1e-6 ? (madMips / meanMips) : 0.0;
        }
        Log.printlnConcat(readSpace.getNow(), ": [monitor_v17] computed utilization coefficient of variation for ", vms.size(), " VMs");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "vm-cpu-utilization-coefficient-of-variation-mad-over-mean-mips-scaled";
    }

    @Override
    public int outputGuid() {
        return 1300;
    }
}
