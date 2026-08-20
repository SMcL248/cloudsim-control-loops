package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

import java.util.List;

// VM level - demand volatility scaled from the raw utilization MAD into MIPS units.
public class monitor_v9 implements Monitor<double[]> {

    private static final int OUTPUT_GUID = 1300;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);
            double mad = readSpace.getVmUtilizationMad(vm);
            double mips = readSpace.getVmMips(vm);
            result[i] = mad * mips;
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v9] computed vm-utilization-volatility-mips for ", vms.size(), " vms.");

        return result;
    }

    @Override
    public String outputSemantic() {
        return "vm-utilization-volatility-mips: 30-reading mean absolute deviation of this VM's CPU utilization, "
                + "scaled into MIPS units by multiplying by the VM's MIPS rating (getVmUtilizationMad is not MIPS "
                + "scaled on its own), representing the typical absolute swing in this VM's real workload demand. "
                + "Index i corresponds to getVmList().get(i).";
    }

    @Override
    public int outputGuid() {
        return OUTPUT_GUID;
    }
}
