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

// Strategy: Severity-Scaled Elastic Scale-Out.
// Diagnosis is host-level. When any hosts are flagged OVERLOADED, this
// variant requests a brand new VM rather than redistributing existing load,
// on the reasoning that sustained overload across a meaningful share of the
// cluster means there is simply not enough aggregate serving capacity to
// balance our way out of, and adding a fresh VM is the only lever that
// grows total throughput headroom. The requested VM's size class escalates
// with the severity of the overload (fraction of active hosts overloaded):
// a small pocket of pressure gets a small VM, widespread pressure gets a
// large one, so the reinforcement is proportionate to the crisis.
public class planner_v9 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {

        List<HostEntity> hosts = readSpace.getAllHosts();

        int overloadedCount = 0;
        int consideredCount = 0;

        for (int i = 0; i < diagnosis.length && i < hosts.size(); i++) {
            HostEntity h = hosts.get(i);
            if (readSpace.isHostFailed(h) || readSpace.isHostPermanentlyDead(h) || readSpace.isHostPoweredDown(h)) {
                continue;
            }
            consideredCount++;
            if (diagnosis[i] == LoadState.OVERLOADED) {
                overloadedCount++;
            }
        }

        if (consideredCount == 0 || overloadedCount == 0) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v9] No sustained overload detected, no VM creation issued.");
            return new int[]{-1, -1, -1};
        }

        List<GuestEntity> vms = readSpace.getVmList();
        if (vms.isEmpty()) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v9] No existing VM available to resolve target datacenter, no VM creation issued.");
            return new int[]{-1, -1, -1};
        }

        double overloadRatio = (double) overloadedCount / consideredCount;

        // tierIndex represents the VM size class (0 = small, 1 = medium, 2 = large,
        // per the small/medium/large convention described for this environment).
        // Storage tier is scaled to match the compute class in this prototype.
        int tierIndex;
        if (overloadRatio > 0.66) {
            tierIndex = 2;
        } else if (overloadRatio > 0.33) {
            tierIndex = 1;
        } else {
            tierIndex = 0;
        }
        int sizeTierIndex = tierIndex;

        int datacenterId = readSpace.getDatacenterFor(readSpace.getId(vms.get(0)));

        Log.printlnConcat(readSpace.getNow(), ": [planner_v9] Elastic scale-out: overload ratio ", overloadRatio, " requesting tier ", tierIndex, " VM in datacenter ", datacenterId, ".");

        return new int[]{tierIndex, sizeTierIndex, datacenterId};
    }

    @Override
    public String inputSemantic() {
        return "host-loadstate-classification";
    }

    @Override
    public String outputSemantic() {
        return "requestVmCreation";
    }

    @Override
    public int inputGuid() {
        return 2200;
    }

    @Override
    public int outputGuid() {
        return 3003;
    }
}
