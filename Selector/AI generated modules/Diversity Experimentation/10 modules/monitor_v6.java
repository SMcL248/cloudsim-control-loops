package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

// VM-level monitor: CPU utilisation volatility.
// Reports a coefficient-of-variation-like burstiness score per VM: the
// 30-reading MAD of utilisation, scaled up to the same order of magnitude as
// the mean by multiplying by the VM's MIPS rating (per the documented caveat
// that the raw MAD figure is not MIPS-scaled and must not be compared to the
// mean directly), divided by the mean utilisation itself. Higher values
// indicate a VM whose load swings unpredictably rather than sitting steady.
public class monitor_v6 implements Monitor<double[]> {

    private static final double EPSILON = 1e-6;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);

            double mean = readSpace.getVmUtilizationMean(vm);
            double mad = readSpace.getVmUtilizationMad(vm);
            double mips = readSpace.getVmMips(vm);

            if (mean <= EPSILON) {
                result[i] = -1.0;
                continue;
            }

            double scaledMad = mad * mips;
            result[i] = scaledMad / mean;
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v6] computed vm cpu utilisation volatility for ",
                vms.size(), " vms");

        return result;
    }

    @Override
    public String outputSemantic() {
        return "vm-cpu-util-volatility-mad-mips-scaled-over-mean";
    }

    @Override
    public int outputGuid() {
        return 1300;
    }
}
