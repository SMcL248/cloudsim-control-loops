package org.cloudbus.cloudsim.examples;
 
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
 
import org.cloudbus.cloudsim.distributions.ExponentialDistr;
import org.cloudbus.cloudsim.distributions.UniformDistr;
import org.cloudbus.cloudsim.distributions.LognormalDistr;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerVm;
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.power.models.PowerModelLinear;
import org.cloudbus.cloudsim.power.models.PowerModelSpecPowerHpProLiantMl110G3PentiumD930;
import org.cloudbus.cloudsim.power.models.PowerModelSpecPowerHpProLiantMl110G5Xeon3075;
import org.cloudbus.cloudsim.power.models.PowerModelSpecPowerIbmX3550XeonX5675;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.VmAllocationWithSelectionPolicy;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.selectionPolicies.SelectionPolicy;
import org.cloudbus.cloudsim.selectionPolicies.SelectionPolicyCustomRandom;
import org.cloudbus.cloudsim.selectionPolicies.SelectionPolicyLeastFullByCapacity;
 
public class SelectorMultiController {
 
    // Datacenter broker
    public static SelectorNoLogs broker;
 
    // Cloudlet and VM lists
    private static List<Cloudlet> cloudletList;
    private static List<PowerVm> vmlist;
 
    // CSV objects
    static PrintWriter csvWriter;
    static PrintWriter crashWriter;
 
    // === Scenario-construction constants — copied from SelectorScenario.java (2026-07-20) ===

    // VMs are either small, medium-sized or large.
    private static final int[] MIPS_TIERS = {250, 500, 1000};
    private static final int[] RAM_TIERS = {1024, 2048, 4096};
    private static final int[] BW_TIERS = {625, 1250, 2500};

    // Hosts (computers) are either legacy, standard, or modern.
    private static final int[] HOST_MIPS_TIERS = {800, 1000, 1000};
    private static final int[] HOST_RAM_TIERS  = {12288, 16384, 24576};
    private static final int[] HOST_BW_TIERS   = {6000, 10000, 16000};
    private static final PowerModel[] POWER_MODEL_TIERS = {
        new PowerModelSpecPowerHpProLiantMl110G3PentiumD930(),
        new PowerModelSpecPowerHpProLiantMl110G5Xeon3075(),
        new PowerModelSpecPowerIbmX3550XeonX5675()
    };
    
    // VMs or Cloudlets can request 1, 2, or 4 cores to be hosted/ processed on.
    private static final int[] CORE_TIERS = {1, 2, 4};

    // Cost constants
    private static final double COST_PER_SECOND = 1e-5;
    private static final double PRICE_PER_GB_TRANSFER = 0.09;
    private static final double PRICE_PER_GB_SECOND_STORAGE = 3.09e-8;
    private static final double MB_PER_GB = 1024.0;
    private static final double PRICE_PER_KWH = 0.12;
    private static final double WATT_SECONDS_PER_KWH = 3_600_000.0;
 
    // Distribution constants
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
 
    // Simulation size
    private static final int MAX_INJECTIONS = 10;// How many workload injections will there be?
    private static final int NUM_VMS = 12;// Starting VM count
    private static final int NUM_CLOUDLETS = 36;// Starting cloudlet count
    private static final int NUM_HOSTS = 4;// Starting number of quad-core hosts
 
    // Random allocation toggles
    private static final boolean RANDOM_PLACEMENT = true;//Placements of VMs on Hosts
    private static final boolean RANDOM_ASSIGNMENT = true;//Allocation of Cloudlets to VMs

    private static final Random lengthRng = new Random(); //length of cloudlets
    private static final Random fileSizeRng = new Random();// file size of cloudlets
    private static final Random outputSizeRng = new Random();// output file size of cloudlets
    private static final Random vmRng = new Random();// Tier of VM, small, medium, large
    private static final Random vmSizeRng = new Random();// VM memory footprint size
    private static final Random assignRng = new Random();// VM 
    private static final Random hostTierRng = new Random();// Tier of host: legacy, current, modern
    private static final Random repairRng = new Random();// Repair time 
    private static final UniformDistr coreCountDist = new UniformDistr(1, 101);// Used as weighted dice roll for how many cores a cloudlet may request
    private static final Random unrecoverableRng = new Random();// Chance of a host being deemed unrecoverbale after a failure
 
