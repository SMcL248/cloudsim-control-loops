package org.cloudbus.cloudsim.examples;// always include

// Import whats needed
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;

// Action: moveCloudlet -- throughput/load-balancing. Rebalances a cloudlet between two VMs,
// skipping degenerate moves where source and destination are the same VM.
public class executor_v2 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length < 3) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] invalid payload, aborting moveCloudlet");
            return false;
        }

        int cloudletId = actions[0];
        int fromVmId = actions[1];
        int toVmId = actions[2];

        if (fromVmId == toVmId) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] source and destination vm are identical, id=", fromVmId, ", skipping no-op move");
            return false;
        }

        Cloudlet cloudlet = actionSpace.getCloudletById(cloudletId);
        GuestEntity fromVm = actionSpace.getVmById(fromVmId);
        GuestEntity toVm = actionSpace.getVmById(toVmId);

        if (cloudlet == null || fromVm == null || toVm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] could not resolve cloudlet or vm ids, aborting");
            return false;
        }

        actionSpace.moveCloudlet(cloudletId, fromVmId, toVmId);
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v2] moved cloudlet id=", cloudletId, " from vm=", fromVmId, " to vm=", toVmId);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "moveCloudlet";
    }

    @Override
    public int inputGuid() {
        return 3003;
    }

    @Override
    public int getSuccessfulActionCount() {
        return successfulActionCount;
    }
}
