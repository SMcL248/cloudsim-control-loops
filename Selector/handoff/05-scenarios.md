# 05 — Scenarios, Permutation Constructor, Mixed-Pool Preparation

## Simulation Scenario

`ConstructorVariableVM.java` — current scenario configuration:

- **Hosts**: 6 × quad-core (4 PEs × 1000 MIPS = 4000 MIPS total per host)
- **VMs**: 12, MIPS drawn randomly from `{2000, 500, 1000}` with seed 42. Total VM demand ≈ 14,000 MIPS vs 24,000 MIPS host capacity — scenario is undersubscribed, creating genuine migration headroom on light hosts.
- **Cloudlets**: 60, lengths uniform random in [10,000, 500,000] MI with seed 42, `UtilizationModelFull`. Longer upper bound (500,000 MI) extends simulation duration, giving controllers more observation cycles to act and differentiate.
- **Scheduling**: `CloudletSchedulerTimeShared`, `VmSchedulerTimeShared`
- **Placement**: `VmAllocationPolicySimpler` (First Fit) — packs VMs onto early hosts in order, creating structural imbalance. Chosen deliberately over Worst Fit / LeastFull to create an uneven initial state that controllers can act on.
- **Observation rate**: 100 time units.

### Ground Truth Metric

**Status: RESOLVED 2026-07-01.** Ground truth has been switched from `getTotalUtilizationOfCpuMips(now)` to `getCurrentRequestedTotalMips()`. Old baseline (2.687) and the util-based 5×5×5×3 run are retained above as historical/superseded. New baseline: **0.222222**. See "5×5×5×3 Run — Demand-Based Ground Truth (Current)" above for full results.

Why the switch is justified, not just preferred — confirmed against CloudSim source (`Cloudslab/cloudsim`):

- `getTotalUtilizationOfCpuMips` = `getTotalUtilizationOfCpu(time) * getMips()`, where `getTotalUtilizationOfCpu` sums `cl.getUtilizationOfCpu(time)` over every cloudlet in the VM's exec list with no normalisation. Under `UtilizationModelFull` (fixed at 1.0/cloudlet), this is literally a count of concurrently-executing cloudlets, and `getMips()` is the VM's **per-PE** rating, not total capacity — the product has MIPS units but no relationship to host capacity. This is why observed cpu-util readings hit 2.75× host capacity (see t=100 snippet, 2026-07-01 conversation) instead of staying in [0,1] as the GUID type table promises.
- `getCurrentRequestedTotalMips()` sums `currentMipsShare`, the MIPS actually allocated by the host's `VmSchedulerTimeShared`. That scheduler's Javadoc states it explicitly: *"This scheduler does not support over-subscription."* Its `allocatePesForGuest` hard-rejects any allocation that would push the running total past the host's `availableMips` (initialised to `host.getTotalMips()`). This means **the sum of demand pressure across all VMs on a host is capped at 1.0 by construction** — not an empirical property of this undersubscribed scenario, but a scheduler invariant. This is the property you want in a ground-truth signal.

Net effect: cpu-util was both noisy (scheduler-timing-dependent, can read exactly 0.0 mid-run) and structurally unbounded (can read many multiples of 1.0), i.e. it failed in both directions. cpu-demand is bounded and deterministic. The old `host-cpu-util` GUID should be considered unreliable as a [0,1]-typed signal going forward; `host-cpu-demand` is the validated signal.

`HollowedControl` measured **average CPU demand pressure variance across hosts** (historical/deprecated implementation shown below used `getTotalUtilizationOfCpuMips`; current implementation uses `getCurrentRequestedTotalMips` in the same position), computed independently of the MAPE pipeline on every observation cycle:

```java
// Per cycle:
double[] utils = new double[hosts.size()];
double mean = 0.0;
for (int i = 0; i < hosts.size(); i++) {
    double usedMips = 0;
    for (GuestEntity vm : hosts.get(i).getGuestList()) {
        usedMips += vm.getTotalUtilizationOfCpuMips(now);
    }
    utils[i] = usedMips / hosts.get(i).getTotalMips();
    mean += utils[i];
}
mean /= hosts.size();
double variance = 0.0;
for (double u : utils) variance += (u - mean) * (u - mean);
groundTruthVarianceSum += variance / hosts.size();
groundTruthCycleCount++;

// Result exposed via:
public double getGroundTruthAvgVariance() {
    return groundTruthCycleCount == 0 ? 0.0 : groundTruthVarianceSum / groundTruthCycleCount;
}
```

**Interpretation**: population variance of per-host demand pressure ratios, averaged over the run. Lower = more balanced load distribution. Values can exceed 0.25 (the theoretical max for values in [0,1]) because demand pressure can exceed 1.0 in oversubscribed hosts — this is expected and correct.

**Why not threshold-based overloaded host-cycles?** Earlier metric (`total_overloaded`, counting host-cycles above 0.8 threshold) collapsed to zero variance across all controllers (all produced exactly 7) after scenario configuration changes. The threshold is too blunt when all hosts are near-saturated; any controller that acts at all lands at the same floor. Demand pressure variance captures the *distribution* of load rather than a binary threshold, and is sensitive to migration decisions regardless of absolute utilisation level.

**Why not clamp utils to [0,1]?** In the current undersubscribed scenario demand pressure values do stay below 1.0 on most hosts, so clamping would not collapse variance — but it is omitted for consistency and because demand pressure is the intended signal. In earlier oversubscribed configurations (24 VMs), clamping would have collapsed all values to 1.0 and zeroed variance.

**Ground truth is not independent of the monitors.** The current (demand-based) ground truth uses the same `getCurrentRequestedTotalMips()` call as monitor_v2. The deprecated util-based ground truth likewise duplicated monitor_v1's internal call. See CloudSim Observability Constraint below.

**Ground truth metric — resolved 2026-07-01.** `getTotalUtilizationOfCpuMips(now)` returned 0.0 at many hosts even when cloudlets were actively running (scheduler-timing artifact) and, per the source analysis above, could also read many multiples of 1.0 depending on cloudlet concurrency — noisy in both directions. Switched to `getCurrentRequestedTotalMips()` (demand pressure), which is scheduler-bounded to ≤1.0 by construction. Re-run complete: see "5×5×5×3 Run — Demand-Based Ground Truth (Current)" above. New baseline: 0.222222 (was 2.687 under the deprecated metric — not comparable in absolute terms, but the qualitative structure of the results transferred almost entirely, with one reversal: planner_v5, previously deemed a failure, now the best performer).

