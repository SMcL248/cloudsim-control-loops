package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

/**
 * Variant 1 - Idle Host Reclamation.
 *
 * Strategy: the safest possible power-reduction move. Only ever targets a
 * host that the diagnosis marks UNDERLOADED *and* which currently hosts zero
 * VMs. Powering down a genuinely empty host cannot strand or destroy any
 * workload, so this variant trades ambition for zero-risk, guaranteed-clean
 * power savings every time it fires.
 */
public class planner_v1 implements Planner<LoadState[], int[]> {

    private static final int INPUT_GUID = 2200;
    private static final int OUTPUT_GUID = 3010;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        int targetHostId = -1;

        int limit = Math.min(diagnosis.length, hosts.size());
        for (int i = 0; i < limit; i++) {
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
            if (!readSpace.getVmListForHost(host).isEmpty()) {
                continue;
            }
            targetHostId = readSpace.getId(host);
            break;
        }

        int[] action = new int[1];
        if (targetHostId == -1) {
            action[0] = -1;
            Log.printlnConcat(readSpace.getNow(), ": [planner_v1] no empty idle host found, emitting no-op");
        } else {
            action[0] = targetHostId;
            Log.printlnConcat(readSpace.getNow(), ": [planner_v1] powering down empty idle host ", targetHostId);
        }
        return action;
    }

    @Override
    public String inputSemantic() {
        return "host-idle-emptiness-loadstate";
    }

    @Override
    public String outputSemantic() {
        return "requestHostPowerDown";
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
