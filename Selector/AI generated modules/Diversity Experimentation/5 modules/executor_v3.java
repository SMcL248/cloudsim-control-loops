package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Executor variant implementing requestVmCreation (GUID suffix 03).
// Payload: {tierIndex, sizeTierIndex, datacenterId}
// Note: requestVmCreation returns GuestEntity, not boolean, so
// getSuccessfulActionCount() is left at its default (this action type
// has no finer success signal to report beyond execute()'s own boolean).
public class executor_v3 implements Executor<int[]> {

    private static final int EXPECTED_LENGTH = 3;

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (actions == null || actions.length != EXPECTED_LENGTH) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v3] rejected payload, expected 3 ints {tierIndex, sizeTierIndex, datacenterId} but got length ",
                    actions == null ? "null" : actions.length);
            return false;
        }

        int tierIndex = actions[0];
        int sizeTierIndex = actions[1];
        int datacenterId = actions[2];

        int mipsTierCount = actionSpace.getMipsTiers().length;
        if (tierIndex < 0 || tierIndex >= mipsTierCount) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v3] skipped creation, tierIndex ", tierIndex,
                    " outside known tier range 0..", mipsTierCount - 1);
            return false;
        }

        GuestEntity created = actionSpace.requestVmCreation(tierIndex, sizeTierIndex, datacenterId);
        if (created == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v3] vm creation attempted but rejected by ActionSpace. tier=",
                    tierIndex, " sizeTier=", sizeTierIndex, " datacenter=", datacenterId);
        } else {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v3] created vm ", actionSpace.getId(created),
                    " tier=", tierIndex, " sizeTier=", sizeTierIndex, " datacenter=", datacenterId);
        }
        return true;
    }

    @Override
    public String inputSemantic() {
        return "requestVmCreation action payload {tierIndex, sizeTierIndex, datacenterId}";
    }

    @Override
    public int inputGuid() {
        return 3003;
    }
}
