package org.cloudbus.cloudsim.examples;// always include

// Import whats needed
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.core.PowerGuestEntity;
import org.cloudbus.cloudsim.core.PowerHostEntity;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVm;

import java.util.List;

// Variant 3: VM-level CPU utilisation volatility (coefficient of variation).
// getVmUtilizationMad() is explicitly not MIPS-scaled while
// getVmUtilizationMean() is, so the MAD is scaled by the VM's MIPS rating
// before the ratio is taken, giving a dimensionless volatility score.
// A high value signals a bursty/unpredictable workload that a planner may
// want to treat more conservatively (e.g. avoid tight-packing that VM);
// this is a stability signal, orthogonal to raw utilisation level.
public class monitor_v3 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        double[] volatility = new double[vms.size()];

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);

            double meanUtil = readSpace.getVmUtilizationMean(vm);
            double madUtil = readSpace.getVmUtilizationMad(vm);
            double vmMips = readSpace.getVmMips(vm);

            // madUtil is not MIPS-scaled; scale it so it shares units with
            // meanUtil before dividing.
            double madScaled = madUtil * vmMips;

            if (meanUtil <= 0.0) {
                volatility[i] = 0.0;
            } else {
                volatility[i] = madScaled / meanUtil;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v3] vm cpu utilisation volatility (CV) computed for ", vms.size(), " vms (index-aligned with getVmList).");
        return volatility;
    }

    @Override
    public String outputSemantic() {
        return "3-vmCpuUtilVolatilityCV-madUtilScaledByVmMips_divByUtilMeanMips_dimensionless";
    }

    @Override
    public int outputGuid() {
        return 1300;
    }

}
