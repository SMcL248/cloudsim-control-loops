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


// Strategy: Headroom-Aware Scaling. Before scaling up, locates the vm's current
// host by scanning getAllHosts()/getVmListForHost() and refuses the scale-up if
// the host does not report enough ram headroom for the delta. Scale-downs are
// always allowed through unchecked.
public class executor_v11 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        String tag = "executor_v11";

        if (actions == null || actions.length != 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] rejected malformed payload, expected 2 ints, got ",
                    (actions == null ? "null" : actions.length));
            return false;
        }

        int vmId = actions[0];
        int tierIndex = actions[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        int[] ramTiers = actionSpace.getRamTiers();

        if (vm == null || tierIndex < 0 || tierIndex >= ramTiers.length) {
            Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] cannot resolve vm ", vmId,
                    " or tier index ", tierIndex, " is out of bounds");
            return false;
        }

        double targetRam = ramTiers[tierIndex];
        double currentRam = actionSpace.getVmRam(vm);
        double delta = targetRam - currentRam;

        if (delta > 0) {
            HostEntity host = findHostForVm(vm, actionSpace);
            if (host != null && actionSpace.getHostAvailableRam(host) < delta) {
                Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] refused ram scale-up of vm ", vmId,
                        " needing additional ", delta, " ram, host only has ",
                        actionSpace.getHostAvailableRam(host), " available");
                return false;
            }
        }

        boolean succeeded = actionSpace.requestRamScaling(vm, targetRam);
        if (succeeded) {
            successfulActionCount++;
        }

        Log.printlnConcat(actionSpace.getNow(), ": [" + tag + "] attempted headroom-checked ram scaling of vm ", vmId,
                " to tier ", tierIndex, " value ", targetRam, ", succeeded ", succeeded);
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
        return "scale vm ram to given tier, gated on host ram headroom for scale-ups";
    }

    @Override
    public int inputGuid() {
        return 3006;
    }

    @Override
    public int getSuccessfulActionCount() {
        return successfulActionCount;
    }
}
