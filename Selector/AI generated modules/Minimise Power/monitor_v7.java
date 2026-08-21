package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

public class monitor_v7 implements Monitor<double[]> {

    private static final int OUTPUT_GUID = 1303;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];
        double totalWaste = 0.0;

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);
            double provisioned = readSpace.getVmRequestedMips(vm);
            double util = Math.max(0.0, Math.min(1.0, readSpace.getVmCpuUtil(vm)));
            double waste = provisioned * (1.0 - util);
            result[i] = waste;
            totalWaste += waste;
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v7] Idle provisioned capacity across ", vms.size(), " VMs totals ", totalWaste, " MIPS");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "vm-overprovision_waste_mips";
    }

    @Override
    public int outputGuid() {
        return OUTPUT_GUID;
    }
}