---

## Permutation Constructor

`ConstructorVariableVM.java` — iterates all module combinations, runs each through the standard simulation scenario, collects `SimulationResult` records, prints a summary after all runs complete.

### Design
- **Module registries** — `monitorDict`, `analyserDict`, `plannerDict`, `executorDict` as static (raw-typed) arrays. Add new variants here. Kept raw deliberately — see "Instrumentation Architecture" below for why raw-typing here is fine but was NOT fine downstream.
- **GUID validation** — checked per combination but does not gate execution. Incompatible combinations still run.
- **Fresh instances per run** — modules instantiated via reflection through a single generic `instantiate(Class<?>)` helper (one `@SuppressWarnings("unchecked")` chokepoint) to prevent counter accumulation across runs and to restore proper generic typing (`Monitor<double[]>`, `Analyser<double[],LoadState[]>`, `Planner<LoadState[],int[]>`, `Executor<int[]>`) after the raw-typed registry lookup.
- **`SimulationResult` record** — `monitorId`, `analyserId`, `plannerId`, `executorId`, `makespan`, `groundTruthAvgVariance`, `actionableCycles` (imbalance), `opportunityCycles`, `actionsExecuted`, `conversionRate`, `compatible` (boolean), `status` (ACTIVE / INERT).
- **Deferred logging** — `logResult` called after all simulations complete.

### SimulationResult Metrics
- `makespan` — max cloudlet finish time. `-1` signals failure. Non-discriminating in current scenario.
- `groundTruthAvgVariance` — average CPU demand pressure variance across hosts per cycle (double). Primary performance metric. Lower = better load balance.
- `actionableCycles` — cycles where Analyser detected at least one OVERLOADED **or** UNDERLOADED state. **Weak gate, analyser-health diagnostic only** — saturates to the ceiling for 3 of 5 analysers regardless of monitor. See "Actionable-Cycle Gate" above. Column name retained for CSV continuity with prior runs; conceptually this is "imbalance detected."
- `opportunityCycles` — **new (2026-07-01)**. Cycles where Analyser detected ≥1 OVERLOADED **and** ≥1 UNDERLOADED simultaneously. Planner-agnostic proxy for "a migration was actually possible." The intended denominator for a more meaningful conversion rate going forward.
- `actionsExecuted` — migrations actually dispatched. **Source changed 2026-07-01**: previously read from `Executor.getActionsExecuted()` (self-reported by the module); now read from `HollowedControl.getActionsExecuted()`, incremented directly off the `boolean success` already returned by `execute()` — no generic type inspection needed for this one, since `execute()`'s return type was never generic.
- `conversionRate` — `actionsExecuted / actionableCycles * 100`. Measures pipeline activity against the weak gate; historically treated as a selectivity/quality signal, which conflates "declined to act" with "had nothing valid to act on." A `conversionRate` against `opportunityCycles` is the more defensible version of this stat going forward — not yet computed in the CSV, straightforward to add.
- `status` — INERT if `actionableCycles == 0`; ACTIVE otherwise. Unchanged semantics — INERT still means the analyser never flagged anything at all, which is a different (and rarer) condition than `opportunityCycles == 0`.

### Interface Amendments — REMOVED 2026-07-08 (previously superseded 2026-07-01, see Instrumentation Architecture below)
- `Analyser` — `getActionableCycles()`. Incremented when at least one OVERLOADED **or** UNDERLOADED present in output array.
- `Executor` — `getActionsExecuted()`. Incremented once per non-sentinel `execute()` call.

**Update 2026-07-08:** these two methods, their backing counter fields, and all increment call sites have now been fully removed — from both interfaces and from all 8 affected modules (analyser_v1–v5, executor_v1–v3). This supersedes the original "leave them in place, unused" decision below: once validated end-to-end against a complete 375-row run (see "5×5×5×3 Run — Demand-Based Ground Truth" above), the self-reporting methods were confirmed to be pure vestige with no remaining reader, and — checked directly against `analyser_v1`'s source — cleanly separable from the classification/planning logic (single field, isolated increment sites, single getter), making removal mechanically low-risk. The `<Type> Specification.md` prompt documents have also been updated to drop the self-tracking requirement, so future generation batches won't be asked to implement it.

**Confirmed 2026-07-08:** full 375-permutation re-run against the trimmed modules reproduces the existing dataset exactly (same baseline 0.222222, same best 0.021875, same ACTIVE/INERT split). Removing the self-tracking bookkeeping was purely cosmetic, as expected — no classification or planning behaviour changed. The trimmed modules are now the canonical version the published results are traceable to.

Historical context (why this was originally left alone, and how it was reversed): the Analyser/Executor Java interfaces were initially left untouched specifically to avoid regeneration risk on the 18 existing modules — the reasoning below explains that original caution.

### Instrumentation Architecture (2026-07-01)

**Problem:** `getActionableCycles()`/`getActionsExecuted()` being self-reported by 18 independently LLM-generated modules meant every count in every prior analysis was trusting each module's own bookkeeping, never independently verified. Diagnosing the actionable-cycle saturation (above) was the trigger for fixing this.

**Decision:** move all three counters (`imbalanceCycles`/old `actionableCycles`, new `opportunityCycles`, `actionsExecuted`) into `HollowedControl`, computed once by hand-authored framework code, the same way for every permutation — removing an entire class of unverified measurement risk, not just tidying code.

**Generic-type obstacle:** `HollowedControl<M,D,A>` is generic, so it can't statically know `D = LoadState[]` to inspect co-occurrence, and `execute()`'s `boolean` return needed no such inspection (concrete type, not generic). Resolved by injecting two `Predicate<D>` hooks at construction time (Option B: push the type-specific logic to the boundary where the type is concretely known), defined in `ConstructorVariableVM` where `D` is bound to `LoadState[]`:

```java
// HollowedControl<M,D,A> — stays fully generic, never mentions LoadState
private final Predicate<D> imbalancePredicate;
private final Predicate<D> opportunityPredicate;
private int imbalanceCycles = 0;
private int opportunityCycles = 0;
private int actionsExecuted = 0;

// inside observeAndAct(), after analyser.analyse(...):
if (imbalancePredicate.test(diagnosis))   imbalanceCycles++;
if (opportunityPredicate.test(diagnosis)) opportunityCycles++;
...
boolean success = executor.execute(actions, this);
if (success) actionsExecuted++;   // success already IS "non-sentinel action" — no generic inspection of A needed
```

