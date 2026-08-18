# 02 — QoS Family (VM Migration, 5×5×5×3)

_Architecture & GUID rules: 01-architecture.md. Scenario & constructor: 05-scenarios.md._

## LLM-Generated Modules — VM Migration Family (5×5×5×3)

### Current Variants

**Monitors** (outputGuid in parentheses)
- **monitor_v1** — CPU utilisation. `used_mips / total_mips`. (`host-cpu-util`)
- **monitor_v2** — CPU demand pressure. `sum(vm.getCurrentRequestedTotalMips()) / host.getTotalMips()`, clamped to 1.0. (`host-cpu-demand`)
- **monitor_v3** — VM count. Raw count of resident VMs per host. (`host-vm-count`)
- **monitor_v4** — Novel cpu-related metric. GUID and implementation to be confirmed from source.
- **monitor_v5** — CPU utilisation variance. Invented metric outside the permitted spec selection. See observations. (`host-cpu-variance` or similar)

**Analysers** (inputGuid → outputGuid)
- **analyser_v1** — Fixed thresholds (UPPER=0.8, LOWER=0.2). (`host-cpu-util` → `host-cpu-util-loadstate`)
- **analyser_v2** — Statistical (mean ± stddev) with absolute floor. (`host-cpu-util` → `host-cpu-util-loadstate`)
- **analyser_v3** — Statistical (mean ± stddev), wider sensitivity. (GUID to be confirmed from source)
- **analyser_v4** — IQR outlier detection. (`host-cpu-demand` → `host-cpu-demand-loadstate`)
- **analyser_v5** — Mean-relative thresholds (OVERLOADED if value > mean×1.5, UNDERLOADED if value < mean×0.5). (`host-vm-count` → `host-vm-count-loadstate`)

**Planners** (inputGuid → `host-migration-pair`)
- **planner_v1** — Overload relief. Triggers on OVERLOADED. Selects highest-MIPS VM, prefers UNDERLOADED destination, falls back to BALANCED.
- **planner_v2** — Consolidation. Triggers on UNDERLOADED. Selects lowest-MIPS VM, prefers BALANCED destination.
- **planner_v3** — Overload relief variant.
- **planner_v4** — Variant. Implementation to be confirmed from source.
- **planner_v5** — Low conversion rate (9–17% on active pairings), but produces the single best load-balance result in the dataset under the corrected demand-based ground truth (0.021875, a 90.2% reduction vs. baseline). Originally characterised as a "systematic failure" under the deprecated cpu-util ground truth — that verdict was an artifact of ground-truth noise, not a flaw in planner logic. See "Demand-Based Ground Truth Run" and "Known Limitations" below.

**Executors** (inputGuid: `host-migration-pair`)
- **executor_v1, v2, v3** — All functionally equivalent. Resolve VM and host by ID, call `requestVmMigration()`. Confirmed non-discriminating across all 375 permutations.

### 5×5×5×3 Permutation Results Summary — CPU-Util Ground Truth (SUPERSEDED 2026-07-01)

**Superseded.** This run used the deprecated `getTotalUtilizationOfCpuMips`-based ground truth, later found to be scheduler-timing-noise-dependent and structurally uncapped (see "CloudSim Observability Constraint" and "Ground Truth Metric" below). Retained for historical comparison — most qualitative findings replicated under the corrected demand-based metric (see "5×5×5×3 Run — Demand-Based Ground Truth (Current)" below), with one notable reversal (planner_v5).

375 total permutations. All completed without crash (FAILED status: 0).

**Baseline (no controller, `MeasuringBroker`, same scenario): 2.687.**

22 distinct variance values across 375 permutations. Range: 0.665 to 2.687 (spread = 2.02).

| Tier | variance | vs baseline | controllers |
|---|---|---|---|
| Baseline (no controller) | 2.687 | — | — |
| Dead controllers (analyser_v2/v4) | 2.687 | 0% | 120 |
| Compatible (worst) | 2.687 | 0% | — |
| Compatible (best) | 0.707 | −74% | monitor_v3+analyser_v5+planner_v3 |
| Incompatible (best) | 0.665 | −75% | monitor_v1+analyser_v3+planner_v4 |

**The planner slot is now the primary discriminating component.** Within `monitor_v1 + analyser_v1`, planner choice alone spans a variance range of 0.95 (0.670 to 1.617) while all planners have similar conversion rates (9–36%). Migration strategy quality matters far more than migration frequency.

