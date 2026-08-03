package org.cloudbus.cloudsim.examples;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * AverageByController
 *
 * Takes a multi-seed SelectorMultiController sweep CSV (one baseline row +
 * N controller rows per seed, same schema as simulation_results_*.csv) and
 * writes a new CSV with one row per unique (monitor, analyser, planner,
 * executor) combination, averaged across all seeds present in the input.
 *
 * Key design choice: makespan/power/cost are normalised against each row's
 * own seed's baseline BEFORE averaging across seeds (% change), never
 * averaged as raw values. Raw values aren't comparable across seeds
 * directly (seed-to-seed baseline difficulty varies - see
 * 07-limitations.md, "RANDOM_ASSIGNMENT ... is MIPS-tier-blind"), so a
 * plain mean of e.g. makespan across seeds would be mixing easy-seed and
 * hard-seed runs together.
 *
 * Also applies the standard validity gate from this project's 2026-07-29
 * multi-seed work and reports how many of the seeds present each combo
 * passes:
 *   completion_rate              >= own-seed baseline - 0.005
 *   cloudlets_still_in_flight    == 0
 *   num_cloudlets_still_deferred == 0
 *   num_cloudlets_abandoned      <= own-seed baseline + 1
 *   min_live_vm_count            >= own-seed baseline - 1
 *   guid_compatible              == true
 *
 * Also reports completion_rate_recovery_pct: how much of a seed's own
 * baseline shortfall (1 - baseline completion_rate) the controller closed,
 * per-seed before averaging - not a blanket comparison against a universal
 * 1.0 ceiling, since baseline < 1.0 is often genuine unavoidable seed-level
 * loss (e.g. a host permanently died and its cloudlets couldn't all be
 * evacuated in time), not something any controller could fix. Formula:
 *   recovery = (controller_completion_rate - baseline_completion_rate)
 *              / (1 - baseline_completion_rate) * 100
 * 0% = no different from baseline, 100% = recovered every cloudlet baseline
 * lost, negative% = made it worse. Undefined (blank) when baseline == 1.0
 * (nothing to recover).
 *
 * Usage:
 *   java AverageByController <input_csv> <output_csv>
 *
 * No external dependencies - plain CSV parsing (comma-split, no quoting/
 * escaping needed since SelectorMultiController.java's own CSV writer never
 * emits quoted or comma-containing fields).
 */
public class simulationAverage {

    // Metrics normalised against own-seed baseline before averaging (% change)
    private static final String[] PCT_CHANGE_METRICS = {
        "makespan", "avg_power", "controllable_cost", "total_cost", "total_energy"
    };

    // Metrics already on a comparable/bounded scale - averaged as raw values
    private static final String[] RAW_AVERAGE_METRICS = {
        "completion_rate", "num_cloudlets_abandoned", "cloudlets_still_in_flight",
        "num_cloudlets_still_deferred", "min_live_vm_count", "live_host_count",
        "actions_executed", "avg_ram_util", "peak_ram_util", "avg_bw_util", "peak_bw_util"
    };

    // Fields the validity gate needs from each controller row and its own-seed baseline
    private static final String[] GATE_ROW_FIELDS = {
        "completion_rate", "cloudlets_still_in_flight", "num_cloudlets_still_deferred",
        "num_cloudlets_abandoned", "min_live_vm_count", "guid_compatible"
    };
    private static final String[] GATE_BASE_FIELDS = {
        "completion_rate", "num_cloudlets_abandoned", "min_live_vm_count"
    };

    /** Simple running-stats accumulator: mean and sample standard deviation (ddof=1, matches pandas default). */
    private static class RunningValues {
        private final List<Double> values = new ArrayList<>();

        void add(double v) {
            if (!Double.isNaN(v)) values.add(v);
        }

        double mean() {
            if (values.isEmpty()) return Double.NaN;
            double sum = 0.0;
            for (double v : values) sum += v;
            return sum / values.size();
        }

        double sampleStd() {
            int n = values.size();
            if (n < 2) return Double.NaN;
            double m = mean();
            double sq = 0.0;
            for (double v : values) sq += (v - m) * (v - m);
            return Math.sqrt(sq / (n - 1));
        }
    }

    /** Per-combo accumulator: one instance per unique (monitor, analyser, planner, executor). */
    private static class ComboAgg {
        final String monitor, analyser, planner, executor;
        final Set<Long> seedsPresent = new HashSet<>();
        int seedsPassingGate = 0;
        String guidCompatible = null; // "True"/"False" as first seen, informational only

        final Map<String, RunningValues> pct = new LinkedHashMap<>();
        final Map<String, RunningValues> raw = new LinkedHashMap<>();
        // Seed-normalised recovery-toward-perfect-completion metric (own-seed baseline as
        // floor, 1.0 as ceiling) - see class javadoc for why this isn't just "vs 1.0".
        final RunningValues completionRecovery = new RunningValues();

