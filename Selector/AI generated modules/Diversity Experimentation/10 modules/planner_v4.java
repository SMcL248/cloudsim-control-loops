package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

// Strategy: Occupancy-driven consolidation migration.
// For the first UNDERLOADED VM found, this planner does not rank destination
// hosts by spare MIPS capacity. Instead it picks the eligible host that is
// already hosting the MOST other guests, deliberately crowding light VMs onto
// already-busy hosts (ties broken by available MIPS headroom). The intent is to
// actively empty out lightly-used hosts elsewhere, rather than spreading load
// evenly, so a power-management module can later shut those hosts down.
public class planner_v4 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {

        Log.printlnConcat(readSpace.getNow(), ": [planner_v4] ", "scanning for underloaded VMs to consolidate");

        List<GuestEntity> vms = readSpace.getVmList();
        List<HostEntity> hosts = readSpace.getAllHosts();

        int limit = Math.min(diagnosis.length, vms.size());
        for (int i = 0; i < limit; i++) {

            if (diagnosis[i] != LoadState.UNDERLOADED) {
                continue;
            }

            GuestEntity vm = vms.get(i);

            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }

            HostEntity currentHost = findHostOf(readSpace, hosts, vm);

            HostEntity busiestHost = null;
            int busiestCount = -1;
            double busiestHeadroom = -1;

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

                int guestCount = readSpace.getVmListForHost(host).size();
                double headroom = readSpace.getHostAvailableMips(host);

                if (guestCount > busiestCount
                        || (guestCount == busiestCount && headroom > busiestHeadroom)) {
                    busiestCount = guestCount;
                    busiestHeadroom = headroom;
                    busiestHost = host;
                }
            }

            if (busiestHost != null) {
                int vmId = readSpace.getId(vm);
                int hostId = readSpace.getId(busiestHost);
                Log.printlnConcat(readSpace.getNow(), ": [planner_v4] ", "consolidating vmId=" + vmId + " onto occupied hostId=" + hostId);
                return new int[] { vmId, hostId };
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [planner_v4] ", "no underloaded VM had a viable consolidation host");
        return null;
    }

    private HostEntity findHostOf(ReadSpace readSpace, List<HostEntity> hosts, GuestEntity vm) {
        for (HostEntity host : hosts) {
            if (readSpace.getVmListForHost(host).contains(vm)) {
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
        return "vm-migration-worstfit-consolidation";
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
