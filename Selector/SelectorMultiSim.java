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
 
/**
 * Multi-scenario/multi-seed sweep harness for SelectorScenario's fixed controller list
 * (controller-mips-scaler / controller-pe-scaler / controller-pe-descaler / controller-load-balancer).
 *
 * Unlike PowerConstructorMultiSim (which sweeps a 5x5x5 GRID of swappable GUID-matched
 * modules), this file has nothing to grid-search — the controller list is fixed. The only
 * axis being swept is the scenario seed, and the only comparison per seed is baseline
 * (no controller) vs. controller (the standard four-controller rotation).
 *
 * Scenario-construction code (createVM / createCloudlet / createDatacenter, and every
 * constant above them) is copied from SelectorScenario.java as of 2026-07-20, including
 * the corrected (non-double-counted) storage cost formula and the host-tier heterogeneity
 * pass. THIS IS A MANUALLY-SYNCED COPY, NOT SHARED CODE — SelectorScenario.java is the
 * authoritative single-run reference (00-INDEX.md / 05-scenarios.md); re-diff against it
 * before trusting a sweep result if either file has changed since.
 *
 * Companion change required in Selector.java, not yet applied there: logVmAllocation()
 * prints via raw System.out.println (deliberately bypassing Log.disable()/enable(), per
 * 04-family-power.md's "VM-allocation logging" note), so it will NOT be suppressed by this
 * file's Log.disable() calls and will spam ~2 dumps x 2 conditions x NUM_SCENARIOS runs of
 * console output. Add to Selector.java:
 *
 *     public static boolean suppressDebugLogging = false;
 *
 *     private void logVmAllocation(ReadSpace readSpace) {
 *         if (suppressDebugLogging) return;
 *         ... (unchanged)
 *     }
 *
 * and this file sets Selector.suppressDebugLogging = true at the top of main().
 */
public class SelectorMultiSim {
 
    public static Selector broker;
 
    private static List<Cloudlet> cloudletList;
    private static List<PowerVm> vmlist;
 
    static PrintWriter csvWriter;
 
    // === Scenario-construction constants — copied from SelectorScenario.java (2026-07-20) ===
    // Architecure constants (low, mid, high)
    private static final int[] MIPS_TIERS = {250, 500, 1000};
    private static final int[] RAM_TIERS = {1024, 2048, 4096};
    private static final int[] BW_TIERS = {625, 1250, 2500};
    private static final int[] HOST_MIPS_TIERS = {800, 1000, 1000};   // Legacy, Standard, Modern
    private static final int[] HOST_RAM_TIERS  = {12288, 16384, 24576};
    private static final int[] HOST_BW_TIERS   = {6000, 10000, 16000};
        private static final PowerModel[] POWER_MODEL_TIERS = {
        new PowerModelSpecPowerHpProLiantMl110G3PentiumD930(),
        new PowerModelSpecPowerHpProLiantMl110G5Xeon3075(),
        new PowerModelSpecPowerIbmX3550XeonX5675()
    };
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
    private static final int MAX_INJECTIONS = 10;
    private static final int NUM_VMS = 12;
    private static final int NUM_CLOUDLETS = 36;
    private static final int NUM_HOSTS = 4;
 
    // Random allocation toggles
    private static final boolean RANDOM_PLACEMENT = true;
    private static final boolean RANDOM_ASSIGNMENT = true;
 
    // Re-seeded once per runSimulation() call via seedAllStreams(scenarioSeed) — NOT per
    // createVM/createCloudlet call. Must stay persistent WITHIN one run (createCloudlet is
    // called 11 times per run: 1 initial batch + 10 injections) — this is the exact
    // "RNG recreated per call" bug already root-caused in this project (07-limitations.md).
    // Reseeding must happen only BETWEEN runs, never inside the per-cloudlet-batch loop.
    private static final Random lengthRng = new Random();
    private static final Random fileSizeRng = new Random();
    private static final Random outputSizeRng = new Random();
    private static final Random vmRng = new Random();
    private static final Random vmSizeRng = new Random();
    private static final Random assignRng = new Random();
    private static final Random hostTierRng = new Random();
    private static final Random repairRng = new Random();
    private static final UniformDistr coreCountDist = new UniformDistr(1, 101);
    private static final Random unrecoverableRng = new Random();
 
    // === Multi-sim controls ===
    private static final int NUM_SCENARIOS = 10;
    private static final long SCENARIO_SEED_BASE = 42L;
 