| Analyser | Variance range | Note |
|---|---|---|
| analyser_v1 | 0.670–1.617 | Fixed thresholds — active; wide planner-driven spread |
| analyser_v2 | 2.687 (always) | Dead signal in this scenario — 0% conversion, baseline-level variance |
| analyser_v3 | 0.665–1.061 | Statistical thresholds — effective, narrower spread than v1 |
| analyser_v4 | 2.687 (always) | IQR collapses — zero migrations, baseline-level variance |
| analyser_v5 | 0.665–1.133 | Mean-relative — effective regardless of monitor GUID pairing |

**Conversion rate is negatively correlated with quality at the planner level.** planner_v2 (consolidation, triggers on UNDERLOADED) achieves 36% conversion but produces the worst load balance among active planners (1.617). planner_v4 achieves 13% but produces the best (0.670). Fewer well-chosen migrations outperform frequent poor ones. The consolidation strategy actively worsens balance by moving VMs in the wrong direction.

**GUID compatibility does not predict performance.** Compatible mean variance = 1.934; Incompatible mean = 1.695. Compatible combinations include both the best (0.707) and worst (2.687) outcomes — the analyser's internal logic determines quality, not its GUID label.

**analyser_v2 and analyser_v4 are functionally equivalent to no controller** — their variance matches the unmanaged baseline exactly.

**makespan**: non-discriminating — identical across all 375 permutations.

**Status breakdown** (125 unique controller configurations): ACTIVE 89, INERT 10.

### Key Observations from 5×5×5×3 Run — CPU-Util Ground Truth (SUPERSEDED 2026-07-01)

**Superseded — see caveat above.** One finding below (planner_v5) was overturned by the demand-based re-run; the rest replicated. Kept intact for the record.

**Structural compliance: 100%** — all modules compiled and integrated without modification.

**GUID self-labelling is accurate.** Modules that operate on a novel or non-standard metric correctly declared non-standard GUIDs rather than falsely claiming a standard one. analyser_v4 labelled its output `host-cpu-demand-loadstate`; analyser_v5 labelled `host-vm-count-loadstate`; monitor_v5 declared a novel GUID for its invented metric. The LLM's self-labelling behaviour is honest even when it breaks compatibility.

**monitor_v5 invented a metric outside the spec** — CPU utilisation variance is not in the permitted metric selection (cpu-util, cpu-demand, vm-count). It is a valid and meaningful metric computed using only approved API calls. This is *metric invention*, not API hallucination: structurally valid, semantically coherent, outside the specification boundary.

**analyser_v4 (IQR on cpu-demand) — dead signal.** IQR outlier detection requires variance to find outliers. CPU demand values in the current scenario cluster tightly, causing the IQR to collapse and fences to converge. Only 3–7 actionable cycles detected across all monitor pairings, 0 conversion in all runs. IQR is statistically rigorous but environmentally brittle in a low-variance, small-host-count scenario.

**analyser_v5 (mean-relative on vm-count) — dominant performer despite GUID incompatibility.** Mean×1.5/0.5 thresholds are distribution-agnostic: they adapt to whatever scale they receive. When paired with cpu-util monitors, the analyser calibrates to [0,1] values and effectively becomes a relative CPU threshold. This produces the highest conversion rates in the dataset (50–89%), all from GUID-incompatible combinations. The LLM generated a policy so generic it transcends the GUID contract it was supposed to implement.

**GUID compatibility does not predict performance.** Compatible combinations reliably filter INERT pipelines (no compatible combination is INERT), but do not predict quality within the ACTIVE set. The best-performing controllers are all GUID-incompatible.

**`LoadState` is an underspecified contract.** `UNDERLOADED` means the host is below the analyser's load threshold for the monitored metric. It does not imply spare provisioned capacity. `isSuitableForGuest` checks provisioned capacity, not runtime utilisation — a host correctly classified UNDERLOADED can still reject every migration attempt. This semantic gap is not detectable by GUID matching.

**`isSuitableForGuest` misused as selection criterion.** LLM-generated planners use it as the primary destination selection criterion rather than as a final feasibility guard. Correct pattern: (1) select destination on utilisation grounds using the metric indicated by inputGuid, (2) call `isSuitableForGuest` as a final guard, (3) iterate candidates on rejection rather than returning sentinel. The approved API list includes `isSuitableForGuest` but does not specify its intended role — this is a prompting gap.

**planner_v5 — systematic failure (OVERTURNED, see Demand-Based Ground Truth Run below).** 15% conversion rate, avg 9% when it does convert. Under the cpu-util ground truth this looked like a failure to find valid migration targets. Under the corrected demand-based ground truth, the same low-conversion behaviour produces the best variance in the dataset — the "failure" label was measuring ground-truth noise, not planner quality.

