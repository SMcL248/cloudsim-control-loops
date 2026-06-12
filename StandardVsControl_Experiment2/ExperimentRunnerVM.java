package org.cloudbus.cloudsim.examples;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.*;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.*;

/**
 * ExperimentRunner
 *
 * Runs a matrix of experiments varying the VM MIPS seed across a fixed number
 * of VMs and cloudlets. For each seed, the same scenario is run twice:
 *   1. Baseline — standard DatacenterBroker, no migration
 *   2. Controlled — ControlBroker with observe/decide/act loop
 *
 * Cloudlet lengths are fixed (CLOUDLET_SEED constant) so that the only
 * variable between rows is the MIPS assignment. This isolates the effect
 * of MIPS heterogeneity on controller performance.
 *
 * Output: a table of makespan deltas (baseline - controlled) per seed.
 */
public class ExperimentRunnerVM {

    // --- Experiment parameters ---
    private static final int NUM_VMS        = 5;
    private static final int NUM_CLOUDLETS  = 10;
    private static final long CLOUDLET_SEED = 42L;   // fixed — never changes
    private static final int OBSERVATION_RATE = 100;

    private static final int[] MIPS_TIERS = {250, 500, 1000, 2000};

    // Seeds to sweep across — edit this list to add more experiments
    private static final long[] VM_SEEDS = {1L, 2L, 3L, 4L, 5L, 7L, 13L, 42L, 99L, 123L};

    // --- Result record ---
    private static class ExperimentResult {
        final long vmSeed;
        final double baselineMakespan;
        final double controlledMakespan;
        final double delta;           // baseline - controlled (positive = controller helped)
        final String mipsAssignment;  // human-readable VM MIPS list

        ExperimentResult(long vmSeed, double baselineMakespan, double controlledMakespan, String mipsAssignment) {
            this.vmSeed            = vmSeed;
            this.baselineMakespan  = baselineMakespan;
            this.controlledMakespan = controlledMakespan;
            this.delta             = baselineMakespan - controlledMakespan;
            this.mipsAssignment    = mipsAssignment;
        }
    }

    public static void main(String[] args) {
        Log.disable(); // suppress per-event CloudSim noise; results printed below

        List<ExperimentResult> results = new ArrayList<>();

        for (long vmSeed : VM_SEEDS) {
            double baselineMakespan   = runSimulation(vmSeed, false);
            double controlledMakespan = runSimulation(vmSeed, true);
            String mipsAssignment     = describeMips(vmSeed);
            results.add(new ExperimentResult(vmSeed, baselineMakespan, controlledMakespan, mipsAssignment));
        }

        printResults(results);
        writeCSV(results, "experiment_results_vm.csv");
    }

    /**
     * Runs one simulation and returns the makespan (finish time of last cloudlet).
     *
     * @param vmSeed     seed used to assign MIPS tiers to VMs
     * @param controlled true = use ControlBroker; false = use plain DatacenterBroker
     */
    private static double runSimulation(long vmSeed, boolean controlled) {
        try {
            CloudSim.init(1, Calendar.getInstance(), false);

            Datacenter dc0 = createDatacenter("DC_0");
            Datacenter dc1 = createDatacenter("DC_1");

            DatacenterBroker broker;
            if (controlled) {
                broker = new ControlBroker("Broker", OBSERVATION_RATE);
            } else {
                broker = new DatacenterBroker("Broker");
            }

            int brokerId = broker.getId();

            List<Vm> vms = createVms(brokerId, vmSeed);
            List<Cloudlet> cloudlets = createCloudlets(brokerId);

            broker.submitGuestList(vms);
            broker.submitCloudletList(cloudlets);

            CloudSim.startSimulation();
            List<Cloudlet> finished = broker.getCloudletReceivedList();
            CloudSim.stopSimulation();

            return makespan(finished);

        } catch (Exception e) {
            e.printStackTrace();
            return Double.NaN;
        }
    }

    // --- VM creation ---

    private static List<Vm> createVms(int brokerId, long seed) {
        Random rng = new Random(seed);
        List<Vm> vms = new ArrayList<>();
        for (int i = 0; i < NUM_VMS; i++) {
            int mips = MIPS_TIERS[rng.nextInt(MIPS_TIERS.length)];
            vms.add(new Vm(i, brokerId, mips, 1, 512, 1000, 10000,
                    "Xen", new CloudletSchedulerTimeShared()));
        }
        return vms;
    }

