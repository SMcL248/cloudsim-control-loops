package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

import java.util.List;

// VM level - smoothed rolling-average utilization per VM.
public class monitor_v8 implements Monitor<double[]> {

    private static final int OUTPUT_GUID = 1300;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);
            result[i] = readSpace.getVmUtilizationMean(vm);
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v8] computed vm-utilization-mean-rolling30 for ", vms.size(), " vms.");

        return result;
    }

    @Override
    public String outputSemantic() {
        return "vm-utilization-mean-rolling30: 30-reading rolling average of this VM's CPU utilization fraction, "
                + "sourced directly from ReadSpace's smoothed utilization tracker (getVmUtilizationMean). Index i "
                + "corresponds to getVmList().get(i).";
    }

    @Override
    public int outputGuid() {
        return OUTPUT_GUID;
    }
}