    // === Multi-sim controls ===
    private static final int NUM_SCENARIOS = 1;//How many seeds do you want to run?
    private static final long SCENARIO_SEED_BASE = 42L;//Starting seed
    private static final int PROGRESS_INTERVAL = 10;//Granularity of progress logging

     // === Module Library (LLM Generated)===
    static Monitor[] monitorLib  = {
        new monitor_v1(), new monitor_v2(), new monitor_v3(), new monitor_v4(), new monitor_v5(),
        new monitor_v6(), new monitor_v7(), new monitor_v8(), new monitor_v9(), new monitor_v10()
    };
    static Analyser[] analyserLib  = {
        new analyser_v1(), new analyser_v2(), new analyser_v3(), new analyser_v4(), new analyser_v5(),
        new analyser_v6(), new analyser_v7(), new analyser_v8(), new analyser_v9(), new analyser_v10()
    };
    static Planner[] plannerLib = {
        new planner_v1(), new planner_v2(), new planner_v3(), new planner_v4(), new planner_v5(),
        new planner_v6(), new planner_v7(), new planner_v8(), new planner_v9(), new planner_v10() 
    };
    static Executor[] executorLib  = {
        new executor_v1(), new executor_v2(), new executor_v3(), new executor_v4(), new executor_v5(),
        new executor_v6(), new executor_v7(), new executor_v8(), new executor_v9(), new executor_v10(),
        new executor_v11()
    };

    public static void main(String[] args) throws Exception {
 
        SelectorNoLogs.suppressDebugLogging = false;
 
        // Total controller combinations
        int combosPerScenario = monitorLib.length * analyserLib.length * plannerLib.length * executorLib.length;
        // Total simulations
        int numSimulations = combosPerScenario * NUM_SCENARIOS;

        // Initialise CSV output files
        initCsv();
        initCrashLog();

        // Real-time tracking
        long[] runtimesNanos = new long[numSimulations];
        int runIndex = 0;
        long sweepStartNanos = System.nanoTime();

        // For each scenario, run every controller
        for (int scenarioIdx = 0; scenarioIdx < NUM_SCENARIOS; scenarioIdx++) {

            long scenarioSeed = SCENARIO_SEED_BASE + scenarioIdx;

            long baselineRunStartNanos = System.nanoTime();
            SimResult baseline = runSimulation(new ArrayList<>(), scenarioSeed, "N/A");
            long baselineRunNanos = System.nanoTime() - baselineRunStartNanos;

            // Log-baseline (no controller) stats
            logResult(baseline, scenarioSeed, null, "baseline", baselineRunNanos, "N/A", "N/A", "N/A", "N/A");

            // Simulate every controller combination
            for (Monitor m : monitorLib){
                for (Analyser a : analyserLib) {
                    for (Planner p : plannerLib){
                        for (Executor e : executorLib){

                            String guidCompatible = "False";

                            // Check semantic compatibility, i.e are all modules working at the same level of analysis/ using the same actions
                            if (m.outputGuid() == a.inputGuid() && a.outputGuid() == p.inputGuid() && p.outputGuid() == e.inputGuid()){
                                guidCompatible = "True";
                            }

                            // Build new controller
                            List<ControlUnit> controllerList = new ArrayList<>();
                            ControlUnit controller = new Controller<>("controller", m, a, p, e, null, null, null);
                            controllerList.add(controller);
                            
                            // run the simulation for this controller, track real timings
                            long controlRunStartNanos = System.nanoTime();
                            SimResult control = runSimulation(controllerList, scenarioSeed, guidCompatible);
                            long controlRunNanos = System.nanoTime() - controlRunStartNanos;

                            // log results
                            logResult(control, scenarioSeed, controller, "controller", controlRunNanos,
                                m.getClass().getSimpleName(), a.getClass().getSimpleName(),
                                p.getClass().getSimpleName(), e.getClass().getSimpleName()
                            );

                            // log crash (if applicable)
                            if (control.exceptionClass != null) {
                                logCrash(control, scenarioSeed, m.getClass().getSimpleName(), a.getClass().getSimpleName(),
                                        p.getClass().getSimpleName(), e.getClass().getSimpleName());
                            }

                            // Flush CSVs
                            csvWriter.flush();
                            crashWriter.flush();
                
                            runtimesNanos[runIndex] = controlRunNanos;
                            runIndex++;

                            // Print progress
                            if (runIndex % PROGRESS_INTERVAL == 0 || runIndex == numSimulations) {
                                csvWriter.flush();
                                crashWriter.flush();
                                printProgress(runIndex, numSimulations, sweepStartNanos);
                            }

                        }
                    }
                }
            }
        }
 
        // Close CSVs
        csvWriter.close();
        crashWriter.close();

        // Print complete run summary + timing extrapolations
        long sweepTotalNanos = System.nanoTime() - sweepStartNanos;
        printTimingSummary(runtimesNanos, sweepTotalNanos);

    }
 
