package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

import java.util.List;

/**
 * VM-level monitor.
 *
 * Approach: temporal volatility. Reports each VM's 30-reading CPU
 * utilisation MAD (mean absolute deviation), scaled into MIPS units via
 * the VM's MIPS rating. This is a noise/burstiness signal rather than a
 * load signal: a VM sitting at 40% mean utilisation that swings wildly
 * reading-to-reading is a much riskier scaling/placement candidate than
 * a VM steady at 40%, even though both would report the same mean.
 */
public class monitor_v3 implements Monitor<double[]> {

    private static final String SEMANTIC = "vm-utilVolatilityMips-mad30scaled";
    private static final int GUID = 1300;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];

        double sum = 0.0;
        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);

            double mad = readSpace.getVmUtilizationMad(vm);
            double mips = readSpace.getVmMips(vm);
            double volatility = mad * mips;

            result[i] = volatility;
            sum += volatility;
        }

        double mean = vms.isEmpty() ? 0.0 : sum / vms.size();
        String message = "vms=" + vms.size() + " meanUtilVolatilityMips=" + mean;
        Log.printlnConcat(readSpace.getNow(), ": [monitor_v3] ", message);

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
