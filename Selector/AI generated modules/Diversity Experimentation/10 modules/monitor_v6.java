package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

/**
 * VM-level monitor. Computes a coefficient-of-variation style burstiness
 * score from the rolling 30-reading utilisation mean and MAD. The MAD is
 * not on the same scale as the raw utilisation mean, so the mean is
 * first converted to absolute MIPS (mean fraction x VM MIPS rating)
 * before the ratio is taken, per the documented scaling caveat. A low
 * score means the VM's load is steady and predictable; a high score
 * means its load swings sharply from reading to reading, which
 * distinguishes two VMs with identical average utilisation but very
 * different volatility.
 */
public class monitor_v6 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);

            double meanUtil = readSpace.getVmUtilizationMean(vm);
            double vmMips = readSpace.getVmMips(vm);
            double meanAbsolute = meanUtil * vmMips;
            double mad = readSpace.getVmUtilizationMad(vm);

            result[i] = meanAbsolute > 0 ? mad / meanAbsolute : 0.0;
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v6] ", "computed utilisation burstiness for ", vms.size(), " vms");
        return result;
    }

    @Override
    public String outputSemantic() {
        return "vm-utilization-burstiness-coefficient-of-variation";
    }

    @Override
    public int outputGuid() {
        return 1300;
    }
}
