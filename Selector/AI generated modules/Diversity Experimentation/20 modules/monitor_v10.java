package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

import java.util.List;

// VM level - proportional MIPS headroom available at the next permitted scaling tier.
public class monitor_v10 implements Monitor<double[]> {

    private static final int OUTPUT_GUID = 1300;

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);
            double next = readSpace.getNextMipsTier(vm);
            double current = readSpace.getVmMips(vm);

            if (next < 0.0 || current <= 0.0) {
                result[i] = 0.0;
            } else {
                result[i] = (next - current) / current;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v10] computed vm-mips-scaleup-headroom-ratio for ", vms.size(), " vms.");

        return result;
    }

    @Override
    public String outputSemantic() {
        return "vm-mips-scaleup-headroom-ratio: proportional MIPS capacity gain available if this VM were scaled "
                + "to its next permitted MIPS tier, computed as (nextTier - current) / current. 0.0 if the VM is "
                + "already at its maximum tier or getNextMipsTier returns the -1 sentinel. Index i corresponds to "
                + "getVmList().get(i).";
    }

    @Override
    public int outputGuid() {
        return OUTPUT_GUID;
    }
}
