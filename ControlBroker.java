package org.cloudbus.cloudsim.examples;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudActionTags;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.lists.VmList;


public class ControlBroker extends DatacenterBroker {

    // This map keeps track of the total work assigned to each VM. 
    // The key is the VM id and the value is the total length of cloudlets assigned to that VM.
    private Map<Integer, Double> vmAssignedWork = new HashMap<>();
    private int observationRate;

    public ControlBroker(String name, int observationRate) throws Exception {
        super(name);
        this.observationRate = observationRate;
    }

    @Override//
    protected void submitCloudlets() {

		List<Cloudlet> successfullySubmitted = new ArrayList<>();
		for (Cloudlet cloudlet : getCloudletList()) {
			GuestEntity vm;
			// if user didn't bind this cloudlet and it has not been executed yet
			if (cloudlet.getGuestId() == -1) {
				vm = selectVmForCloudlet(cloudlet);
			} else { // submit to the specific vm
				vm = VmList.getById(getGuestsCreatedList(), cloudlet.getGuestId());
				if (vm == null) { // vm was not created
					vm = VmList.getById(getGuestList(), cloudlet.getGuestId()); // check if exists in the submitted list

					if(!Log.isDisabled()) {
						if (vm != null) {
							Log.printlnConcat(CloudSim.clock(), ": ", getName(), ": Postponing execution of cloudlet ",
									cloudlet.getCloudletId(), ": bount ", vm.getClassName(), " #", vm.getId(), " not available");
						} else {
							Log.printlnConcat(CloudSim.clock(), ": ", getName(), ": Postponing execution of cloudlet ",
									cloudlet.getCloudletId(), ": bount guest entity of id ", cloudlet.getGuestId(), " doesn't exist");
						}
					}
					continue;
				}
			}

			if (!Log.isDisabled()) {
				Log.printlnConcat(CloudSim.clock(), ": ", getName(), ": Sending ", cloudlet.getClass().getSimpleName(),
						" #", cloudlet.getCloudletId(), " to " + vm.getClassName() + " #", vm.getId());
			}
			
			cloudlet.setGuestId(vm.getId());
			sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudActionTags.CLOUDLET_SUBMIT, cloudlet);
			cloudletsSubmitted++;
			getCloudletSubmittedList().add(cloudlet);
			successfullySubmitted.add(cloudlet);
		}

