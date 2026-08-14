package org.cloudbus.cloudsim.examples;
 
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVm;
import org.cloudbus.cloudsim.VmAllocationPolicy.GuestMapping;
import org.cloudbus.cloudsim.VmScheduler;
import org.cloudbus.cloudsim.core.CloudActionTags;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.distributions.ExponentialDistr;
import org.cloudbus.cloudsim.distributions.LognormalDistr;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.core.PowerGuestEntity;
import org.cloudbus.cloudsim.core.PowerHostEntity;
 
 
public class SelectorNoLogs extends DatacenterBroker implements ActionSpace {

    private final List<ControlUnit> controllers;// Set of available controllers
    private ControlUnit selected;// Controller in use
    private PowerDatacenter cachedDatacenter;// The datacenter object
    private final int observationRate;// Time between observations
    private final int[] mipsTiers;// MIPS tiers (small, medium, large)

    private final boolean SWITCHABLE = false;// Can we switch controllers?
    private boolean vmAllocationLogged = false;// Have we logged the host to VM allocations?
    private boolean initialAbandonSweepDone = false;// Have we handled Cloudlets that could not be assigned?
    private boolean completedNaturally = false;// Did we crash or timeout?

    // Utilisation (RAM, MIPS, BW) averages and variances
    private double groundTruthVarianceSum = 0.0;
    private double ramUtilVarianceSum = 0.0;
    private double bwUtilVarianceSum = 0.0;
    private double mipsUtilVarianceSum = 0.0;
    private int hostUtilVarianceCycleCount = 0;

    private double vmMipsUtilVarianceSum = 0.0;
    private int vmUtilVarianceCycleCount = 0;
    private double peakBwUtilization;
    private double peakRamUtilization;
    private double peakMipsUtilization;
    private double peakStorageUtilization;
    private double bwUtilizationSum;
    private double ramUtilizationSum;
    private double mipsUtilizationSum;
    private double storageUtilizationSum;
    private double storageUtilVarianceSum = 0.0;
    private double peakVmMipsUtilization;
    private double vmMipsUtilizationSum;
    private int utilizationCycleCount;
    private int groundTruthCycleCount = 0;

    // Headroom sums
    private double hostFreeMipsSum;
    private double hostFreeStorageSum;
    private double hostFreeRamSum;
    private double hostFreeBwSum;
    private double vmFreeMipsSum;

    private double energyExpelledRunningTotal = 0;
    private double workCompleteRunningTotal = 0;
    private double cloudletsAbandonedRunningTotal = 0;
    private double abandonedWorkProcessed = 0;

    // Energy expelled, work completed and cloudlets abandoned between each observation
    private List<Double> energyExpelledReadings = new ArrayList<>();
    private List<Double> workCompleteReadings = new ArrayList<>();
    private List<Double> cloudletsAbandonedReadings = new ArrayList<>();
    private List<Double> demandVarianceReadings = new ArrayList<>();
    private List<Double> ramUtilReadings = new ArrayList<>();
    private List<Double> bwUtilReadings = new ArrayList<>();
    private List<Double> mipsUtilReadings = new ArrayList<>();
    private List<Double> storageUtilReadings = new ArrayList<>();
    private List<Double> vmMipsUtilReadings = new ArrayList<>();
    private List<Double> ramUtilVarianceReadings = new ArrayList<>();
    private List<Double> bwUtilVarianceReadings = new ArrayList<>();
    private List<Double> mipsUtilVarianceReadings = new ArrayList<>();
    private List<Double> storageUtilVarianceReadings = new ArrayList<>();
    private List<Double> vmMipsUtilVarianceReadings = new ArrayList<>();
    private List<Double> hostFreeMipsReadings = new ArrayList<>();
    private List<Double> hostFreeRamReadings = new ArrayList<>();
    private List<Double> hostFreeBwReadings = new ArrayList<>();
    private List<Double> vmFreeMipsReadings = new ArrayList<>();

    public static boolean suppressDebugLogging = false;

    private int pendingInjections = 0;// Workloads yet to be allocated
    private int pendingFailures = 0;// Host failures yet to occur
    private int pendingRepairs = 0;// Failed hosts yet to be repaired
    private final List<PendingInjection> injectionSchedule = new ArrayList<>();  // (delay, batch) pairs
    private final List<PendingFailure> failureSchedule = new ArrayList<>();  // (delay, id) pairs
    private final LognormalDistr repairDurationDist;
    private final Random unrecoverableRng;
    private final Map<Integer, List<Cloudlet>> deferredCloudletsByHost = new HashMap<>();

    // Failure tracking
    private int numRealFailures = 0;
    private double totalDowntime = 0.0;
    private int currentFailedHostCount = 0;
    private int peakSimultaneousFailedHosts = 0;
    private int numCloudletsDeferred = 0;
    private int numCloudletsAbandoned = 0;
    private int numCloudletsAbandonedHostPoweredDown = 0;
    private int numCloudletsAbandonedVmNeverCreated = 0;   // sweepAbandonedCloudlets + admitCloudlets "vm==null"
    private int numCloudletsAbandonedHostDead = 0;          // admitCloudlets, target host permanently dead
    private int numCloudletsAbandonedEvacuationFailed = 0;  // evacuateHost, no healthy destination found
    private int numCloudletsAbandonedVmDestroyed = 0;       // strandAndDestroyGuest, deliberate requestVmDestruction
    private int numSubmittedCloudletsAbandoned = 0;
    private int numActionsSucceededOnFailedHost = 0;
    private double totalDeferredWaitTime = 0.0;
    private int minLiveVmCount = Integer.MAX_VALUE;
    private final Map<Integer, Double> failureStartTimeByHost = new HashMap<>();
    private final Map<Integer, Double> deferralStartTimeByCloudlet = new HashMap<>();
    private final Set<Integer> permanentlyDeadHostIds = new HashSet<>();
    private final Set<Integer> exposedCloudletIds = new HashSet<>();
    private final Set<Integer> poweredDownHostIds = new HashSet<>();
    private final Set<Integer> poweringUpHostIds = new HashSet<>();
    private final Map<Integer, PowerModel> savedPowerModels = new HashMap<>();

    private static final double UNRECOVERABLE_PROBABILITY = 0.20;
    private static final double SPIKE_MULTIPLIER = 1.2;
    private static final int[] RAM_TIERS = {1024, 2048, 4096};
    private static final int[] BW_TIERS = {625, 1250, 2500};
    private static final int[] CORE_TIERS = {1, 2, 4};
    private static final long[] SIZE_TIERS = {5_000L, 10_000L, 20_000L};
    private static final PowerModel OFF_MODEL = new PowerModelOff();

    private static final String VM_VMM = "Xen";
    private static final int VM_PRIORITY = 1;
    private static final double VM_SCHEDULING_INTERVAL = 1;

    private int nextVmId = -1;

    public <M,D,A> SelectorNoLogs(String name, int observationRate, Monitor<M> monitor, Analyser<M,D> analyser,
                             Planner<D,A> planner, Executor<A> executor, int[] mipsTiers, LognormalDistr repairDurationDist, Random unrecoverableRng) throws Exception {
        this(name, observationRate, monitor, analyser, planner, executor, mipsTiers, repairDurationDist, unrecoverableRng, null, null, null);
    }

    public <M,D,A> SelectorNoLogs(String name, int observationRate, Monitor<M> monitor, Analyser<M,D> analyser,
                             Planner<D,A> planner, Executor<A> executor, int[] mipsTiers, LognormalDistr repairDurationDist, Random unrecoverableRng,
                             Predicate<D> imbalancePredicate, Predicate<D> opportunityPredicate) throws Exception {
        this(name, observationRate, monitor, analyser, planner, executor, mipsTiers, repairDurationDist, unrecoverableRng, imbalancePredicate, opportunityPredicate, null);
    }

    public <M,D,A> SelectorNoLogs(String name, int observationRate, Monitor<M> monitor, Analyser<M,D> analyser,
                             Planner<D,A> planner, Executor<A> executor, int[] mipsTiers, LognormalDistr repairDurationDist, Random unrecoverableRng,
                             Predicate<D> imbalancePredicate, Predicate<D> opportunityPredicate,
                             Predicate<A> actionProposedPredicate) throws Exception {
        this(name, observationRate,
             List.of(new Controller<>(name + "-ctrl0", monitor, analyser, planner, executor,
                                       imbalancePredicate, opportunityPredicate, actionProposedPredicate)),
             mipsTiers, repairDurationDist, unrecoverableRng);
    }

    public SelectorNoLogs(String name, int observationRate, List<ControlUnit> controllers, int[] mipsTiers, LognormalDistr repairDurationDist, Random unrecoverableRng) throws Exception {
        super(name);
        this.observationRate = observationRate;
        this.controllers = controllers;
        this.selected = controllers.isEmpty() ? null : controllers.get(0);
        this.mipsTiers = mipsTiers;
        this.repairDurationDist = repairDurationDist;
        this.unrecoverableRng = unrecoverableRng;
    }

    ////////////////////// Overridden methods from DatacenterBroker ////////////////////////////

    @Override
    // Recieves event tag and directs to corresponding method
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
 
        }else if (tag == CloudActionTags.VM_BROKER_EVENT) {
            mapeCycle();

        } else if (tag == CloudActionTags.CLOUDLET_INJECT) {
             processCloudletInjection(ev); 

        } else if (tag == CloudActionTags.HOST_FAIL) {
            processHostFailure(ev);

        } else if (tag == CloudActionTags.HOST_REPAIR) {
            processHostRepair(ev);

        } else if (tag == CloudActionTags.END_OF_SIMULATION) {
            if (pendingInjections == 0 && pendingFailures == 0 && pendingRepairs == 0
                && getCloudletList().isEmpty()
                && getCloudletSubmittedList().size() - numSubmittedCloudletsAbandoned == getCloudletReceivedList().size()) {
                shutdownEntity();
            }

        } else if (tag == CloudActionTags.HOST_POWER_DOWN) {
            processHostPowerDown(ev);

        } else if (tag == CloudActionTags.HOST_POWER_UP) {
            processHostPowerUp(ev);

        } else {
            processOtherEvent(ev);
        }
	}

    @Override
    public void startEntity() {
        super.startEntity();
        schedule(getId(), observationRate, CloudActionTags.VM_BROKER_EVENT);
        for (PendingInjection p : injectionSchedule) {
            schedule(getId(), p.getTime(), CloudActionTags.CLOUDLET_INJECT, p.getBatch());
        }
        for (PendingFailure f : failureSchedule) {
            schedule(getId(), f.getTime(), CloudActionTags.HOST_FAIL, f.getHostId());
        }
    }

    @Override
    public void shutdownEntity() {
        CloudSim.cancelAll(getId(), CloudSim.SIM_ANY);
        super.shutdownEntity();
    }
 
    @Override
    protected void processCloudletReturn(SimEvent ev) {

        Cloudlet cloudlet = (Cloudlet) ev.getData();
        getCloudletReceivedList().add(cloudlet);
        cloudletsSubmitted--;

        if (getCloudletList().isEmpty() 
            && cloudletsSubmitted == 0 
            && pendingInjections == 0
            && pendingFailures == 0 
            && pendingRepairs == 0) {
            completedNaturally = true;

            minLiveVmCount = Math.min(minLiveVmCount, getGuestsCreatedList().size());

            clearDatacenters();
            finishExecution();
        }
    }

    /////////////////////////// Contract Methods ////////////////////////////////////
 
