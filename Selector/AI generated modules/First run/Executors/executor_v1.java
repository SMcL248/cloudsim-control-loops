package org.cloudbus.cloudsim.examples;// always include

// Import whats needed
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;

// Action: sendCloudlet -- throughput. Dispatches a cloudlet to a datacenter as-is.
public class executor_v1 implements Executor<int[]> {

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length < 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v1] invalid payload, aborting sendCloudlet");
            return false;
        }

        int cloudletId = actions[0];
        int datacenterId = actions[1];

        Cloudlet cloudlet = actionSpace.getCloudletById(cloudletId);
        if (cloudlet == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v1] cloudlet not found, id=", cloudletId, ", aborting");
            return false;
        }

        actionSpace.sendCloudlet(datacenterId, cloudlet);
        Log.printlnConcat(actionSpace.getNow(), ": [executor_v1] sent cloudlet id=", cloudletId, " to datacenter=", datacenterId);
        return true;
    }

    @Override
    public String inputSemantic() {
        return "sendCloudlet";
    }

    @Override
    public int inputGuid() {
        return 3001;
    }

    @Override
    public int getSuccessfulActionCount() {
        return successfulActionCount;
    }
}
