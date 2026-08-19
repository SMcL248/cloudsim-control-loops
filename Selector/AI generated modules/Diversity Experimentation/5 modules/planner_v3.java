package org.cloudbus.cloudsim.examples;// always include

// Import whats needed
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

// Strategy: vertical elastic MIPS scaling at the VM level.
// Overload is always addressed before underload: an under-provisioned VM risks Cloudlet
// completion failures, while an over-provisioned VM only risks wasted energy. Among VMs
// sharing a state, breaks ties using live CPU utilisation (worst offender / most idle).
public class planner_v3 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        int[] mipsTiers = readSpace.getMipsTiers();

        GuestEntity target = null;
        boolean scaleUp = true;
        double extremeUtil = -1.0;

        // Overload takes priority: an under-provisioned VM risks Cloudlet completion failures,
        // an over-provisioned VM only risks wasted capacity.
        for (int i = 0; i < diagnosis.length && i < vms.size(); i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) continue;
            GuestEntity vm = vms.get(i);
            if (readSpace.isVmBeingInstantiated(vm) || readSpace.isVmMigrating(vm)) continue;
            double util = readSpace.getVmCpuUtil(vm);
            if (util > extremeUtil) {
                extremeUtil = util;
                target = vm;
                scaleUp = true;
            }
        }

        if (target == null) {
            extremeUtil = Double.MAX_VALUE;
            for (int i = 0; i < diagnosis.length && i < vms.size(); i++) {
                if (diagnosis[i] != LoadState.UNDERLOADED) continue;
                GuestEntity vm = vms.get(i);
                if (readSpace.isVmBeingInstantiated(vm) || readSpace.isVmMigrating(vm)) continue;
                double util = readSpace.getVmCpuUtil(vm);
                if (util < extremeUtil) {
                    extremeUtil = util;
                    target = vm;
                    scaleUp = false;
                }
            }
        }

        if (target == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v3] ", "no vm requires mips rescaling");
            return new int[0];
        }

        int tierIndex = -1;

        if (scaleUp) {
            double nextTier = readSpace.getNextMipsTier(target);
            if (nextTier < 0) {
                Log.printlnConcat(readSpace.getNow(), ": [planner_v3] ", "overloaded vm already at max mips tier, no action");
                return new int[0];
            }
            for (int t = 0; t < mipsTiers.length; t++) {
                if (mipsTiers[t] == (int) nextTier) {
                    tierIndex = t;
                    break;
                }
            }
        } else {
            double currentMips = readSpace.getVmMips(target);
            int currentIdx = -1;
            for (int t = 0; t < mipsTiers.length; t++) {
                if (mipsTiers[t] == (int) currentMips) {
                    currentIdx = t;
                    break;
                }
            }
            if (currentIdx > 0) {
                tierIndex = currentIdx - 1;
            }
        }

        if (tierIndex < 0) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v3] ", "could not resolve a valid target mips tier, no action");
            return new int[0];
        }

        Log.printlnConcat(readSpace.getNow(), ": [planner_v3] planning mips rescale of vm ", readSpace.getId(target), " to tier index ", tierIndex);

        return new int[] { readSpace.getId(target), tierIndex };
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-elastic-signal";
    }

    @Override
    public String outputSemantic() {
        return "requestMipsScaling";
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
