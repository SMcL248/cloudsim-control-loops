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

// Variant 4: VM-level scale-up headroom fraction.
// Counts how many of the three scalable dimensions (MIPS, RAM, BW) still
// have a next tier available for this VM (getNext*Tier != -1 sentinel),
// expressed as a fraction of 3. This is a vertical-scaling capacity signal:
// a VM at 0.0 is already maxed on every dimension and vertical scaling is a
// dead end for it, whereas 1.0 has full room to grow. Distinct from
// monitor_v3's volatility metric -- this describes available ceiling, not
// observed behaviour -- and shares GUID 1300 with it deliberately (both are
// VM-level, level 3, but semantically different; consumers must key off
// outputSemantic() to tell them apart, not the guid alone).
public class monitor_v4 implements Monitor<double[]> {

    @Override
    public double[] observe(ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        double[] headroom = new double[vms.size()];

        for (int i = 0; i < vms.size(); i++) {
            GuestEntity vm = vms.get(i);

            int dimsWithRoom = 0;
            if (readSpace.getNextMipsTier(vm) != -1.0) {
                dimsWithRoom++;
            }
            if (readSpace.getNextRamTier(vm) != -1.0) {
                dimsWithRoom++;
            }
            if (readSpace.getNextBwTier(vm) != -1.0) {
                dimsWithRoom++;
            }

            headroom[i] = dimsWithRoom / 3.0;
        }

        Log.printlnConcat(readSpace.getNow(), ": [monitor_v4] vm scale-up headroom fraction computed for ", vms.size(), " vms (index-aligned with getVmList).");
        return headroom;
    }

    @Override
    public String outputSemantic() {
        return "3-vmScaleUpHeadroomFraction-countOfMipsRamBwDimsWithNextTierAvailable_divBy3";
    }

    @Override
    public int outputGuid() {
        return 1300;
    }

}