**monitor_v2 consistently underperforms.** Best achievable variance with monitor_v2 is 0.823 (paired with analyser_v5) vs 0.665 with other monitors. Despite measuring the same underlying signal as monitor_v1 via a different API call, its values lead to worse planner decisions. See CloudSim Observability Constraint below.

**monitor_v3 (vm-count) — active but mid-tier performance.** Achieves 0.707 when paired with compatible analyser_v5+planner_v3. VM count is a coarser signal than demand pressure but still actionable in an undersubscribed scenario where host occupancy correlates with load.

**planner_v2 (consolidation) is actively harmful.** Triggers on UNDERLOADED hosts and moves VMs away from them, which redistributes load to already-busy hosts. In an undersubscribed scenario, this worsens rather than improves balance. Highest conversion rate (up to 36%) but worst load balance outcome among active planners.

**Executor slot confirmed non-discriminating** across all 375 permutations.

**makespan non-discriminating** — identical across all permutations. VM migration redistributes CPU load but cannot create MIPS. In a time-shared scheduler, total work-time is conserved regardless of migration decisions.

---

### 5×5×5×3 Run — Demand-Based Ground Truth (CURRENT, 2026-07-01)

Same 375-permutation sweep (5 monitors × 5 analysers × 5 planners × 3 executors), re-run with the ground truth switched from `getTotalUtilizationOfCpuMips` to `getCurrentRequestedTotalMips` (demand pressure) — see "Ground Truth Metric" and "CloudSim Observability Constraint" below for why this switch was made and why it is structurally sound.

**Baseline (no controller): 0.222222.** This is not just an empirically low number — it is the exact value every dead/INERT pipeline converges to, confirming it is the true unmanaged floor for this scenario (same convergence property held under the old metric).

**Overall best: 0.021875** (`monitor_v1 + analyser_v1 + planner_v5`) — a **90.2% reduction** vs. baseline, well beyond the best result under the old metric (74%). 32 distinct variance values across 375 permutations; range 0.021875–0.222222.

125 unique (monitor, analyser, planner) triples: 115 ACTIVE, 10 INERT (× 3 non-discriminating executors = 375 rows). The same 10 triples are INERT as in the util-based run — `{monitor_v2, monitor_v3} × analyser_v4 × {all planners}`.

| Tier | variance | vs baseline | controllers |
|---|---|---|---|
| Baseline (no controller / INERT) | 0.222222 | — | 10 unique triples |
| Dead-but-ACTIVE (analyser_v2 always; analyser_v4 w/ monitor_v1/v4/v5) | 0.222222 | 0% | 0% conversion in all cases |
| Best | 0.021875 | −90.2% | monitor_v1+analyser_v1+planner_v5 |
| Second tier | 0.024537 | −89.0% | several pairings, mostly with planner_v5 |

**Replicated findings (unchanged from the cpu-util run):**

- **analyser_v2 and analyser_v4 are still dead signal.** analyser_v2 flags actionable cycles (45/45) but converts 0% of them in every single pairing, landing exactly on baseline. analyser_v4 (IQR) is INERT (0 actionable cycles) when paired with monitor_v2/monitor_v3, and ACTIVE-but-0%-conversion with the other three monitors. Same root cause as before (IQR collapses on low-variance input).
- **analyser_v5 (mean-relative) is the dominant analyser**, and more clearly so now: mean variance 0.053 — less than half of the next-best analyser (analyser_v1 at 0.112) — and it never bottoms out at baseline (max 0.126, i.e. no dead combination exists for it).
- **planner_v2 (consolidation) is still the worst active planner** — highest mean variance among active planners (0.098 on converting runs) and the highest average conversion rate (37.6%). Confirms "frequent migrations in the wrong direction beat nothing, but underperform selective correct ones."
- **GUID compatibility still does not predict quality.** Compatible-combination mean (0.155) is slightly worse than incompatible-combination mean (0.146) — same direction as the cpu-util run.
- **Executor and makespan remain non-discriminating.**
- **Conversion rate vs. quality, restated more precisely:** across converting runs, mean variance ranks with conversion rate almost in lockstep — planner_v5 (17% conversion → 0.032 variance), planner_v3 (32% → 0.058), planner_v4 (33% → 0.068), planner_v1 (28% → 0.074), planner_v2 (38% → 0.098). Fewer, well-targeted migrations consistently outperform frequent ones.