    // Print experiment progress
    private static void printProgress(int done, int total, long sweepStartNanos) {
        double elapsedSec = (System.nanoTime() - sweepStartNanos) / 1e9;
        double rate = done / elapsedSec;
        double remainingSec = (total - done) / rate;
        System.out.printf(
            "[progress] %d/%d runs (%.1f%%) | elapsed=%.1fs | rate=%.1f runs/s | ETA=%.1fs%n",
            done, total, 100.0 * done / total, elapsedSec, rate, remainingSec);
    }

    // Print final experiment summary
    private static void printTimingSummary(long[] runtimesNanos, long sweepTotalNanos) {

        if (runtimesNanos.length == 0) {
            System.out.println("No simulations were run (module libraries empty) — skipping timing summary.");
            return;
        }

        long[] sorted = runtimesNanos.clone();
        Arrays.sort(sorted);
        int n = sorted.length;
        double meanMs = Arrays.stream(sorted).average().orElse(0) / 1e6;
        double medianMs = sorted[n / 2] / 1e6;
        double p95Ms = sorted[(int) (n * 0.95)] / 1e6;
        double totalSec = sweepTotalNanos / 1e9;

        System.out.println();
        System.out.printf("runs: %d | mean: %.2fms | median: %.2fms | p95: %.2fms | total: %.1fs (%.2fmin)%n",
            n, meanMs, medianMs, p95Ms, totalSec, totalSec / 60.0);
        System.out.println();
        System.out.println("Extrapolation guide (this machine, single-threaded, steady state):");
        System.out.printf("  10,000 combos x 1  scenario  ~= %.1f min%n", (meanMs * 10_000) / 60_000.0);
        System.out.printf("  10,000 combos x 10 scenarios ~= %.1f min%n", (meanMs * 100_000) / 60_000.0);
    }

    // Initialise general results .csv
    static void initCsv() throws IOException {
        csvWriter = new PrintWriter(new FileWriter("simulation_results_updated.csv"));
        csvWriter.println("scenario_seed,guid_compatible,completed_naturally,crashed,condition,monitor,analyser,planner,executor,compute_cost,energy_cost,bw_cost,storage_cost,controllable_cost,total_cost,"
            + "makespan,total_energy,avg_power,avg_ram_util,peak_ram_util,avg_bw_util,peak_bw_util,"
            + "avg_host_free_mips,avg_host_free_ram,avg_host_free_bw,avg_vm_free_mips,avg_load_variance,"
            + "num_real_failures,peak_simultaneous_failed_hosts,total_downtime,"
            + "num_cloudlets_deferred,total_deferred_wait_time,num_cloudlets_exposed,fraction_exposed,num_cloudlets_abandoned,num_cloudlets_abandoned_power_down,"
            + "cloudlets_still_in_flight,num_cloudlets_still_deferred,min_live_vm_count,live_host_count,completion_rate,"
            + "time_to_execute_nanos,actions_executed");
    }
 
