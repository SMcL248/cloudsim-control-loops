package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.Cloudlet;

import java.util.List;

// Strategy: power-led, safety-checked scale-in. Diagnosis is per-VM. Destroys a VM only when it is
// both underloaded and idle (zero assigned cloudlets), so the irreversible action never risks
// permanent cloudlet loss.
public class planner_v4 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {

        List<GuestEntity> vms = readSpace.getVmList();

        if (diagnosis == null || vms == null || diagnosis.length != vms.size()) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v4] diagnosis/VM list mismatch, no-op");
            return new int[0];
        }

        int targetIndex = -1;
        for (int i = 0; i < diagnosis.length; i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) continue;
            GuestEntity vm = vms.get(i);
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) continue;
            List<Cloudlet> owned = readSpace.getVmCloudletList(vm);
            if (owned == null || owned.isEmpty()) {
                targetIndex = i;
                break;
            }
        }

        if (targetIndex == -1) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v4] no idle underloaded VM safe to destroy, no-op");
            return new int[0];
        }

        int vmId = readSpace.getId(vms.get(targetIndex));

        Log.printlnConcat(readSpace.getNow(), ": [planner_v4] destroying idle underloaded VM ", vmId, " to reduce power draw");

        return new int[] { vmId };
    }

    @Override
    public String inputSemantic() {
        return "vm-cpu-load-state";
    }

    @Override
    public String outputSemantic() {
        return "requestVmDestruction";
    }

    @Override
    public int inputGuid() {
        return 2300;
    }

    @Override
    public int outputGuid() {
        return 3004;
    }
}
