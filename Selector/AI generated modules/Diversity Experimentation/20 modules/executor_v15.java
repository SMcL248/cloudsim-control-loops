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


// Strategy: Host-Capacity-Aware Allocation. Locates the vm's current host and
// checks hostHasFreePe() before attempting, avoiding requests that are already
// known to be doomed given the host's reported pe headroom.
public class executor_v15 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        String tag = "executor_v15";

        if (actions == null || actions.length != 1) {
            Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] rejected malformed payload, expected 1 int, got ",
                    (actions == null ? "null" : actions.length));
            return false;
        }

        int vmId = actions[0];
        GuestEntity vm = actionSpace.getVmById(vmId);

        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] cannot resolve vm ", vmId);
            return false;
        }

        HostEntity host = findHostForVm(vm, actionSpace);
        if (host == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] cannot locate host for vm ", vmId,
                    ", skipping pe allocation");
            return false;
        }

        if (!actionSpace.hostHasFreePe(host)) {
            Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] refused pe allocation for vm ", vmId,
                    " because host ", actionSpace.getId(host), " reports no free pe headroom");
            return false;
        }

        boolean succeeded = actionSpace.requestPeAllocation(vm);
        if (succeeded) {
            successfulActionCount++;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] attempted pe allocation for vm ", vmId,
                " on host ", actionSpace.getId(host), ", succeeded ", succeeded);
        return true;
    }

    private HostEntity findHostForVm(GuestEntity vm, ActionSpace actionSpace) {
        for (HostEntity host : actionSpace.getAllHosts()) {
            for (GuestEntity candidate : actionSpace.getVmListForHost(host)) {
                if (actionSpace.getId(candidate) == actionSpace.getId(vm)) {
                    return host;
                }
            }
        }
        return null;
    }

    @Override
    public String inputSemantic() {
        return "allocate additional pe to vm, gated on host reporting free pe headroom";
    }

    @Override
    public int inputGuid() {
        return 3008;
    }

    @Override
    public int getSuccessfulActionCount() {
        return successfulActionCount;
    }
}
