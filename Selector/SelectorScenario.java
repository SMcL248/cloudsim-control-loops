/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation
 *               of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.examples;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.cloudbus.cloudsim.distributions.ExponentialDistr;
import org.cloudbus.cloudsim.distributions.UniformDistr;
import org.cloudbus.cloudsim.distributions.LognormalDistr;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.power.models.PowerModelLinear;
import org.cloudbus.cloudsim.power.models.PowerModelSpecPowerHpProLiantMl110G3PentiumD930;
import org.cloudbus.cloudsim.power.models.PowerModelSpecPowerHpProLiantMl110G5Xeon3075;
import org.cloudbus.cloudsim.power.models.PowerModelSpecPowerIbmX3550XeonX5675;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.UtilizationModelNull;
import org.cloudbus.cloudsim.UtilizationModelPlanetLabInMemory;
import org.cloudbus.cloudsim.UtilizationModelStochastic;
import org.cloudbus.cloudsim.power.PowerVm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmAllocationWithSelectionPolicy;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.selectionPolicies.SelectionPolicy;
import org.cloudbus.cloudsim.selectionPolicies.SelectionPolicyCustomRandom;
import org.cloudbus.cloudsim.selectionPolicies.SelectionPolicyFirstFit;
import org.cloudbus.cloudsim.selectionPolicies.SelectionPolicyLeastFullByCapacity;

/**
 * An example showing how to pause and resume the simulation,
 * and create simulation entities (a DatacenterBroker in this example)
 * dynamically.
 */
public class SelectorScenario {

    public static Selector broker;

	/** The cloudlet list. */
	private static List<Cloudlet> cloudletList;

	/** The vmlist. */
	private static List<PowerVm> vmlist;

    private static final long SEED = 42L; 

	// Possible VM MIPS capacities
    private static final int[] MIPS_TIERS = {250, 500, 1000};
    private static final int[] RAM_TIERS = {1024, 2048, 4096};
    private static final int[] BW_TIERS = {625, 1250, 2500};
    private static final int[] CORE_TIERS = {1, 2, 4};
    private static final int[] HOST_MIPS_TIERS = {800, 1000, 1000};   // Legacy, Standard, Modern
    private static final int[] HOST_RAM_TIERS  = {12288, 16384, 24576};
    private static final int[] HOST_BW_TIERS   = {6000, 10000, 16000};
    private static final PowerModel[] POWER_MODEL_TIERS = {
        new PowerModelSpecPowerHpProLiantMl110G3PentiumD930(),
        new PowerModelSpecPowerHpProLiantMl110G5Xeon3075(),
        new PowerModelSpecPowerIbmX3550XeonX5675()
    };
    private static final long HOST_TIER_SEED = SEED ^ 0x9FB21C651E98DF25L;
    private static final Random hostTierRng = new Random(HOST_TIER_SEED);   

    private static final double COST_PER_SECOND = 1e-5;
    private static final double PRICE_PER_GB_TRANSFER = 0.09;   
    private static final double PRICE_PER_GB_SECOND_STORAGE = 3.09e-8;
    private static final double MB_PER_GB = 1024.0;     
    private static final double PRICE_PER_KWH = 0.12;
    private static final double WATT_SECONDS_PER_KWH = 3_600_000.0;