    // Initiaise the crash log .csv
    static void initCrashLog() throws IOException {
        crashWriter = new PrintWriter(new FileWriter("crash_log_updated.csv"));
        crashWriter.println("scenario_seed,guid_compatible,monitor,analyser,planner,executor,exception_class,bridge,exception_message");
    }

    // Log results to .csv
    static void logResult(SimResult r, long scenarioSeed, ControlUnit c, String condition, long timeNanos,
                        String monitor, String analyser, String planner, String executor) {

        int executed = (c == null) ? 0 : c.getActionsExecuted();

        csvWriter.printf("%d,%s,%b,%b,%s,%s,%s,%s,%s,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,"
            + "%d,%d,%.4f,%d,%.4f,%d,%.4f,%d,%d,"
            + "%d,%d,%d,%d,%.4f,"
            + "%d,%d%n",
            scenarioSeed, r.guidCompatible, r.completedNaturally, r.crashed, condition, monitor, analyser, planner, executor,
            r.computeCost, r.energyCost, r.bwCost, r.storageCost, r.controllableCost, r.totalCost,
            r.makespan, r.totalEnergy, r.avgPower,
            r.avgRamUtil, r.peakRamUtil, r.avgBwUtil, r.peakBwUtil,
            r.avgHostFreeMips, r.avgHostFreeRam, r.avgHostFreeBw, r.avgVmFreeMips,
            r.avgLoadVariance,
            r.numRealFailures, r.peakSimultaneousFailedHosts, r.totalDowntime,
            r.numCloudletsDeferred, r.totalDeferredWaitTime, r.numCloudletsExposed, r.fractionExposed, r.numCloudletsAbandoned, 
            r.numCloudletsAbandonedHostPoweredDown,
            r.cloudletsStillInFlight, r.numCloudletsStillDeferred, r.liveVmCount, r.liveHostCount, r.completionRate,
            timeNanos, executed);

    }
 
    // Log crash details to .csv
    static void logCrash(SimResult r, long scenarioSeed, String monitor, String analyser, String planner, String executor) {
        String msg = (r.exceptionMessage == null) ? "" : r.exceptionMessage.replace(",", ";").replace("\n", " ");
        crashWriter.printf("%d,%s,%s,%s,%s,%s,%s,%s,%s%n",
            scenarioSeed, r.guidCompatible, monitor, analyser, planner, executor,
            r.exceptionClass, r.bridge == null ? "" : r.bridge, msg);
        crashWriter.flush();
    }

    // Simulation result object, stores all important metrics
    private static class SimResult {
        double computeCost, energyCost, bwCost, storageCost, controllableCost, totalCost;
        double makespan, totalEnergy, avgPower;
        double avgRamUtil, peakRamUtil, avgBwUtil, peakBwUtil;
        double avgHostFreeMips, avgHostFreeRam, avgHostFreeBw, avgVmFreeMips;
        double avgLoadVariance;
        int numRealFailures, peakSimultaneousFailedHosts, numCloudletsDeferred, numCloudletsExposed, numCloudletsAbandoned, numCloudletsAbandonedHostPoweredDown;
        int cloudletsStillInFlight, numCloudletsStillDeferred, liveVmCount, liveHostCount;
        double totalDowntime, totalDeferredWaitTime, fractionExposed, completionRate;
        String guidCompatible;
        boolean completedNaturally, crashed;
        String exceptionClass, exceptionMessage, bridge;
    }
 
