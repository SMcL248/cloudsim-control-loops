package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

public class monitor_v7 implements Monitor<double[]> {

    private static final String SEMANTIC = "vm-util-cpu";
    private static final int GUID = 1300;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);
            result[i] = readSpace.getVmCpuUtil(vm);
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v7] vm cpu utilisation sampled for ", vms.size(), " vms");
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
