package org.cloudbus.cloudsim.examples;

import java.util.Arrays;
import java.util.List;

import org.cloudbus.cloudsim.HostDynamicWorkload;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.VmScheduler;
import org.cloudbus.cloudsim.core.PowerHostEntity;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.provisioners.BwProvisioner;
import org.cloudbus.cloudsim.provisioners.RamProvisioner;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.VirtualEntity;
import org.cloudbus.cloudsim.lists.PeList;
import org.cloudbus.cloudsim.provisioners.BwProvisioner;
import org.cloudbus.cloudsim.provisioners.RamProvisioner;


public class FixedPowerHost extends PowerHost {

    public FixedPowerHost(int id, RamProvisioner ramProvisioner, BwProvisioner bwProvisioner,
                           long storage, List<? extends Pe> peList, VmScheduler vmScheduler,
                           PowerModel powerModel) {
        super(id, ramProvisioner, bwProvisioner, storage, peList, vmScheduler, powerModel);
    }

    // Workaround for a CloudSim 7.0 library bug in HostDynamicWorkload.getCompletedVms():
    // its `guest.getNumberOfGuests() == 0` check is unconditionally true for any VM not
    // hosting nested containers (every VM in this project), so it flags ALL VMs for removal
    // after the first processing cycle regardless of real cloudlet completion. This override
    // restores "completed" to mean only "zero current MIPS demand" -- correct for a project
    // that never uses nested containers.
    @Override
    public List<GuestEntity> getCompletedVms() {
        List<GuestEntity> vmsToRemove = new ArrayList<>();
        for (GuestEntity guest : getGuestList()) {
            if (guest.isInMigration()) continue;
            if (guest instanceof VirtualEntity vm && vm.isInWaiting()) continue;
            if (guest.getCurrentRequestedTotalMips() == 0) {
                vmsToRemove.add(guest);
            }
        }
        return vmsToRemove;
    }

    // Skip this host's per-cycle processing entirely while failed.
    // HostDynamicWorkload.updateCloudletsProcessing() unconditionally deallocates and
    // re-grants every resident's own requested MIPS every cycle -- it has no concept of
    // host failure, so it would silently undo any manual halt on the very next tick.
    // Skipping it also means getCurrentRequestedTotalMips() for this host's VMs is never
    // refreshed while failed, so it holds its last real (non-zero) value and
    // getCompletedVms() above won't misread it as "done" and destroy the VM.
    // Double.MAX_VALUE matches Host.updateCloudletsProcessing()'s own "nothing to do" sentinel.
    @Override
    public double updateCloudletsProcessing(double currentTime) {
        if (isFailed()) {
            return Double.MAX_VALUE;
        }
        return super.updateCloudletsProcessing(currentTime);
    }
    
}