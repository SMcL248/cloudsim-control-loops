package org.cloudbus.cloudsim.examples;
 
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.function.Predicate;
 
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerVm;
import org.cloudbus.cloudsim.VmAllocationPolicy.GuestMapping;
import org.cloudbus.cloudsim.VmScheduler;
import org.cloudbus.cloudsim.core.CloudActionTags;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.core.PowerGuestEntity;
import org.cloudbus.cloudsim.core.PowerHostEntity;
 
 
public class HollowedControl<M,D,A> extends DatacenterBroker implements ActionSpace {
 
    private final Monitor<M> monitor;
    private final Analyser<M,D> analyser;
    private final Planner<D,A> planner;
    private final Executor<A> executor;
    private final int observationRate;
    private final int[] mipsTiers;
    private PowerDatacenter cachedDatacenter;
 
    // Injected at construction, defined where D/A are concretely known (ConstructorVariableVM).
    // HollowedControl itself never sees LoadState[] or int[] directly — it only ever sees D/A.
    private final Predicate<D> imbalancePredicate;   // "at least one OVERLOADED or UNDERLOADED"
    private final Predicate<D> opportunityPredicate; // "at least one OVERLOADED AND one UNDERLOADED"
    private final Predicate<A> actionProposedPredicate; // "planner emitted a non-sentinel action" (diagnostic)

    private final Set<Integer> permanentlyDeadHostIds = new HashSet<>();
    private final Set<Integer> poweredDownHostIds = new HashSet<>();
    private final Set<Integer> poweringUpHostIds = new HashSet<>();
    private final Map<Integer, PowerModel> savedPowerModels = new HashMap<>();
 
    // Tracks CPU demand variance across observations
    private double groundTruthVarianceSum = 0.0;
    private int groundTruthCycleCount = 0;
 
    private int imbalanceCycles = 0;  // number of times any imbalance is detected
    private int opportunityCycles = 0; // number of times an action oppertunity is detected
    private int actionsProposed = 0; // number of times the planner proposed a non-sentinel action
    private int actionsExecuted = 0; // number of times an action is taken

    private boolean vmAllocationLogged = false;// Debugging artefact

    private static final double SPIKE_MULTIPLIER = 1.2;
    private static final int[] RAM_TIERS = {1024, 2048, 4096};
    private static final int[] BW_TIERS = {625, 1250, 2500};
    private static final long[] SIZE_TIERS = {5_000L, 10_000L, 20_000L};
    private static final PowerModel OFF_MODEL = new PowerModelOff() {
    };

    private static final String VM_VMM = "Xen";
    private static final int VM_PRIORITY = 1;
    private static final double VM_SCHEDULING_INTERVAL = 1;

    private int nextVmId = -1;
 
    // Backward-compatible overload: instrumentation is opt-in, defaults to "never true"
    public HollowedControl(String name, int observationRate, Monitor<M> monitor, Analyser<M,D> analyser,
                            Planner<D,A> planner, Executor<A> executor, int[] mipsTiers) throws Exception {
        this(name, observationRate, monitor, analyser, planner, executor, mipsTiers, null, null, null);
    }
 
    // Backward-compatible overload: existing 2-predicate call sites (imbalance/opportunity only,
    // e.g. the validated VM-migration sweep) keep compiling unchanged. actionProposedPredicate
    // defaults to "never true" for these callers.
    public HollowedControl(String name, int observationRate, Monitor<M> monitor, Analyser<M,D> analyser,
                            Planner<D,A> planner, Executor<A> executor, int[] mipsTiers,
                            Predicate<D> imbalancePredicate, Predicate<D> opportunityPredicate) throws Exception {
        this(name, observationRate, monitor, analyser, planner, executor, mipsTiers, imbalancePredicate, opportunityPredicate, null);
    }
 