    private static final long ARRIVAL_SEED = SEED ^ 0x2545F4914F6CDD1DL;
    private static final long BATCH_SEED = SEED ^ 0xBF58476D1CE4E5B9L;
    private static final long FAIL_ARRIVAL_SEED = SEED ^ 0x688B16AEA1636CEAL;   // task #2 — failure inter-arrival (Exponential)
    private static final long FAIL_HOST_SEED = SEED ^ 0x254A2AC1AB035645L;  // task #2 — which host fails (Uniform)
    private static final long CLOUDLET_PE_COUNT_SEED = SEED ^ 0x8E1B4CA83278F4A1L;
    private static final UniformDistr coreCountDist = new UniformDistr(1, 101, CLOUDLET_PE_COUNT_SEED);
    private static final Random lengthRng = new Random(SEED ^ 0x94D049BB133111EBL);
    private static final Random fileSizeRng = new Random(SEED ^ 0xFF51AFD7ED558CCDL);
    private static final Random outputSizeRng = new Random(SEED ^ 0xC4CEB9FE1A85EC53L);
    private static final Random vmRng = new Random(SEED ^ 0xD1B54A32D192ED03L);
    private static final Random vmSizeRng = new Random(SEED ^ 0x27D4EB2F165667C5L);
    private static final Random assignRng = new Random(SEED ^ 0x9E3779B97F4A7C15L);
    private static final Random repairRng = new Random(SEED ^ 0x73D6205122BB088FL);  
    private static final Random unrecoverableRng = new Random(SEED ^ 0x5B3E7A9C1D4F602AL);

    private static final int MAX_INJECTIONS = 10;
    private static final double MEAN_INTER_ARRIVAL = 500.0;
    private static final double MEAN_INTER_FAIL_ARRIVAL = 3000;
    private static final double END_OF_INJECTION_WINDOW = 8500;
    private static final double START_BUFFER = 50;
    private static final double LOG_MEDIAN_LENGTH = Math.log(50_000.0);
    private static final double SHAPE_LENGTH = 1.0;
    private static final double LOG_MEDIAN_SIZE = Math.log(20.0);
    private static final double SHAPE_SIZE = 1.3;
    private static final double LOG_MEDIAN_VM_SIZE = Math.log(10_000.0);
    private static final double SHAPE_VM_SIZE = 1.0;    
    private static final double LOG_MEDIAN_REPAIR = Math.log(800.0);  
    private static final double SHAPE_REPAIR = 0.6;

	private static final int NUM_VMS = 12;
	private static final int NUM_CLOUDLETS = 36;
	private static final int NUM_HOSTS = 4;

    // Random allocation of entites switch
	private static final boolean RANDOM_PLACEMENT = true;// Should VMs be randomly allocated to hosts?
	private static final boolean RANDOM_ASSIGNMENT = true;// Should cloudlets be randomly allocated to VMs?

	private static List<PowerVm> createVM(int userId, int vms, int idShift) {
		//Creates a container to store VMs. This list is passed to the broker later
		LinkedList<PowerVm> list = new LinkedList<>();

        LognormalDistr sizeDist = new LognormalDistr(vmSizeRng, SHAPE_VM_SIZE, LOG_MEDIAN_VM_SIZE);

		//VM Parameters
		String vmm = "Xen"; //VMM name

		//create VMs
		PowerVm[] vm = new PowerVm[vms];

		for(int i=0;i<vms;i++){
            int randomInt = vmRng.nextInt(MIPS_TIERS.length);
			vm[i] = new PowerVm(
                idShift + i, 
                userId, 
                MIPS_TIERS[randomInt], 
                CORE_TIERS[randomInt], 	
                RAM_TIERS[randomInt], 
                BW_TIERS[randomInt], 
                (long) Math.max(sizeDist.sample(),500), 
                1,
                vmm, 
                new CloudletSchedulerTimeShared(), 
                1
            );
			list.add(vm[i]);
            Log.println("VM #" + vm[i].getId() + " | MIPS: " + vm[i].getMips());
		}

		return list;

	}