// -----------------------ActionSpace------------------------------

    @Override
    // Send the given cloudlet to the given datacenter
    public void sendCloudlet(int datacenterId, Cloudlet cloudlet) {
        if (!getDatacenterIdsList().contains(datacenterId)) {
            Log.printlnConcat(getNow(), ": [Selector] Invalid datacenterId=", datacenterId,
                " in sendCloudlet -- not a registered datacenter. Aborting.");
            return;
        }
        sendNow(datacenterId, CloudActionTags.CLOUDLET_SUBMIT, cloudlet);
    }
 
    @Override
    // Migrate a cloudlet from a given VM to a given VM
    public void moveCloudlet(int cloudletId, int fromVmId, int toVmId) {
        Integer destDatacenterId = getDatacenterFor(toVmId);
        Integer fromDatacenterId = getDatacenterFor(fromVmId);
        if (destDatacenterId == null || fromDatacenterId == null){return;}
        int[] data = new int[5];
        data[0] = cloudletId;
        data[1] = getUserId();
        data[2] = fromVmId;
        data[3] = toVmId;
        data[4] = destDatacenterId;
        sendNow(fromDatacenterId, CloudActionTags.CLOUDLET_MOVE, data);
    }
 
    @Override
    // Cancel a previously submitted cloudlet
    public void requestCloudletCancellation(int datacenterId, Cloudlet cl) {
        if (datacenterId < 0 || datacenterId >= CloudSim.getNumEntities()) {
            Log.printlnConcat(getNow(), ": [Selector] Invalid datacenterId=", datacenterId, " in sendCloudlet. Aborting.");
            return;
        }
        sendNow(datacenterId, CloudActionTags.CLOUDLET_CANCEL, cl);
    }

    @Override
    // Pause processing on the given cloudlet
    public void requestCloudletPause(int datacenterId, Cloudlet cl){
        if (datacenterId < 0 || datacenterId >= CloudSim.getNumEntities()) {
            Log.printlnConcat(getNow(), ": [Selector] Invalid datacenterId=", datacenterId, " in sendCloudlet. Aborting.");
            return;
        }
        sendNow(datacenterId, CloudActionTags.CLOUDLET_PAUSE, cl);
    }

    @Override
    // Resume processing of the given cloudlet
    public void requestCloudletResume(int datacenterId, Cloudlet cl) {
        if (datacenterId < 0 || datacenterId >= CloudSim.getNumEntities()) {
            Log.printlnConcat(getNow(), ": [Selector] Invalid datacenterId=", datacenterId, " in sendCloudlet. Aborting.");
            return;
        }
        sendNow(datacenterId, CloudActionTags.CLOUDLET_RESUME, cl);
    }

    @Override
    // Request the migration of a VM to the given host
    public void requestVmMigration(GuestEntity vm, HostEntity targetHost){
        Integer datacenterId = getDatacenterFor(vm.getId());
        if (datacenterId == null) {
            Log.printlnConcat(getNow(), ": [Selector] No datacenter found for VM#", vm.getId(), " in requestVmMigration. Aborting.");
            return;
        }
        GuestMapping payload = new GuestMapping(vm, targetHost);
        send(datacenterId, 0, CloudActionTags.VM_MIGRATE, payload);
    }

    @Override
    // Alter MIPS rating of the given VM.
    public boolean requestMipsScaling(GuestEntity vm, double newMips) {

        if (!(vm instanceof Vm)) {
            return false;
        }

        boolean targetHostFailed = isHostFailed(vm.getHost()) || isHostPermanentlyDead(vm.getHost());


        List<Double> newMipsShare = new ArrayList<>();
        for (int i = 0; i < vm.getNumberOfPes(); i++) {
            newMipsShare.add(newMips);
        }

        Host host = vm.getHost();
        VmScheduler scheduler = host.getGuestScheduler();

        // allocatePesForGuest doesn't credit back an existing allocation before
        // subtracting the new one — calling it directly on an already-allocated VM
        // leaks MIPS/PE-count on the host with every scale. Release this VM's
        // current claim first, then re-request at the new value.
        scheduler.deallocatePesForGuest(vm);
        boolean success = scheduler.allocatePesForGuest(vm, newMipsShare);

        if (!success) {
            // Not enough room even after freeing this VM's own prior share —
            // restore the original allocation rather than leaving it deallocated.
            List<Double> originalShare = new ArrayList<>();
            for (int i = 0; i < vm.getNumberOfPes(); i++) {
                originalShare.add(vm.getMips());
            }
            scheduler.allocatePesForGuest(vm, originalShare);

            Log.printlnConcat(getNow(), ": [Selector] FAILED.");
            Log.disable();

            return false;
        }

        ((Vm) vm).setMips(newMips);
        vm.getCloudletScheduler().updateCloudletsProcessing(getNow(), newMipsShare);

        if (targetHostFailed) {
            numActionsSucceededOnFailedHost++;
            Log.printlnConcat(getNow(), ": [Selector] requestMipsScaling succeeded against a failed/dead host — VM#", vm.getId());
            Log.disable();
        }

        return true;

    }

    @Override
    // Allocate an additional core to a VM
    public boolean requestPeAllocation(GuestEntity vm){

        if (!(vm instanceof Vm)) {
        return false;
        }

        boolean targetHostFailed = isHostFailed(vm.getHost()) || isHostPermanentlyDead(vm.getHost());

        HostEntity host = vm.getHost();
        double peMips = vm.getMips();

        List<Double> currentShare = host.getGuestScheduler().getAllocatedMipsForGuest(vm);
        if (currentShare == null) {
            Log.printlnConcat(getNow(), ": [requestPeAllocation] VM#", vm.getId(), " no current allocation found, aborting");
            Log.disable();
            return false;
        }

        List<Double> newShare = new ArrayList<>(currentShare);
        newShare.add(peMips);

        host.getGuestScheduler().deallocatePesForGuest(vm);
        boolean success = host.getGuestScheduler().allocatePesForGuest(vm, newShare);

        Log.printlnConcat(getNow(), ": [requestPeScaling] VM#", vm.getId(),
            " success=", success, " requestedShareSize=", newShare.size(),
            " availableMipsAfter=", host.getGuestScheduler().getAvailableMips());
        Log.disable();

        if (success) {
            ((Vm) vm).setNumberOfPes(vm.getNumberOfPes() + 1);
            vm.getCloudletScheduler().updateCloudletsProcessing(getNow(), newShare);
            Log.printlnConcat(getNow(), ": [requestPeScaling] VM#", vm.getId(), " numberOfPes now=", vm.getNumberOfPes());
            Log.disable();
            if (targetHostFailed) {
                numActionsSucceededOnFailedHost++;
                Log.printlnConcat(getNow(), ": [Selector] requestPeAllocation succeeded against a failed/dead host — VM#", vm.getId());
                Log.disable();
            }
        } else {
            // Insufficient host capacity for the extra PE -- restore the VM's original
            // share rather than leaving it deallocated.
            host.getGuestScheduler().allocatePesForGuest(vm, currentShare);
            Log.printlnConcat(getNow(), ": [requestPeScaling] VM#", vm.getId(),
                " FAILED to add PE -- insufficient host capacity, original allocation restored.");
            Log.disable();
            return false;
        }

        return success;

    }

    @Override
    // Deallocate a core from a VM
    public boolean requestPeDeallocation(GuestEntity vm){

        if (!(vm instanceof Vm)) {
        return false;
        }

        boolean targetHostFailed = isHostFailed(vm.getHost()) || isHostPermanentlyDead(vm.getHost());

        HostEntity host = vm.getHost();
        double peMips = vm.getMips();

        List<Double> currentShare = host.getGuestScheduler().getAllocatedMipsForGuest(vm);
        if (currentShare == null) {
            Log.printlnConcat(getNow(), ": [requestPeAllocation] VM#", vm.getId(),
                " FIRST TIME no current allocation found -- lost host-level PE allocation somewhere before this point, aborting");
            Log.disable();
            return false;
        }

        List<Double> newShare = new ArrayList<>(currentShare);
 
        boolean removed = newShare.remove(peMips);
 
        if (!removed) {
            Log.printlnConcat(getNow(), ": [requestPeDeallocation] VM#", vm.getId(),
                " no matching peMips entry found, aborting");
            Log.disable();
            return false;
        }
 
        host.getGuestScheduler().deallocatePesForGuest(vm);
 
        boolean success = host.getGuestScheduler().allocatePesForGuest(vm, newShare);
        Log.printlnConcat(getNow(), ": [requestPeDeallocation] VM#", vm.getId(),
            " success=", success, " requestedShareSize=", newShare.size(),
            " availableMipsAfter=", host.getGuestScheduler().getAvailableMips());
        Log.disable();
 
        if (success) {
            ((Vm) vm).setNumberOfPes(vm.getNumberOfPes() - 1);
            vm.getCloudletScheduler().updateCloudletsProcessing(getNow(), newShare);
            Log.printlnConcat(getNow(), ": [requestPeDeallocation] VM#", vm.getId(), " numberOfPes now=", vm.getNumberOfPes());
            Log.disable();
            if (targetHostFailed) {
                numActionsSucceededOnFailedHost++;
                // Log.enable();
                Log.printlnConcat(getNow(), ": [Selector] requestPeDeallocation succeeded against a failed/dead host — VM#", vm.getId());
                Log.disable();
            }
        } else {
            host.getGuestScheduler().allocatePesForGuest(vm, currentShare);
            Log.printlnConcat(getNow(), ": [requestPeDeallocation] VM#", vm.getId(),
                " FAILED -- insufficient host capacity, original allocation restored.");
            Log.disable();
            return false;
        }
 
        return success;

    }

    @Override
    // Adjust RAM of VM
    public boolean requestRamScaling(GuestEntity vm, double newRam) {

        if (!(vm instanceof Vm)) return false;

        boolean targetHostFailed = isHostFailed(vm.getHost()) || isHostPermanentlyDead(vm.getHost());

        HostEntity host = vm.getHost();
        int originalRam = vm.getRam();
        int requestedRam = (int) newRam;

        // Must set the VM's own ceiling BEFORE calling the provisioner -- it clamps
        // incoming requests to guest.getRam(), so growth would silently no-op otherwise.
        ((Vm) vm).setRam(requestedRam);
        boolean success = host.getGuestRamProvisioner().allocateRamForGuest(vm, requestedRam);

        if (!success) {
            // Provisioner already zeroed the allocation internally (deallocates as its
            // first step, no rollback of its own) -- restore both the VM field and the
            // actual allocation ourselves.
            ((Vm) vm).setRam(originalRam);
            host.getGuestRamProvisioner().allocateRamForGuest(vm, originalRam);
            Log.printlnConcat(getNow(), ": [requestRamScaling] VM#", vm.getId(), " FAILED.");
            Log.disable();
            return false;
        }

        if (targetHostFailed) {
            numActionsSucceededOnFailedHost++;
            Log.printlnConcat(getNow(), ": [Selector] requestRamScaling succeeded against a failed/dead host — VM#", vm.getId());
            Log.disable();
        }

        return true;
    }

    @Override
    // Adjust BW of VM
    public boolean requestBwScaling(GuestEntity vm, double newBw) {

        if (!(vm instanceof Vm)) return false;
        
        boolean targetHostFailed = isHostFailed(vm.getHost()) || isHostPermanentlyDead(vm.getHost());

        HostEntity host = vm.getHost();
        long requestedBw = (long) newBw;

        // No clamp-against-self trap here, and failure leaves the existing allocation
        // untouched (true no-op) -- no manual rollback needed, unlike RAM.
        boolean success = host.getGuestBwProvisioner().allocateBwForGuest(vm, requestedBw);

        if (success) {
            ((Vm) vm).setBw(requestedBw);
            if (targetHostFailed) {
                numActionsSucceededOnFailedHost++;
                Log.printlnConcat(getNow(), ": [Selector] requestBwScaling succeeded against a failed/dead host — VM#", vm.getId());
                Log.disable();
            }
            return true;
        } else {
            Log.printlnConcat(getNow(), ": [requestBwScaling] VM#", vm.getId(), " FAILED.");
            Log.disable();
            return false;
        }
    }

    @Override
    // Create VM and schedule its allocation
    public GuestEntity requestVmCreation(int tierIndex, int sizeTierIndex, int datacenterId) {
        
        if (tierIndex < 0 || tierIndex >= mipsTiers.length
            || sizeTierIndex < 0 || sizeTierIndex >= SIZE_TIERS.length) {
            Log.printlnConcat(getNow(), ": [Selector] Invalid tier index in requestVmCreation (tier=", tierIndex, ", sizeTier=", sizeTierIndex, "). Aborting.");
            return null;
        }
        if (datacenterId < 0 || datacenterId >= CloudSim.getNumEntities()) {
            Log.printlnConcat(getNow(), ": [Selector] Invalid datacenterId=", datacenterId, " in requestVmCreation. Aborting.");
            return null;
        }
        int id = allocateNextVmId();

        PowerVm newVm = new PowerVm(
            id,
            getUserId(),
            mipsTiers[tierIndex],
            CORE_TIERS[tierIndex],
            RAM_TIERS[tierIndex],
            BW_TIERS[tierIndex],
            SIZE_TIERS[sizeTierIndex],
            VM_PRIORITY,
            VM_VMM,
            new CloudletSchedulerTimeShared(),
            VM_SCHEDULING_INTERVAL
        );

        getGuestList().add(newVm);
        sendNow(datacenterId, CloudActionTags.VM_CREATE, newVm);

        return newVm;
    }

    @Override
    // Request the destruciton of a VM (and its workload)
    public void requestVmDestruction(GuestEntity vm) {

        Integer datacenterId = getDatacenterFor(vm.getId());
        if (datacenterId == null) { return; }

        strandAndDestroyGuest(vm, false);

    }
    
    @Override
    // Request to turn off the given host (losing any assigned VMs and workloads)
    public void requestHostPowerDown(HostEntity host){

        //Log.enable();

        if(isHostPoweredDown(host)){
            Log.printlnConcat(getNow(), ": [Selector] Cannot power down Host #", host.getId(), " | Host already powered down.");
            Log.disable();
            return;
        } else if (isHostPoweringUp(host)){
            Log.printlnConcat(getNow(), ": [Selector] Cannot power down Host #", host.getId(), " | Host is currently being powered up.");
            Log.disable();
            return;
        }

        Log.printlnConcat(getNow(), ": [Selector] Powering Host #", host.getId(), " down.");
        Log.disable();

        send(getId(), 0, CloudActionTags.HOST_POWER_DOWN, getId(host));

    }

    @Override
    // Request to power on the given host
    public void requestHostPowerUp(HostEntity host){

        if(!isHostPoweredDown(host)){
            //Log.enable();
            Log.printlnConcat(getNow(), ": [Selector] Cannot power up Host #", host.getId(), " | Host already on.");
            Log.disable();
            return;
        }   
        if (isHostPoweringUp(host)) {
            //Log.enable();
            Log.printlnConcat(getNow(), ": [Selector] Cannot power up Host #", host.getId(), " | Already booting.");
            Log.disable();
            return;
        }

        poweringUpHostIds.add(getId(host));
        PowerModel original = savedPowerModels.get(getId(host));
        ((PowerHostEntity) host).setPowerModel(new PowerModelSpike(original, SPIKE_MULTIPLIER));

        //Log.enable();
        Log.printlnConcat(getNow(), ": [Selector] Powering Host #", host.getId(), " up.");
        Log.disable();
        send(getId(), 100, CloudActionTags.HOST_POWER_UP, getId(host));

    }

