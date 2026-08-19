package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

// VM-level monitor: elastic MIPS headroom fraction.
// For each VM, reports how much scale-up room remains before the next MIPS
// tier, expressed as a fraction of current MIPS: (nextTier - current) /
// current. VMs already at the top tier, or whose current value does not
// match a known tier (both signalled by the -1 sentinel from
// getNextMipsTier), report zero headroom. VMs mid-migration or still being
// instantiated report the module's own -1.0 sentinel, since scaling
// decisions during those transitions are unreliable.
public class monitor_v7 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        double[] result = new double[vms.size()];

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);

            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                result[i] = -1.0;
                continue;
            }

            double current = readSpace.getVmMips(vm);
            double next = readSpace.getNextMipsTier(vm);

            if (next < 0.0 || current <= 0.0) {
                result[i] = 0.0;
                continue;
            }

            result[i] = (next - current) / current;
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v7] computed vm elastic mips headroom for ",
                vms.size(), " vms");

        return result;
    }

    @Override
    public String outputSemantic() {
        return "vm-elastic-mips-headroom-fraction-to-next-tier";
    }

    @Override
    public int outputGuid() {
        return 1300;
    }
}