    public HollowedControl(String name, int observationRate, Monitor<M> monitor, Analyser<M,D> analyser,
        Planner<D,A> planner, Executor<A> executor, int[] mipsTiers, Predicate<D> imbalancePredicate, Predicate<D> opportunityPredicate,
        Predicate<A> actionProposedPredicate)
        throws Exception {
        super(name);
        this.observationRate = observationRate;
        this.monitor = monitor;
        this.analyser = analyser;
        this.planner = planner;
        this.executor = executor;
        this.mipsTiers = mipsTiers;
        this.imbalancePredicate = (imbalancePredicate != null) ? imbalancePredicate : d -> false;
        this.opportunityPredicate = (opportunityPredicate != null) ? opportunityPredicate : d -> false;
        this.actionProposedPredicate = (actionProposedPredicate != null) ? actionProposedPredicate : a -> false;
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
 
            // if the simulation finishes
        } else if (tag == CloudActionTags.END_OF_SIMULATION) {
            shutdownEntity();
 
            // Initiate MAPE cycle
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
 
    /////////////////////////// Contract Methods ////////////////////////////////////
 
// -----------------------ActionSpace------------------------------

    @Override
    // Submit a new cloudlet to a given datacenter
    public void sendCloudlet(int datacenterId, Cloudlet cloudlet) {
        sendNow(datacenterId, CloudActionTags.CLOUDLET_SUBMIT, cloudlet);
    }
 
    @Override
    // Migrate a cloudlet from a given VM to a given VM
    public void moveCloudlet(int cloudletId, int fromVmId, int toVmId) {
        Integer destDatacenterId = getDatacenterFor(toVmId);
        int[] data = new int[5];
        data[0] = cloudletId;
        data[1] = getUserId();
        data[2] = fromVmId;
        data[3] = toVmId;
        data[4] = destDatacenterId;
        sendNow(getDatacenterFor(fromVmId), CloudActionTags.CLOUDLET_MOVE, data);
    }

    @Override
    // Cancel a previously submitted cloudlet
    public void requestCloudletCancellation(int datacenterId, Cloudlet cl) {
        sendNow(datacenterId, CloudActionTags.CLOUDLET_CANCEL, cl);
    }

    @Override
    public void requestCloudletPause(int datacenterId, Cloudlet cl) {
        sendNow(datacenterId, CloudActionTags.CLOUDLET_PAUSE, cl);
    }

    @Override
    public void requestCloudletResume(int datacenterId, Cloudlet cl) {
        sendNow(datacenterId, CloudActionTags.CLOUDLET_RESUME, cl);
    }

    @Override
    // Migrate VM to target host
    public void requestVmMigration(GuestEntity vm, HostEntity targetHost){
        GuestMapping payload = new GuestMapping(vm, targetHost);
        send(getDatacenterFor(vm.getId()), 0, CloudActionTags.VM_MIGRATE, payload);
    }

    @Override
    // Request VM MIPS rating adjustment
    public boolean requestMipsScaling(GuestEntity vm, double newMips) {

        if (!(vm instanceof Vm)) {
            return false;
        }

        if (isHostFailed(vm.getHost())) {
            Log.enable();
            Log.printlnConcat(getNow(), ": [Selector] VM#", vm.getId(), " host is failed, action ignored.");
            Log.disable();
            return false;
        }

        double peMips = getHostCapacity(vm);
        // Ensure host PE can support new MIPS rating, if not, will scale to original value.
        double boundedMips = Math.min(newMips, peMips);

        List<Double> newMipsShare = new ArrayList<>();
        for (int i = 0; i < vm.getNumberOfPes(); i++) {
            newMipsShare.add(boundedMips);
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

            Log.enable();
            Log.printlnConcat(getNow(), ": [Selector] FAILED.");
            Log.disable();

            return false;
        }

        ((Vm) vm).setMips(boundedMips);
        vm.getCloudletScheduler().updateCloudletsProcessing(getNow(), newMipsShare);

        return true;

    }

    @Override
    // Allocate an additional core to a VM
    public boolean requestPeAllocation(GuestEntity vm){

        if (!(vm instanceof Vm)) {
        return false;
        }

        if (isHostFailed(vm.getHost())) {
            Log.enable();
            Log.printlnConcat(getNow(), ": [Selector] VM#", vm.getId(), " host is failed, action ignored.");
            Log.disable();
            return false;
        }

        HostEntity host = vm.getHost();
        double peMips = vm.getMips();


        List<Double> currentShare = host.getGuestScheduler().getAllocatedMipsForGuest(vm);
        if (currentShare == null) {
            Log.enable();
            Log.printlnConcat(getNow(), ": [requestPeAllocation] VM#", vm.getId(), " no current allocation found, aborting");
            Log.disable();
            return false;
        }
        List<Double> newShare = new ArrayList<>(currentShare);
        newShare.add(peMips);


        host.getGuestScheduler().deallocatePesForGuest(vm);
        boolean success = host.getGuestScheduler().allocatePesForGuest(vm, newShare);
        Log.enable();
        Log.printlnConcat(getNow(), ": [requestPeScaling] VM#", vm.getId(),
            " success=", success, " requestedShareSize=", newShare.size(),
            " availableMipsAfter=", host.getGuestScheduler().getAvailableMips());
        Log.disable();

        if (success) {
            ((Vm) vm).setNumberOfPes(vm.getNumberOfPes() + 1);
            vm.getCloudletScheduler().updateCloudletsProcessing(getNow(), newShare);
            Log.enable();
            Log.printlnConcat(getNow(), ": [requestPeScaling] VM#", vm.getId(), " numberOfPes now=", vm.getNumberOfPes());
            Log.disable();
        }

        return success;

    }

    @Override
    // Deallocate a core from a VM
    public boolean requestPeDeallocation(GuestEntity vm){

        if (!(vm instanceof Vm)) {
        return false;
        }

        if (isHostFailed(vm.getHost())) {
            Log.enable();
            Log.printlnConcat(getNow(), ": [Selector] VM#", vm.getId(), " host is failed, action ignored.");
            Log.disable();
            return false;
        }

        HostEntity host = vm.getHost();
        double peMips = vm.getMips();

        List<Double> currentShare = host.getGuestScheduler().getAllocatedMipsForGuest(vm);

        if (currentShare == null) {
            Log.enable();
            Log.printlnConcat(getNow(), ": [requestPeDeallocation] VM#", vm.getId(), " no current allocation found, aborting");
            Log.disable();
            return false;
        }

        if (currentShare.size() > 1){

            List<Double> newShare = new ArrayList<>(currentShare);

            boolean removed = newShare.remove(peMips);

            if (!removed) {
                Log.enable();
                Log.printlnConcat(getNow(), ": [requestPeDeallocation] VM#", vm.getId(),
                    " no matching peMips entry found, aborting");
                Log.disable();
                return false;
            }

            host.getGuestScheduler().deallocatePesForGuest(vm);

            boolean success = host.getGuestScheduler().allocatePesForGuest(vm, newShare);
            Log.enable();
            Log.printlnConcat(getNow(), ": [requestPeDeallocation] VM#", vm.getId(),
                " success=", success, " requestedShareSize=", newShare.size(),
                " availableMipsAfter=", host.getGuestScheduler().getAvailableMips());
            Log.disable();

            if (success) {
                ((Vm) vm).setNumberOfPes(vm.getNumberOfPes() - 1);
                vm.getCloudletScheduler().updateCloudletsProcessing(getNow(), newShare);
                Log.enable();
                Log.printlnConcat(getNow(), ": [requestPeDeallocation] VM#", vm.getId(), " numberOfPes now=", vm.getNumberOfPes());
                Log.disable();
            }

            return success;

        }else{
            Log.enable();
            Log.printlnConcat(getNow(), ": [requestPeDeallocation] Unable to deallocate a PE from VM#", vm.getId());
            Log.disable();
            return false;
        }

    }

    @Override
    // Adjust RAM of VM
    public boolean requestRamScaling(GuestEntity vm, double newRam) {
        if (!(vm instanceof Vm)) return false;
        if (isHostFailed(vm.getHost())) {
            Log.enable();
            Log.printlnConcat(getNow(), ": [Selector] VM#", vm.getId(), " host is failed, action ignored.");
            Log.disable();
            return false;
        }

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
            Log.enable();
            Log.printlnConcat(getNow(), ": [requestRamScaling] VM#", vm.getId(), " FAILED.");
            Log.disable();
            return false;
        }

        return true;
    }

