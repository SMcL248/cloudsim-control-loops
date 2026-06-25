package org.cloudbus.cloudsim.examples;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;

public class ConstructorVariableVM {

    public static HollowedControl broker;

    /** The cloudlet list. */
	private static List<Cloudlet> cloudletList;

	/** The vmlist. */
	private static List<Vm> vmlist;

    // List of results
    static List<SimulationResult> results = new ArrayList<>();

    // CSV writer
    static PrintWriter csvWriter;

    // Possible VM MIPS capacities
    private static final int[] MIPS_TIERS = {250, 500, 1000};

    // Module library
    static Monitor[] monitorDict = {new monitor_v1(), new monitor_v2(), new monitor_v3()};
    static Analyser[] analyserDict = {new analyser_v1(), new analyser_v2(), new analyser_v3()};
    static Planner[] plannerDict = {new planner_v1(), new planner_v2(), new planner_v3()};
    static Executor[] executorDict = {new executor_v1(), new executor_v2(), new executor_v3()};

    public static void main (String[] args) throws Exception{

        int compatibleCounter = 0;// # of compatible controllers
        int failCounter = 0;// # of failures
        int compatibleAndFailCounter = 0;// # of compatible controllers who failed
        int notCompatibleAndSucceededCounter = 0;// # of non-compatible controllers that succeeded
        int numCombinations = monitorDict.length * analyserDict.length * plannerDict.length * executorDict.length;// total # of combinations

        initCsv();

        // Loop through every possible controller
        for (Monitor m : monitorDict){
            for (Analyser a : analyserDict) {
                for (Planner p : plannerDict){
                    for (Executor e : executorDict){

                        Monitor mFresh = m.getClass().getDeclaredConstructor().newInstance();
                        Analyser aFresh = a.getClass().getDeclaredConstructor().newInstance();
                        Planner pFresh = p.getClass().getDeclaredConstructor().newInstance();
                        Executor eFresh = e.getClass().getDeclaredConstructor().newInstance();

                        // Run Simulation & document results
                        SimulationResult result = runSimulation(mFresh, aFresh, pFresh, eFresh);
                        results.add(result);

                        if (result.compatible()){
                            compatibleCounter++;
                        }

                        if (result.makespan() == -1){
                            failCounter++;
                        }

                        if (result.compatible() && 
                            result.makespan() == -1){
                            compatibleAndFailCounter++;
                        }

                        if (!(result.compatible()) && 
                            result.makespan() != -1){
                            notCompatibleAndSucceededCounter++;
                        }
                    }
                }
            }
        }

        // Print simulation results for each controller
        for (SimulationResult r : results){
                logResult(r);
        }

        csvWriter.close();

        // Print statistics
        Log.printlnConcat((double) compatibleCounter/(numCombinations) * 100, "% of combinations are semantically compatible.");
        Log.printlnConcat((double) failCounter/(numCombinations) * 100, "% of combinations could not be deployed.");
        if (compatibleCounter > 0) {
            Log.printlnConcat((double) compatibleAndFailCounter / compatibleCounter * 100, "% of semantically compatible combinations could not be deployed.");
        }
        if (numCombinations - compatibleCounter > 0) {
            Log.printlnConcat((double) notCompatibleAndSucceededCounter / (numCombinations-compatibleCounter) * 100, "% of non-semantically compatible combinations can be deployed.");
        }

    }

    static void initCsv() throws IOException {
        csvWriter = new PrintWriter(new FileWriter("simulation_results.csv"));
        csvWriter.println("compatible,monitor,analyser,planner,executor,makespan,actionable_cycles,actions_executed,conversion_rate,status");
    }

    static SimulationResult runSimulation(Monitor m, Analyser a, Planner p, Executor e) {

        boolean compatible = m.outputGuid().equals(a.inputGuid()) && 
            a.outputGuid().equals(p.inputGuid()) && 
            p.outputGuid().equals(e.inputGuid());

		try {

            Log.disable();

			// First step: Initialize the CloudSim package. It should be called
			// before creating any entities.
			int num_user = 1;   // number of grid users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false;  // mean trace events

			// Initialize the CloudSim library
			CloudSim.init(num_user, calendar, trace_flag);

			// Second step: Create Datacenters
			//Datacenters are the resource providers in CloudSim. We need at list one of them to run a CloudSim simulation
			Datacenter datacenter0 = createDatacenter("Datacenter_0", 4, 4);

			//Third step: Create Broker
			broker = new HollowedControl("Broker_0", 100, m, a, p, e);
			int brokerId = broker.getId();

			//Fourth step: Create VMs and Cloudlets and send them to broker
			vmlist = createVM(brokerId, 16, 0); //creating 5 vms
			cloudletList = createCloudlet(brokerId, 60, 0); // creating 10 cloudlets

			broker.submitGuestList(vmlist);
			broker.submitCloudletList(cloudletList);

			// Fifth step: Starts the simulation
			CloudSim.startSimulation();

			// Final step: Print results when simulation is over
			List<Cloudlet> newList = broker.getCloudletReceivedList();

            double makespan = newList.stream().mapToDouble(Cloudlet::getExecFinishTime).max().orElse(-1);

			CloudSim.stopSimulation();

            Log.enable();

            return new SimulationResult(m.getClass().getSimpleName(), 
                a.getClass().getSimpleName(), 
                p.getClass().getSimpleName(), 
                e.getClass().getSimpleName(), 
                a.getActionableCycles(), 
                e.getActionsExecuted(),  
                makespan, compatible);

		}
		catch (Exception exception)
		{
			exception.printStackTrace();
            Log.enable();
			Log.println("The simulation has been terminated due to an unexpected error");
            return new SimulationResult(m.getClass().getSimpleName(), 
                a.getClass().getSimpleName(), 
                p.getClass().getSimpleName(), 
                e.getClass().getSimpleName(), 
                a.getActionableCycles(), 
                e.getActionsExecuted(), 
                -1, compatible);    

		}

    }

