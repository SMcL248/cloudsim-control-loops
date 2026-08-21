package org.cloudbus.cloudsim.examples;// always include

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

// Strategy: Idle Reclamation for Consolidation Headroom.
// Diagnosis is VM-level. Only ever targets VMs flagged UNDERLOADED that are
// currently carrying zero active cloudlets, so no in-flight work is ever at
// risk of loss -- this is the one variant that touches destruction, and it
// is deliberately conservative about it. Among such idle VMs, destroys the
// one resident on the host with the least available MIPS headroom, freeing
// that constrained host's resources for its busier siblings or for future
// migrations, which is where the actual throughput benefit accrues.
public class planner_v10 implements Planner<LoadState[], int[]> {

    private HostEntity hostOf(GuestEntity vm, ReadSpace readSpace) {
        for (HostEntity h : readSpace.getAllHosts()) {
            for (GuestEntity v : readSpace.getVmListForHost(h)) {
                if (readSpace.getId(v) == readSpace.getId(vm)) {
                    return h;
                }
            }
        }
        return null;
    }

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {

        List<GuestEntity> vms = readSpace.getVmList();

        GuestEntity target = null;
        double leastHeadroom = Double.MAX_VALUE;

        for (int i = 0; i < diagnosis.length && i < vms.size(); i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) {
                continue;
            }
            GuestEntity vm = vms.get(i);
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }
            if (!readSpace.getVmCloudletList(vm).isEmpty()) {
                continue;
            }
            HostEntity host = hostOf(vm, readSpace);
            if (host == null) {
                continue;
            }
            double headroom = readSpace.getHostAvailableMips(host);
            if (headroom < leastHeadroom) {
                leastHeadroom = headroom;
                target = vm;
            }
        }

        if (target == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v10] No idle underloaded VM eligible for reclamation found.");
            return new int[]{-1};
        }

        Log.printlnConcat(readSpace.getNow(), ": [planner_v10] Idle reclamation: destroying VM ", readSpace.getId(target), " on constrained host.");

        return new int[]{readSpace.getId(target)};
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-classification";
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
