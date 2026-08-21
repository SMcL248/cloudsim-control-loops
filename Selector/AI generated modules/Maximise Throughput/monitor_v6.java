package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

import java.util.List;

// Variant 6: VM scale-up headroom score.
// Averages the normalised distance to the next MIPS, RAM and BW tier for
// each VM. A VM already near its ceiling on all three dimensions has
// little room left to absorb more load vertically - this is a forward-
// looking capacity signal rather than a current-load signal.
public class monitor_v6 implements Monitor<double[]> {

    private static final String MODULE_NAME = "monitor_v6";

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);

            double mipsRatio = tierRatio(readSpace.getNextMipsTier(vm), readSpace.getVmMips(vm));
            double ramRatio = tierRatio(readSpace.getNextRamTier(vm), readSpace.getVmRam(vm));
            double bwRatio = tierRatio(readSpace.getNextBwTier(vm), readSpace.getVmBw(vm));

            result[i] = (mipsRatio + ramRatio + bwRatio) / 3.0;
        }

        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] ",
                "scale-up headroom score computed for ", vms.size(), " VMs");

        return result;
    }

    // Normalised distance to the next resource tier. Returns 0 if already
    // maxed out (sentinel -1) or the current value is non-positive.
    private double tierRatio(double nextTier, double current) {
        if (nextTier < 0 || current <= 0.0) {
            return 0.0;
        }
        return (nextTier - current) / current;
    }

    @Override
    public String outputSemantic() {
        return "vm-scale-up-headroom-ratio";
    }

    @Override
    public int outputGuid() {
        return 1300;
    }
}