    // Reseed all random objects at the strat of every simulation
    private static void seedAllStreams(long scenarioSeed) {
        lengthRng.setSeed(scenarioSeed ^ 0x94D049BB133111EBL);
        fileSizeRng.setSeed(scenarioSeed ^ 0xFF51AFD7ED558CCDL);
        outputSizeRng.setSeed(scenarioSeed ^ 0xC4CEB9FE1A85EC53L);
        vmRng.setSeed(scenarioSeed ^ 0xD1B54A32D192ED03L);
        vmSizeRng.setSeed(scenarioSeed ^ 0x27D4EB2F165667C5L);
        assignRng.setSeed(scenarioSeed ^ 0x9E3779B97F4A7C15L);
        hostTierRng.setSeed(scenarioSeed ^ 0x9FB21C651E98DF25L);
        coreCountDist.setSeed(scenarioSeed ^ 0x8E1B4CA83278F4A1L);
        repairRng.setSeed(scenarioSeed ^ 0x73D6205122BB088FL);
        unrecoverableRng.setSeed(scenarioSeed ^ 0x5B3E7A9C1D4F602AL);
    }
 
    // Run the simulation with the given controller and the given seed
    static SimResult runSimulation(List<ControlUnit> controllerList, long scenarioSeed, String guidCompatible) throws Exception {
 
        try{
            // Set seeds
            seedAllStreams(scenarioSeed);
            long arrivalSeed = scenarioSeed ^ 0x2545F4914F6CDD1DL;
            long batchSeed = scenarioSeed ^ 0xBF58476D1CE4E5B9L;
            long failArrivalSeed = scenarioSeed ^ 0x688B16AEA1636CEAL;
            long failHostSeed = scenarioSeed ^ 0x254A2AC1AB035645L;
            long selectionPolicySeed = scenarioSeed ^ 0x632BE59BD9B4E019L;
    
            broker = null; // reset before each attempt
            Log.disable();
    
            int num_user = 1;
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;
            CloudSim.init(num_user, calendar, trace_flag);
            CloudSim.terminateSimulation(END_OF_INJECTION_WINDOW * 4); // hard ceiling — any combo that hasn't
                // naturally terminated by 4x the injection window gets forcibly cut off instead of hanging forever
            LognormalDistr repairDurationDist = new LognormalDistr(repairRng, SHAPE_REPAIR, LOG_MEDIAN_REPAIR);
    
            PowerDatacenter datacenter0 = createDatacenter("Datacenter_0", NUM_HOSTS, 4, selectionPolicySeed);
            datacenter0.setDisableMigrations(true);
    
            // Initialise new selector, passing it the latest controller
            broker = new SelectorNoLogs("broker_0", 100, controllerList, MIPS_TIERS, repairDurationDist, unrecoverableRng);
            int brokerId = broker.getId();
    
            // Create VMs and Cloudlets
            vmlist = createVM(brokerId, NUM_VMS, 0);
            cloudletList = createCloudlet(brokerId, NUM_CLOUDLETS, 0);
    
            // Submit Cloudlet and VMs to selector (which acts as the broker)
            broker.submitGuestList(vmlist);
            broker.submitCloudletList(cloudletList);
    
            ExponentialDistr interArrival = new ExponentialDistr(arrivalSeed, MEAN_INTER_ARRIVAL);
            UniformDistr batchSizeDist = new UniformDistr(1, 11, batchSeed);
    
            ExponentialDistr interFailArrival = new ExponentialDistr(failArrivalSeed, MEAN_INTER_FAIL_ARRIVAL); // Poisson process
            UniformDistr failedHostDist = new UniformDistr(0, NUM_HOSTS, failHostSeed); // continuous, rounded below

            // Schedule host failures
            double tFailure = START_BUFFER;
            
            while (true){

                tFailure += interFailArrival.sample();
                if (tFailure > END_OF_INJECTION_WINDOW){break;}
                int hostId = (int) Math.floor(failedHostDist.sample());
                broker.registerFailure(tFailure, hostId);

            }

            // Schedule workload injections
            double t = 0;
            int nextCloudletId = NUM_CLOUDLETS;
            for (int i = 1; i <= MAX_INJECTIONS; i++) {
                t += interArrival.sample();
                int batchSize = (int) Math.floor(batchSizeDist.sample());
                List<Cloudlet> injectedBatch = createCloudlet(brokerId, batchSize, nextCloudletId);
                nextCloudletId += batchSize;
                broker.registerInjection(t, injectedBatch);
            }
    
            // Start simulation
            CloudSim.startSimulation();
    
            List<Cloudlet> newList = broker.getCloudletReceivedList();

            CloudSim.stopSimulation();

            Log.enable();
    
            // Calculate and build results object.
            double makespan = newList.stream().mapToDouble(Cloudlet::getExecFinishTime).max().orElse(-1);
            double cpuTimeTotal = 0;
            double totalDataTransferredMB = 0;
            double vmSizeMB = 0;
    
            for (Cloudlet c : newList) {
                cpuTimeTotal += c.getActualCPUTime();
                totalDataTransferredMB += c.getCloudletFileSize() + c.getCloudletOutputSize();
            }

            for (PowerVm vm : vmlist) {
                vmSizeMB += vm.getSize();
            }
    
            double totalEnergy = datacenter0.getPower();
    
            SimResult r = new SimResult();
            r.computeCost = COST_PER_SECOND * cpuTimeTotal;
            r.energyCost = (totalEnergy / WATT_SECONDS_PER_KWH) * PRICE_PER_KWH;
            r.bwCost = PRICE_PER_GB_TRANSFER * totalDataTransferredMB / MB_PER_GB;
            // Corrected formula (07-limitations.md, "storageCost's formula double-counted VM count") —
            // vmSizeMB is ALREADY the summed total footprint; do not multiply by NUM_VMS again.
            r.storageCost = PRICE_PER_GB_SECOND_STORAGE * (vmSizeMB / MB_PER_GB) * makespan;
            r.controllableCost = r.computeCost + r.energyCost + r.storageCost;
            r.totalCost = r.controllableCost + r.bwCost;
            r.makespan = makespan;
            r.totalEnergy = totalEnergy;
            r.avgPower = totalEnergy / makespan;
            r.avgRamUtil = broker.getAvgRamUtilization();
            r.peakRamUtil = broker.getPeakRamUtilization();
            r.avgBwUtil = broker.getAvgBwUtilization();
            r.peakBwUtil = broker.getPeakBwUtilization();
            r.avgHostFreeMips = broker.getAvgHostFreeMips();
            r.avgHostFreeRam = broker.getAvgHostFreeRam();
            r.avgHostFreeBw = broker.getAvgHostFreeBw();
            r.avgVmFreeMips = broker.getAvgVmFreeMips();
            r.avgLoadVariance = broker.getGroundTruthAvgVariance();
            r.numRealFailures = broker.getNumRealFailures();
            r.peakSimultaneousFailedHosts = broker.getPeakSimultaneousFailedHosts();
            r.totalDowntime = broker.getTotalDowntime();
            r.numCloudletsDeferred = broker.getNumCloudletsDeferred();
            r.totalDeferredWaitTime = broker.getTotalDeferredWaitTime();
            r.numCloudletsExposed = broker.getNumCloudletsExposedToFailure();
            r.numCloudletsAbandoned = broker.getNumCloudletsAbandoned();
            r.numCloudletsAbandonedHostPoweredDown = broker.getNumCloudletsAbandonedHostPoweredDown();
            r.cloudletsStillInFlight = broker.getCloudletsStillInFlight();
            r.numCloudletsStillDeferred = broker.getNumCloudletsStillDeferred();
            r.liveVmCount = broker.getMinLiveVmCount();
            r.liveHostCount = broker.getLiveHostCount();
            int totalAccountedFor = newList.size() + r.numCloudletsAbandoned + r.numCloudletsAbandonedHostPoweredDown
            + r.cloudletsStillInFlight + r.numCloudletsStillDeferred;
            r.completionRate = totalAccountedFor == 0 ? 0.0 : newList.size() / (double) totalAccountedFor;
            r.fractionExposed = newList.isEmpty() ? 0.0 : r.numCloudletsExposed / (double) newList.size();
            r.guidCompatible = guidCompatible;
            r.completedNaturally = broker.getCompletedNaturally();
            r.crashed = false;

            return r;
		
        }catch (Exception e) {
            // Cactch crashes and populate results object to reflect.
            e.printStackTrace();
            Log.enable();
            Log.println("The simulation has been terminated due to an unexpected error");
            Log.disable();
            SimResult r = new SimResult();
            r.makespan = -1;
            r.guidCompatible = guidCompatible;
            r.crashed = true;
            r.exceptionClass = e.getClass().getSimpleName();
            r.exceptionMessage = e.getMessage();
            r.bridge = (e instanceof StructuralMismatchException sme) ? sme.bridge : null;
            return r;
        }
    }
 