        ComboAgg(String monitor, String analyser, String planner, String executor) {
            this.monitor = monitor;
            this.analyser = analyser;
            this.planner = planner;
            this.executor = executor;
            for (String m : PCT_CHANGE_METRICS) pct.put(m, new RunningValues());
            for (String m : RAW_AVERAGE_METRICS) raw.put(m, new RunningValues());
        }
    }

    /** One seed's baseline row, holding only the fields this tool needs. */
    private static class BaselineRow {
        final Map<String, Double> values = new HashMap<>();
    }

    public static void main(String[] args) throws IOException {

        if (args.length != 2) {
            System.out.println("Usage: java AverageByController <input_csv> <output_csv>");
            System.exit(1);
        }

        String inputCsv = args[0];
        String outputCsv = args[1];

        List<String> lines = Files.readAllLines(Paths.get(inputCsv));
        if (lines.isEmpty()) {
            System.out.println("Input CSV is empty.");
            return;
        }

        String[] header = lines.get(0).split(",", -1);
        Map<String, Integer> col = new HashMap<>();
        for (int i = 0; i < header.length; i++) col.put(header[i].trim(), i);

        // Filter the metric lists down to columns actually present, warning about any missing ones.
        List<String> pctMetrics = filterPresent(PCT_CHANGE_METRICS, col, "pct-change");
        List<String> rawMetrics = filterPresent(RAW_AVERAGE_METRICS, col, "raw-average");
        boolean canGate = allPresent(GATE_ROW_FIELDS, col) && col.containsKey("scenario_seed")
                && col.containsKey("condition");
        boolean canRecovery = col.containsKey("completion_rate");

        requireColumns(col, "scenario_seed", "condition", "monitor", "analyser", "planner", "executor");

        // Pass 1: collect baseline rows, keyed by scenario_seed.
        Map<Long, BaselineRow> baselineBySeed = new HashMap<>();
        Set<Long> allSeeds = new HashSet<>();

        for (int li = 1; li < lines.size(); li++) {
            String line = lines.get(li);
            if (line.isEmpty()) continue;
            String[] f = line.split(",", -1);

            long seed = parseLong(f[col.get("scenario_seed")]);
            allSeeds.add(seed);
            String condition = f[col.get("condition")].trim();

            if (condition.equalsIgnoreCase("baseline")) {
                if (col.containsKey("crashed") && f[col.get("crashed")].trim().equalsIgnoreCase("true")) continue;
                BaselineRow b = new BaselineRow();
                for (String m : pctMetrics) b.values.put(m, parseDouble(f[col.get(m)]));
                for (String m : GATE_BASE_FIELDS) {
                    if (col.containsKey(m)) b.values.put(m, parseDouble(f[col.get(m)]));
                }
                baselineBySeed.put(seed, b);
            }
        }

        // Pass 2: process controller rows, grouped by (monitor, analyser, planner, executor).
        Map<String, ComboAgg> combos = new LinkedHashMap<>();

        for (int li = 1; li < lines.size(); li++) {
            String line = lines.get(li);
            if (line.isEmpty()) continue;
            String[] f = line.split(",", -1);

            String condition = f[col.get("condition")].trim();
            if (!condition.equalsIgnoreCase("controller")) continue;
            if (col.containsKey("crashed") && f[col.get("crashed")].trim().equalsIgnoreCase("true")) continue;

            long seed = parseLong(f[col.get("scenario_seed")]);
            BaselineRow base = baselineBySeed.get(seed);
            if (base == null) continue; // no baseline for this seed - skip, can't normalise

            String monitor = f[col.get("monitor")].trim();
            String analyser = f[col.get("analyser")].trim();
            String planner = f[col.get("planner")].trim();
            String executor = f[col.get("executor")].trim();
            // Delimiter is a runtime char(1) (SOH), built via (char) 1 rather than a
            // string-literal escape - guaranteed not to appear in any generated module
            // name, so this key is collision-proof regardless of naming convention.
            String comboKey = monitor + (char) 1 + analyser + (char) 1 + planner + (char) 1 + executor;

            ComboAgg agg = combos.computeIfAbsent(comboKey,
                    k -> new ComboAgg(monitor, analyser, planner, executor));
            agg.seedsPresent.add(seed);

            if (agg.guidCompatible == null && col.containsKey("guid_compatible")) {
                agg.guidCompatible = f[col.get("guid_compatible")].trim();
            }

            for (String m : pctMetrics) {
                double value = parseDouble(f[col.get(m)]);
                double baseValue = base.values.get(m);
                double pctChange = (baseValue == 0.0) ? Double.NaN
                        : (value - baseValue) / baseValue * 100.0;
                agg.pct.get(m).add(pctChange);
            }

            for (String m : rawMetrics) {
                agg.raw.get(m).add(parseDouble(f[col.get(m)]));
            }

            if (canRecovery && base.values.containsKey("completion_rate")) {
                double completionRate = parseDouble(f[col.get("completion_rate")]);
                double baseCompletion = base.values.get("completion_rate");
                double denom = 1.0 - baseCompletion;
                double recovery = (denom == 0.0) ? Double.NaN
                        : (completionRate - baseCompletion) / denom * 100.0;
                agg.completionRecovery.add(recovery);
            }

            if (canGate && passesGate(f, col, base)) {
                agg.seedsPassingGate++;
            }
        }

        writeOutput(outputCsv, combos.values(), pctMetrics, rawMetrics, allSeeds.size());

        System.out.println("Wrote " + combos.size() + " unique controller combinations -> " + outputCsv);
        System.out.println("Seeds in input: " + allSeeds.size());
    }

