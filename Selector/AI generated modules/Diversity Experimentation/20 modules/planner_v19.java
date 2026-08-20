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

/**
 * Strategy: progress-protective preemptive drain.
 * Among hosts flagged UNDERLOADED (candidates for future consolidation),
 * picks the one carrying the most guest VMs - the host whose eventual
 * power-down would be most disruptive - and starts draining it early by
 * migrating the VM whose cloudlets are closest to completion, protecting
 * near-finished work before it can be caught by a later, more urgent
 * eviction. Placement is first-fit rather than optimised, since the goal
 * is safety, not efficiency.
 */
public class planner_v19 implements Planner<LoadState[], int[]> {

    private static final String MODULE_NAME = "planner_v19";

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();

        HostEntity source = null;
        int mostGuests = -1;
        for (int i = 0; i < diagnosis.length && i < hosts.size(); i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) {
                continue;
            }
            HostEntity host = hosts.get(i);
            if (readSpace.isHostFailed(host) || readSpace.isHostPermanentlyDead(host)) {
                continue;
            }
            if (readSpace.isHostPoweredDown(host) || readSpace.isHostPoweringUp(host)) {
                continue;
            }
            int guestCount = readSpace.getVmListForHost(host).size();
            if (guestCount > mostGuests) {
                mostGuests = guestCount;
                source = host;
            }
        }

        if (source == null) {
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] no underloaded host eligible for preemptive drain");
            return new int[0];
        }

        GuestEntity protectedVm = null;
        double closestRatio = Double.MAX_VALUE;
        for (GuestEntity vm : readSpace.getVmListForHost(source)) {
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }
            List<Cloudlet> cloudlets = readSpace.getVmCloudletList(vm);
            for (Cloudlet cl : cloudlets) {
                long total = readSpace.getTotalLength(cl);
                if (total <= 0) {
                    continue;
                }
                double ratio = (double) readSpace.getRemainingLength(cl) / (double) total;
                if (ratio < closestRatio) {
                    closestRatio = ratio;
                    protectedVm = vm;
                }
            }
        }

        if (protectedVm == null) {
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] no vm with active cloudlets found on drain candidate host " + readSpace.getId(source));
            return new int[0];
        }

        HostEntity target = null;
        for (HostEntity host : hosts) {
            if (host == source) {
                continue;
            }
            if (readSpace.isHostFailed(host) || readSpace.isHostPermanentlyDead(host)) {
                continue;
            }
            if (readSpace.isHostPoweredDown(host) || readSpace.isHostPoweringUp(host)) {
                continue;
            }
            if (readSpace.canMigrateGuestToHost(host, protectedVm)) {
                target = host;
                break;
            }
        }

        if (target == null) {
            Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] no safe first-fit host found to protect vm " + readSpace.getId(protectedVm));
            return new int[0];
        }

        int vmId = readSpace.getId(protectedVm);
        int hostId = readSpace.getId(target);
        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] protectively draining vm " + vmId + " from host " + readSpace.getId(source) + " to host " + hostId);
        return new int[]{vmId, hostId};
    }

    @Override
    public String inputSemantic() {
        return "host-cpu-load-drain-candidate";
    }

    @Override
    public String outputSemantic() {
        return "migrate-vm-protect-near-completion";
    }

    @Override
    public int inputGuid() {
        return 2200;
    }

    @Override
    public int outputGuid() {
        return 3002;
    }
}
