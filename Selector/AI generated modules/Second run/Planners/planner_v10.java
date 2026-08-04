package org.cloudbus.cloudsim.examples;// always include

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

// Cloudlet-level planner. diagnosis[i] is the load state of
// readSpace.getActiveCloudlets().get(i), where OVERLOADED is interpreted by
// the paired analyser as "at risk": the cloudlet's VM sits on a host that is
// failed or permanently dead, so it may be evacuated, and possibly lost, if
// no VM survives the evacuation.
// Goal: maximise service quality (cloudlet completion rate).
// Strategy: proactively rescue the first at-risk cloudlet by moving it onto
// a VM hosted on a healthy host with free processing capacity, before the
// evacuation/loss path can claim it. ReadSpace exposes no direct
// cloudlet->VM or VM->host lookup, so both are resolved by membership scan.
public class planner_v10 implements Planner<LoadState[], int[]> {

    private static final String MODULE_NAME = "planner_v10";
    private static final int INPUT_GUID = 2400;
    private static final int OUTPUT_GUID = 3001;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();

        if (diagnosis == null || diagnosis.length != cloudlets.size()) {
            Log.printlnConcat(now, ": [" + MODULE_NAME + "] diagnosis/cloudlet size mismatch, no-op");
            return new int[]{-1, -1, -1};
        }

        List<GuestEntity> vms = readSpace.getVmList();
        List<HostEntity> hosts = readSpace.getAllHosts();

        for (int i = 0; i < cloudlets.size(); i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) continue;
            Cloudlet cl = cloudlets.get(i);

            GuestEntity sourceVm = findOwningVm(readSpace, vms, cl);
            if (sourceVm == null) continue;

            HostEntity sourceHost = findOwningHost(readSpace, hosts, sourceVm);
            if (sourceHost == null) continue;
            if (!readSpace.isHostFailed(sourceHost) && !readSpace.isHostPermanentlyDead(sourceHost)) {
                continue; // not actually at risk, leave to another module
            }

            GuestEntity destVm = findHealthyDestination(readSpace, vms, hosts, sourceVm);
            if (destVm == null) continue;

            int cloudletId = readSpace.getId(cl);
            int fromVmId = readSpace.getId(sourceVm);
            int toVmId = readSpace.getId(destVm);

            Log.printlnConcat(now, ": [" + MODULE_NAME + "] plan rescue cloudlet ", cloudletId,
                    " off at-risk VM ", fromVmId, " -> VM ", toVmId);
            return new int[]{cloudletId, fromVmId, toVmId};
        }

        Log.printlnConcat(now, ": [" + MODULE_NAME + "] no at-risk cloudlet requiring rescue, no-op");
        return new int[]{-1, -1, -1};
    }

    private GuestEntity findOwningVm(ReadSpace readSpace, List<GuestEntity> vms, Cloudlet cl) {
        int targetId = readSpace.getId(cl);
        for (GuestEntity vm : vms) {
            for (Cloudlet owned : readSpace.getVmCloudletList(vm)) {
                if (readSpace.getId(owned) == targetId) {
                    return vm;
                }
            }
        }
        return null;
    }

    private HostEntity findOwningHost(ReadSpace readSpace, List<HostEntity> hosts, GuestEntity vm) {
        int targetId = readSpace.getId(vm);
        for (HostEntity host : hosts) {
            for (GuestEntity guest : readSpace.getVmListForHost(host)) {
                if (readSpace.getId(guest) == targetId) {
                    return host;
                }
            }
        }
        return null;
    }

    private GuestEntity findHealthyDestination(ReadSpace readSpace, List<GuestEntity> vms,
            List<HostEntity> hosts, GuestEntity sourceVm) {
        int sourceId = readSpace.getId(sourceVm);
        for (GuestEntity candidate : vms) {
            if (readSpace.getId(candidate) == sourceId) continue;
            if (readSpace.isVmMigrating(candidate) || readSpace.isVmBeingInstantiated(candidate)) continue;

            HostEntity host = findOwningHost(readSpace, hosts, candidate);
            if (host == null) continue;
            if (readSpace.isHostFailed(host) || readSpace.isHostPermanentlyDead(host)) continue;
            if (!readSpace.hostHasFreePe(host)) continue;

            return candidate;
        }
        return null;
    }

    @Override
    public String inputSemantic() {
        return "cloudlet-hostfailure-risk";
    }

    @Override
    public String outputSemantic() {
        return "movecloudlet";
    }

    @Override
    public int inputGuid() {
        return INPUT_GUID;
    }

    @Override
    public int outputGuid() {
        return OUTPUT_GUID;
    }
}
