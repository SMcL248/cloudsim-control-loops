package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

import java.util.List;

// VM level - ordinal encoding of VM placement lifecycle stability.
public class monitor_v12 implements Monitor<double[]> {

    private static final int OUTPUT_GUID = 1300;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);

            if (readSpace.isVmBeingInstantiated(vm)) {
                result[i] = 0.0;
            } else if (readSpace.isVmMigrating(vm)) {
                result[i] = 0.5;
            } else {
                result[i] = 1.0;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v12] computed vm-lifecycle-stability-score for ", vms.size(), " vms.");

        return result;
    }

    @Override
    public String outputSemantic() {
        return "vm-lifecycle-stability-score: ordinal encoding of VM placement lifecycle - 0.0 = still being "
                + "instantiated/unplaced, 0.5 = actively migrating, 1.0 = stably placed and not migrating. Index i "
                + "corresponds to getVmList().get(i).";
    }

    @Override
    public int outputGuid() {
        return OUTPUT_GUID;
    }
}