**Reversed finding — the one to flag prominently in the thesis:**

**planner_v5 flips from "systematic failure" to best-in-dataset.** Its conversion rate is unchanged (9–17%, same low-activity profile as before) — what changed is the ground truth used to judge the outcome. Under the noisy cpu-util metric, its results looked mediocre-to-bad; under the demand-based metric, the same decisions produce the lowest variance in the entire sweep. This is a concrete, dataset-level demonstration that a module-quality conclusion can be an artifact of ground-truth measurement error rather than the module's actual logic — worth its own paragraph in the methodology/limitations discussion, independent of the GUID-composability findings.

**Practical implication:** any per-module characterisation made under the old ground truth (monitor_v2 underperformance, planner rankings, etc.) should be treated as provisional until cross-checked against the demand-based numbers. Everything except planner_v5 held up under the switch, which is reassuring for the framework's overall validity, but the planner_v5 case shows this cross-check is necessary, not optional.

### planner_v5 — Mechanism Confirmed from Source (2026-07-01)

Reading `planner_v5.java` resolved *why* the reversal happens, and it's a better explanation than "ground-truth noise" alone.

`planner_v5` implements **global variance minimisation via exhaustive search**: it snapshots every host's load as `Σ vm.getCurrentRequestedTotalMips()` (demand pressure, not cpu-util), simulates every candidate (OVERLOADED-source, UNDERLOADED-destination, suitable) migration, and only commits to the one that strictly reduces that variance versus doing nothing. Two consequences:

- **It only uses the `LoadState[] diagnosis` array as a coarse gate** (source must be OVERLOADED, destination must be UNDERLOADED) — the actual scoring is recomputed independently from raw `ReadSpace` entity state, via the `getCurrentRequestedTotalMips()` call already on the approved Planner API list. This is legitimate use of the existing boundary leak (`ReadSpace` exposes CloudSim entities — see Key Design Decisions), not a hallucination or an unapproved call.
- **Its internal objective function and the demand-based ground truth are essentially the same quantity.** Both are population variance of per-host demand pressure; since all 6 hosts in this scenario are homogeneous (identical MIPS capacity), variance of raw requested-MIPS load and variance of the capacity-normalised ratio rank identically. planner_v5 isn't winning because it "got smarter" under the new metric — it's winning because the evaluation metric now finally measures the same thing it was always internally optimising for. Under the old cpu-util ground truth, it was being scored on a quantity it never used internally, which is why it looked mediocre.

