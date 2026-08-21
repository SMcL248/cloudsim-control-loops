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

// Strategy: Parallelism Expansion.
// Diagnosis is VM-level. Among VMs flagged OVERLOADED, looks for VMs that
// are PE-starved -- carrying more concurrently queued cloudlets than they
// have allocated PEs to service them in parallel -- and whose host still
// has a free PE to give. Grants an extra PE to the VM with the largest
// deficit (queued cloudlets minus current PEs). Unlike vertical MIPS
// scaling, this targets throughput lost to parallelism starvation rather
// than raw clock speed: many small queued cloudlets competing for too few
// PEs on one VM.
public class planner_v4 implements Planner<LoadState[], int[]> {

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
        int worstDeficit = 0;

        for (int i = 0; i < diagnosis.length && i < vms.size(); i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) {
                continue;
            }
            GuestEntity vm = vms.get(i);
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }
            int pes = readSpace.getVmNumberOfPes(vm);
            int queued = readSpace.getVmCloudletList(vm).size();
            int deficit = queued - pes;
            if (deficit <= 0) {
                continue;
            }
            HostEntity host = hostOf(vm, readSpace);
            if (host == null || !readSpace.hostHasFreePe(host)) {
                continue;
            }
            if (deficit > worstDeficit) {
                worstDeficit = deficit;
                target = vm;
            }
        }

        if (target == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v4] No PE-starved overloaded VM with a free host PE found.");
            return new int[]{-1};
        }

        Log.printlnConcat(readSpace.getNow(), ": [planner_v4] PE allocation: VM ", readSpace.getId(target), " deficit ", worstDeficit, ".");

        return new int[]{readSpace.getId(target)};
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-classification";
    }

    @Override
    public String outputSemantic() {
        return "requestPeAllocation";
    }

    @Override
    public int inputGuid() {
        return 2300;
    }

    @Override
    public int outputGuid() {
        return 3008;
    }
}