```java
// ConstructorVariableVM.java — the only place LoadState is mentioned in this wiring
diagnosis -> hasAny(diagnosis, LoadState.OVERLOADED) || hasAny(diagnosis, LoadState.UNDERLOADED),   // imbalance
diagnosis -> hasAny(diagnosis, LoadState.OVERLOADED) && hasAny(diagnosis, LoadState.UNDERLOADED)     // opportunity
```

**Reflection/generics interaction:** passing raw-typed reflection results (`monitorDict` etc. are raw arrays) directly into `HollowedControl<>`'s generic constructor poisons the whole call to raw type inference (a standard Java gotcha — once one argument is raw, the compiler can't verify the rest, and the whole invocation, including the lambda parameter types, collapses to `Object`). Fixed with a single generic factory method as the one unchecked-cast chokepoint:

```java
@SuppressWarnings("unchecked")
private static <T> T instantiate(Class<?> clazz) throws Exception {
    return (T) clazz.getDeclaredConstructor().newInstance();
}
// call sites use target-typing to infer T, e.g.:
Monitor<double[]> mFresh = instantiate(m.getClass());
```
This is safe not because the annotation makes it safe, but because each module already declares its concrete generic parameterisation at its own class definition (`class monitor_v1 implements Monitor<double[]>`), and the registries only ever contain classes validated against this family's interfaces (100% structural compliance across all runs so far) — the cast recovers information reflection's API can't preserve through erasure, it doesn't invent a new guarantee.

**Backward compatibility / future generation specs — originally proposed 2026-07-01, actioned 2026-07-08:** the Analyser/Executor Java interfaces were initially left untouched to avoid any regeneration risk on the 18 existing modules, with a deprecated-`default`-method conversion floated as a lower-risk middle ground. In the end the full removal was done instead (see "Interface Amendments" above) — both interface methods, their backing fields, and all call sites are gone from all 8 modules, and the `<Type> Specification.md` prompt documents have been trimmed to match, so future generation batches won't be asked to implement this bookkeeping at all. Pending: confirmation re-run against the trimmed modules (see above).

### Validated Runs
- **3×3×3×1 (27 permutations)** — counter accumulation bug identified and fixed via reflection instantiation.
- **3×3×3×3 (81 permutations)** — executor slot confirmed non-discriminating. makespan confirmed non-discriminating.
- **5×5×5×3 (375 permutations)** — current validated scale. Results summarised above.

### Baseline Benchmark

`MeasuringBroker` — a minimal `DatacenterBroker` subclass that runs the same scenario with no MAPE pipeline, measuring `groundTruthAvgVariance` on the same 100-time-unit schedule. Run in `ManualControllerVmMigrationSimpleCompare.java`.

**Current baseline (demand-based ground truth, 2026-07-01): 0.222222.** Confirmed as the exact convergence value of every INERT/dead pipeline in the demand-based 5×5×5×3 run — see above.

**Historical baseline (util-based ground truth, superseded): 2.687** — the natural variance reading under First Fit placement with no controller intervention, under the now-deprecated `getTotalUtilizationOfCpuMips` metric. Not comparable in absolute terms to the current baseline (different metric, different units) — retained for reference only.

Critical: both simulations must use identical scenario parameters (VMs, cloudlets, allocation policy, seeds). Cloudlet `maxLength` must be `500000` in both files — earlier runs with `100000` in `ConstructorVariableVM` produced non-comparable results. This constraint applies equally to the demand-based re-run.

---

## Mixed-Pool Preparation — Random Scenario and Stage-0 Characterisation (2026-07-08)

### Next-Step Decision: Cross-Family Mixed-Pool Sweep

With all three families independently validated (QoS/VM-migration, throughput/cloudlet-migration, power), the agreed next step is the original Phase-6 design goal: a merged-registry sweep across all ~15 monitors × 15 analysers × 15 planners × ~7 executors, testing composition across action types (2- vs 3-tuple), indexing domains (host- vs VM-indexed), and objectives. Key design decision: **log all three ground truths (demand variance, makespan, avg power/energy) on every row regardless of family** — every controller then gets scored against all three objectives, yielding the empirical Pareto surface (generalising the r = −0.94 QoS/power opposition) without building the Fusion stage first. Fusion becomes a follow-up informed by real frontier data. Known prerequisites, all documented in prior sections: length-agnostic sentinel predicates, per-boundary compatibility booleans, per-run try/catch so cross-family type crashes log as FAILED rows (they are data, per the Executor2 ClassCastException precedent), and a scenario fair to all three objectives — which motivated everything below.

### Random Scenario — Design

**Problem:** both existing placement policies are structurally biased. First-Fit manufactures imbalance (QoS-friendly) but lands at/near the consolidation optimum (power-degenerate, confirmed by direct arithmetic); Least-Full manufactures spread (power-friendly) but leaves a load balancer almost nothing (baseline variance 0.007). Each is near-degenerate for one objective. Likewise the broker's default round-robin cloudlet binding is a hidden balancing heuristic — exactly 5 cloudlets per VM, workload variance only through length draws.

**Solution: two new seeded random axes**, each independently toggleable in the scenario file (`RANDOM_PLACEMENT`, `RANDOM_ASSIGNMENT`):

- **Random VM placement** — `SelectionPolicyCustomRandom` (uniform draw among non-excluded hosts, `Random` injected via constructor, returns null on exhaustion), plugged into `VmAllocationWithSelectionPolicy`'s exclusion-retry protocol. The **stock CloudSim 7.x `SelectionPolicyRandomSelection` is unusable**: it constructs an unseeded `RandomGen` per call (non-reproducible placement — breaks per-seed baselines and every exact-reproduction validation) and its retry loop spins forever when all candidates are excluded (silent-hang class, same as `schedulingInterval=0`). Third entry in the stock-component defect list alongside `SelectionPolicyLeastFull` and `getCompletedVms()`.
- **Random cloudlet→VM assignment** — `cloudlet.setGuestId(assignRng.nextInt(NUM_VMS))` at creation; broker round-robin only binds cloudlets left at guestId −1, so the toggle needs no broker changes.