    private static List<Cloudlet> createCloudlet(int userId, int cloudlets, int idShift){

        // Creates a container to store Cloudlets
        LinkedList<Cloudlet> list = new LinkedList<>();

        LognormalDistr lengthDist = new LognormalDistr(lengthRng, SHAPE_LENGTH, LOG_MEDIAN_LENGTH); 
        LognormalDistr fileSizeDist = new LognormalDistr(fileSizeRng, SHAPE_SIZE, LOG_MEDIAN_SIZE);
        LognormalDistr outputSizeDist = new LognormalDistr(outputSizeRng, SHAPE_SIZE, LOG_MEDIAN_SIZE); 

        //cloudlet parameters   
        //int pesNumber = 1;
        UtilizationModel utilizationModel = new UtilizationModelFull();

        Cloudlet[] cloudlet = new Cloudlet[cloudlets];

        for(int i=0;i<cloudlets;i++){

            long length = (long) Math.max(lengthDist.sample(),1000);
            long fileSize = (long) Math.max(fileSizeDist.sample(),1);
            long outputSize = (long) Math.max(outputSizeDist.sample(),1);
            int roll = (int) Math.floor(coreCountDist.sample());

            int pesNumber = (roll <= 80) ? CORE_TIERS[0]
            : (roll <= 95) ? CORE_TIERS[1]          
            : CORE_TIERS[2];

            cloudlet[i] = new Cloudlet(
                idShift + i, 
                length, 
                pesNumber, 
                fileSize, 
                outputSize, 
                utilizationModel, 
                utilizationModel, 
                utilizationModel
            );
            // setting the owner of these Cloudlets
            cloudlet[i].setUserId(userId);
            if (RANDOM_ASSIGNMENT) {
                cloudlet[i].setGuestId(assignRng.nextInt(NUM_VMS));// VM ids are 0..numVms-1 with idShift 0
            }
            list.add(cloudlet[i]);
        }

        return list;
    }

	////////////////////////// STATIC METHODS ///////////////////////