    private static boolean passesGate(String[] f, Map<String, Integer> col, BaselineRow base) {

        double completionRate = parseDouble(f[col.get("completion_rate")]);
        double stillInFlight = parseDouble(f[col.get("cloudlets_still_in_flight")]);
        double stillDeferred = parseDouble(f[col.get("num_cloudlets_still_deferred")]);
        double abandoned = parseDouble(f[col.get("num_cloudlets_abandoned")]);
        double minLiveVm = parseDouble(f[col.get("min_live_vm_count")]);
        String guidCompatible = f[col.get("guid_compatible")].trim();

        Double baseCompletion = base.values.get("completion_rate");
        Double baseAbandoned = base.values.get("num_cloudlets_abandoned");
        Double baseMinLiveVm = base.values.get("min_live_vm_count");
        if (baseCompletion == null || baseAbandoned == null || baseMinLiveVm == null) return false;

        return completionRate >= baseCompletion - 0.005
                && stillInFlight == 0.0
                && stillDeferred == 0.0
                && abandoned <= baseAbandoned + 1
                && minLiveVm >= baseMinLiveVm - 1
                && guidCompatible.equalsIgnoreCase("true");
    }

    private static void writeOutput(String outputCsv, Collection<ComboAgg> combosIn,
            List<String> pctMetrics, List<String> rawMetrics, int seedsTotal) throws IOException {

        List<ComboAgg> combos = new ArrayList<>(combosIn);
        // Sort by mean makespan % change ascending (biggest improvement first), if present.
        combos.sort(Comparator.comparingDouble(c ->
                pctMetrics.contains("makespan") ? c.pct.get("makespan").mean() : 0.0));

        StringBuilder header = new StringBuilder();
        header.append("monitor,analyser,planner,executor,seeds_present,seeds_passing_gate,seeds_total,guid_compatible");
        for (String m : pctMetrics) header.append(",pct_").append(m).append("_mean,pct_").append(m).append("_std");
        for (String m : rawMetrics) header.append(",").append(m).append("_mean,").append(m).append("_std");
        header.append(",completion_rate_recovery_pct_mean,completion_rate_recovery_pct_std");

        try (PrintWriter pw = new PrintWriter(new FileWriter(outputCsv))) {
            pw.println(header);
            for (ComboAgg c : combos) {
                StringBuilder row = new StringBuilder();
                row.append(c.monitor).append(",").append(c.analyser).append(",")
                   .append(c.planner).append(",").append(c.executor).append(",")
                   .append(c.seedsPresent.size()).append(",")
                   .append(c.seedsPassingGate).append(",")
                   .append(seedsTotal).append(",")
                   .append(c.guidCompatible == null ? "" : c.guidCompatible);
                for (String m : pctMetrics) {
                    RunningValues rv = c.pct.get(m);
                    row.append(",").append(fmt(rv.mean())).append(",").append(fmt(rv.sampleStd()));
                }
                for (String m : rawMetrics) {
                    RunningValues rv = c.raw.get(m);
                    row.append(",").append(fmt(rv.mean())).append(",").append(fmt(rv.sampleStd()));
                }
                row.append(",").append(fmt(c.completionRecovery.mean()))
                   .append(",").append(fmt(c.completionRecovery.sampleStd()));
                pw.println(row);
            }
        }
    }

    private static String fmt(double v) {
        return Double.isNaN(v) ? "" : String.format(Locale.US, "%.6f", v);
    }

    private static double parseDouble(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    private static long parseLong(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (Exception e) {
            return -1L;
        }
    }

    private static List<String> filterPresent(String[] metrics, Map<String, Integer> col, String label) {
        List<String> present = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String m : metrics) {
            if (col.containsKey(m)) present.add(m); else missing.add(m);
        }
        if (!missing.isEmpty()) {
            System.out.println("Note: " + label + " columns not found in this CSV, skipped: " + missing);
        }
        return present;
    }

    private static boolean allPresent(String[] fields, Map<String, Integer> col) {
        for (String f : fields) if (!col.containsKey(f)) return false;
        return true;
    }

    private static void requireColumns(Map<String, Integer> col, String... required) {
        for (String r : required) {
            if (!col.containsKey(r)) {
                throw new IllegalArgumentException("Required column missing from CSV: " + r);
            }
        }
    }
}