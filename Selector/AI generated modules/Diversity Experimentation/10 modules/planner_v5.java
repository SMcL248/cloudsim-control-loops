package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

// Strategy: Sustained-load confirmation before vertical scale-up.
// A VM being OVERLOADED at this instant is not, by itself, trusted. This planner
// additionally reads the 30-reading rolling mean and MAD of the VM's utilisation
// (scaling the MAD by the VM's MIPS rating, since it is not natively MIPS-scaled)
// to distinguish a sustained high load from a noisy transient spike. Only a VM
// that is both persistently high AND stable is scaled up a MIPS tier, avoiding
// scale-up thrash on short-lived bursts.
public class planner_v5 implements Planner<LoadState[], int[]> {

    private static final double SUSTAINED_MEAN_THRESHOLD = 0.75;
    private static final double STABILITY_RATIO = 0.15;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {

        Log.printlnConcat(readSpace.getNow(), ": [planner_v5] ", "checking overloaded VMs for sustained, stable load");

        List<GuestEntity> vms = readSpace.getVmList();
        int[] mipsTiers = readSpace.getMipsTiers();

        int limit = Math.min(diagnosis.length, vms.size());
        for (int i = 0; i < limit; i++) {

            if (diagnosis[i] != LoadState.OVERLOADED) {
                continue;
            }

            GuestEntity vm = vms.get(i);

            double mean = readSpace.getVmUtilizationMean(vm);
            double mad = readSpace.getVmUtilizationMad(vm);
            double vmMips = readSpace.getVmMips(vm);
            double scaledMad = mad * vmMips;

            boolean sustained = mean >= SUSTAINED_MEAN_THRESHOLD;
            boolean stable = scaledMad <= (mean * vmMips * STABILITY_RATIO);

            if (!(sustained && stable)) {
                continue;
            }

            double nextTierValue = readSpace.getNextMipsTier(vm);
            if (nextTierValue < 0) {
                continue;
            }

            int tierIndex = indexOfTier(mipsTiers, nextTierValue);
            if (tierIndex < 0) {
                continue;
            }

            int vmId = readSpace.getId(vm);
            Log.printlnConcat(readSpace.getNow(), ": [planner_v5] ", "confirmed sustained overload, scaling vmId=" + vmId + " to mips tier index=" + tierIndex);
            return new int[] { vmId, tierIndex };
        }

        Log.printlnConcat(readSpace.getNow(), ": [planner_v5] ", "no VM showed sustained, stable overload this cycle");
        return null;
    }

    private int indexOfTier(int[] tiers, double value) {
        for (int i = 0; i < tiers.length; i++) {
            if (tiers[i] == value) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String inputSemantic() {
        return "vm-cpuutil-loadstate";
    }

    @Override
    public String outputSemantic() {
        return "vm-mips-scaleup-sustained";
    }

    @Override
    public int inputGuid() {
        return 2300;
    }

    @Override
    public int outputGuid() {
        return 3005;
    }
}