		// remove submitted cloudlets from waiting list
		getCloudletList().removeAll(successfullySubmitted);
	}

    @Override
	public void processEvent(SimEvent ev) {
		CloudSimTags tag = ev.getTag();
        // Resource characteristics request
        if (tag == CloudActionTags.RESOURCE_CHARACTERISTICS_REQUEST) {
            processResourceCharacteristicsRequest(ev);

            // Resource characteristics answer
        } else if (tag == CloudActionTags.RESOURCE_CHARACTERISTICS) {
            processResourceCharacteristics(ev);

            // VM Creation answer
        } else if (tag == CloudActionTags.VM_CREATE_ACK) {
            processVmCreateAck(ev);

            // A finished cloudlet returned
        } else if (tag == CloudActionTags.CLOUDLET_RETURN) {
            processCloudletReturn(ev);

            // if the simulation finishes
        } else if (tag == CloudActionTags.END_OF_SIMULATION) {
            shutdownEntity();

        } else if (tag == CloudActionTags.VM_BROKER_EVENT) {
            observeAndAct();
        }else {
            processOtherEvent(ev);
        }
	}

    @Override
    // This method is called at the beginning of the simulation. It schedules first observation.
    public void startEntity() {
        super.startEntity();
        schedule(getId(), observationRate, CloudActionTags.VM_BROKER_EVENT);
    }

    @Override
    // This method is called at the end of the simulation. It cancels any pending events for this entity.
    public void shutdownEntity() {
        CloudSim.cancelAll(getId(), CloudSim.SIM_ANY);
        super.shutdownEntity();
    }

    // Implements a simple least occupied policy: selects the VM with least work assigned to it. 
    // Work is measured in terms of total length of cloudlets assigned to the VM.
    private GuestEntity selectVmForCloudlet(Cloudlet cloudlet) {

        Map<HostEntity, List<GuestEntity>> hostToVmMap = new HashMap<>();

        for (GuestEntity vm : getGuestsCreatedList()) {
            hostToVmMap.computeIfAbsent(vm.getHost(), k -> new ArrayList<>()).add(vm);
        }

        HostEntity bestHost = null;
        double bestWork = Double.MAX_VALUE;

        for (Map.Entry<HostEntity, List<GuestEntity>> entry : hostToVmMap.entrySet()) {
            double totalWork = 0;
            for (GuestEntity vm : entry.getValue()) {
                totalWork += vmAssignedWork.getOrDefault(vm.getId(), 0.0);
            }
            if (totalWork < bestWork) {
                bestWork = totalWork;   
                bestHost = entry.getKey(); 
            }
        }

        if (bestHost == null) {
            return getGuestsCreatedList().get(0);
        }      

        GuestEntity bestVm = null;
        double bestVmWork = Double.MAX_VALUE;

        for (GuestEntity vm : hostToVmMap.get(bestHost)) {
            double etc = vmAssignedWork.getOrDefault(vm.getId(), 0.0);
            if (etc < bestVmWork) {
                bestVmWork = etc;
                bestVm = vm;
            }
        }

        vmAssignedWork.merge(bestVm.getId(), cloudlet.getCloudletLength() / (double) bestVm.getMips(), Double::sum);

        return bestVm;
    }

    // New control loop implementing observe -> decide -> act flow.
    private void observeAndAct() {

        if (getCloudletList().isEmpty() && getCloudletSubmittedList().size() == getCloudletReceivedList().size()) {
            return; // all work done, skip observation
        }

        Map<GuestEntity, Double> systemMetrics = observe();

        // decide which migrations (if any) should occur
        List<MigrationPair> migrations = decide(systemMetrics);

        // perform migrations and get success status
        boolean success = act(migrations);

        if (!success) {
            Log.println("The system is balanced. No migration needed.");
        }

        // refresh internal workload state based on executing cloudlets
        refreshWork();

        schedule(getId(), observationRate, CloudActionTags.VM_BROKER_EVENT);

    }

    // This method observes the system and returns metrics (ETC per VM).
    private Map<GuestEntity, Double> observe(){

        double now = CloudSim.clock();

        Log.printlnConcat(now, ": ", getName(), ": Observing...");

        Map<GuestEntity, Double> metrics = new HashMap<>();

        for (GuestEntity vm : getGuestsCreatedList()) {
            long remainingWork = 0;
            for (Cloudlet cloudlet : vm.getCloudletScheduler().getCloudletExecList()) {
                remainingWork += cloudlet.getRemainingCloudletLength();
            }

            double currentETC = remainingWork / vm.getMips();
            int numCloudlets = vm.getCloudletScheduler().getCloudletExecList().size();
            Log.printlnConcat("VM #", vm.getId(), "| MIPS: ", vm.getMips(), "| Outstanding Work: ", remainingWork, " | Current ETC: ", currentETC, " | Number of Cloudlets: ", numCloudlets);
            metrics.put(vm, currentETC);
        }

        return metrics;
    }

    // This method refreshes the work assigned to each VM by checking the cloudlets currently executing on it and summing their remaining lengths.
    private void refreshWork() {

        for (GuestEntity vm : getGuestsCreatedList()) {
            double remainingWork = 0;
            for (Cloudlet cloudlet : vm.getCloudletScheduler().getCloudletExecList()) {
                remainingWork += cloudlet.getRemainingCloudletLength();
                }
            vmAssignedWork.put(vm.getId(), remainingWork / (double) vm.getMips());

        }
    }

    // This method decides which migrations (if any) should be performed based on current metrics.
    private List<MigrationPair> decide(Map<GuestEntity, Double> metrics) {

        List<MigrationPair> migrations = new ArrayList<>();

        double meanETC = metrics.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double standardDeviation = Math.sqrt(metrics.values().stream().mapToDouble(work -> (work - meanETC) * (work - meanETC)).sum()/metrics.size());

        double smallestETC = Double.MAX_VALUE;
        double largestETC = 0;

        GuestEntity leastLoadedVm = null;
        GuestEntity mostLoadedVm = null;

        for (GuestEntity vm : getGuestsCreatedList()) {

            double etc = metrics.getOrDefault(vm, 0.0);

            if (etc < smallestETC) {
                smallestETC = etc;
                leastLoadedVm = vm;
            }

            if (etc > meanETC + standardDeviation) {
                Log.printlnConcat("VM #", vm.getId(), " is overloaded.");
                if (etc > largestETC) {
                    largestETC = etc;
                    mostLoadedVm = vm;
                }
            } else if (etc < meanETC - standardDeviation) {
                Log.printlnConcat("VM #", vm.getId(), " is underloaded.");
            } else {
                Log.printlnConcat("VM #", vm.getId(), " is balanced. ");
            }

        }

        if (largestETC > 2.0 * smallestETC && 
            mostLoadedVm != null && 
            leastLoadedVm != null && 
            mostLoadedVm != leastLoadedVm && 
            migrationWorthwhile(mostLoadedVm, leastLoadedVm, meanETC)) {

            migrations.add(new MigrationPair(mostLoadedVm, leastLoadedVm));

        }

        return migrations;
    }

    // This method performs the migrations decided by `decide` and returns success/failure.
    private boolean act(List<MigrationPair> migrations) {
        boolean any = false;
        for (MigrationPair p : migrations) {
            migrateCloudlet(p.from, p.to);
            any = true;
        }
        return any;
    }

    // Simple holder for a migration from one VM to another
    private static class MigrationPair {
        final GuestEntity from;
        final GuestEntity to;
        MigrationPair(GuestEntity from, GuestEntity to) {
            this.from = from;
            this.to = to;
        }
    }

    // This method migrates a cloudlet from the most loaded VM to the least loaded VM.
    // It selects the cloudlet with the most remaining work on the most loaded VM, pauses it, and resubmits it to the least loaded VM.
    private void migrateCloudlet(GuestEntity fromVm, GuestEntity toVm) {

        Log.printlnConcat("Migrating a cloudlet from VM #", fromVm.getId(), " to VM #", toVm.getId());

        // Select the cloudlet with the most remaining work on the most loaded VM
        double mostWork = 0;
        Cloudlet cloudletToMigrate = null;
        for (Cloudlet cloudlet : fromVm.getCloudletScheduler().getCloudletExecList()) {
            if (cloudlet.getRemainingCloudletLength() > mostWork) {
                mostWork = cloudlet.getRemainingCloudletLength();
                cloudletToMigrate = cloudlet;
            }
        }

        if (cloudletToMigrate == null) {
            Log.println("No cloudlet to migrate from VM #" + fromVm.getId());
            return;
        }

        long remainingLength = cloudletToMigrate.getRemainingCloudletLength(); // get the remaining length of the cloudlet to migrate

        // Pause the cloudlet on the current VM
        if (!fromVm.getCloudletScheduler().cloudletPause(cloudletToMigrate.getCloudletId())) {
            Log.printlnConcat("Failed to pause cloudlet #", cloudletToMigrate.getCloudletId(), " on VM #", fromVm.getId());
            return;
        }

        // Adjust the cloudlet's parameters for migration
        cloudletToMigrate.setGuestId(toVm.getId());
        cloudletToMigrate.setCloudletLength(remainingLength);
        cloudletToMigrate.setCloudletFinishedSoFar(0);

        // Find the datacenter id of the destination VM
        Integer datacenterId = getVmsToDatacentersMap().get(toVm.getId());
        if (datacenterId == null) {
            Log.printlnConcat("Destination datacenter for VM #", toVm.getId(), " not found. Migration aborted.");
            return;
        }

        // Resubmit the cloudlet to the new VM
        send(datacenterId, 0.001, CloudActionTags.CLOUDLET_SUBMIT, cloudletToMigrate);

        // Update the work assigned to each VM
        vmAssignedWork.merge(fromVm.getId(), -remainingLength / (double) fromVm.getMips(), Double::sum);
        vmAssignedWork.merge(toVm.getId(), remainingLength / (double) toVm.getMips(), Double::sum);
    }

    // This method determines if migrating a cloudlet from one VM to another is worthwhile.
    // It compares the estimated remaining execution time of the cloudlet on the current VM with the estimated execution time on the new VM, taking into account the current load (work) on both VMs.
    private boolean migrationWorthwhile(GuestEntity fromVm, GuestEntity toVm, double meanWork) {

        double mostWork = 0;
        for (Cloudlet cloudlet : fromVm.getCloudletScheduler().getCloudletExecList()) {
            if (cloudlet.getRemainingCloudletLength() > mostWork) {
                mostWork = cloudlet.getRemainingCloudletLength();
            }
        }

        if (mostWork == 0) {
            return false;
        }

        int fromCount = fromVm.getCloudletScheduler().getCloudletExecList().size();
        int toCount = toVm.getCloudletScheduler().getCloudletExecList().size();

        double effectiveMipsFrom = fromVm.getMips() / Math.max(fromCount, 1);
        double effectiveMipsTo = toVm.getMips() / Math.max(toCount + 1, 1);

        double improvement = (mostWork / effectiveMipsFrom) - (mostWork / effectiveMipsTo);

        double MINIMUM_IMPROVEMENT = meanWork * 0.05;

        return improvement > MINIMUM_IMPROVEMENT;
    }
        
}

    