**Seed discipline:** `lengthRng = new Random(seed)` exactly (preserves the historical seed-42 cloudlet population); `assignRng = new Random(seed ^ 0x9E3779B97F4A7C15L)` — derived sub-seed, because two `Random` instances with the *same* seed produce identical state sequences, structurally correlating lengths with assignments. Rule: **never change the seed of a stream whose draws are already published; freely derive sub-seeds for streams with no history.** (This is the correct generalisation of the `CLOUDLET_SEED_OFFSET` lesson, which was about the former.) Documented accepted deviation: the placement policy uses the *raw* seed (shares a stream with `lengthRng`) — anchors were minted against it before the correlation was flagged; acceptable for placement since the correlation is weak, but noted in the scenario source.

**Random drifts toward Least-Full, not First-Fit** (question raised, resolved by theory then confirmed by survey): uniform random has no ordering bias, so expected occupancy is spread-with-fluctuations — LF-like occupancy, FF-ish unevenness. Predictions: mean occupancy 5.33/6, P(host empty) = (5/6)¹² ≈ 0.11.

### Instrumentation: MeasuringBroker Reintroduced (family-agnostic)

The QoS-era `MeasuringBroker` (no control logic, observation only) extended rather than duplicated — same "existing type already covers this" principle. Changes: dispatch via the `processOtherEvent` hook instead of a hand-copied `processEvent` tag chain (inherit base dispatch, add exactly one behaviour); first-observation-cycle placement dump + occupied-host count (**measurement unconditional, only printing behind a `verbose` flag** — a triple bug where the capture sat inside the verbose gate with `placementDumped` initialised true produced −1 occupancy across two survey runs before being caught); a clock guard (`throw if CloudSim.clock() > 100,000`) converting any never-terminating run into a named exception — insurance against the silent-hang class. Division of labour: selection policy logs placement *decisions* (System.out, includes retries), MeasuringBroker logs confirmed *state*, creation-time histogram logs assignment; energy read externally from `PowerDatacenter.getPower()`.

### Three-Anchor Validation (seed 42) — All Published Constants Reproduced Exactly

| Config | Variance | Occupied hosts | Total energy (W·sec) | Matches |
|---|---|---|---|---|
| First-Fit + RR | 0.190647 | 3/6 | 4,090,075.0 | published FF constants, to the digit |
| Least-Full + RR | 0.007053 | 6/6 | 6,842,275.0 | published LF constants (avg power 1118.57), to the digit |
| Random + Random | 0.120334 | 5/6 | 6,059,531.25 | new anchor (makespan 6256.01) |

Reproducing the FF and LF constants to full precision validates the entire reconstructed pipeline (scenario file, MeasuringBroker, FixedPowerHost, policy chain) against the published datasets. Bonus finding: identical cloudlet finish tables across FF/LF/Random under round-robin — **makespan is placement-invariant when every VM receives its full MIPS grant** (the established non-discrimination result, observed directly), so makespan is an *assignment-axis* descriptor. A config-mislabelling incident (a run labelled Least-Full was actually First-Fit — caught because the energy matched the FF constant exactly) motivated replacing the boolean with explicit labels in all output; three placement policies cannot be expressed by one boolean.

### 100-Seed Survey (`PowerScenarioSurvey`, seeds 42–141, no controller)

Streaming per-row CSV (`scenario_survey.csv`: seed, placement, assignment, makespan, total_energy, average_power, average_cpu_demand_variance, occupied_hosts, max_vm_cloudlets). Results against prediction:

