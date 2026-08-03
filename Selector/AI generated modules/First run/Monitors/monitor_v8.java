package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

public class monitor_v8 implements Monitor<double[]> {

    private static final String SEMANTIC = "vm-mips-headroom";
    private static final int GUID = 1300;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);
            double headroom = readSpace.getVmMaxMips(vm) - readSpace.getVmMips(vm);
            result[i] = Math.max(headroom, 0.0);
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v8] vm mips headroom computed for ", vms.size(), " vms");
        return result;
    }

    @Override
    public String outputSemantic() {
        return SEMANTIC;
    }

    @Override
    public int outputGuid() {
        return GUID;
    }
}