// -----------------------ReadSpace-------------------------------
    
    @Override
    // Get the list of VMs allocated the given host.
    public List<GuestEntity> getVmListForHost(HostEntity host){
        return host.getGuestList();
    }

    @Override
    // Returns the datacenter ID of the given VM
    public Integer getDatacenterFor(int vmId) {
        return getVmsToDatacentersMap().get(vmId);
    }

    @Override
    // Retrieve the complete list of created VMs
    public List<GuestEntity> getVmList() {
        return getGuestsCreatedList();
    }
 
    @Override
    // Get the ID of the Selector (CloudSim Broker)
    public int getUserId() {
        return getId();
    }
 
    @Override
    // Retrieve the complete list of hosts
    public List<HostEntity> getAllHosts() {
        return getDatacenterCharacteristicsList().values().iterator().next().getHostList();
    }
 
    @Override
    // Retreive the current CloudSim time
    public double getNow(){
        return CloudSim.clock();
    }
 
    // Retrieve possible VM MIPS tiers.
    @Override
    public int[] getMipsTiers(){
        return Arrays.copyOf(mipsTiers, mipsTiers.length);
    }

    //Retrieve the MIPS rating per core on the host of the given VM.
    @Override
    public double getHostCapacity(GuestEntity vm) {
        return vm.getHost().getGuestScheduler().getPeCapacity();
    }

    @Override 
    public double getHostMipsPerPe(HostEntity host){
        return host.getTotalMips()/host.getNumberOfPes();
    }

    @Override
    public int getHostPeCount(HostEntity host){
        return host.getNumberOfPes();
    }

    // Does the given host have an unused PE?
    @Override 
    public boolean hostHasFreePe(HostEntity host){
        return host.getGuestScheduler().getAvailableMips() >= host.getGuestScheduler().getPeCapacity();
    }

    // Has the given host failed? i.e. is processing paused?
    @Override
    public boolean isHostFailed(HostEntity host) {
        return host.isFailed();
    }

    // Has the given host been deemed unrepairable?
    @Override
    public boolean isHostPermanentlyDead(HostEntity host) {
        return permanentlyDeadHostIds.contains(host.getId());
    }

    @Override
    public int getNumberCloudletsAbandoned(){
        return numCloudletsAbandoned;
    }

    // Get a VM object by its ID
    @Override
    public GuestEntity getVmById(int vmId) {
        for (GuestEntity vm : getVmList()) {
            if (vm.getId() == vmId) {
                return vm;
            }
        }
        return null;
    }

    // Get a Host object by its ID
    @Override
    public HostEntity getHostById(int hostId){
        List<HostEntity> hosts = getAllHosts();
        for (HostEntity host : hosts){
            if (host.getId() == hostId){
                return host;
            }
        }
        return null;
    }

    // Retrieve the next level of MIPS
    @Override
    public double getNextMipsTier(GuestEntity vm) {
        double currentMips = vm.getMips();
        for (int i = 0; i < mipsTiers.length - 1; i++) {
            if (mipsTiers[i] == currentMips) {
                return (double) mipsTiers[i + 1];
            }
        }
        return -1.0; // already at top tier, or MIPS doesn't match a known tier
    }

    // Retrieve RAM headroom of the given host
    @Override
    public double getHostAvailableRam(HostEntity host) {
        return host.getGuestRamProvisioner().getAvailableRam();
    }

    // Retrieve BW headroom of the given host
    @Override
    public double getHostAvailableBw(HostEntity host) {
        return host.getGuestBwProvisioner().getAvailableBw();
    }

    // Retrieve total RAM of the given host
    @Override
    public double getHostTotalRam(HostEntity host) {
        return host.getRam();
    }

    // Retrieve total RAM of the given host
    @Override
    public double getHostTotalBw(HostEntity host) {
        return host.getBw();
    }

    // Retrieve total MIPS of the given host.
    @Override
    public double getHostTotalMips(HostEntity host) {
        return host.getTotalMips();
    }
   
    // Get the ID of the input host
    @Override
    public int getId(HostEntity host){
        return host.getId();
    }

    // Get the id of the input VM
    @Override
    public int getId(GuestEntity vm){
        return vm.getId();
    }

    // Get the ID of the input cloudlet
    @Override
    public int getId(Cloudlet cl){
        return cl.getCloudletId();
    }

    /**
     * Wraps host.isSuitableForGuest(vm) — CloudSim's own placement-admission
     * check (PE capacity, available MIPS, RAM, BW, storage). Deliberately NOT
     * failure-aware: confirmed via source that this check has no isFailed()
     * clause, so a failed or permanently-dead host can still read as
     * "suitable." Combine with isHostFailed(host)/isHostPermanentlyDead(host)
     * yourself if failure-awareness matters for your use case — this wraps
     * CloudSim's real behavior faithfully, blind spot included, rather than
     * silently patching it.
     */
    @Override
    public boolean isHostSuitableForGuest(HostEntity host, GuestEntity vm) {
        return host.isSuitableForGuest(vm);
    }

    @Override
    public boolean canMigrateGuestToHost(HostEntity host, GuestEntity vm) {
        return host.isSuitableForGuest(vm) && host.getStorage() >= vm.getSize();
    }

    /**
     * Wraps vm.getTotalUtilizationOfCpuMips(now). NOT PE-count-normalized and
     * NOT bounded to [0,1]: sums per-cloudlet UtilizationModel values
     * (effectively concurrent-cloudlet count under UtilizationModelFull) times
     * the VM's per-PE MIPS rate, uncorrected for VMs with >1 PE. Known-noisy.
     */
    @Override
    public double getVmCpuUtil(GuestEntity vm){
        return vm.getTotalUtilizationOfCpuMips(getNow());
    }

    // get 30-reading rolling average of VM util
    @Override
    public double getVmUtilizationMean(GuestEntity vm) {
        return (vm instanceof PowerGuestEntity) ? ((PowerGuestEntity) vm).getUtilizationMean() : 0.0;
    }

    /**
     * Wraps PowerGuestEntity.getUtilizationMad() — median absolute deviation of
     * the VM's last 30 recorded activity samples. NOTE: unlike
     * getVmUtilizationMean() (which multiplies by vm.getMips() before
     * returning), this value is NOT MIPS-scaled — CloudSim's own
     * getUtilizationMad() never applies that multiplication, and there's no
     * CloudSim-authored "MAD in MIPS" method to mirror instead. Do not combine
     * this directly with getVmUtilizationMean() (e.g. mean ± MAD) without first
     * multiplying this value by getVmMips(vm) yourself — the two are not in the
     * same units as returned.
     */
    @Override
    public double getVmUtilizationMad(GuestEntity vm) {
        return (vm instanceof PowerGuestEntity) ? ((PowerGuestEntity) vm).getUtilizationMad() : 0.0;
    }

    /**
     * Wraps vm.getCurrentRequestedTotalMips() — a placement signal, not an
     * activity signal: granted in full at VM creation, never falls even after
     * all cloudlets finish. The per-VM ratio (this / vm.getMips()) is a proven
     * degenerate signal, reading exactly 1.0 for every placed VM. Aggregate
     * across getVmListForHost() first if you want a meaningful demand signal.
     */
    @Override
    public double getVmRequestedMips(GuestEntity vm){
        return vm.getCurrentRequestedTotalMips();
    }

    /**
     * Estimates the VM's current aggregate CPU throughput in MIPS, based on how
     * many of its PEs are actually being kept busy by in-flight cloudlets right
     * now.
     *
     * Sums the number of PEs requested by each cloudlet in the VM's execution
     * list (getCloudletExecList() -- running cloudlets only, not queued or
     * completed ones), then caps that sum at the VM's own PE count, since the VM
     * can never have more PEs active than it was allocated. Multiplying by
     * getVmMips(vm) (the per-PE MIPS rate) converts "PEs in use" into an
     * aggregate MIPS figure.
     *
     * This differs from getVmMips(vm) * vm.getNumberOfPes() (the VM's full rated
     * capacity): if fewer cloudlets are running than the VM has PEs, this
     * reflects the smaller, actual in-use throughput rather than the VM's
     * theoretical ceiling.
     *
     * @param vm the VM to inspect
     * @return the VM's current effective throughput, in MIPS
     */
    @Override
    public double getVmEffectiveThroughput(GuestEntity vm){ 

        List<Cloudlet> cloudletList = vm.getCloudletScheduler().getCloudletExecList();
        int peDemand = 0;

        for (Cloudlet cl : cloudletList){
            peDemand += cl.getNumberOfPes();
        }

        return getVmMips(vm) * Math.min(peDemand, vm.getNumberOfPes());
    }

    // Get the lsit of cloudlets allocated to the given VM.
    @Override
    public List<Cloudlet> getVmCloudletList(GuestEntity vm){
        return vm.getCloudletScheduler().getCloudletExecList();
    }

    // Get the list of completed cloudlets
    @Override
    public List<Cloudlet> getCompletedCloudletList(){
        return getCloudletReceivedList();
    }

    // Get the remaining cloudlet length yet to be processed
    @Override
    public long getRemainingLength(Cloudlet cl){
        return cl.getRemainingCloudletLength();
    }

    // Get the total length of the given cloudlet
    @Override
    public long getTotalLength(Cloudlet cl){
        return cl.getCloudletTotalLength();
    }

    // Get the number of unallocated MIPS of the given host
    @Override
    public double getHostAvailableMips(HostEntity host){
        return host.getGuestScheduler().getAvailableMips();
    }

    // Retrieve the MIPS rating of this VM.
    @Override
    public double getVmMips(GuestEntity vm){
        return vm.getMips();
    }

    // Get the largest MIPS share this VM receievd from one of its allocated cores.
    @Override 
    public double getVmMaxMips(GuestEntity vm){
        return vm.getCurrentRequestedMaxMips();
    }

    // Retrieve the list of the MIPS shares given to this VM by each allocated PE.
    @Override
    public List<Double> getVmMipsPerPe(GuestEntity vm){
        return vm.getCurrentRequestedMips();
    }

    // Get the RAM rating of the given VM
    @Override
    public double getVmRam(GuestEntity vm){
        return vm.getRam();
    }

    // get the BW rating of the given VM
    @Override
    public double getVmBw(GuestEntity vm){
        return vm.getBw();
    }

    // get the number of PEs requested by the given VM
    @Override
    public int getVmNumberOfPes(GuestEntity vm){
        return vm.getNumberOfPes();
    }   

    // get the number of PEs requested by the given cloudlet
    @Override
    public int getCloudletNumberOfPes(Cloudlet cl){
        return cl.getNumberOfPes();
    }

    // is the given VM currently in migration
    @Override 
    public boolean isVmMigrating(GuestEntity vm){
        return vm.isInMigration();
    }

    // get current host power draw
    @Override
    public double getHostPower(HostEntity host){
        return (host instanceof PowerHostEntity) ? ((PowerHostEntity) host).getPower() : 0.0;
    }

    // get power draw of host at give util level
    @Override
    public double getHostPowerAtUtil(HostEntity host, double util){
        return (host instanceof PowerHostEntity) ? ((PowerHostEntity) host).getPower(util) : 0.0;
    }

    // get power draw of host if working at 100% util
    @Override
    public double getHostMaxPower(HostEntity host){
        return (host instanceof PowerHostEntity) ? ((PowerHostEntity) host).getMaxPower() : 0.0;
    }

    // estimate energy dispelled while changing util over a given time
    @Override
    public double getHostEnergyEstimate(HostEntity host, double fromUtil, double toUtil, double time){
        return (host instanceof PowerHostEntity)
        ? ((PowerHostEntity) host).getEnergyLinearInterpolation(fromUtil, toUtil, time)
        : 0.0;
    }

    // get the total energy (J) consumed so far by the datacenter
    @Override
    public double getTotalEnergyConsumedSoFar() {
        PowerDatacenter dc = getDatacenter();
        return (dc != null) ? dc.getPower() : 0.0;
    }

    @Override
    public double getTotalWorkProcessedSoFar() {
        return workCompleteRunningTotal;
    }

    @Override
    public boolean isHostPoweredDown(HostEntity host){
        return poweredDownHostIds.contains(host.getId());
    }

    @Override
    public boolean isHostPoweringUp(HostEntity host){
        return poweringUpHostIds.contains(getId(host));
    }

    @Override
    public boolean isVmBeingInstantiated(GuestEntity vm){
        return vm.isBeingInstantiated();
    }

    @Override
    public double getCloudletEstimatedFinishTime(GuestEntity vm, Cloudlet cl){
        return vm.getCloudletScheduler().getEstimatedFinishTime(cl, getNow());
    }

    @Override
    public List<Cloudlet> getActiveCloudlets(){
        List<Cloudlet> cloudlets = new ArrayList<>();
        for (GuestEntity vm : getVmList()){
            cloudlets.addAll(getCloudletsOnVm(vm));
        }
        return cloudlets;
    }

    // Retrieve our defined RAM tiers.
    @Override
    public int[] getRamTiers(){
        return Arrays.copyOf(RAM_TIERS, RAM_TIERS.length);
    }

    // Retrieve our defined BW tiers.
    @Override
    public int[] getBwTiers(){
        return Arrays.copyOf(BW_TIERS, BW_TIERS.length);
    }

    // Get a Cloudlet object by its ID
    @Override
    public Cloudlet getCloudletById(int cloudletId){
        for (Cloudlet cl : getActiveCloudlets()){
            if (cl.getCloudletId() == cloudletId){
                return cl;
            }
        }
        return null;
    }

    // Retrieve the next level of RAM
    @Override
    public double getNextRamTier(GuestEntity vm) {
        double currentRam = vm.getRam();
        for (int i = 0; i < RAM_TIERS.length - 1; i++) {
            if (RAM_TIERS[i] == currentRam) {
                return (double) RAM_TIERS[i + 1];
            }
        }
        return -1.0; // already at top tier, or RAM doesn't match a known tier
    }

    // Retrieve the next level of BW
    @Override
    public double getNextBwTier(GuestEntity vm) {
        double currentBw = vm.getBw();
        for (int i = 0; i < BW_TIERS.length - 1; i++) {
            if (BW_TIERS[i] == currentBw) {
                return (double) BW_TIERS[i + 1];
            }
        }
        return -1.0; // already at top tier, or BW doesn't match a known tier
    }

    //////////////////////////// MAPE Cycle /////////////////////////////////////////
 
    // Execute MAPE cycle of the selected controller
    private void mapeCycle() {

        sweepAbandonedCloudlets();

        // Debugging: Print VM Allocation 
        // Will always fire upon first observation
        // Insert further log times in condition, must be divisible by observation rate (100 by default).
        if (!vmAllocationLogged || getNow() == 1000000) { 
            logVmAllocation(this); 
            vmAllocationLogged = true; 
        }

        // Quit if we have finished Cloudlet workload.
        if (pendingInjections == 0 
            && pendingFailures == 0
            && pendingRepairs == 0
            && getCloudletList().isEmpty() 
            && getCloudletSubmittedList().size() - numSubmittedCloudletsAbandoned == getCloudletReceivedList().size()) {
            completedNaturally = true;
            return; 
        }

        // Update Knowledge base
        updateKnowledge();
        // Change controller (if permitted and deemed necessary)
        updateSelected(); 

        // Given a controller, execute its MAPE cycle
        if (selected != null){
            selected.observeAndAct(this);
        }

        // Schedule next observation
        schedule(getId(), observationRate, CloudActionTags.VM_BROKER_EVENT);
    }
    
    /////////////////////////// HELPER METHODS ///////////////////////////////////////

    public int getNumRealFailures() { return numRealFailures; }
    public double getTotalDowntime() { return totalDowntime; }
    public int getPeakSimultaneousFailedHosts() { return peakSimultaneousFailedHosts; }
    public int getNumCloudletsDeferred() { return numCloudletsDeferred; }
    public double getTotalDeferredWaitTime() { return totalDeferredWaitTime; }
    public int getNumCloudletsExposedToFailure() { return exposedCloudletIds.size(); }
    public double getGroundTruthAvgVariance() {return groundTruthCycleCount == 0 ? 0.0 : groundTruthVarianceSum / groundTruthCycleCount; }
    public double getPeakRamUtilization() { return peakRamUtilization; }
    public double getPeakBwUtilization() { return peakBwUtilization; }
    public double getPeakMipsUtilization() { return peakMipsUtilization; }
    public double getAvgRamUtilization() { return utilizationCycleCount == 0 ? 0.0 : ramUtilizationSum / utilizationCycleCount; }
    public double getAvgBwUtilization() { return utilizationCycleCount == 0 ? 0.0 : bwUtilizationSum / utilizationCycleCount; }
    public double getAvgMipsUtilization() { return utilizationCycleCount == 0 ? 0.0 : mipsUtilizationSum / utilizationCycleCount; }
    public double getAvgVmMipsUtilization() { return utilizationCycleCount == 0 ? 0.0 : vmMipsUtilizationSum / utilizationCycleCount; }
    public double getPeakVmMipsUtilization() { return peakVmMipsUtilization; }
    public double getAvgHostRamUtilVariance()  { return hostUtilVarianceCycleCount == 0 ? 0.0 : ramUtilVarianceSum  / hostUtilVarianceCycleCount; }
    public double getAvgHostBwUtilVariance()   { return hostUtilVarianceCycleCount == 0 ? 0.0 : bwUtilVarianceSum   / hostUtilVarianceCycleCount; }
    public double getAvgHostMipsUtilVariance() { return hostUtilVarianceCycleCount == 0 ? 0.0 : mipsUtilVarianceSum / hostUtilVarianceCycleCount; }
    public double getAvgVmMipsUtilVariance()   { return vmUtilVarianceCycleCount == 0 ? 0.0 : vmMipsUtilVarianceSum / vmUtilVarianceCycleCount; }
    public int getNumCloudletsAbandoned() { return numCloudletsAbandoned; }
    public int getCloudletsStillInFlight() { return cloudletsSubmitted; }
    public int getNumCloudletsStillDeferred() { return deferralStartTimeByCloudlet.size(); }
    public int getMinLiveVmCount() {return minLiveVmCount == Integer.MAX_VALUE ? getGuestsCreatedList().size() : minLiveVmCount;}
    public int getLiveHostCount() {
        int count = 0;
        for (HostEntity h : getAllHosts()) {
            if (!isHostPermanentlyDead(h)) count++;
        }
        return count;
    }
    public boolean getCompletedNaturally(){return completedNaturally;}
    public int getNumCloudletsAbandonedHostPoweredDown() {return numCloudletsAbandonedHostPoweredDown;}
    public int getNumCloudletsAbandonedVmNeverCreated() { return numCloudletsAbandonedVmNeverCreated; }
    public int getNumCloudletsAbandonedHostDead() { return numCloudletsAbandonedHostDead; }
    public int getNumCloudletsAbandonedEvacuationFailed() { return numCloudletsAbandonedEvacuationFailed; }
    public int getNumCloudletsAbandonedVmDestroyed() { return numCloudletsAbandonedVmDestroyed; }
    public int getNumActionsSucceededOnFailedHost() { return numActionsSucceededOnFailedHost; }
    public double getAvgHostFreeMips() { return utilizationCycleCount == 0 ? 0.0 : hostFreeMipsSum / utilizationCycleCount; }
    public double getAvgHostFreeStorage() { return utilizationCycleCount == 0 ? 0.0 : hostFreeStorageSum / utilizationCycleCount; }
    public double getAvgStorageUtilization() { return utilizationCycleCount == 0 ? 0.0 : storageUtilizationSum / utilizationCycleCount; }
    public double getPeakStorageUtilization() { return peakStorageUtilization; }
    public double getAvgHostStorageUtilVariance() { return hostUtilVarianceCycleCount == 0 ? 0.0 : storageUtilVarianceSum / hostUtilVarianceCycleCount; }
    public double getAvgHostFreeRam()  { return utilizationCycleCount == 0 ? 0.0 : hostFreeRamSum  / utilizationCycleCount; }
    public double getAvgHostFreeBw()   { return utilizationCycleCount == 0 ? 0.0 : hostFreeBwSum   / utilizationCycleCount; }
    public double getAvgVmFreeMips()   { return utilizationCycleCount == 0 ? 0.0 : vmFreeMipsSum   / utilizationCycleCount; }
    public double getAbandonedWorkProcessed() {return abandonedWorkProcessed;}
    public List<Double> getWorkPerCycle() { return workCompleteReadings;}
    public List<Double> getEnergyPerCycle() { return energyExpelledReadings;}

    /////////////////////// PRIVATE SELECTOR METHODS /////////////////////////////
    
    // Updtae key system metris — runs every cycle, independent of pipeline.
    // Single entry point for every ground-truth signal the Selector may need
    // to judge whether the assigned goal has been achieved.
    private void updateKnowledge(){

        List<HostEntity> hosts = getAllHosts();
        List<GuestEntity> vms = getVmList();

        updateDemandVarianceGroundTruth(hosts);//may be defunct

        // Average host and VM resource utilisation
        updateHostResourceUtilizationGroundTruth(hosts);
        updateVmResourceUtilizationGroundTruth(vms);
        // Average host and VM resource headroom
        updateHostCapacityGroundTruth(hosts);
        updateVmCapacityGroundTruth(vms);
        // Variance in host and VM resource utilisation
        updateHostUtilizationVarianceGroundTruth(hosts);
        updateVmUtilizationVarianceGroundTruth(vms);
        // Key metrics 
        updateEnergyExpelledReadings(getDatacenter());
        updateWorkCompletedReadings(vms);
        updateCloudletsAbandonded();

    }

    // Update the variance in demand across hosts, objectively captures current load-balance
    private void updateDemandVarianceGroundTruth(List<HostEntity> hosts){
        List<Double> demands = new ArrayList<>();
        double sum = 0.0;

        for (HostEntity host : hosts) {
            if (isHostFailed(host) || isHostPermanentlyDead(host) || isHostPoweredDown(host)) continue;
            double usedMips = 0;
            for (GuestEntity vm : host.getGuestList()) {
                usedMips += vm.getCurrentRequestedTotalMips();
            }
            double demand = usedMips / host.getTotalMips();
            demands.add(demand);
            sum += demand;
        }

        if (demands.size() > 0) {
            double mean = sum / demands.size();
            double variance = 0.0;
            for (double d : demands) {
                variance += (d - mean) * (d - mean);
            }
            groundTruthVarianceSum += variance / demands.size();
            groundTruthCycleCount++;
            demandVarianceReadings.add(variance / demands.size());
        }

        
    }


    private void updateHostResourceUtilizationGroundTruth(List<HostEntity> hosts){

        double maxRamUtilThisCycle = 0, maxBwUtilThisCycle = 0, maxMipsUtilThisCycle = 0, maxStorageUtilThisCycle = 0;
        double sumRamUtilThisCycle = 0, sumBwUtilThisCycle = 0, sumMipsUtilThisCycle = 0, sumStorageUtilThisCycle = 0;
        int liveHosts = 0;

        for (HostEntity host : hosts) {
            if (isHostFailed(host) || isHostPermanentlyDead(host) || isHostPoweredDown(host)) continue;

            double hostRamUsed = 0, hostBwUsed = 0, hostMipsUsed = 0, hostStorageUsed = 0;
            for (GuestEntity vm : host.getGuestList()) {
                hostRamUsed += vm.getRam();
                hostBwUsed += vm.getBw();
                hostMipsUsed += vm.getTotalMips();
                hostStorageUsed += vm.getSize();
            }
            double hostRamUtil = hostRamUsed / host.getRam();
            double hostBwUtil = hostBwUsed / host.getBw();
            double hostMipsUtil = hostMipsUsed / host.getTotalMips();
            double hostStorageUtil = hostStorageUsed / (hostStorageUsed + host.getStorage());

            maxRamUtilThisCycle = Math.max(maxRamUtilThisCycle, hostRamUtil);
            maxBwUtilThisCycle = Math.max(maxBwUtilThisCycle, hostBwUtil);
            maxMipsUtilThisCycle = Math.max(maxMipsUtilThisCycle, hostMipsUtil);
            maxStorageUtilThisCycle = Math.max(maxStorageUtilThisCycle, hostStorageUtil);
            sumRamUtilThisCycle += hostRamUtil;
            sumBwUtilThisCycle += hostBwUtil;
            sumMipsUtilThisCycle += hostMipsUtil;
            sumStorageUtilThisCycle += hostStorageUtil;
            liveHosts++;
        }

        if (liveHosts > 0) {
            // Peak = the single worst host, at its worst moment across the whole run
            peakRamUtilization = Math.max(peakRamUtilization, maxRamUtilThisCycle);
            peakBwUtilization = Math.max(peakBwUtilization, maxBwUtilThisCycle);
            peakMipsUtilization = Math.max(peakMipsUtilization, maxMipsUtilThisCycle);
            peakStorageUtilization = Math.max(peakStorageUtilization, maxStorageUtilThisCycle);

            // Average = mean across live hosts this cycle, averaged again across cycles
            ramUtilizationSum += sumRamUtilThisCycle / liveHosts;
            bwUtilizationSum += sumBwUtilThisCycle / liveHosts;
            mipsUtilizationSum += sumMipsUtilThisCycle / liveHosts;
            storageUtilizationSum += sumStorageUtilThisCycle / liveHosts;
            utilizationCycleCount++;

            ramUtilReadings.add(sumRamUtilThisCycle / liveHosts);
            bwUtilReadings.add(sumBwUtilThisCycle / liveHosts);
            mipsUtilReadings.add(sumMipsUtilThisCycle / liveHosts);
            storageUtilReadings.add(sumStorageUtilThisCycle / liveHosts);
        }
    }

    // Update VM MIPS util
    private void updateVmResourceUtilizationGroundTruth(List<GuestEntity> vms){

        double sumMipsUtilThisCycle = 0, maxMipsUtilThisCycle = 0;
        int countedVms = 0;

        for (GuestEntity vm : vms) {
            int peDemand = 0;
            for (Cloudlet cl : vm.getCloudletScheduler().getCloudletExecList()) {
                peDemand += cl.getNumberOfPes();
            }
            double vmMipsUtil = peDemand / (double) vm.getNumberOfPes();
            sumMipsUtilThisCycle += vmMipsUtil;
            maxMipsUtilThisCycle = Math.max(maxMipsUtilThisCycle, vmMipsUtil);
            countedVms++;
        }

        if (countedVms > 0) {
            vmMipsUtilizationSum += sumMipsUtilThisCycle / countedVms;
            peakVmMipsUtilization = Math.max(peakVmMipsUtilization, maxMipsUtilThisCycle);
        }

        vmMipsUtilReadings.add(sumMipsUtilThisCycle / countedVms);

    }

    // Update the variance in RAM/BW/MIPS utilization across hosts -- captures allocation
    // imbalance independent of the average level (updateResourceUtilizationGroundTruth).
    // Excludes dead/failed/powered-down hosts, same as updateDemandVarianceGroundTruth --
    // an evacuated host reads as 0% utilized, which is an artifact, not real spread.
    
    private void updateHostUtilizationVarianceGroundTruth(List<HostEntity> hosts){

        List<Double> ramUtils = new ArrayList<>();
        List<Double> bwUtils = new ArrayList<>();
        List<Double> mipsUtils = new ArrayList<>();
        List<Double> storageUtils = new ArrayList<>();
        double ramSum = 0.0, bwSum = 0.0, mipsSum = 0.0, storageSum = 0.0;

        for (HostEntity host : hosts) {
            if (isHostFailed(host) || isHostPermanentlyDead(host) || isHostPoweredDown(host)) continue;

            double hostRamUsed = 0, hostBwUsed = 0, hostMipsUsed = 0, hostStorageUsed = 0;
            for (GuestEntity vm : host.getGuestList()) {
                hostRamUsed += vm.getRam();
                hostBwUsed += vm.getBw();
                hostMipsUsed += vm.getTotalMips();
                hostStorageUsed += vm.getSize();
            }

            double ramUtil = hostRamUsed / host.getRam();
            double bwUtil = hostBwUsed / host.getBw();
            double mipsUtil = hostMipsUsed / host.getTotalMips();
            double storageUtil = hostStorageUsed / (hostStorageUsed + host.getStorage());

            ramUtils.add(ramUtil);
            bwUtils.add(bwUtil);
            mipsUtils.add(mipsUtil);
            storageUtils.add(storageUtil);
            ramSum += ramUtil;
            bwSum += bwUtil;
            mipsSum += mipsUtil;
            storageSum += storageUtil;
        }

        if (ramUtils.size() > 0) {
            double ramMean = ramSum / ramUtils.size();
            double bwMean = bwSum / bwUtils.size();
            double mipsMean = mipsSum / mipsUtils.size();
            double storageMean = storageSum / storageUtils.size();

            double ramVariance = 0.0, bwVariance = 0.0, mipsVariance = 0.0, storageVariance = 0.0;
            for (int i = 0; i < ramUtils.size(); i++) {
                ramVariance += (ramUtils.get(i) - ramMean) * (ramUtils.get(i) - ramMean);
                bwVariance += (bwUtils.get(i) - bwMean) * (bwUtils.get(i) - bwMean);
                mipsVariance += (mipsUtils.get(i) - mipsMean) * (mipsUtils.get(i) - mipsMean);
                storageVariance += (storageUtils.get(i) - storageMean) * (storageUtils.get(i) - storageMean);
            }

            ramUtilVarianceSum += ramVariance / ramUtils.size();
            bwUtilVarianceSum += bwVariance / bwUtils.size();
            mipsUtilVarianceSum += mipsVariance / mipsUtils.size();
            storageUtilVarianceSum += storageVariance / storageUtils.size();
            hostUtilVarianceCycleCount++;

            ramUtilVarianceReadings.add(ramVariance / ramUtils.size());
            bwUtilVarianceReadings.add(bwVariance / bwUtils.size());
            mipsUtilVarianceReadings.add(mipsVariance / mipsUtils.size());
            storageUtilVarianceReadings.add(storageVariance / storageUtils.size());
        }

    }

    // Update the variance in MIPS utilization across VMs -- captures whether individual
    // VMs are unevenly loaded relative to their own allocated capacity, independent of the
    // fleet-wide average (updateVmResourceUtilizationGroundTruth).
    private void updateVmUtilizationVarianceGroundTruth(List<GuestEntity> vms){

        List<Double> mipsUtils = new ArrayList<>();
        double sum = 0.0;

        for (GuestEntity vm : vms) {
            int peDemand = 0;
            for (Cloudlet cl : vm.getCloudletScheduler().getCloudletExecList()) {
                peDemand += cl.getNumberOfPes();
            }
            double vmMipsUtil = peDemand / (double) vm.getNumberOfPes();
            mipsUtils.add(vmMipsUtil);
            sum += vmMipsUtil;
        }

        if (mipsUtils.size() > 0) {
            double mean = sum / mipsUtils.size();
            double variance = 0.0;
            for (double u : mipsUtils) {
                variance += (u - mean) * (u - mean);
            }
            vmMipsUtilVarianceSum += variance / mipsUtils.size();
            vmUtilVarianceCycleCount++;
            vmMipsUtilVarianceReadings.add(variance / mipsUtils.size());
        }

    }

    // Update total MIPS, RAM and BW headroom
    private void updateHostCapacityGroundTruth(List<HostEntity> hosts){

        double sumFreeMipsThisCycle = 0, sumFreeRamThisCycle = 0, sumFreeBwThisCycle = 0, sumFreeStorageThisCycle = 0;
        int liveHosts = 0;

        for (HostEntity host : hosts) {
            if (isHostFailed(host) || isHostPermanentlyDead(host) || isHostPoweredDown(host)) continue;
            sumFreeMipsThisCycle += getHostAvailableMips(host);
            sumFreeRamThisCycle += getHostAvailableRam(host);
            sumFreeBwThisCycle += getHostAvailableBw(host);
            sumFreeStorageThisCycle += host.getStorage();
            liveHosts++;
        }

        if (liveHosts > 0) {
            hostFreeMipsSum += sumFreeMipsThisCycle / liveHosts;
            hostFreeRamSum += sumFreeRamThisCycle / liveHosts;
            hostFreeBwSum += sumFreeBwThisCycle / liveHosts;
            hostFreeStorageSum += sumFreeStorageThisCycle / liveHosts;
            hostFreeMipsReadings.add(sumFreeMipsThisCycle / liveHosts);
            hostFreeRamReadings.add(sumFreeRamThisCycle / liveHosts);
            hostFreeBwReadings.add(sumFreeBwThisCycle / liveHosts);
        }

    }

    // Update VM MIPS headroom
    private void updateVmCapacityGroundTruth(List<GuestEntity> vms){

        double sumFreeMipsThisCycle = 0;
        int countedVms = 0;

        for (GuestEntity vm : vms) {
            int peDemand = 0;
            for (Cloudlet cl : vm.getCloudletScheduler().getCloudletExecList()) {
                peDemand += cl.getNumberOfPes();
            }
            int freePes = Math.max(0, vm.getNumberOfPes() - peDemand);
            sumFreeMipsThisCycle += freePes * vm.getMips();
            countedVms++;
        }

        if (countedVms > 0) {
            vmFreeMipsSum += sumFreeMipsThisCycle / countedVms;
            vmFreeMipsReadings.add(sumFreeMipsThisCycle / countedVms);
        }

    }

    // Calculate total energy expelled since the last observation
    private void updateEnergyExpelledReadings(PowerDatacenter dc){

        double totalEnergyExpelled = (dc != null) ? dc.getPower() : 0.0;
        double energyExpelledSinceLastCycle = totalEnergyExpelled - energyExpelledRunningTotal;
        energyExpelledRunningTotal = totalEnergyExpelled;

        energyExpelledReadings.add(energyExpelledSinceLastCycle);

    }

    // Calculate total workload (in MI) that has been processed since the last observation
    private void updateWorkCompletedReadings(List<GuestEntity> vms){

        double totalWorkCompletedTest = 0;

        for (Cloudlet cl : getCloudletSubmittedList()){
            totalWorkCompletedTest += cl.getCloudletTotalLength() - cl.getRemainingCloudletLength();
        }
        
        double workCompleteSinceLastCycle = totalWorkCompletedTest - workCompleteRunningTotal;
        workCompleteRunningTotal = totalWorkCompletedTest;

        workCompleteReadings.add(workCompleteSinceLastCycle);

    }

    // Calculate number of cloudlets abandoned since last cycle
    private void updateCloudletsAbandonded(){

        double totalCloudletsAbandoned = (double) numCloudletsAbandoned;
        double cloudletsAbandonedSinceLastCycle = totalCloudletsAbandoned - cloudletsAbandonedRunningTotal;
        cloudletsAbandonedRunningTotal = totalCloudletsAbandoned;

        cloudletsAbandonedReadings.add(cloudletsAbandonedSinceLastCycle);

    }

    // Switch controller based upon ground truth readings and current goals
    private void updateSelected(){

        if (SWITCHABLE){
            if (getNow() % 500 == 0 && selected != null){

                // Select next controller algorithm.
                // (MAKING THIS LOGIC MORE COMPLEX IS LEFT TO LATER WORK).

                int index = 0;
                int counter = 0;

                for (ControlUnit c : controllers){
                    if (c.equals(selected)){
                        index = counter;
                        break;
                    }
                    counter++;
                }

                selected = controllers.get((index + 1) % controllers.size());
                //Log.enable();
                Log.printlnConcat(getNow(), ": [Selector] Switching to ", selected.getName());
                Log.disable();
                
            }
        }

    }

    // Send cloudlet list to broker for allocation
    @SuppressWarnings("unchecked")
    private void processCloudletInjection(SimEvent ev) {
        List<Cloudlet> batch = (List<Cloudlet>) ev.getData();
        admitCloudlets(batch);
        pendingInjections--;
        //Log.enable();
        Log.printlnConcat(getNow(), ": [Selector] ", batch.size(), " Cloudlets injected.");
        Log.disable();
    }

    // Pause workloads on failed hosts.
    @SuppressWarnings("unchecked")
    private void processHostFailure(SimEvent ev) {

        List<HostEntity> hosts = getAllHosts();
        HostEntity targetHost = null;

        int id = (int) ev.getData();

        for (HostEntity h : hosts){
            if (h.getId() == id){
                targetHost = h;
                break;
            }
        }

        pendingFailures--;

        // CHANGE 1: log the previously-silent null case instead of just returning.
        if (targetHost == null) {
            //Log.enable();
            Log.printlnConcat(getNow(), ": [Selector] Host #", id, " failure event fired but no matching host found — ignored.");
            Log.disable();
            return;
        }

        if (isHostFailed(targetHost)) {
            //Log.enable();
            Log.printlnConcat(getNow(), ": [Selector] Host #", id, " failure ignored — already failed, existing repair unaffected.");
            Log.disable();
            return;
        }

        //Log.enable();

        // CHANGE 2: abort on cast failure instead of logging and falling through.
        if (!(targetHost instanceof Host)) {
            Log.printlnConcat(getNow(), ": [Selector] Host #", targetHost.getId(),
                " is not a Host instance, cannot mark failed — aborting failure processing.");
            Log.disable();
            return;
        }
        ((Host) targetHost).setFailed(true);

        numRealFailures++;
        currentFailedHostCount++;
        peakSimultaneousFailedHosts = Math.max(peakSimultaneousFailedHosts, currentFailedHostCount);
        failureStartTimeByHost.put(id, getNow());

        double repairDelay = repairDurationDist.sample();
        pendingRepairs++;
        schedule(getId(), repairDelay, CloudActionTags.HOST_REPAIR, id);
        Log.printlnConcat(getNow(), ": [Selector] Host #", id, " repair scheduled for time ", getNow() + repairDelay);

        for (GuestEntity vm : targetHost.getGuestList()) {
            VmScheduler scheduler = targetHost.getGuestScheduler();
            scheduler.deallocatePesForGuest(vm);
            // CHANGE 3: was getCloudletExecList() only, now uses the shared helper (exec + paused).
            for (Cloudlet cl : getCloudletsOnVm(vm)) {
                exposedCloudletIds.add(cl.getCloudletId());
            }
        }

        Log.printlnConcat(getNow(), ": [Selector] Host #", targetHost.getId(), " has failed.");

        Log.disable();
    }

    // Repair failed host and restart its workload processing.
    @SuppressWarnings("unchecked")
    private void processHostRepair(SimEvent ev) {

        int id = (int) ev.getData();
        pendingRepairs--;
        HostEntity targetHost = null;

        for (HostEntity h : getAllHosts()) {
            if (h.getId() == id) { targetHost = h; break; }
        }

        if (targetHost == null){ return; }

        boolean unrecoverable = unrecoverableRng.nextDouble() < UNRECOVERABLE_PROBABILITY;

        if (unrecoverable) {

            Double startTime = failureStartTimeByHost.remove(id);
            if (startTime != null) {
                totalDowntime += (getNow() - startTime);
            }

            permanentlyDeadHostIds.add(id);

            //Log.enable();
            Log.printlnConcat(getNow(), ": [Selector] Host #", id, " repair failed — permanently dead, evacuating.");
            Log.disable();

            evacuateHost(targetHost);

        }else if (targetHost instanceof Host) {

            ((Host) targetHost).setFailed(false);

            currentFailedHostCount--;
            Double startTime = failureStartTimeByHost.remove(id);
            if (startTime != null) {
                totalDowntime += (getNow() - startTime);
            }

            VmScheduler scheduler = targetHost.getGuestScheduler();
            for (GuestEntity vm : targetHost.getGuestList()) {
                scheduler.allocatePesForGuest(vm, vm.getCurrentRequestedMips());
            }

            //Log.enable();
            Log.printlnConcat(getNow(), ": [Selector] Host #", id, " has been repaired.");
            Log.disable();

        }

        // Force the datacenter to re-check cloudlet completion. The datacenter's
        // own VM_DATACENTER_EVENT reschedule chain (Datacenter.updateCloudletProcessing())
        // dies permanently if, at any point, every host with outstanding cloudlets was
        // simultaneously failed (smallerTime stays Double.MAX_VALUE, so it skips
        // rescheduling itself). Repairing PEs alone doesn't revive that chain — only a
        // fresh CLOUDLET_SUBMIT/CLOUDLET_MOVE does, and after the last repair nothing
        // else ever submits. Without this kick, cloudlets that finished mid-outage never
        // get their CLOUDLET_RETURN sent, and the sim hangs forever.
        if (!getDatacenterIdsList().isEmpty()) {
            sendNow(getDatacenterIdsList().getFirst(), CloudActionTags.VM_DATACENTER_EVENT);
        }

        List<Cloudlet> deferred = deferredCloudletsByHost.remove(id);

        if (deferred != null && !deferred.isEmpty()) {

            for (Cloudlet cl : deferred) {
                Double deferredAt = deferralStartTimeByCloudlet.remove(cl.getCloudletId());
                if (deferredAt != null) {
                    totalDeferredWaitTime += (getNow() - deferredAt);
                }
            }

            //Log.enable();
            Log.printlnConcat(getNow(), ": [Selector] Admitting ", deferred.size(),
                " deferred cloudlets for Host #", id, ".");
            Log.disable();
            submitCloudletList(deferred);
            //Log.enable();
            submitCloudlets();
            Log.disable();

        }

    }
    
    // Turn given host OFF. Swtiches to OFF power model which consumes no energy
    private void processHostPowerDown(SimEvent ev) {

        //Log.enable();

        // Retrieve target host
        int hostId = (int) ev.getData();
        PowerHostEntity host = (PowerHostEntity) getHostById(hostId);

        // Destroy hosted VMs
        List<GuestEntity> residents = new ArrayList<>(getVmListForHost(host));
        for (GuestEntity vm : residents) {
            strandAndDestroyGuest(vm, true);
        }

        // Save working power model so we can turn back on later
        savedPowerModels.put(hostId, host.getPowerModel());
        // Switch to OFF power model, no energy consumption
        host.setPowerModel(OFF_MODEL);
        // Add to OFF list
        poweredDownHostIds.add(hostId);

        Log.printlnConcat(getNow(), ": [Selector] Host #", hostId, " powered down.",
            residents.isEmpty() ? "" : (" " + residents.size() + " VM(s) stranded."));
        Log.disable();

    }

    // Turn on given host. Restore to original power model.
    private void processHostPowerUp(SimEvent ev) {

        int hostId = (int) ev.getData();
        PowerHostEntity host = (PowerHostEntity) getHostById(hostId);
        host.setPowerModel(savedPowerModels.remove(hostId));
        poweredDownHostIds.remove(hostId);
        poweringUpHostIds.remove(hostId);

        //Log.enable();
        Log.printlnConcat(getNow(), ": [Selector] Host #", hostId, " powered up.");
        Log.disable();

    }

    // Register new batch of cloudlets for processing
    public void registerInjection(double delay, List<Cloudlet> batch) {
        injectionSchedule.add(new PendingInjection(delay, batch));
        pendingInjections++;   // prime the mapeCycle guard immediately, before the sim even starts
    }
    
    // Register host failure
    public void registerFailure(double delay, int hostId) {
        failureSchedule.add(new PendingFailure(delay, hostId));
        pendingFailures++;
    }

    // Allocate new cloudlets
    private void admitCloudlets(List<Cloudlet> batch) {
        List<Cloudlet> admissible = new ArrayList<>();
        List<GuestEntity> createdVms = getGuestsCreatedList();

        for (Cloudlet cl : batch) {
            GuestEntity vm = getVmById(cl.getGuestId());

            if (vm == null || !createdVms.contains(vm)) {
                if (!createdVms.isEmpty()) {
                    GuestEntity target = createdVms.get(unrecoverableRng.nextInt(createdVms.size()));
                    cl.setGuestId(target.getId());
                    vm = target; // fall through below using the reassigned target
                    // Log.enable();
                    Log.printlnConcat(getNow(), ": [Selector] Cloudlet #", cl.getCloudletId(),
                        " reassigned | original target VM was never created, now bound to VM#", target.getId());
                    Log.disable();
                } else {
                    abandonedWorkProcessed += cl.getCloudletTotalLength() - cl.getRemainingCloudletLength();
                    numCloudletsAbandoned++;
                    numCloudletsAbandonedVmNeverCreated++;
                    // Log.enable();
                    Log.printlnConcat(getNow(), ": [Selector] Cloudlet #", cl.getCloudletId(),
                        " abandoned | target VM#", cl.getGuestId(), " was never created, and no live VMs exist to reassign to.");
                    Log.disable();
                    continue;
                }
            }

            // vm is now guaranteed non-null and created — either it always was, or it was just reassigned
            if (isHostFailed(vm.getHost())) {

                HostEntity host = vm.getHost();

                if (isHostPermanentlyDead(host)) {
                    abandonedWorkProcessed += cl.getCloudletTotalLength() - cl.getRemainingCloudletLength();
                    numCloudletsAbandoned++;
                    numCloudletsAbandonedHostDead++;
                    // Log.enable();
                    Log.printlnConcat(getNow(), ": [Selector] Cloudlet #", cl.getCloudletId(),
                        " abandoned | target VM#", cl.getGuestId(), " host is permanently dead.");
                    Log.disable();

                } else {
                    int hostId = host.getId();
                    deferredCloudletsByHost.computeIfAbsent(hostId, k -> new ArrayList<>()).add(cl);
                    numCloudletsDeferred++;
                    deferralStartTimeByCloudlet.put(cl.getCloudletId(), getNow());
                    exposedCloudletIds.add(cl.getCloudletId());
                    // Log.enable();
                    Log.printlnConcat(getNow(), ": [Selector] Cloudlet #", cl.getCloudletId(),
                        " deferred | target VM#", cl.getGuestId(), " host is failed.");
                    Log.disable();
                }

            } else {
                admissible.add(cl);
            }
        }
        if (!admissible.isEmpty()) {
            submitCloudletList(admissible);
            submitCloudlets();
        }
    }

    // Destroy a given VM. Reused by host power down and VM destruction actions
    private void strandAndDestroyGuest(GuestEntity vm, boolean dueToHostPowerDown) {

        Integer datacenterId = getDatacenterFor(vm.getId());

        List<Cloudlet> stranded = new ArrayList<>();
        stranded.addAll(vm.getCloudletScheduler().getCloudletExecList());
        stranded.addAll(vm.getCloudletScheduler().getCloudletWaitingList());
        stranded.addAll(vm.getCloudletScheduler().getCloudletPausedList());

        for (Cloudlet cl : stranded) {
            abandonedWorkProcessed += cl.getCloudletTotalLength() - cl.getRemainingCloudletLength();
            vm.getCloudletScheduler().cloudletCancel(cl.getCloudletId());
            numSubmittedCloudletsAbandoned++;
            cloudletsSubmitted--;
            if (dueToHostPowerDown) {
                numCloudletsAbandonedHostPoweredDown++;
            } else {
                numCloudletsAbandoned++;
                numCloudletsAbandonedVmDestroyed++;
            }
            // Log.enable();
            Log.printlnConcat(getNow(), ": [Selector] Cloudlet #", cl.getCloudletId(),
                " abandoned | VM#", vm.getId(),
                dueToHostPowerDown ? " was destroyed | host powered down." : " was destroyed.");
            Log.disable();
        }

        getGuestsCreatedList().remove(vm);
        minLiveVmCount = Math.min(minLiveVmCount, getGuestsCreatedList().size());
        if (datacenterId != null) {
            sendNow(datacenterId, CloudActionTags.VM_DESTROY, vm);
        }
    }

    // Upon determining a host to be unrecoverable - attempt to migrate all VMs to other hosts
    private void evacuateHost(HostEntity deadHost) {

        List<GuestEntity> guests = new ArrayList<>(deadHost.getGuestList());
        Set<Integer> claimedThisBatch = new HashSet<>();

        for (GuestEntity vm : guests) {
            HostEntity destination = null;

            for (HostEntity candidate : getAllHosts()) {
                if (candidate.getId() == deadHost.getId()) continue;
                if (isHostFailed(candidate)) continue;
                if (claimedThisBatch.contains(candidate.getId())) continue;
                if (candidate.isSuitableForGuest(vm)) {
                    destination = candidate;
                    break;
                }
            }

            // Pull this VM's deferred cloudlets out now, regardless of outcome, rather than
            // leaving them for the outer flush to find later.
            List<Cloudlet> deferredForThisVm = new ArrayList<>();
            List<Cloudlet> deferredHere = deferredCloudletsByHost.get(deadHost.getId());
            if (deferredHere != null) {
                for (Cloudlet cl : deferredHere) {
                    if (cl.getGuestId() == vm.getId()) deferredForThisVm.add(cl);
                }
                deferredHere.removeAll(deferredForThisVm);
                if (deferredHere.isEmpty()) deferredCloudletsByHost.remove(deadHost.getId());
            }

            if (destination != null) {
                claimedThisBatch.add(destination.getId());
                //Log.enable();
                Log.printlnConcat(getNow(), ": [Selector] Evacuating VM#", vm.getId(),
                    " from Host #", deadHost.getId(), " to Host #", destination.getId());
                Log.disable();
                requestVmMigration(vm, destination);

                if (!deferredForThisVm.isEmpty()) {
                    for (Cloudlet cl : deferredForThisVm) {
                        Double deferredAt = deferralStartTimeByCloudlet.remove(cl.getCloudletId());
                        if (deferredAt != null) totalDeferredWaitTime += (getNow() - deferredAt);
                    }
                    //Log.enable();
                    Log.printlnConcat(getNow(), ": [Selector] Admitting ", deferredForThisVm.size(),
                        " deferred cloudlets for evacuated VM#", vm.getId(), ".");
                    Log.disable();
                    submitCloudletList(deferredForThisVm);
                    //Log.enable();
                    submitCloudlets();
                    Log.disable();
                }

            } else {
                //Log.enable();
                Log.printlnConcat(getNow(), ": [Selector] VM#", vm.getId(),
                    " could not be evacuated | no healthy host has room. Abandoning its workload.");
                Log.disable();

                List<Cloudlet> stranded = new ArrayList<>(vm.getCloudletScheduler().getCloudletExecList());
                for (Cloudlet cl : stranded) {
                    abandonedWorkProcessed += cl.getCloudletTotalLength() - cl.getRemainingCloudletLength();
                    vm.getCloudletScheduler().cloudletCancel(cl.getCloudletId());
                    numSubmittedCloudletsAbandoned++;
                    cloudletsSubmitted--;
                    numCloudletsAbandoned++;
                    numCloudletsAbandonedEvacuationFailed++;
                    //Log.enable();
                    Log.printlnConcat(getNow(), ": [Selector] Cloudlet #", cl.getCloudletId(),
                        " abandoned | VM#", vm.getId(), " could not be evacuated.");
                    Log.disable();
                }

                for (Cloudlet cl : deferredForThisVm) {
                    abandonedWorkProcessed += cl.getCloudletTotalLength() - cl.getRemainingCloudletLength();
                    deferralStartTimeByCloudlet.remove(cl.getCloudletId());
                    numCloudletsAbandoned++;
                    numCloudletsAbandonedEvacuationFailed++;
                    //Log.enable();
                    Log.printlnConcat(getNow(), ": [Selector] Cloudlet #", cl.getCloudletId(),
                        " abandoned | VM#", vm.getId(), " could not be evacuated.");
                    Log.disable();
                }
            }
        }
    }

    // Debugging: Print Host->VM allocations
    private void logVmAllocation(ReadSpace readSpace) {
        if (suppressDebugLogging) return;
        //System.out.println("=== VM Allocation (t=" + readSpace.getNow() + ") ===");
        for (HostEntity host : readSpace.getAllHosts()) {
            List<GuestEntity> guests = host.getGuestList();
            if (guests.isEmpty()) {
                //System.out.println("  Host #" + host.getId() + ": (empty)");
            } else {
                for (GuestEntity vm : guests) {
                    List<Cloudlet> execList = vm.getCloudletScheduler().getCloudletExecList();
                    double remainingLength = 0;
                    for (Cloudlet cl : execList) {
                        remainingLength += cl.getRemainingCloudletLength();
                        // System.out.println("VM #" + vm.getId()
                        //         + ", Cloudlet #" + cl.getCloudletId());
                    }
                    //System.out.println("  Host #" + host.getId()
                            // + " <- VM #" + vm.getId()
                            // + " (mips=" + vm.getMips()
                            // + ", RAM=" + vm.getRam()
                            // + ", BW=" + vm.getBw()
                            // + ", Image size=" + vm.getSize()
                            // + ", cloudlets=" + execList.size()
                            // + ", cpu=" + vm.getNumberOfPes()
                            // + ", remainingMI=" + remainingLength + ")");

                }
            }
        }
        //System.out.println("=== End VM Allocation ===");
    }

    // One-time sweep: catches initial-batch cloudlets whose target VM was never created (admitCloudlets() only covers injected/repair-flushed batches)
    private void sweepAbandonedCloudlets() {

        if (initialAbandonSweepDone) return;
        initialAbandonSweepDone = true;

        List<Cloudlet> stuck = new ArrayList<>();
        for (Cloudlet cl : getCloudletList()) {
            GuestEntity vm = getVmById(cl.getGuestId());
            if (vm == null || !getGuestsCreatedList().contains(vm)) {
                stuck.add(cl);
            }
        }

        List<GuestEntity> createdVms = getGuestsCreatedList();
        boolean anySubmittable = false;

        for (Cloudlet cl : stuck) {

            if (createdVms.isEmpty()) {
                abandonedWorkProcessed += cl.getCloudletTotalLength() - cl.getRemainingCloudletLength();
                numCloudletsAbandoned++;
                numCloudletsAbandonedVmNeverCreated++;
                getCloudletList().remove(cl);
                //Log.enable();
                Log.printlnConcat(getNow(), ": [Selector] Cloudlet #", cl.getCloudletId(),
                    " abandoned | target VM#", cl.getGuestId(), " was never created, and no live VMs exist to reassign to.");
                Log.disable();
                continue;
            }

            GuestEntity target = createdVms.get(unrecoverableRng.nextInt(createdVms.size()));
            cl.setGuestId(target.getId());
            //Log.enable();
            Log.printlnConcat(getNow(), ": [Selector] Cloudlet #", cl.getCloudletId(),
                " reassigned | original target VM was never created, now bound to VM#", target.getId());
            Log.disable();

            // Same failed/dead/healthy fall-through used in admitCloudlets
            if (isHostFailed(target.getHost())) {

                HostEntity host = target.getHost();

                if (isHostPermanentlyDead(host)) {
                    abandonedWorkProcessed += cl.getCloudletTotalLength() - cl.getRemainingCloudletLength();
                    numCloudletsAbandoned++;
                    numCloudletsAbandonedHostDead++;
                    getCloudletList().remove(cl);
                    //Log.enable();
                    Log.printlnConcat(getNow(), ": [Selector] Cloudlet #", cl.getCloudletId(),
                        " abandoned | reassigned target VM#", cl.getGuestId(), " host is permanently dead.");
                    Log.disable();

                } else {
                    int hostId = host.getId();
                    deferredCloudletsByHost.computeIfAbsent(hostId, k -> new ArrayList<>()).add(cl);
                    numCloudletsDeferred++;
                    deferralStartTimeByCloudlet.put(cl.getCloudletId(), getNow());
                    exposedCloudletIds.add(cl.getCloudletId());
                    getCloudletList().remove(cl);
                    //Log.enable();
                    Log.printlnConcat(getNow(), ": [Selector] Cloudlet #", cl.getCloudletId(),
                        " deferred | reassigned target VM#", cl.getGuestId(), " host is failed.");
                    Log.disable();
                }

            } else {
                anySubmittable = true; // stays in getCloudletList(), bound to a healthy VM now
            }
        }

        if (anySubmittable) {
            //Log.enable();
            submitCloudlets();
            Log.disable();
        }
    }

    // Retrieve the datacenter object (single datacenter sim so always returns the single datacenter)
    private PowerDatacenter getDatacenter() {
        if (cachedDatacenter == null && !getDatacenterIdsList().isEmpty()) {
            Object entity = CloudSim.getEntity(getDatacenterIdsList().getFirst());
            if (entity instanceof PowerDatacenter) {
                cachedDatacenter = (PowerDatacenter) entity;
            }
        }
        return cachedDatacenter;
    }

    // Keep track of the next ID to be given to new VMs
    private int allocateNextVmId() {
        if (nextVmId == -1) {
            int maxExisting = -1;
            for (GuestEntity vm : getVmList()) {
                maxExisting = Math.max(maxExisting, vm.getId());
            }
            nextVmId = maxExisting + 1;
        }
        return nextVmId++;
    }

    // Retrieve complete list of executing and paused cloudlets on the given VM
    private List<Cloudlet> getCloudletsOnVm(GuestEntity vm) {
        List<Cloudlet> cloudlets = new ArrayList<>();
        cloudlets.addAll(vm.getCloudletScheduler().getCloudletExecList());
        cloudlets.addAll(vm.getCloudletScheduler().getCloudletPausedList());
        return cloudlets;
    }

    private static double rollingAverage(List<Double> readings, int window) {
        if (readings.isEmpty()) return 0.0;
        int n = Math.min(window, readings.size());
        double sum = 0;
        for (int i = readings.size() - n; i < readings.size(); i++) {
            sum += readings.get(i);
        }
        return sum / n;
    }

}