	/**
	 * Creates main() to run this example
	 */
	public static void main(String[] args) {
		Log.println("Starting SelectorScenario...");

		try {
			// First step: Initialize the CloudSim package. It should be called
			// before creating any entities.
			int num_user = 1;   // number of grid users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false;  // mean trace events

			// Initialize the CloudSim library
			CloudSim.init(num_user, calendar, trace_flag);

			// Second step: Create Datacenters
			//Datacenters are the resource providers in CloudSim. We need at list one of them to run a CloudSim simulation
			PowerDatacenter datacenter0 = createDatacenter("Datacenter_0", NUM_HOSTS, 4);

            datacenter0.setDisableMigrations(true);//Disable CloudSim native migration

            ControlUnit controller1 = new Controller<>(
                "controller-mips-scaler", 
                new monitor_ETC(), 
                new analyser_ETC2(), 
                new Planner6(), 
                new Executor6(), 
                null, 
                null, 
                null);

            ControlUnit controller2 = new Controller<>(
                "controller-pe-scaler", 
                new monitor_ETC(), 
                new analyser_ETC3(), 
                new Planner7(), 
                new Executor7(), 
                null, 
                null, 
                null);

            ControlUnit controller3 = new Controller<>(
                "controller-pe-descaler", 
                new monitor_ETC(), 
                new analyser_ETC4(), 
                new Planner8(), 
                new Executor8(), 
                null, 
                null, 
                null); 

            ControlUnit controller4 = new Controller<>(
                "controller-load-balancer", 
                new monitor_v1(), 
                new Analyser8(),
                new Planner4(), 
                new Executor5(), 
                null, 
                null, 
                null);

            List<ControlUnit> controllerList = new ArrayList<>();

            // Add controllers to controller list.
            // Comment out to simulate no controller.
            controllerList.add(controller1);
            controllerList.add(controller2);
            controllerList.add(controller3);
            controllerList.add(controller4); 

            LognormalDistr repairDurationDist = new LognormalDistr(repairRng, SHAPE_REPAIR, LOG_MEDIAN_REPAIR);

			//Third step: Create Broker
			broker = new Selector("broker_0", 
                100,
                controllerList,
				MIPS_TIERS,
                repairDurationDist,
                unrecoverableRng
            );

			int brokerId = broker.getId();

			//Fourth step: Create VMs and Cloudlets and send them to broker
			vmlist = createVM(brokerId, NUM_VMS, 0); //creating 5 vms
			cloudletList = createCloudlet(brokerId, NUM_CLOUDLETS, 0); // creating 10 cloudlets

			broker.submitGuestList(vmlist);
			broker.submitCloudletList(cloudletList);

			Log.disable();

            // Distributions for stochastic failures and workload injections
            ExponentialDistr interArrival = new ExponentialDistr(ARRIVAL_SEED, MEAN_INTER_ARRIVAL); // Poisson process
            UniformDistr batchSizeDist = new UniformDistr(1, 11, BATCH_SEED); // continuous, rounded below

            ExponentialDistr interFailArrival = new ExponentialDistr(FAIL_ARRIVAL_SEED, MEAN_INTER_FAIL_ARRIVAL); // Poisson process
            UniformDistr failedHostDist = new UniformDistr(0, NUM_HOSTS, FAIL_HOST_SEED); // continuous, rounded below

            // Schedule host failure
            double tFailure = START_BUFFER;
            
            while (true){

                tFailure += interFailArrival.sample();
                if (tFailure > END_OF_INJECTION_WINDOW){break;}
                int hostId = (int) Math.floor(failedHostDist.sample());
                System.out.println("Host #" + hostId + " scheduled for failure at time " + tFailure);
                broker.registerFailure(tFailure, hostId);

            }

            // Schedule Cloudlet workload injections
            double t = 0;
            int nextCloudletId = NUM_CLOUDLETS; // continue ID numbering after the initial 36

            for (int i = 1; i <= MAX_INJECTIONS; i++) {
                t += interArrival.sample();

                int batchSize = (int) Math.floor(batchSizeDist.sample());

                List<Cloudlet> injectedBatch = createCloudlet(brokerId, batchSize, nextCloudletId);
                nextCloudletId += batchSize;

                broker.registerInjection(t, injectedBatch);
            }

            
			// Fifth step: Starts the simulation
			CloudSim.startSimulation();

			// Final step: Print results when simulation is over
			List<Cloudlet> newList = broker.getCloudletReceivedList();

			CloudSim.stopSimulation();
			Log.enable();

			printCloudletList(newList);

            double makespan = newList.stream().mapToDouble(Cloudlet::getExecFinishTime).max().orElse(-1);
            double cpuTimeTotal = 0;
            double totalDataTransferredMB = 0;
            double vmSizeMB = 0;

            for(Cloudlet c : newList){
                cpuTimeTotal += c.getActualCPUTime();
                totalDataTransferredMB += c.getCloudletFileSize() + c.getCloudletOutputSize();
            }

            for (PowerVm vm : vmlist){
                vmSizeMB += vm.getSize();
            }
    
            double totalEnergy = datacenter0.getPower();
            double computeCost = COST_PER_SECOND * cpuTimeTotal;
            double energyCost = (totalEnergy/WATT_SECONDS_PER_KWH) * PRICE_PER_KWH;
            double bwCost = PRICE_PER_GB_TRANSFER * totalDataTransferredMB / MB_PER_GB;
            double storageCost = PRICE_PER_GB_SECOND_STORAGE * (vmSizeMB / MB_PER_GB) * makespan;

            Log.formatLine("Total compute cost: $%.4f", computeCost);
            Log.formatLine("Total energy cost: $%.4f", energyCost);
            Log.formatLine("Total BW cost: $%.4f", bwCost);
            Log.formatLine("Total storage cost: $%.4f", storageCost);   
            Log.formatLine("Total controllable cost: $%.4f", storageCost + computeCost + energyCost);
            Log.formatLine("Total cost: $%.4f", storageCost + computeCost + energyCost + bwCost);
            Log.formatLine("Total data transferred: %.4f MB", totalDataTransferredMB);
            Log.formatLine("Total VM disk footprint: %.4f MB", vmSizeMB);
            Log.formatLine("Average RAM util: %.2f%%", broker.getAvgRamUtilization() * 100);
            Log.formatLine("Peak RAM util: %.2f%%", broker.getPeakRamUtilization() * 100);
            Log.formatLine("Average BW util: %.2f%%", broker.getAvgBwUtilization() * 100);
            Log.formatLine("Peak BW util: %.2f%%", broker.getPeakBwUtilization() * 100);
            Log.formatLine("Makespan: %.4f", makespan);
            Log.formatLine("Total energy: %.4f W*sec", + totalEnergy);
            Log.formatLine("Average power: %.4f W", totalEnergy/makespan);
			Log.println("SelectorScenario finished!");

		}
		catch (Exception e)
		{
			e.printStackTrace();
			Log.println("The simulation has been terminated due to an unexpected error");
		}
	}