    public static void main(String[] args) throws Exception {
 
        Selector.suppressDebugLogging = true; // requires the Selector.java companion change above
 
        initCsv();
 
        double[] totalCostPct = new double[NUM_SCENARIOS];
        double[] controllableCostPct = new double[NUM_SCENARIOS];
        double[] makespanPct = new double[NUM_SCENARIOS];
        double[] energyPct = new double[NUM_SCENARIOS];
 
        long sweepStartNanos = System.nanoTime();
 
        for (int i = 0; i < NUM_SCENARIOS; i++) {
 
            long scenarioSeed = SCENARIO_SEED_BASE + i;
 
            SimResult baseline = runSimulation(scenarioSeed, false);
            SimResult controller = runSimulation(scenarioSeed, true);
 
            logResult(baseline, scenarioSeed, "baseline");
            logResult(controller, scenarioSeed, "controller");
            csvWriter.flush();
 
            totalCostPct[i] = pctChange(baseline.totalCost, controller.totalCost);
            controllableCostPct[i] = pctChange(baseline.controllableCost, controller.controllableCost);
            makespanPct[i] = pctChange(baseline.makespan, controller.makespan);
            energyPct[i] = pctChange(baseline.totalEnergy, controller.totalEnergy);
 
            System.out.printf(
                "[scenario %d/%d, seed=%d] total cost %.2f%% | controllable cost %.2f%% | makespan %.2f%% | energy %.2f%%%n",
                i + 1, NUM_SCENARIOS, scenarioSeed,
                totalCostPct[i], controllableCostPct[i], makespanPct[i], energyPct[i]);
        }
 
        csvWriter.close();
 
        double elapsedSec = (System.nanoTime() - sweepStartNanos) / 1e9;
        System.out.printf("%nSweep complete: %d scenarios (%d runs) in %.1fs%n%n", NUM_SCENARIOS, NUM_SCENARIOS * 2, elapsedSec);
 
        printSummary("Total cost", totalCostPct);
        printSummary("Controllable cost", controllableCostPct);
        printSummary("Makespan", makespanPct);
        printSummary("Total energy", energyPct);
    }
 
    // % change vs. that scenario's own baseline — the normalization convention already
    // decided in 00-INDEX.md/04-family-power.md, since cost/makespan/energy are all
    // extensive quantities that shouldn't be pooled as raw totals across seeds.
    private static double pctChange(double baselineVal, double controllerVal) {
        if (baselineVal == 0) return 0.0;
        return (controllerVal - baselineVal) / baselineVal * 100.0;
    }
 
    private static void printSummary(String label, double[] pctChanges) {
        double mean = Arrays.stream(pctChanges).average().orElse(0.0);
        double variance = Arrays.stream(pctChanges).map(x -> (x - mean) * (x - mean)).sum() / pctChanges.length;
        double sd = Math.sqrt(variance);
        System.out.printf("%-20s mean %% change = %6.2f%%  (sd %.2f%%, n=%d scenarios)%n", label + ":", mean, sd, pctChanges.length);
    }
 
    static void initCsv() throws IOException {
        csvWriter = new PrintWriter(new FileWriter("simulation_results_selector_multi_final.csv"));
        csvWriter.println("scenario_seed,condition,compute_cost,energy_cost,bw_cost,storage_cost,controllable_cost,total_cost,"
            + "makespan,total_energy,avg_power,avg_ram_util,peak_ram_util,avg_bw_util,peak_bw_util,"
            + "num_real_failures,peak_simultaneous_failed_hosts,total_downtime,"
            + "num_cloudlets_deferred,total_deferred_wait_time,num_cloudlets_exposed,fraction_exposed");
    }
 
    static void logResult(SimResult r, long scenarioSeed, String condition) {
        csvWriter.printf("%d,%s,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,"
                + "%d,%d,%.4f,%d,%.4f,%d,%.4f%n",
            scenarioSeed, condition,
            r.computeCost, r.energyCost, r.bwCost, r.storageCost, r.controllableCost, r.totalCost,
            r.makespan, r.totalEnergy, r.avgPower,
            r.avgRamUtil, r.peakRamUtil, r.avgBwUtil, r.peakBwUtil,
            r.numRealFailures, r.peakSimultaneousFailedHosts, r.totalDowntime,
            r.numCloudletsDeferred, r.totalDeferredWaitTime, r.numCloudletsExposed, r.fractionExposed);
    }
 
