package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

// VM-level monitor: reports historical utilization volatility (mean absolute deviation)
// per VM. Supports the service-quality goal - VMs with volatile utilization are more
// likely to breach capacity and threaten cloudlet completion, even at moderate mean load.
public class monitor_v9 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        double[] metrics = new double[vms.size()];

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);
            metrics[i] = readSpace.getVmUtilizationMad(vm);
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v9] computed utilization volatility (MAD) for ", vms.size(), " VMs");
        return metrics;
    }

    @Override
    public String outputSemantic() {
        return "vm-utilization-volatility-mean-absolute-deviation-per-vm";
    }

    @Override
    public int outputGuid() {
        return 1303;
    }

}