- **Occupancy {4: 8, 5: 47, 6: 45} — mean 5.37 (predicted 5.33), empty-host rate 0.63 (predicted 0.67)** — the policy is unbiased to within sampling noise. Occupancy↔avg_power r = 0.962, as the 150W-floor power model demands.
- **QoS headroom:** variance median 0.0573, range 0.0022–0.1507; 15/100 seeds QoS-thin (<0.02), 4 below the LF anchor — honest scenario diversity, absorbed by per-seed baselines. Max never approaches FF's 0.19.
- **Power headroom: 100/100 draws** (occupancy always > the packing minimum of 3). **Throughput headroom: 100/100 draws** (max cloudlets/VM 7–11 vs round-robin's uniform 5).
- **Makespan 3622–14623 — a 4× spread across seeds**, the strongest justification yet for the per-seed-baseline discipline; a fixed global baseline would be off by up to ~2× either way.

**Verdict: the random scenario is live for all three objectives in ~85% of draws and for power/throughput in 100% — validated as the mixed-pool scenario.** FF and LF remain the within-family scenarios for the published results; nothing run on the random scenario is comparable to prior datasets.

### Methodology Lessons (this stage)

- **Result CSVs are write-once artifacts; nothing that can save them ever opens them.** An Excel open-and-save silently rewrote numeric columns to scientific notation. View copies if needed; the canonical file is only ever produced by the harness. Contaminated files are regenerated, not repaired.
- **Mid-run snapshots of streaming CSVs are readable but truncated — check completeness (row count, last-line integrity) before diagnosing crashes.** Two apparent harness "crashes at iteration ~84" were investigated (leak hypotheses, forensics on partial final lines) before resolving as stale/incomplete file copies; the completed run does all 100 iterations without issue. The streaming-write discipline is what made partial files readable — a feature, but one that makes truncation look like well-formed data.

### Rollout Ladder (agreed)

- **Stage 0 — no controller, multi-seed characterisation: DONE (above).**
- **Stage 1 — single controller, single sim:** one hand-coded reference per family (QoS, throughput, power) against a random-scenario seed; question is only "does each controller class engage." Not started.
- **Stage 2 — multi controller, multi sim:** per-family 5×5×5 × 10 seeds on the random scenario (skip single-sim sweeps — no information a 10-seed run lacks), then the merged pool. Not started.

Discipline: never change the scenario and the module pool in the same step.

---

## Stress-Testing Extensions — Dynamic Workload + Host Failure (Decided 2026-07-09)

Supervisor sync outcome: the core feasibility question (can LLM-generated modules compose into functional, positive-effect controllers) is treated as answered across all three families. Direction shifts from adding new control-loop families to **stress-testing simulation complexity and design space** on the existing three, via two concrete scenario extensions:

- **Dynamic cloudlet injection.** Current scenario submits all 60 cloudlets at t=0 (see "Simulation Scenario" above). Extension: inject additional cloudlet workload during the run rather than only at start, testing controllers against live-arrival workload instead of a fixed batch. **Built 2026-07-16 — see "Dynamic Cloudlet Injection" below.**
- **Host-failure scenarios.** Simulate hosts failing mid-run and observe how controllers adjust — tests resilience/adaptation rather than just steady-state optimisation. Also the proposed validation mechanism for the deferred meta-controller idea (see 01-architecture.md, "Meta-Controller (Deferred)"). **Not started.**
- **Whiteboard amendment, 2026-07-09: "Host failure: none" — reconfirms the host-failure extension above** (no unexpected host failure simulated at all right now), not a separate new item; corrected after an initial mistranscription as "VM failure." Kept as a note in passing: VM-level failure (independent of its host — internal fault, forced eviction, OOM-style condition) is a plausible, distinct future gap, but hasn't actually been discussed or decided — do not treat it as scoped work.

**Status**: dynamic cloudlet injection built and validated (below); host-failure scenarios not started. Relationship to the Mixed-Pool/Stage-1/Stage-2 rollout ladder above is not yet resolved — open question whether stress-testing extensions land before or interleaved with the mixed-pool sweep. Per the standing discipline above ("never change the scenario and the module pool in the same step"), these are scenario-axis changes and should be sequenced deliberately relative to Stage 1/2, not folded in silently. Note the ladder above and the constructor it describes (`ConstructorVariableVM`/`HollowedControl`) are a separate scenario architecture from the one dynamic injection was actually built against — see below.

### Dynamic Cloudlet Injection — Built and Validated Against `SelectorScenario` (2026-07-16)

Built against the newer `Selector`/`Controller`/`ControlUnit` architecture (`SelectorScenario.java`, 00-INDEX.md "Structural multi-controller support built," 2026-07-15) rather than the `ConstructorVariableVM`/`HollowedControl` permutation-sweep architecture the rest of this file describes — the two are separate scenario harnesses; nothing below is directly comparable to the 5×5×5×3 sweep numbers above.

**Two-phase pattern, required because CloudSim's `main()` setup code isn't itself time-aware.** Injection can't just call `schedule()` from `main()` before `CloudSim.startSimulation()` — confirmed by a test run that registered an injection and produced output identical to the no-injection baseline, meaning the event never fired at all. Resolved into: a `registerInjection(time, batch)` method that stores each pending injection in a list at setup time, and the actual `schedule(getId(), delay, CloudActionTags.CLOUDLET_INJECT, batch)` call moved into `Selector.startEntity()`, which *is* part of the entity lifecycle CloudSim's scheduler recognises.

**Four real bugs found and fixed via actual test-run logs, not speculation** (individual mechanism findings duplicated in 07-limitations.md for the trap list):
1. **Silent pre-simulation scheduling drop** — the symptom that motivated the two-phase pattern above.
2. **`processCloudletInjection` never actually dispatched the batch.** `submitCloudletList()` only appends to a list; the real dispatch method (`submitCloudlets()`, protected on `DatacenterBroker`) is only ever triggered internally by `processVmCreateAck`, which doesn't refire for VMs already created. Confirmed via direct `DatacenterBroker.java` source read. Fixed by calling `submitCloudlets()` explicitly right after `submitCloudletList()` inside the injection handler.
3. **Base `DatacenterBroker.processCloudletReturn()` has its own independent termination check**, unaware of `Selector`'s `pendingInjections` bookkeeping — if the original workload fully drains before a scheduled injection fires, the base class calls `finishExecution()`/`shutdownEntity()` (which cancels all pending events) regardless of what's still queued to arrive. Fixed by overriding `processCloudletReturn` in `Selector` to add `&& pendingInjections == 0` to the emptiness check. Directly verified in a Stage-2-style test: injection scheduled for t=4000, well after the original 36 cloudlets' natural completion (~t=3800) — the simulation correctly stayed alive through the idle gap and delivered the injection rather than terminating early.
4. **`createVM`'s RNG and `createCloudlet`'s length RNG were both literally `new Random(42)`** — the exact "never reuse a stream's seed" violation this project's own convention exists to prevent (05-scenarios.md "Seed discipline" above), reintroduced in the newer scenario file. Fixed with a derived `VM_SEED = SEED ^ 0xD1B54A32D192ED03L`.

**Upgraded from fixed-interval/fixed-size to a genuine stochastic arrival process, per supervisor guidance to use CloudSim's built-in statistical distributions rather than a bigger fixed batch.** `org.cloudbus.cloudsim.distributions.ExponentialDistr` (verified against actual CloudSim source, not guessed — constructor is `ExponentialDistr(long seed, double mean)`) drives inter-arrival timing as a genuine Poisson process; `UniformDistr(1, 11, seed)` (note the `11`, not `10` — `Math.floor()` of a continuous `[1,11)` draw gives an exactly-uniform discrete `{1,...,10}`, whereas `UniformDistr(1,10)` + `Math.round()` under-samples both endpoints by half a bucket-width) drives batch size. Each distribution gets its own derived sub-seed (`ARRIVAL_SEED`, `BATCH_SEED`), never the raw scenario `SEED`. Cloudlet IDs tracked via a running `nextCloudletId` counter rather than `NUM_CLOUDLETS + i`, required once batch size stopped being fixed at 1 (fixed-offset IDs collide the moment batches vary in size).

**Validated at scale**: a 10-injection run (mean inter-arrival 100, batch size 1–10) delivered all injections on schedule with irregular Poisson-consistent spacing (including a ~2200-unit gap, expected variance for an exponential process, not a bug), correct variable-size batch dispatch, and clean termination — no hangs, no dropped events, no ID collisions across 93 total cloudlets. See 04-family-power.md, "Combined Load-Balancer + Rate-Scaling Result," for the controller-behaviour findings this scenario went on to surface once built.

### Simulation Parameters Flagged for Randomisation (Whiteboard, 2026-07-09)

Captured alongside the design-space inventory (00-INDEX.md, "Design-Space Direction") from the same whiteboard session. Currently-constant scenario parameters flagged as further simulation-complexity candidates, beyond the dynamic-cloudlet-injection and host-failure extensions above:

- **Host count** (currently fixed at 6, later 4) and **per-PE MIPS rating** (currently fixed at 1000). **Per-PE MIPS/RAM/BW rating built as discrete host-generation tiers, 2026-07-20 — see "Host Parameter Heterogeneity" below.** Host count itself remains fixed, deliberately — see the pre-existing "Population size" note in 00-INDEX.md.
- **VM count** (currently fixed at 12) — MIPS values per VM are already randomised via `MIPS_TIERS`, the count itself isn't.
- **Cloudlet count** (currently fixed at 60) — lengths are already randomised, count isn't.
- **VM RAM and BW footprint** (currently hardcoded identical per VM) — this is the same degeneracy already root-caused in 04-family-power.md, "RAM/BW as Additional Metrics": uniform footprint makes RAM/BW a pure VM-count proxy. Randomising these is the prerequisite for the RAM/BW metrics listed as not-yet-added in 00-INDEX.md, not a separate task. **Built and realism-calibrated, 2026-07-17 — see "VM RAM/BW Footprint — Realism Pass" below.**
- **Cloudlet file size** (currently constant) — new, not previously flagged anywhere in this handoff.
- **Num PEs per VM** (currently fixed at 1) — added on the whiteboard's updated pass. This is the same `pesNumber=1` constant already implicated in the `MIPS_TIERS`/`PeProvisionerSimple` mismatch (07-limitations.md) and in the verified VM PE-count-scaling mechanism (06-cloudsim-notes.md, "VM PE-Count Scaling") — randomising it would directly interact with the contention threshold (`pesInUse > cpus`) that mechanism depends on, likely making PE contention variable across VMs rather than close to universal as it probably is today.

The same whiteboard independently corroborates the dynamic-cloudlet-injection decision above: noted as "cloudlet injections: 60 @ t=0, ..." — same direction, captured twice from two angles.

**Status**: flagged, not actioned. No priority ordering given yet among these plus the two stress-testing extensions above (dynamic injection, host failure) — sequencing still needs deciding before implementation starts.

### VM RAM/BW Footprint — Realism Pass, Built and Calibrated (2026-07-17)

**Coupled tier design, decided over independent per-resource randomisation.** Given every VM in this scenario is single-core (`pesNumber=1`), MIPS/RAM/BW were built as three correlated "VM size" tuples (`MIPS_TIERS`/`RAM_TIERS`/`BW_TIERS`, one shared random index per VM) rather than three independently-drawn parameters — mirroring how real cloud instance families bundle compute/memory/network into fixed SKUs rather than mixing arbitrary combinations. Explicit project stance driving this: "keep VM parameters realistic... no point having VM parameters that make no sense."

**First version used an arbitrary small magnitude** (RAM 256/512/1024, BW 500/1000/2000, derived by preserving the existing MIPS tiers' 1:2:4 ratio and anchoring the middle tier to the scenario's old fixed defaults). Confirmed bit-identical to the pre-tiering baseline at the original cloudlet count — inert at that scale. Bumping cloudlet count 38→60 in the same session changed the raw dollar cost figures, but that was traced to the increased workload (an extensive quantity), not RAM/BW becoming binding — re-confirmed by re-running at the original cloudlet count with tiering still active and reproducing the old baseline/controller numbers bit-for-bit again.

**Rescaled to real-world magnitudes, per explicit decision to prioritise realism over continuity with prior results.** Anchored on the confirmed industry-standard 4GB-RAM-per-vCPU ratio for general-purpose cloud instances (consistent across AWS/Azure/GCP as of 2026): treating the 1000-MIPS tier as 1 vCPU-equivalent gives Large RAM = 4096MB, scaled down 1:2:4 for Medium/Small. Host RAM (16384) and BW (10000) turned out to already be realistic for a 4-core host at this ratio and needed no change — only the VM tiers were miscalibrated. Final tiers:

| Tier | MIPS | RAM | BW |
|---|---|---|---|
| Small | 250 | 1024 | 625 |
| Medium | 500 | 2048 | 1250 |
| Large | 1000 | 4096 | 2500 |

**Also confirmed bit-identical to every prior baseline, including the original pre-tiering one — this time for a structural reason, not lack of scale.** Root-caused via actual per-host VM allocation logs, not speculation: RAM and BW are exact linear multiples of MIPS for every tier (4.096× and 2.5× respectively), and the host's own RAM:MIPS/BW:MIPS capacity ratios are the identical constants, so RAM/BW can never independently bind — any VM mix filling MIPS to capacity fills RAM/BW to capacity simultaneously, confirmed directly (one host's 8 VMs summed to exactly 4000/4000 MIPS, 16384/16384 RAM, 10000/10000 BW at once). See 07-limitations.md for the full mechanism and why this is a faithful property of general-purpose instance sizing, not a bug.