    /** Returns a compact string like "1000/250/500/1000/250" for logging. */
    private static String describeMips(long seed) {
        Random rng = new Random(seed);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < NUM_VMS; i++) {
            if (i > 0) sb.append("/");
            sb.append(MIPS_TIERS[rng.nextInt(MIPS_TIERS.length)]);
        }
        return sb.toString();
    }

    // --- Cloudlet creation ---

    private static List<Cloudlet> createCloudlets(int brokerId) {
        Random rng = new Random(CLOUDLET_SEED);
        List<Cloudlet> cloudlets = new ArrayList<>();
        UtilizationModel full = new UtilizationModelFull();
        for (int i = 0; i < NUM_CLOUDLETS; i++) {
            long length = 10000 + (long)(rng.nextDouble() * 90000); // 10k–100k MI
            Cloudlet c = new Cloudlet(i, length, 1, 300, 300, full, full, full);
            c.setUserId(brokerId);
            cloudlets.add(c);
        }
        return cloudlets;
    }

    // --- Datacenter creation ---

    private static Datacenter createDatacenter(String name) throws Exception {
        List<Pe> peList1 = new ArrayList<>();
        for (int i = 0; i < 4; i++) peList1.add(new Pe(i, new PeProvisionerSimple(2000)));

        List<Pe> peList2 = new ArrayList<>();
        for (int i = 0; i < 2; i++) peList2.add(new Pe(i, new PeProvisionerSimple(2000)));

        List<Host> hosts = new ArrayList<>();
        hosts.add(new Host(0, new RamProvisionerSimple(16384), new BwProvisionerSimple(10000),
                1000000, peList1, new VmSchedulerTimeShared(peList1)));
        hosts.add(new Host(1, new RamProvisionerSimple(16384), new BwProvisionerSimple(10000),
                1000000, peList2, new VmSchedulerTimeShared(peList2)));

        DatacenterCharacteristics ch = new DatacenterCharacteristics(
                "x86", "Linux", "Xen", hosts, 10.0, 3.0, 0.05, 0.1, 0.1);

        return new Datacenter(name, ch, new VmAllocationPolicySimpler(hosts), new LinkedList<>(), 0);
    }

    // --- Metrics ---

    private static double makespan(List<Cloudlet> cloudlets) {
        double max = 0;
        for (Cloudlet c : cloudlets) {
            if (c.getStatus() == Cloudlet.CloudletStatus.SUCCESS) {
                max = Math.max(max, c.getExecFinishTime());
            }
        }
        return max;
    }

    // --- Output ---

    private static void printResults(List<ExperimentResult> results) {
        DecimalFormat df = new DecimalFormat("####.##");
        String sep = "+----------+---------------------+------------+------------+-----------+----------+";

        System.out.println("\n========== EXPERIMENT RESULTS ==========");
        System.out.println("  Fixed params: " + NUM_VMS + " VMs, " + NUM_CLOUDLETS
                + " cloudlets, cloudlet seed=" + CLOUDLET_SEED
                + ", observation rate=" + OBSERVATION_RATE);
        System.out.println(sep);
        System.out.printf("| %-8s | %-19s | %-10s | %-10s | %-9s | %-8s |%n",
                "VM Seed", "MIPS (VM0..N)", "Baseline", "Controlled", "Delta", "Improved");
        System.out.println(sep);

        double totalDelta = 0;
        int improvedCount = 0;

        for (ExperimentResult r : results) {
            boolean improved = r.delta > 0;
            if (improved) improvedCount++;
            totalDelta += r.delta;

            System.out.printf("| %-8d | %-19s | %-10s | %-10s | %-9s | %-8s |%n",
                    r.vmSeed,
                    r.mipsAssignment,
                    df.format(r.baselineMakespan),
                    df.format(r.controlledMakespan),
                    df.format(r.delta),
                    improved ? "YES  +" : "NO   -");
        }

        System.out.println(sep);
        System.out.printf("| %-8s   %-19s   %-10s   %-10s   %-9s   %-8s |%n",
                "SUMMARY", "",
                "avg: " + df.format(avgBaseline(results)),
                "avg: " + df.format(avgControlled(results)),
                "avg: " + df.format(totalDelta / results.size()),
                improvedCount + "/" + results.size());
        System.out.println(sep);
    }

    private static double avgBaseline(List<ExperimentResult> results) {
        return results.stream().mapToDouble(r -> r.baselineMakespan).average().orElse(0);
    }

    private static double avgControlled(List<ExperimentResult> results) {
        return results.stream().mapToDouble(r -> r.controlledMakespan).average().orElse(0);
    }

    // --- CSV export ---

    private static void writeCSV(List<ExperimentResult> results, String filename) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {

            // Header — metadata as comment rows so the CSV is self-documenting
            pw.println("# ExperimentRunner output");
            pw.println("# num_vms=" + NUM_VMS
                    + " num_cloudlets=" + NUM_CLOUDLETS
                    + " cloudlet_seed=" + CLOUDLET_SEED
                    + " observation_rate=" + OBSERVATION_RATE);
            pw.println("# mips_tiers=" + Arrays.toString(MIPS_TIERS));
            pw.println("# delta = baseline_makespan - controlled_makespan (positive = controller improved makespan)");

            // Column headers
            pw.println("vm_seed,mips_assignment,baseline_makespan,controlled_makespan,delta,improved");

            // One row per experiment
            for (ExperimentResult r : results) {
                pw.printf("%d,\"%s\",%.4f,%.4f,%.4f,%s%n",
                        r.vmSeed,
                        r.mipsAssignment,
                        r.baselineMakespan,
                        r.controlledMakespan,
                        r.delta,
                        r.delta > 0 ? "true" : "false");
            }

            // Summary row
            double avgBase       = avgBaseline(results);
            double avgControlled = avgControlled(results);
            long improvedCount   = results.stream().filter(r -> r.delta > 0).count();
            pw.printf("SUMMARY,\"%d/%d improved\",%.4f,%.4f,%.4f,%n",
                    improvedCount, results.size(),
                    avgBase, avgControlled,
                    avgBase - avgControlled);

            System.out.println("Results written to " + filename);

        } catch (IOException e) {
            System.err.println("Failed to write CSV: " + e.getMessage());
        }
    }
}