    @Override
    // Adjust BW of VM
    public boolean requestBwScaling(GuestEntity vm, double newBw) {
        if (!(vm instanceof Vm)) return false;
        if (isHostFailed(vm.getHost())) {
            Log.enable();
            Log.printlnConcat(getNow(), ": [Selector] VM#", vm.getId(), " host is failed, action ignored.");
            Log.disable();
            return false;
        }

        HostEntity host = vm.getHost();
        long requestedBw = (long) newBw;

        // No clamp-against-self trap here, and failure leaves the existing allocation
        // untouched (true no-op) -- no manual rollback needed, unlike RAM.
        boolean success = host.getGuestBwProvisioner().allocateBwForGuest(vm, requestedBw);

        if (success) {
            ((Vm) vm).setBw(requestedBw);
            return true;
        } else {
            Log.enable();
            Log.printlnConcat(getNow(), ": [requestBwScaling] VM#", vm.getId(), " FAILED.");
            Log.disable();
            return false;
        }
    }

    @Override
    // Create VM object and schedules in allocation
    public GuestEntity requestVmCreation(int tierIndex, int sizeTierIndex, int datacenterId) {
        int id = allocateNextVmId();

        PowerVm newVm = new PowerVm(
            id,
            getUserId(),
            mipsTiers[tierIndex],
            1,                          // pesNumber -- always 1 at creation, matches initial population; scale via requestPeAllocation
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
    // Destroy/remove a VM entirely
    public void requestVmDestruction(GuestEntity vm) {
        Integer datacenterId = getDatacenterFor(vm.getId());
        if (datacenterId == null) { return; }

        getGuestsCreatedList().remove(vm);
        sendNow(datacenterId, CloudActionTags.VM_DESTROY, vm);
    }

    @Override
    public void requestHostPowerDown(HostEntity host){

        if (getVmListForHost(host).size() != 0){
            Log.enable();
            Log.printlnConcat(getNow(), ": [Selector] Cannot power down Host #", host.getId(), " | Host must have no VMs allocated.");
            Log.disable();
            return;
        }else if(isHostPoweredDown(host)){
            Log.enable();
            Log.printlnConcat(getNow(), ": [Selector] Cannot power down Host #", host.getId(), " | Host already powered down.");
            Log.disable();
            return;
        } else if (isHostPoweringUp(host)){
            Log.enable();
            Log.printlnConcat(getNow(), ": [Selector] Cannot power down Host #", host.getId(), " | Host is currently being powered up.");
            Log.disable();
            return;
        }


        Log.enable();
        Log.printlnConcat(getNow(), ": [Selector] Powering Host #", host.getId(), " down.");
        Log.disable();
        send(getId(), 0, CloudActionTags.HOST_POWER_DOWN, getId(host));

    }

    @Override
    public void requestHostPowerUp(HostEntity host){

        if(!isHostPoweredDown(host)){
            Log.enable();
            Log.printlnConcat(getNow(), ": [Selector] Cannot power up Host #", host.getId(), " | Host already on.");
            Log.disable();
            return;
        }   
        if (isHostPoweringUp(host)) {
            Log.enable();
            Log.printlnConcat(getNow(), ": [Selector] Cannot power up Host #", host.getId(), " | Already booting.");
            Log.disable();
            return;
        }

        poweringUpHostIds.add(getId(host));
        PowerModel original = savedPowerModels.get(getId(host));
        ((PowerHostEntity) host).setPowerModel(new PowerModelSpike(original, SPIKE_MULTIPLIER));

        Log.enable();
        Log.printlnConcat(getNow(), ": [Selector] Powering Host #", host.getId(), " up.");
        Log.disable();
        send(getId(), 100, CloudActionTags.HOST_POWER_UP, getId(host));

    }

// -----------------------ReadSpace-------------------------------
    @Override
    // May become ReadSpace
    public boolean isHostPermanentlyDead(HostEntity host) {
        return permanentlyDeadHostIds.contains(host.getId());
    }

    @Override
    public GuestEntity getVmById(int vmId) {
        for (GuestEntity vm : getVmList()) {
            if (vm.getId() == vmId) {
                return vm;
            }
        }
        return null;
    }

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

    @Override
    // Returns the datacenter ID of the given VM
    public Integer getDatacenterFor(int vmId) {
        return getVmsToDatacentersMap().get(vmId);
    }

    @Override
    // Retrieve the complete list of VMs
    public List<GuestEntity> getVmList() {
        return getGuestsCreatedList();
    }
 
    @Override
    // Get the ID of the controller
    public int getUserId() {
        return getId();
    }
 
    @Override
    // Retrieve the complete list of hosts
    public List<HostEntity> getAllHosts() {
        return getDatacenterCharacteristicsList().values().iterator().next().getHostList();
    }
 
    @Override
    // Retreive the current time
    public double getNow(){
        return CloudSim.clock();
    }
 
    // Retrieve our defined MIPS tiers.
    @Override
    public int[] getMipsTiers(){
        return Arrays.copyOf(mipsTiers, mipsTiers.length);
    }

    //Retrieve the MIPS rating of cores on the host of the given VM.
    @Override
    public double getHostCapacity(GuestEntity vm) {
        return vm.getHost().getGuestScheduler().getPeCapacity();
    }

    // Does the host of the given VM have an unused PE.
    @Override 
    public boolean hostHasFreePe(HostEntity host){
        return host.getGuestScheduler().getAvailableMips() >= host.getGuestScheduler().getPeCapacity();
    }
    
    @Override
    public boolean isHostFailed(HostEntity host) {
        return (host instanceof Host) && ((Host) host).isFailed();
    }

    @Override
    public double getHostAvailableRam(HostEntity host) {
        return host.getGuestRamProvisioner().getAvailableRam();
    }

    @Override
    public double getHostAvailableBw(HostEntity host) {
        return host.getGuestBwProvisioner().getAvailableBw();
    }

    @Override
    public double getHostTotalRam(HostEntity host) {
        return host.getRam();
    }

    @Override
    public double getHostTotalBw(HostEntity host) {
        return host.getBw();
    }

    @Override
    public double getHostTotalMips(HostEntity host) {
        return host.getTotalMips();
    }

    @Override
    public double getVmRequestedMips(GuestEntity vm){
        return vm.getCurrentRequestedTotalMips();
    }

    @Override
    public double getVmCpuUtil(GuestEntity vm){
        return vm.getTotalUtilizationOfCpuMips(getNow());
    }

    @Override
    public double getVmEffectiveThroughput(GuestEntity vm) {
        List<Cloudlet> cloudletList = vm.getCloudletScheduler().getCloudletExecList();
        int peDemand = 0;

        for (Cloudlet cl : cloudletList){
            peDemand += cl.getNumberOfPes();
        }
        return vm.getMips() * Math.min(peDemand, vm.getNumberOfPes());
    }

    @Override
    public List<Cloudlet> getVmCloudletList(GuestEntity vm){
        return vm.getCloudletScheduler().getCloudletExecList();
    }

    @Override
    public long getRemainingLength(Cloudlet cl){
        return cl.getRemainingCloudletLength();
    }

    @Override
    public double getHostAvailableMips(HostEntity host){
        return host.getTotalMips();
    }

    @Override
    public List<GuestEntity> getVmListForHost(HostEntity host){
        return host.getGuestList();
    }

    @Override
    public int getId(HostEntity host){
        return host.getId();
    }

    @Override
    public int getId(GuestEntity vm){
        return vm.getId();
    }

    @Override
    public int getId(Cloudlet cl){
        return cl.getCloudletId();
    }

    @Override
    public boolean isHostSuitableForGuest(HostEntity host, GuestEntity vm) {
        return host.isSuitableForGuest(vm);
    }

    // Retrieve the MIPS rating of this VM.
    @Override
    public double getVmMips(GuestEntity vm){
        return vm.getMips();
    }

    // Get the largestt MIPS share this VM receievd from one of its allocated cores.
    @Override 
    public double getVmMaxMips(GuestEntity vm){
        return vm.getCurrentRequestedMaxMips();
    }

    @Override
    public List<Double> getVmMipsPerPe(GuestEntity vm){
        return vm.getCurrentRequestedMips();
    }

    @Override
    public double getVmRam(GuestEntity vm){
        return vm.getRam();
    }

    @Override
    public double getVmBw(GuestEntity vm){
        return vm.getBw();
    }

    @Override
    public int getVmNumberOfPes(GuestEntity vm){
        return vm.getNumberOfPes();
    }   

    @Override
    public int getCloudletNumberOfPes(Cloudlet cl){
        return cl.getNumberOfPes();
    }

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

    @Override
    public List<Cloudlet> getCompletedCloudletList(){
        return getCloudletReceivedList();
    }

    @Override
    public long getTotalLength(Cloudlet cl){
        return cl.getCloudletTotalLength();
    }

    @Override
    public double getTotalEnergyConsumedSoFar() {
        PowerDatacenter dc = getDatacenter();
        return (dc != null) ? dc.getPower() : 0.0;
    }

    @Override
    public boolean isHostPoweredDown(HostEntity host){
        return poweredDownHostIds.contains(host.getId());
    }

    @Override
    public boolean isHostPoweringUp(HostEntity host){
        return poweringUpHostIds.contains(getId(host));
    }

    //////////////////////////// MAPE Cycle /////////////////////////////////////////
 
    // This method observes the current state of the system, analyzes it, plans actions if necessary, and executes them.
    private void observeAndAct() {

        //Debugging: prints VM allocaton
        if (!vmAllocationLogged) {
            logVmAllocation(this);   // HollowedControl implements ActionSpace, which extends ReadSpace
            vmAllocationLogged = true;
        }

        // If there is no more work, cancel observation
        if (getCloudletList().isEmpty() && getCloudletSubmittedList().size() == getCloudletReceivedList().size()) {
            return;
        }
 
        //Update variance in CPU demand across hosts
        updateGroundTruth();
 
        M metrics = monitor.observe(this);
        D diagnosis = analyser.analyse(metrics, this);
 
        // Iterate imbalanced and action oppertunity counters
        if (imbalancePredicate.test(diagnosis))   imbalanceCycles++;
        if (opportunityPredicate.test(diagnosis)) opportunityCycles++;
 
        A actions = planner.plan(diagnosis, this);
 
        // Iterate action proposed counter
        if (actionProposedPredicate.test(actions)) actionsProposed++;
 
        boolean success = executor.execute(actions, this);
 
        // Iteration action counter
        if (success) {
            actionsExecuted++;
        } else {
            Log.printlnConcat(getNow(), ": The system is balanced. No action required.");
        }
 
        // Schedule next observation
        schedule(getId(), observationRate, CloudActionTags.VM_BROKER_EVENT);
    }
 
    /////////////////////////// HELPER METHODS ///////////////////////////////////////

    // Getters
    public int getImbalanceCycles()   { return imbalanceCycles; }
    public int getOpportunityCycles() { return opportunityCycles; }
    public int getActionsProposed()   { return actionsProposed; }
    public int getActionsExecuted()   { return actionsExecuted; }
    public double getGroundTruthAvgVariance() {
        return groundTruthCycleCount == 0 ? 0.0 : groundTruthVarianceSum / groundTruthCycleCount;
    }
 
    // Ground truth measurement — runs every cycle, independent of pipeline
    private void updateGroundTruth(){
 
        List<HostEntity> hosts = getAllHosts();
        double[] demands = new double[hosts.size()];
        double mean = 0.0;
 
        for (int i = 0; i < hosts.size(); i++) {
            double usedMips = 0;
            for (GuestEntity vm : hosts.get(i).getGuestList()) {
                usedMips += vm.getCurrentRequestedTotalMips();
            }
            demands[i] = usedMips / hosts.get(i).getTotalMips();
            mean += demands[i];
        }
 
        mean /= hosts.size();
        double variance = 0.0;
 
        for (double d : demands) {
            variance += (d - mean) * (d - mean);
        }
 
        groundTruthVarianceSum += variance / hosts.size();
        groundTruthCycleCount++;
 
    }

    // Debugging: Print Host->VM allocations
    private void logVmAllocation(ReadSpace readSpace) {
        System.out.println("=== VM Allocation (t=" + readSpace.getNow() + ") ===");
        for (HostEntity host : readSpace.getAllHosts()) {
            List<GuestEntity> guests = host.getGuestList();
            if (guests.isEmpty()) {
                System.out.println("  Host #" + host.getId() + ": (empty)");
            } else {
                for (GuestEntity vm : guests) {
                    List<Cloudlet> execList = vm.getCloudletScheduler().getCloudletExecList();
                    double remainingLength = 0;
                    for (Cloudlet cl : execList) {
                        remainingLength += cl.getRemainingCloudletLength();
                    }
                    System.out.println("  Host #" + host.getId()
                            + " <- VM #" + vm.getId()
                            + " (mips=" + vm.getMips()
                            + ", cloudlets=" + execList.size()
                            + ", remainingMI=" + remainingLength + ")");
                }
            }
        }
        System.out.println("=== End VM Allocation ===");
    }

    private PowerDatacenter getDatacenter() {
        if (cachedDatacenter == null && !getDatacenterIdsList().isEmpty()) {
            Object entity = CloudSim.getEntity(getDatacenterIdsList().getFirst());
            if (entity instanceof PowerDatacenter) {
                cachedDatacenter = (PowerDatacenter) entity;
            }
        }
        return cachedDatacenter;
    }

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


    private void processHostPowerDown(SimEvent ev) {

        int hostId = (int) ev.getData();
        PowerHostEntity host = (PowerHostEntity) getHostById(hostId);
        savedPowerModels.put(hostId, host.getPowerModel());
        host.setPowerModel(OFF_MODEL);
        poweredDownHostIds.add(hostId);

        Log.enable();
        Log.printlnConcat(getNow(), ": [Selector] Host #", hostId, " powered down.");
        Log.disable();

    }

    private void processHostPowerUp(SimEvent ev) {

        int hostId = (int) ev.getData();
        PowerHostEntity host = (PowerHostEntity) getHostById(hostId);
        host.setPowerModel(savedPowerModels.remove(hostId));
        poweredDownHostIds.remove(hostId);
        poweringUpHostIds.remove(hostId);

        Log.enable();
        Log.printlnConcat(getNow(), ": [Selector] Host #", hostId, " powered up.");
        Log.disable();

    }
}