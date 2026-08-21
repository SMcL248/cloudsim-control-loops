package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

import java.util.List;

// Variant 5: VM utilisation volatility, MIPS-scaled.
// getVmUtilizationMad is not MIPS-scaled, so it is multiplied by the VM's
// MIPS rating to get an absolute measure of how erratic a VM's workload
// is. Bursty VMs are prime candidates for proactive headroom - a purely
// mean-based view would miss them.
public class monitor_v5 implements Monitor<double[]> {

    private static final String MODULE_NAME = "monitor_v5";

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);
            double mad = readSpace.getVmUtilizationMad(vm);
            double mips = readSpace.getVmMips(vm);
            result[i] = mad * mips;
        }

        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] ",
                "MIPS-scaled utilisation volatility computed for ", vms.size(), " VMs");

        return result;
    }

    @Override
    public String outputSemantic() {
        return "vm-utilization-volatility-mips";
    }

    @Override
    public int outputGuid() {
        return 1300;
    }
}