    // === Scenario construction — standrad, adapted from base CloudSim ===
 
    private static List<PowerVm> createVM(int userId, int vms, int idShift) {
        LinkedList<PowerVm> list = new LinkedList<>();
        LognormalDistr sizeDist = new LognormalDistr(vmSizeRng, SHAPE_VM_SIZE, LOG_MEDIAN_VM_SIZE);
 
        int pesNumber = 1;
        String vmm = "Xen";
        PowerVm[] vm = new PowerVm[vms];
 
        for (int i = 0; i < vms; i++) {
            int randomInt = vmRng.nextInt(MIPS_TIERS.length);
            vm[i] = new PowerVm(
                idShift + i,
                userId,
                MIPS_TIERS[randomInt],
                CORE_TIERS[randomInt],
                RAM_TIERS[randomInt],
                BW_TIERS[randomInt],
                (long) Math.max(sizeDist.sample(), 500),
                1,
                vmm,
                new CloudletSchedulerTimeShared(),
                1
            );
            list.add(vm[i]);
        }
 
        return list;
    }
 
    private static List<Cloudlet> createCloudlet(int userId, int cloudlets, int idShift) {
        LinkedList<Cloudlet> list = new LinkedList<>();
 
        LognormalDistr lengthDist = new LognormalDistr(lengthRng, SHAPE_LENGTH, LOG_MEDIAN_LENGTH);
        LognormalDistr fileSizeDist = new LognormalDistr(fileSizeRng, SHAPE_SIZE, LOG_MEDIAN_SIZE);
        LognormalDistr outputSizeDist = new LognormalDistr(outputSizeRng, SHAPE_SIZE, LOG_MEDIAN_SIZE);
 
        //int pesNumber = 1;
        UtilizationModel utilizationModel = new UtilizationModelFull();
        Cloudlet[] cloudlet = new Cloudlet[cloudlets];
 
        for (int i = 0; i < cloudlets; i++) {
            long length = (long) Math.max(lengthDist.sample(), 1000);
            long fileSize = (long) Math.max(fileSizeDist.sample(), 1);
            long outputSize = (long) Math.max(outputSizeDist.sample(), 1);
            int roll = (int) Math.floor(coreCountDist.sample());


            // Weigh Uniform Dist sample:
            // 80% chance of Cloudlet requesting 1 Core
            // 15% chance of requesting 2 Cores
            // 5% chance of requesting 4 Cores
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
            cloudlet[i].setUserId(userId);
            if (RANDOM_ASSIGNMENT) {
                cloudlet[i].setGuestId(assignRng.nextInt(NUM_VMS));
            }
            list.add(cloudlet[i]);
        }
 
        return list;
    }
 
    private static PowerDatacenter createDatacenter(String name, int numHosts, int pesPerHost, long selectionPolicySeed) {
 
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
 
        String arch = "x86";
        String os = "Linux";
        String vmm = "Xen";
        double time_zone = 10.0;
        double cost = COST_PER_SECOND;
        double costPerMem = 0.05;
        double costPerStorage = 0.1;
        double costPerBw = 0.1;
 
        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
            arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);
 
        SelectionPolicy<HostEntity> selectionPolicy = RANDOM_PLACEMENT
            ? new SelectionPolicyCustomRandom<>(selectionPolicySeed)
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

}