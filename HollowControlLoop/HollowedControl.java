package org.cloudbus.cloudsim.examples;

import java.util.List;
import java.util.function.Predicate; 

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.VmAllocationPolicy.GuestMapping;
import org.cloudbus.cloudsim.core.CloudActionTags;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.core.HostEntity;


public class HollowedControl<M,D,A> extends DatacenterBroker implements ActionSpace {

    private final Monitor<M> monitor;
    private final Analyser<M,D> analyser;
    private final Planner<D,A> planner;
    private final Executor<A> executor;
    private final int observationRate;

    // Injected at construction, defined where D is concretely known (ConstructorVariableVM).
    // HollowedControl itself never sees LoadState[] — it only ever sees D.
    private final Predicate<D> imbalancePredicate;   // "at least one OVERLOADED or UNDERLOADED"
    private final Predicate<D> opportunityPredicate; // "at least one OVERLOADED AND one UNDERLOADED"

    // Metrics we are attempting to optimize.
    private double groundTruthVarianceSum = 0.0;
    private int groundTruthCycleCount = 0;

    private int imbalanceCycles = 0;  // number of times any imbalance is detected
    private int opportunityCycles = 0; // number of times an action oppertunity is detected
    private int actionsExecuted = 0; // number of times an action is taken

    // Backward-compatible overload: instrumentation is opt-in, defaults to "never true"
    public HollowedControl(String name, int observationRate, Monitor<M> monitor, Analyser<M,D> analyser,
                            Planner<D,A> planner, Executor<A> executor) throws Exception {
        this(name, observationRate, monitor, analyser, planner, executor, null, null);
    }

    public HollowedControl(String name, int observationRate, Monitor<M> monitor, Analyser<M,D> analyser, 
        Planner<D,A> planner, Executor<A> executor, Predicate<D> imbalancePredicate, Predicate<D> opportunityPredicate) 
        throws Exception {
        super(name);
        this.observationRate = observationRate;
        this.monitor = monitor;
        this.analyser = analyser;
        this.planner = planner;
        this.executor = executor;
        this.imbalancePredicate = (imbalancePredicate != null) ? imbalancePredicate : d -> false;
        this.opportunityPredicate = (opportunityPredicate != null) ? opportunityPredicate : d -> false;
    }

    ////////////////////// Overridden methods from DatacenterBroker ////////////////////////////

    @Override
    // Recieves tag and directs to corresponding method
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

    @Override
    // This method returns the datacenter ID for a given VM. It uses the mapping of VMs to datacenters maintained by the broker.
    public Integer getDatacenterFor(GuestEntity vm) {
        return getVmsToDatacentersMap().get(vm.getId());    
    }

    @Override
    // This method sends a cloudlet to a datacenter with a specified delay. It uses the send() method of the broker.
    public void sendCloudlet(int datacenterId, Cloudlet cloudlet) {
        sendNow(datacenterId, CloudActionTags.CLOUDLET_SUBMIT, cloudlet);
    }

    @Override
    // This method sends a cloudlet to a datacenter with a specified delay. It uses the send() method of the broker.
    public void moveCloudlet(Cloudlet cloudlet, GuestEntity fromVm, GuestEntity toVm, int destDatacenterId) {
        int[] data = new int[5];
        data[0] = cloudlet.getCloudletId();
        data[1] = cloudlet.getUserId();
        data[2] = fromVm.getId();
        data[3] = toVm.getId();
        data[4] = destDatacenterId;
        sendNow(getDatacenterFor(fromVm), CloudActionTags.CLOUDLET_MOVE, data);
    }

    @Override
    // Migrate VM to target host
    public void requestVmMigration(GuestEntity vm, HostEntity targetHost){
        GuestMapping payload = new GuestMapping(vm, targetHost);
        send(getDatacenterFor(vm), 0, CloudActionTags.VM_MIGRATE, payload);
    }

    @Override
    // Retrieve the complete list of VMs
    public List<GuestEntity> getVmList() {
        return getGuestsCreatedList();
    }

    @Override
    // Get the ID of this broker
    public Integer getUserId() {
        return getId();
    }

    @Override
    // Create VM
    public void requestVmCreation(GuestEntity newVm, int datacenterId) {
        getGuestList().add(newVm);
        sendNow(datacenterId, CloudActionTags.VM_CREATE, newVm);
    }

    @Override
    // Retrieve the complete list of all hosts
    public List<HostEntity> getAllHosts() {
        return getDatacenterCharacteristicsList().values().iterator().next().getHostList();    
    }

    @Override
    // Retreive the current time
    public double getNow(){
        return CloudSim.clock();
    }

    //////////////////////////// MAPE Cycle //////////////////////////////////////////////

    // This method observes the current state of the system, analyzes it, plans actions if necessary, and executes them.
    private void observeAndAct() {

        if (getCloudletList().isEmpty() && getCloudletSubmittedList().size() == getCloudletReceivedList().size()) {
            return;
        }

        updateGroundTruth();

        M metrics = monitor.observe(this);
        D diagnosis = analyser.analyse(metrics, this);

        // Both counters are pure functions of D, evaluated here at the controller,
        // not self-reported by the analyser implementation.
        if (imbalancePredicate.test(diagnosis))   imbalanceCycles++;
        if (opportunityPredicate.test(diagnosis)) opportunityCycles++;

        A actions = planner.plan(diagnosis, this);
        boolean success = executor.execute(actions, this);

        // success already IS "non-sentinel action executed" — no generic inspection of A needed.
        if (success) {
            actionsExecuted++;
        } else {
            Log.printlnConcat(getNow(), ": The system is balanced. No action required.");
        }

        schedule(getId(), observationRate, CloudActionTags.VM_BROKER_EVENT);
    }

    public int getImbalanceCycles()   { return imbalanceCycles; }
    public int getOpportunityCycles() { return opportunityCycles; }
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
        
}


    