    private static Datacenter createDatacenter(String name, int numHosts, int pesPerHost) {

        int mips    = 1000;
        int ram     = 16384;
        long storage = 1000000;
        int bw      = 10000;

        List<Host> hostList = new ArrayList<>();

        for (int hostId = 0; hostId < numHosts; hostId++) {

            List<Pe> peList = new ArrayList<>();
            for (int peId = 0; peId < pesPerHost; peId++) {
                peList.add(new Pe(peId, new PeProvisionerSimple(mips)));
            }

            hostList.add(new Host(
                hostId,
                new RamProvisionerSimple(ram),
                new BwProvisionerSimple(bw),
                storage,
                peList,
                new VmSchedulerTimeShared(peList)
            ));
        }

        String arch      = "x86";
        String os        = "Linux";
        String vmm       = "Xen";
        double time_zone = 10.0;
        double cost          = 3.0;
        double costPerMem    = 0.05;
        double costPerStorage = 0.1;
        double costPerBw     = 0.1;

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
            arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);

        Datacenter datacenter = null;
        try {
            datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), new LinkedList<>(), 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return datacenter;
    }

	private static List<Vm> createVM(int userId, int vms, int idShift) {
		//Creates a container to store VMs. This list is passed to the broker later
		LinkedList<Vm> list = new LinkedList<>();

        Random rng = new Random(42);

		//VM Parameters
		long size = 10000; //image size (MB)
		int ram = 512; //vm memory (MB)
		long bw = 1000;
		int pesNumber = 1; //number of cpus
		String vmm = "Xen"; //VMM name

		//create VMs
		Vm[] vm = new Vm[vms];

		for(int i=0;i<vms;i++){
			vm[i] = new Vm(idShift + i, userId, MIPS_TIERS[rng.nextInt(MIPS_TIERS.length)], pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
			list.add(vm[i]);
            Log.println("VM #" + vm[i].getId() + " | MIPS: " + vm[i].getMips());
		}

		return list;

	}

	private static List<Cloudlet> createCloudlet(int userId, int cloudlets, int idShift){
		// Creates a container to store Cloudlets
		LinkedList<Cloudlet> list = new LinkedList<>();

        Random random = new Random(42); // fixed seed

		//cloudlet parameters
		long minLength = 10000;
        long maxLength = 100000;
		long fileSize = 300;
		long outputSize = 300;
		int pesNumber = 1;
		UtilizationModel utilizationModel = new UtilizationModelFull();

		Cloudlet[] cloudlet = new Cloudlet[cloudlets];

		for(int i=0;i<cloudlets;i++){
			cloudlet[i] = new Cloudlet(idShift + i, (long)(minLength + random.nextDouble() * (maxLength - minLength)), pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
			// setting the owner of these Cloudlets
			cloudlet[i].setUserId(userId);
			list.add(cloudlet[i]);
		}

		return list;
	}


    // log results
    static void logResult(SimulationResult result) {

        boolean failed = result.makespan() == -1;
        boolean inert  = !failed && result.actionableCycles() == 0;
        int conversionRate = (!failed && result.actionableCycles() > 0)
                ? (int) Math.round((double) result.actionsExecuted() / result.actionableCycles() * 100)
                : -1;

        // --- console ---
        if (failed) {
            Log.printlnConcat(
                    "Compatible: ", result.compatible(), " [",
                    result.monitorId(), " + ", result.analyserId(), " + ",
                    result.plannerId(), " + ", result.executorId(), "] FAILED");

        } else if (inert) {
            Log.printlnConcat(
                    "Compatible: ", result.compatible(), " [",
                    result.monitorId(), " + ", result.analyserId(), " + ",
                    result.plannerId(), " + ", result.executorId(), "] makespan=",
                    Math.round(result.makespan()), "| actionable cycles=",
                    result.actionableCycles(), "| actions executed=",
                    result.actionsExecuted());

        } else {
            Log.printlnConcat(
                    "Compatible: ", result.compatible(), " [",
                    result.monitorId(), " + ", result.analyserId(), " + ",
                    result.plannerId(), " + ", result.executorId(), "] makespan=",
                    Math.round(result.makespan()), "| actionable cycles=",
                    result.actionableCycles(), "| actions executed=",
                    result.actionsExecuted(), "| conversion rate=",
                    conversionRate, "%");
        }

        // --- csv ---
        String status = failed ? "FAILED" : inert ? "INERT" : "ACTIVE";
        String conversionRateCell = (conversionRate >= 0) ? String.valueOf(conversionRate) : "";
        String makespanCell = failed ? "" : String.valueOf(Math.round(result.makespan()));

        csvWriter.printf("%b,%s,%s,%s,%s,%s,%d,%d,%s,%s%n",
                result.compatible(),
                result.monitorId(),
                result.analyserId(),
                result.plannerId(),
                result.executorId(),
                makespanCell,
                result.actionableCycles(),
                result.actionsExecuted(),
                conversionRateCell,
                status);
    }
    
}