This also explains why analyser choice barely matters for `monitor_v1 + planner_v5` (byte-identical results across analyser_v1/v3/v5, see below): those three all agree almost every cycle on which hosts are OVERLOADED/UNDERLOADED (monitor_v1's readings are bimodal — near-0 when idle, far above any threshold when active — see CloudSim Observability Constraint), so the candidate pool planner_v5 searches is nearly identical regardless of which analyser produced it, and its own scoring picks the same migration from that pool every time.

**Correction to the GUID self-labelling narrative:** planner_v5 declares `inputGuid: "host-cpu-util-loadstate"` but its scoring logic is entirely demand-based, independent of whatever metric produced the LoadState array. This is not dishonesty about what data it receives (it genuinely does gate on the declared LoadState array) — but it does violate this project's own stated rule that "the GUID is an active semantic contract on planning behaviour, not just a routing key" (see GUID Naming Ruleset). The better framing, on reflection, isn't "mislabelling" — it's that planner_v5 is **GUID-agnostic by design**, structurally the same pattern as analyser_v5 being distribution-agnostic (see below): both of the two best-performing modules in the entire sweep are the ones that decline to let their declared input contract constrain their internal logic, and instead verify against something more universal. That looks like a genuine, generalisable finding: GUID-agnostic modules outperformed GUID-calibrated ones at two independent pipeline stages.

**Caveat:** this is a small-sample result — only 5–6 migrations total across 45 cycles for the best pairings — and monitor_v1's coarse-gate viability is likely scenario-dependent (see "Actionable-Cycle Gate" below and CloudSim Observability Constraint). Worth a seed/VM-mix variation before treating monitor_v1+planner_v5 as a canonical best configuration rather than a favourable alignment specific to this scenario instance.

---

### Actionable-Cycle Gate — Diagnosed as Weak, Instrumentation Redesigned (2026-07-01)

**Finding:** `actionable_cycles` (Analyser flags ≥1 host as OVERLOADED **or** UNDERLOADED) saturates to the ceiling (45/45, the maximum possible given `makespan=4530` / `observationRate=100`) in 66% of all 375 rows and 72% of ACTIVE rows. Breaking down by analyser:

- **analyser_v1, analyser_v2, analyser_v3** hit exactly 45/45 in *every* pairing, with *every* monitor, no exceptions — including monitor_v2/v3/v4, which are well-bounded, sane signals. This isn't downstream of cpu-util's brokenness; it's these three analyser designs (fixed / mean±stddev / floor-based single-pass thresholds) combined with a scenario deliberately engineered for persistent, not transient, imbalance (First Fit placement — see Simulation Scenario). With 6 independently-thresholded hosts and a permanently uneven initial state, "at least one host out of band" is a bar these three analysers apparently never fail to clear.
- **analyser_v4 (IQR) and analyser_v5 (mean-relative)** are the only two with real dynamic range (0–45), and analyser_v5's count visibly *drops* when a better planner is installed for the same monitor+analyser pair — genuine evidence the closed loop is resolving imbalance, not just permanently flagging it.

**Root cause of the metric's weakness:** "at least one OVERLOADED or UNDERLOADED" measures *an anomaly exists*, not *a migration is possible*. A migration needs a source (OVERLOADED) **and** a destination (UNDERLOADED) to coexist in the same cycle — most planners (`planner_v5` strictly; others via a BALANCED-destination fallback) need both, not either. The current gate can be maximally saturated while genuine source/destination co-occurrence is rare, which means part of the gap between high actionable_cycles and low conversion_rate reflects the metric's looseness, not planner selectivity — the two were being conflated.

**Resolution — a stricter, planner-agnostic "opportunity" gate**, computed purely from the same `LoadState[]` array: `opportunityCycles` requires ≥1 OVERLOADED **and** ≥1 UNDERLOADED in the same cycle. This is a materially better denominator for `conversion_rate`, since it approximates "could any reasonable planner have acted here" without needing per-planner fallback knowledge. `imbalanceCycles` (the old `actionable_cycles`, renamed conceptually though the CSV column name is kept for continuity) is retained as an analyser-health diagnostic only — it is not a controller-effectiveness metric, since it's degenerate (constant) for 3 of 5 analysers in this scenario.

A further, deferred option: an *oracle* "improving-opportunity" check — run planner_v5's own exhaustive search as a standalone diagnostic regardless of which planner is actually installed, giving a ceiling any of the five planners' realised conversions could be benchmarked against. Not implemented; noted as a stretch goal.

Ground truth (`groundTruthAvgVariance`) remains the top-line success measure regardless of which gate is used — these are diagnostic layers underneath it, meant to explain results, not replace them.

---

### Connection to the SelectorScenario Bug-Fix Chain and Goals-vs-Mechanisms (2026-07-16)

This family's "**Executor and makespan remain non-discriminating**" finding (above, and repeated across the 375-permutation sweep) turns out to have a mechanistic explanation, independently arrived at via a completely different scenario harness (`SelectorScenario`, not this family's `ConstructorVariableVM`/`HollowedControl` permutation sweep — see 05-scenarios.md for the architecture distinction). That investigation (04-family-power.md, "Rate-Scaling and Load-Balancer Bug Fixes") traced, at the source level, exactly why load-balancing alone doesn't move makespan: migration only relieves *aggregate host contention*, and CloudSim's `VmSchedulerTimeShared` hard-rejects oversubscription with no partial/graceful degradation (confirmed directly from source, see 07-limitations.md) — so unless some other mechanism is actually positioned to consume the freed capacity in a way that shortens the critical path, freeing it changes nothing measurable downstream. This is the same conclusion this family reached empirically across 375 permutations, now with a concrete causal story rather than just a repeated null result.

**The eventual exception (04-family-power.md, "Combined Load-Balancer + Rate-Scaling Result") is consistent with, not a contradiction of, this family's finding.** Makespan only moved once load-balancing was paired with a second mechanism (MIPS rate-scaling) that was both correctly implemented (see the `requestMipsScaling`/`VmScheduler`-bypass fix) and specifically positioned to use the newly-freed capacity on the critical-path VM. Load-balancing's own contribution was enabling, not causal — it did not move makespan by itself in that test either, exactly as this family's sweep already established; it only mattered once a capacity-consuming mechanism existed to exploit the room it made. See 01-architecture.md, "Goals vs. Mechanisms," for the general statement of this distinction: load-balancing is a mechanism, and this family's own ground-truth metric (host-demand variance, not makespan) was the correct choice all along — makespan was never the right axis to expect it to move on its own.

---

