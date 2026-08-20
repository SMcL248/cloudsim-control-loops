package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

import java.util.List;

// VM level - imbalance of MIPS demand across a VM's own allocated PEs.
public class monitor_v15 implements Monitor<double[]> {

    private static final int OUTPUT_GUID = 1300;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);
            List<Double> perPeMips = readSpace.getVmMipsPerPe(vm);

            if (perPeMips.size() <= 1) {
                result[i] = 0.0;
                continue;
            }

            double sum = 0.0;
            for (double v : perPeMips) {
                sum += v;
            }
            double mean = sum / perPeMips.size();

            double deviationSum = 0.0;
            for (double v : perPeMips) {
                deviationSum += Math.abs(v - mean);
            }

            result[i] = deviationSum / perPeMips.size();
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v15] computed vm-per-pe-load-imbalance for ", vms.size(), " vms.");

        return result;
    }

    @Override
    public String outputSemantic() {
        return "vm-per-pe-load-imbalance: mean absolute deviation of MIPS requested across this VM's individual "
                + "allocated PEs (from getVmMipsPerPe), capturing how unevenly this VM's own processing elements "
                + "are loaded relative to one another. 0.0 for a single-PE VM or perfectly even load. Index i "
                + "corresponds to getVmList().get(i).";
    }

    @Override
    public int outputGuid() {
        return OUTPUT_GUID;
    }
}