    // Mirrors every metric SelectorScenario.java's main() prints — packaged for
    // programmatic baseline-vs-controller comparison instead of console output.
    private static class SimResult {
        double computeCost, energyCost, bwCost, storageCost, controllableCost, totalCost;
        double makespan, totalEnergy, avgPower;
        double avgRamUtil, peakRamUtil, avgBwUtil, peakBwUtil;
        int numRealFailures, peakSimultaneousFailedHosts, numCloudletsDeferred, numCloudletsExposed;
        double totalDowntime, totalDeferredWaitTime, fractionExposed;
    }
 
    // Reseeds every persistent RNG stream from scenarioSeed. Called once at the top of
    // runSimulation() — NOT inside createVM/createCloudlet — so baseline and controller
    // runs of the SAME scenarioSeed draw an IDENTICAL VM/cloudlet/host population, and only
    // the controller list differs. Matches the "every simulation-fidelity knob held
    // identical except the thing under test" discipline (07-limitations.md, 2026-07-15).
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
 
    // useController=false -> empty controller list (baseline, ground truth still tracked —
    // Selector.mapeCycle() calls updateGroundTruth() before its `selected != null` guard).
    // useController=true  -> the standard four-controller rotation, identical to
    // SelectorScenario.java's controllerList.
    static SimResult runSimulation(long scenarioSeed, boolean useController) throws Exception {
 
        // Set seeds
        seedAllStreams(scenarioSeed);
        long arrivalSeed = scenarioSeed ^ 0x2545F4914F6CDD1DL;
        long batchSeed = scenarioSeed ^ 0xBF58476D1CE4E5B9L;
        long failArrivalSeed = scenarioSeed ^ 0x688B16AEA1636CEAL;
        long failHostSeed = scenarioSeed ^ 0x254A2AC1AB035645L;
        long selectionPolicySeed = scenarioSeed ^ 0x632BE59BD9B4E019L;
        
        broker = null; // reset before each attempt — same defensive pattern as PowerConstructorMultiSim
        Log.disable();
 
        int num_user = 1;
        Calendar calendar = Calendar.getInstance();
        boolean trace_flag = false;
        CloudSim.init(num_user, calendar, trace_flag);
        LognormalDistr repairDurationDist = new LognormalDistr(repairRng, SHAPE_REPAIR, LOG_MEDIAN_REPAIR);
 
        PowerDatacenter datacenter0 = createDatacenter("Datacenter_0", NUM_HOSTS, 4, selectionPolicySeed);
        datacenter0.setDisableMigrations(true);
 
        List<ControlUnit> controllerList = new ArrayList<>();
        if (useController) {
            controllerList.add(new Controller<>("controller-mips-scaler",
                new monitor_ETC(), new analyser_ETC2(), new Planner6(), new Executor6(), null, null, null));
            controllerList.add(new Controller<>("controller-pe-scaler",
                new monitor_ETC(), new analyser_ETC3(), new Planner7(), new Executor7(), null, null, null));
            controllerList.add(new Controller<>("controller-pe-descaler",
                new monitor_ETC(), new analyser_ETC4(), new Planner8(), new Executor8(), null, null, null));
            controllerList.add(new Controller<>("controller-load-balancer",
                new monitor_v1(), new Analyser8(), new Planner4(), new Executor5(), null, null, null));
        }
 
        broker = new Selector("broker_0", 100, controllerList, MIPS_TIERS, repairDurationDist, unrecoverableRng);
        int brokerId = broker.getId();
 
        vmlist = createVM(brokerId, NUM_VMS, 0);
        cloudletList = createCloudlet(brokerId, NUM_CLOUDLETS, 0);
 
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
            System.out.println("Host #" + hostId + " scheduled for failure at time " + tFailure);
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
 
        CloudSim.startSimulation();
 
        List<Cloudlet> newList = broker.getCloudletReceivedList();
        CloudSim.stopSimulation();
        Log.enable();
 
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
        r.numRealFailures = broker.getNumRealFailures();
        r.peakSimultaneousFailedHosts = broker.getPeakSimultaneousFailedHosts();
        r.totalDowntime = broker.getTotalDowntime();
        r.numCloudletsDeferred = broker.getNumCloudletsDeferred();
        r.totalDeferredWaitTime = broker.getTotalDeferredWaitTime();
        r.numCloudletsExposed = broker.getNumCloudletsExposedToFailure();
        r.fractionExposed = newList.isEmpty() ? 0.0 : r.numCloudletsExposed / (double) newList.size();
 
        return r;
    }
 
    // === Scenario construction — copied from SelectorScenario.java (2026-07-20) ===
 
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