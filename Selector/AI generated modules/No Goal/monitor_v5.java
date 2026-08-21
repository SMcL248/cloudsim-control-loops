package org.cloudbus.cloudsim.examples;// always include

import java.util.List;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Strategy: VM load volatility, expressed as a coefficient of variation
// (rolling MAD / rolling mean of CPU utilisation). Flags bursty, unstable
// VMs regardless of their absolute load level -- a different axis to raw
// utilisation.
public class monitor_v5 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);
            double mean = readSpace.getVmUtilizationMean(vm);
            double mad = readSpace.getVmUtilizationMad(vm);

            // mean and mad are both expressed in the same utilisation-fraction
            // units, so the mips scaling factor cancels out of this ratio.
            if (mean <= 0.0) {
                result[i] = 0.0;
            } else {
                result[i] = mad / mean;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v5] VM load volatility coefficient computed for ", vms.size(), " VMs");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "vm-loadVolatilityCoV-madOverMeanUtil";
    }

    @Override
    public int outputGuid() {
        return 1301;
    }
}
