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

public class monitor_v24 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        java.util.List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];
        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);
            long sumRemaining = 0L;
            for (Cloudlet cl : readSpace.getVmCloudletList(vm)) {
                sumRemaining += readSpace.getRemainingLength(cl);
            }
            result[i] = (double) sumRemaining;
        }
        Log.printlnConcat(readSpace.getNow(), ": [monitor_v24] computed total remaining work for ", vms.size(), " VMs");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "vm-remaining-work-total-mi-sum-across-assigned-cloudlets";
    }

    @Override
    public int outputGuid() {
        return 1300;
    }
}
