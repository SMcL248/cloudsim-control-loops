package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

import java.util.List;

// VM level - composite elasticity headroom across MIPS, RAM and bandwidth scaling dimensions.
public class monitor_v14 implements Monitor<double[]> {

    private static final int OUTPUT_GUID = 1300;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);

            int mipsScalable = (readSpace.getNextMipsTier(vm) >= 0.0) ? 1 : 0;
            int ramScalable = (readSpace.getNextRamTier(vm) >= 0.0) ? 1 : 0;
            int bwScalable = (readSpace.getNextBwTier(vm) >= 0.0) ? 1 : 0;

            result[i] = (mipsScalable + ramScalable + bwScalable) / 3.0;
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v14] computed vm-elasticity-headroom-composite for ", vms.size(), " vms.");

        return result;
    }

    @Override
    public String outputSemantic() {
        return "vm-elasticity-headroom-composite: fraction of the three scalable resource dimensions (MIPS, RAM, "
                + "bandwidth) for which this VM has not yet reached its maximum permitted tier, averaged into a "
                + "single 0.0-1.0 elasticity score. 1.0 means all three dimensions still have upgrade headroom, "
                + "0.0 means all three are maxed out. Index i corresponds to getVmList().get(i).";
    }

    @Override
    public int outputGuid() {
        return OUTPUT_GUID;
    }
}