    private static PowerDatacenter createDatacenter(String name, int numHosts, int pesPerHost) {

        long storage = 1000000;

        List<FixedPowerHost> hostList = new ArrayList<>();

        for (int hostId = 0; hostId < numHosts; hostId++) {

            int tier = hostTierRng.nextInt(HOST_MIPS_TIERS.length);
            int mips = HOST_MIPS_TIERS[tier];
            int ram  = HOST_RAM_TIERS[tier];
            int bw   = HOST_BW_TIERS[tier];
            PowerModel powerModel = POWER_MODEL_TIERS[tier];

            List<Pe> peList = new ArrayList<>();
            for (int peId = 0; peId < pesPerHost; peId++) {
                peList.add(new Pe(peId, new PeProvisionerSimple(mips)));
            }

            hostList.add(new FixedPowerHost(
                hostId,
                new RamProvisionerSimple(ram),
                new BwProvisionerSimple(bw),
                storage,
                peList,
                new VmSchedulerTimeShared(peList),
                powerModel
            ));
        }
        
        String arch      = "x86";
        String os        = "Linux";
        String vmm       = "Xen";
        double time_zone = 10.0;
        double cost          = COST_PER_SECOND;
        double costPerMem    = 0.05;
        double costPerStorage = 0.1;
        double costPerBw     = 0.1;

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
            arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);

        SelectionPolicy<HostEntity> selectionPolicy = (RANDOM_PLACEMENT) 
			? new SelectionPolicyCustomRandom<>(SEED ^ 0x632BE59BD9B4E019L) 
			: new SelectionPolicyLeastFullByCapacity<>();

        PowerDatacenter datacenter = null;
        try {
            datacenter = new PowerDatacenter(
                name, 
                characteristics, 
                new VmAllocationWithSelectionPolicy(hostList, selectionPolicy), 
                new LinkedList<>(), 
                1
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

        return datacenter;
    }

	/**
	 * Prints the Cloudlet objects
	 * @param list  list of Cloudlets
	 */
	private static void printCloudletList(List<Cloudlet> list) {
		int size = list.size();
		Cloudlet cloudlet;

		String indent = "    ";
		Log.println();
		Log.println("========== OUTPUT ==========");
		Log.println("Cloudlet ID" + indent + "STATUS" + indent +
				"Data center ID" + indent + "VM ID" + indent + indent + "Time" + indent + "Start Time" + indent + "Finish Time");

		DecimalFormat dft = new DecimalFormat("###.##");
        for (Cloudlet value : list) {
            cloudlet = value;
            Log.print(indent + cloudlet.getCloudletId() + indent + indent);

            if (cloudlet.getStatus() == Cloudlet.CloudletStatus.SUCCESS) {
                Log.print("SUCCESS");

                Log.println(indent + indent + cloudlet.getResourceId() + indent + indent + indent + cloudlet.getGuestId() +
                        indent + indent + indent + dft.format(cloudlet.getActualCPUTime()) +
                        indent + indent + dft.format(cloudlet.getExecStartTime()) + indent + indent + indent + dft.format(cloudlet.getExecFinishTime()));

            }
        }

	}

}