package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

// Strategy: Best-fit relief migration.
// For the first OVERLOADED VM found, this planner searches every host that can
// legally accept it and picks the one with the SMALLEST available MIPS headroom
// that still satisfies canMigrateGuestToHost. This is a best-fit bin-packing
// choice: it relieves the VM's pressure while minimising fragmentation, rather
// than simply dumping the VM onto whichever host happens to have the most room.
public class planner_v3 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {

        Log.printlnConcat(readSpace.getNow(), ": [planner_v3] ", "scanning for overloaded VMs needing best-fit relief");

        List<GuestEntity> vms = readSpace.getVmList();
        List<HostEntity> hosts = readSpace.getAllHosts();

        int limit = Math.min(diagnosis.length, vms.size());
        for (int i = 0; i < limit; i++) {

            if (diagnosis[i] != LoadState.OVERLOADED) {
                continue;
            }

            GuestEntity vm = vms.get(i);

            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }

            HostEntity currentHost = findHostOf(readSpace, hosts, vm);

            HostEntity bestFitHost = null;
            double bestHeadroom = Double.MAX_VALUE;

            for (HostEntity host : hosts) {

                if (host == currentHost) {
                    continue;
                }
                if (readSpace.isHostFailed(host) || readSpace.isHostPermanentlyDead(host)
                        || readSpace.isHostPoweredDown(host) || readSpace.isHostPoweringUp(host)) {
                    continue;
                }
                if (!readSpace.canMigrateGuestToHost(host, vm)) {
                    continue;
                }

                double headroom = readSpace.getHostAvailableMips(host);
                if (headroom < bestHeadroom) {
                    bestHeadroom = headroom;
                    bestFitHost = host;
                }
            }

            if (bestFitHost != null) {
                int vmId = readSpace.getId(vm);
                int hostId = readSpace.getId(bestFitHost);
                Log.printlnConcat(readSpace.getNow(), ": [planner_v3] ", "best-fit migration vmId=" + vmId + " targetHostId=" + hostId);
                return new int[] { vmId, hostId };
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [planner_v3] ", "no overloaded VM had a viable best-fit destination host");
        return null;
    }

    private HostEntity findHostOf(ReadSpace readSpace, List<HostEntity> hosts, GuestEntity vm) {
        for (HostEntity host : hosts) {
            List<GuestEntity> guests = readSpace.getVmListForHost(host);
            if (guests.contains(vm)) {
                return host;
            }
        }
        return null;
    }

    @Override
    public String inputSemantic() {
        return "vm-cpuutil-loadstate";
    }

    @Override
    public String outputSemantic() {
        return "vm-migration-bestfit-relief";
    }

    @Override
    public int inputGuid() {
        return 2300;
    }

    @Override
    public int outputGuid() {
        return 3002;
    }
}