**Decision: leave it here, not add an artificial memory-optimized VM tier to force RAM/BW to bind.** Doing so would reintroduce exactly the "parameters that make no sense" risk this whole exercise was trying to avoid — manufacturing a second instance family purely to make a metric move, rather than because the scenario is meant to model that workload class. RAM/BW are now realistically calibrated and legitimately non-binding for a homogeneous general-purpose fleet; that is itself the finding, not a dead end. One real, load-bearing side effect regardless of whether RAM/BW ever bind admission: migration delay (`vm.getRam() / (host.getBw()/(2×8000))`, `PowerDatacenter`) is now genuinely differentiated per VM size for the first time, rather than nearly uniform as under the old tiny/degenerate RAM values.

**Also tested: reducing host count 6→4 as an alternative way to increase packing density.** Baseline reproduced bit-identical again; the controller run shifted slightly (compute cost +1.3%, energy −9.3%, makespan unchanged), but this is attributable to the load-balancer having fewer migration destinations available, not RAM/BW admission — baseline (the direct test of static admission) didn't move at all. Confirms host-count reduction alone doesn't exercise RAM/BW either, for the same collinearity reason above.

---

### Host Parameter Heterogeneity — Built, Storage-Cost Bug Found in the Same Pass, Collinearity Genuinely Broken (2026-07-20)

**Design: discrete host-generation tiers, deliberately scoped to MIPS/RAM/BW only — core count per host left fixed.** Mirrors the existing VM-tier pattern (one shared random index per host, own derived seed `HOST_TIER_SEED = SEED ^ 0x9FB21C651E98DF25L`) rather than a continuous distribution, consistent with hosts as discrete hardware SKUs. `pesPerHost` deliberately not touched in the same step — VM/cloudlet core count is a separate, already-flagged checklist item (00-INDEX.md) that interacts with the PE-scaling controller specifically, and stacking both changes in one step would violate the project's own "one axis at a time" discipline.

**Tiers chosen to differ in ratio, not just scale — the whole point being to break the RAM/BW collinearity finding (07-limitations.md), not just resize hosts uniformly.** Framed as hardware generations, consistent with how real datacenters accumulate heterogeneous hardware over years (memory/network density per core tends to grow faster than raw per-core compute across generations):

