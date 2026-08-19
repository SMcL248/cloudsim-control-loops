package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

/**
 * VM-level monitor. Checks all three scaling dimensions available to a
 * VM (MIPS, RAM, Bandwidth tiers) and counts how many are already at
 * their ceiling, using the -1 sentinel each getNextXTier() call returns
 * once a dimension is maxed out. The result is the fraction of
 * dimensions with no further vertical scaling headroom left (0.0 = able
 * to grow in every dimension, 1.0 = no vertical scaling options remain
 * at all). This is a capacity-growth signal, independent of how
 * utilised the VM currently is.
 */
public class monitor_v7 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);

            int maxedCount = 0;
            if (readSpace.getNextMipsTier(vm) < 0) {
                maxedCount++;
            }
            if (readSpace.getNextRamTier(vm) < 0) {
                maxedCount++;
            }
            if (readSpace.getNextBwTier(vm) < 0) {
                maxedCount++;
            }

            result[i] = maxedCount / 3.0;
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v7] ", "computed scale-ceiling proximity for ", vms.size(), " vms");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "vm-scaling-headroom-ceiling-proximity-fraction";
    }

    @Override
    public int outputGuid() {
        return 1300;
    }
}
