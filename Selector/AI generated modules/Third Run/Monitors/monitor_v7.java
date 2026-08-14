package org.cloudbus.cloudsim.examples;

import java.util.List;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// VM-level utilization volatility, measured as mean absolute deviation of CPU
// utilization. High volatility indicates a bursty/unstable VM where aggressive
// scaling or migration decisions risk thrashing; low volatility indicates a
// VM whose steady-state resource needs can be trusted for planning.
public class monitor_v7 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);
            result[i] = readSpace.getVmUtilizationMad(vm);
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v7] ", "computed utilization volatility for ", vms.size(), " VMs");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "vm-cpuUtilizationVolatility-mad";
    }

    @Override
    public int outputGuid() {
        return 1300;
    }
}