| Tier | MIPS/PE | RAM | BW | RAM:MIPS | BW:MIPS |
|---|---|---|---|---|---|
| Legacy | 800 | 12288 | 6000 | 3.84 | 1.875 |
| Standard | 1000 | 16384 | 10000 | 4.096 | 2.5 |
| Modern | 1000 | 24576 | 16000 | 6.144 | 4.0 |

**Deliberate consequence, flagged before running rather than discovered by accident: Standard's ratio is identical to the fixed VM-tier ratio (RAM=4.096×MIPS, BW=2.5×MIPS) — left unchanged from the original host constants for continuity.** This means collinearity is now tier-dependent rather than universally broken: it still holds locally on any Standard-tier host (any VM mix filling MIPS there fills RAM/BW in lockstep, same mechanism as before), and only breaks on Legacy or Modern hosts, where the ratio genuinely diverges from the VM side.

**Side effect, flagged not yet empirically confirmed: `MIPS_TIERS` still includes 1000, above Legacy's 800 MIPS/PE cap.** A "Large" VM is structurally unplaceable on a Legacy host — the same deterministic per-PE-cap placement failure already documented in 07-limitations.md, now occurring per-host-tier rather than universally. Not yet stress-tested for a seed/host-tier draw where too many hosts land Legacy to place every Large VM at all; recommended check before trusting a heterogeneous-host result: read the VM allocation log for a placement failure or a suspiciously empty Legacy host.

**A second, independent bug was caught while validating the first heterogeneous run — not part of the heterogeneity design itself, but found via it.** `storageCost` in `main()` computed `PRICE_PER_GB_SECOND_STORAGE * (NUM_VMS * vmSizeMB / MB_PER_GB) * makespan`, where `vmSizeMB` is already the *summed total* disk footprint across all VMs (the same figure printed as "Total VM disk footprint") — multiplying by `NUM_VMS` again double-counted VM count, inflating every previously-reported storage cost (and every total/controllable cost derived from it) by exactly 12×. Confirmed by back-computing against a previously-posted run: $0.4273 (inflated) ÷ 12 = $0.0356, which is exactly what the corrected formula (`PRICE_PER_GB_SECOND_STORAGE * (vmSizeMB / MB_PER_GB) * makespan`, `NUM_VMS *` removed) produced on re-run. Every storage-cost figure logged prior to 2026-07-20 (see 04-family-power.md, "Cost — Extended to Four Components") is inflated the same way and has been corrected in place there, marked not deleted, per this project's own convention.

**First validated heterogeneous-host result, same combined-controller scenario as the utilization-tracking validation (04-family-power.md):** Average RAM util 39.06%, Average BW util 38.87% — **the first time these two signals have ever diverged in this project's history**, direct empirical confirmation the tier-ratio design genuinely breaks admission collinearity rather than just changing raw numbers. Peak RAM/BW util still coincide exactly at 75.00%/75.00% — consistent with (not contradicting) the tier-dependent explanation above: the specific host that hit peak utilization was very likely Standard-tier, where the ratio still matches the VM side by design; not yet confirmed via allocation log, flagged as the natural follow-up check.

**Compute cost, energy cost, BW cost, makespan, and average power came back bit-identical to the pre-heterogeneity run — plausible, not yet confirmed innocent.** The Legacy 800 MIPS/PE cap only binds when a VM both needs to scale to the 1000 tier *and* lands specifically on a Legacy host; it's plausible that specific combination simply didn't occur this seed. Flagged as worth verifying (via host-tier-per-host logging) before treating "host heterogeneity has zero effect on makespan/cost" as a real finding, rather than a this-seed coincidence — consistent with this project's standing rule to verify rather than assume when a number that plausibly should move doesn't.

---
---

## Scenario Levers — One-Factor-at-a-Time Design + Contention Lever Calibration (2026-08-11)

### `SelectorLoadLever.java` — Three Scenario Levers, Two Bugs Fixed

Three independent scenario "levers" were added to a new harness file, `SelectorLoadLever.java`, extending the earlier `SelectorMultiController.java` sweep pattern: `FAILURE_LOAD_OPTIONS={0,3,6}` (distinct host-failure count), `WORK_LOAD_OPTIONS={5,10,20}` (max cloudlet batch size per injection event), `CONTENTION_LOAD_OPTIONS={2,4,8}` (VM:host count ratio, `NUM_VMS = NUM_HOSTS * CONTENTION_LOAD_OPTIONS[idx]`, `NUM_HOSTS=6` fixed).

Two bugs found and fixed early this session, confirmed applied:
- **Failure-scheduling loop bug.** Original code used `if (!hostsForFailure.contains(hostId))` instead of `while`, and never actually called `hostsForFailure.add(hostId)` — meaning the dedup check was permanently false and the loop only ever registered one failure regardless of `MAX_FAILURES`. Masked because the active config (`FAILURE_LOAD=0`) never exercised more than zero failures. Fixed to `while (failCount < MAX_FAILURES) { ... hostsForFailure.add(hostId); failCount++; }`.
- **Workload lever confound.** Originally varied `MAX_INJECTIONS` (how many injection events occur), which conflates total injected volume with the duration of the injection phase — a scenario with more injections also runs its injection phase longer. Fixed by freezing `MAX_INJECTIONS=10` and varying `MAX_BATCH_SIZE` (cloudlets per injection event) instead, isolating raw volume as the intended axis.

### One-Factor-at-a-Time (OAT) Sweep Design — Decided, Not Yet Built

Rather than the full 3x3x3 = 27-combination factorial, the agreed design holds two levers at their middle index (index 1, "medium") and sweeps the third across all three of its own levels — 7 total scenarios: `{medium,medium,medium}` (baseline) plus `{low,medium,medium}`, `{high,medium,medium}`, `{medium,low,medium}`, `{medium,high,medium}`, `{medium,medium,low}`, `{medium,medium,high}`.

**Explicit tradeoff accepted:** OAT cannot detect interaction effects between levers (e.g. whether contention's effect on power depends on failure load level) — every marginal-effect curve is only valid holding the other two levers at medium, not generalizable across the full cube. In exchange: 7 scenarios instead of 27 per controller/seed, and directly-plottable single-lever marginal-effect curves instead of a 3D slice.

**Not yet implemented.** `FAILURE_LOAD`, `WORK_LOAD`, `CONTENTION_LOAD` are still `private static final int` constants pinned at `0` (floor) each, with no outer sweep loop in `main()`. A first implementation pass this session (converting the three to mutable fields, adding a `LEVER_SETTINGS`/`LEVER_LABELS` table, adding lever-identity CSV columns `failure_load`/`work_load`/`contention_load`/`lever_label`) was drafted but not completed or applied to the file — the project owner wanted the design confirmed conceptually before the code changed. Recommended shape if picked back up: `LEVER_SETTINGS = int[][]{ {1,1,1}, {0,1,1}, {2,1,1}, {1,0,1}, {1,2,1}, {1,1,0}, {1,1,2} }` with a matching `LEVER_LABELS` string array, `NUM_VMS`/`MAX_BATCH_SIZE`/`MAX_FAILURES` re-derived at the top of each outer-loop iteration from the current lever setting.

### Contention Lever Calibration — A Wrong Capacity Model Corrected Against Real Data

**Initial (wrong) model.** Reasoned from `NUM_HOSTS=6` hosts x 4 PEs/host = 24 total "PE slots," and VM tier draw uniform across `MIPS_TIERS={250,500,1000}` paired 1:1 with `CORE_TIERS={1,2,4}` (average demand (1+2+4)/3 = 2.33 PEs/VM). Under this model, `CONTENTION_LOAD=0` (ratio 2, `NUM_VMS=12`) already implied ~28 PE-units of average demand against 24 available (~117% "oversubscribed"), which was read as meaning `ratio=2` was already too loaded to be a genuine "light" floor, and a change to `CONTENTION_LOAD_OPTIONS={1,1.5,3}` was recommended.

**Confirmed wrong, two ways.** (1) Direct source read of `VmSchedulerTimeShared.allocatePesForGuest` (CloudSim library) shows no PE-slot-count ceiling exists at all — the scheduler pools a host's PEs into one aggregate MIPS budget; the only two checks are (a) each individual virtual PE's MIPS request must not exceed one physical PE's MIPS ceiling (`if (mips > peMips) return false;`), and (b) total requested MIPS must not exceed the host's remaining aggregate MIPS. A host can host far more VMs than its PE count if they're small enough — there's no rule capping VM count by PE count. (2) Real debug-log data at `CONTENTION_LOAD=2` (ratio 8, `NUM_VMS=48`) showed **30 VMs successfully created**, not the ~11-13 the PE-slot model implied — heavily skewed toward the smallest tier (17 of 30 at 250 MIPS/1 PE, 11 at 500 MIPS/2 PE, only 2 at 1000 MIPS/4 PE — large VMs are much harder to fit into an increasingly fragmented shared MIPS pool than small ones, not blocked by a PE-count wall).

**Corrected metric and result.** Properly PE-weighted realized utilization (`vm.getCurrentRequestedTotalMips()` — already total, not per-PE, confirmed against the log data by hand: 17x250 + 11x1000 + 2x4000 = 23,250 MIPS realized against 6 hosts x 4000 MIPS/host = 24,000 total = 96.875%, matching the debug print exactly) gives a clean, physically-bounded (<=100% by construction, since a VM can't be created without available capacity) progression across the three ratios: **58.3% (ratio 2) -> 72.9% (ratio 4) -> 96.9% (ratio 8)**. This already satisfies the project owner's stated calibration philosophy ("light and easy, average/expected case, stress test") without any change — light has genuine headroom, medium is busy but under the ceiling, high is essentially fully packed. **`CONTENTION_LOAD_OPTIONS={2,4,8}` is confirmed appropriate; the earlier `{1,1.5,3}` recommendation is retracted.**

**Structural note for interpreting "overload" going forward: this class of realized-utilization metric can never read above 100%, by construction — a VM that can't fit simply never gets created rather than pushing the ratio past 1.0.** True overload (demand exceeding supply) shows up elsewhere: in the growing gap between `NUM_VMS` requested and `min_live_vm_count` realized, and in `num_cloudlets_abandoned_vm_never_created`. At ratio 8, "97% utilized plus 18 of 48 requested VMs never created" is what overload looks like in this harness — not a utilization figure exceeding 100%.

**Also confirmed while investigating this: VM placement is a genuine exhaustive search, not a lazy one-shot random pick.** `SelectionPolicyCustomRandom.select()` only filters already-excluded candidates and picks randomly among what's left — it does not itself check suitability. The exhaustive part lives in `VmAllocationWithSelectionPolicy.findHostForGuest`, which loops calling `select()`, checking `isSuitableForGuest`, and adding failures to the excluded set, up to `getHostList().size()` tries before giving up. A VM only fails to place when genuinely no host in the datacenter can take it — confirmed via source, not assumed. See 06-cloudsim-notes.md for the full mechanism writeup.

### VM/Host Tier Constants — Active vs. Gemini-Suggested, Active Recommended

`SelectorLoadLever.java` carries a second, commented-out set of `MIPS_TIERS`/`HOST_MIPS_TIERS`/`RAM_TIERS`/`BW_TIERS` (labelled as Gemini-suggested) alongside the active values. Compared and active values recommended, for two reasons:

1. **RAM/BW headroom.** Active tiers keep VM RAM (1-4GB) and BW (0.625-2.5Gbps) demand close enough to host RAM (12-24GB) and BW (6-16Gbps) supply that `requestRamScaling`/`requestBwScaling` remain potentially load-bearing `ActionSpace` methods. Gemini's tiers give hosts 16-128GB RAM and 10-100Gbps BW against the same VM demand — so much headroom that those two scaling actions would likely never bind, meaning a controller could ignore them entirely without cost. Since the whole point of exposing those actions is to see whether generated Executors exercise them, the tighter active tiers are the better fit for this project's purposes specifically (not a claim about general realism).
2. **Legacy-host exclusion confound, present under both but worse under Gemini's.** `VmSchedulerTimeShared`'s strict per-virtual-PE ceiling check (see above) means a VM's per-PE MIPS demand must not exceed a host's per-PE MIPS ceiling, full stop, regardless of load. Under active tiers, legacy hosts (800 MIPS/PE) can never host the largest VM tier (1000 MIPS/PE) — a 25% overshoot. Under Gemini's tiers, legacy hosts (1000 MIPS/PE) can never host the largest VM tier (2500 MIPS/PE) — a 150% overshoot. Both schemes mean some VM-creation failures trace to which host-tier a VM's random placement drew rather than purely to the `CONTENTION_LOAD` lever, but the confound is structurally wider under Gemini's spread.

Active tiers also put the largest VM (4000 MIPS total) at exact parity with a standard/modern host's full capacity (4000 MIPS total) — a legible "one large VM can just fill a whole host" boundary condition, versus Gemini's largest VM only using half of a modern host's capacity.

