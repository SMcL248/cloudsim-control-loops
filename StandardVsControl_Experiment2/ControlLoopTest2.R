# ============================================================
# VM MIPS Experiment Analysis
# Varies VM MIPS seed, fixed cloudlet seed and count
# Tests whether ControlBroker improves makespan vs baseline
# (standard DatacenterBroker with no migration)
# ============================================================

library(ggplot2)

# ---- 1. Import ----
df <- read.csv("C:/CloudSim/cloudsim-7.0.1/experiment_results_vm.csv", comment.char = "#")

cat("Total runs:", nrow(df), "\n")

# ---- 2. Exclude failures (NaN makespan) ----
df_clean <- df[!is.nan(df$baseline_makespan) & !is.nan(df$controlled_makespan), ]

cat("Failed runs excluded:", nrow(df) - nrow(df_clean), "\n")
cat("Valid runs for analysis:", nrow(df_clean), "\n\n")

# ---- 3. Descriptive statistics ----
cat("=== Descriptive Statistics ===\n")
cat(sprintf("Baseline (no migration) | Mean: %.2f  SD: %.2f  Min: %.2f  Max: %.2f\n",
            mean(df_clean$baseline_makespan),   sd(df_clean$baseline_makespan),
            min(df_clean$baseline_makespan),    max(df_clean$baseline_makespan)))
cat(sprintf("ControlBroker           | Mean: %.2f  SD: %.2f  Min: %.2f  Max: %.2f\n",
            mean(df_clean$controlled_makespan), sd(df_clean$controlled_makespan),
            min(df_clean$controlled_makespan),  max(df_clean$controlled_makespan)))
cat(sprintf("\nDelta (baseline - controlled) | Mean: %.2f  SD: %.2f\n",
            mean(df_clean$delta), sd(df_clean$delta)))
cat(sprintf("CB faster than baseline: %d / %d runs\n",
            sum(df_clean$delta > 0),  nrow(df_clean)))
cat(sprintf("CB slower than baseline: %d / %d runs\n",
            sum(df_clean$delta < 0),  nrow(df_clean)))
cat(sprintf("Tied:                    %d / %d runs\n\n",
            sum(df_clean$delta == 0), nrow(df_clean)))

# ---- 4. Normality check on deltas ----
cat("=== Shapiro-Wilk Normality Test on Delta ===\n")
# Shapiro-Wilk requires n >= 3; skip gracefully if too few rows
if (nrow(df_clean) >= 3) {
  shapiro_result <- shapiro.test(df_clean$delta)
  print(shapiro_result)
  cat(sprintf("Deltas appear %s (p = %.4f)\n\n",
              ifelse(shapiro_result$p.value >= 0.05, "normally distributed", "non-normal"),
              shapiro_result$p.value))
} else {
  cat("Too few observations for Shapiro-Wilk test.\n\n")
  shapiro_result <- list(p.value = NA)
}

# ---- 5. Paired t-test (parametric) ----
cat("=== Paired t-test (H0: mean delta = 0) ===\n")
t_result <- t.test(df_clean$baseline_makespan, df_clean$controlled_makespan, paired = TRUE)
print(t_result)
cat(sprintf("\nConclusion: %s\n\n",
            ifelse(t_result$p.value < 0.05,
                   "Significant difference (p < 0.05) — ControlBroker meaningfully changes makespan.",
                   "No significant difference (p >= 0.05) — cannot conclude ControlBroker improves makespan.")))

# ---- 6. Wilcoxon signed-rank test (non-parametric) ----
# More appropriate if Shapiro-Wilk flagged non-normality
cat("=== Wilcoxon Signed-Rank Test (H0: median delta = 0) ===\n")
wilcox_result <- wilcox.test(df_clean$baseline_makespan, df_clean$controlled_makespan,
                             paired = TRUE, exact = FALSE)
print(wilcox_result)
cat(sprintf("\nConclusion: %s\n\n",
            ifelse(wilcox_result$p.value < 0.05,
                   "Significant difference (p < 0.05).",
                   "No significant difference (p >= 0.05).")))

# ---- 7. Effect size — Cohen's d ----
cat("=== Effect Size (Cohen's d) ===\n")
cohens_d <- mean(df_clean$delta) / sd(df_clean$delta)
cat(sprintf("Cohen's d = %.4f\n", cohens_d))
cat(sprintf("Interpretation: %s\n\n",
            ifelse(abs(cohens_d) >= 0.8, "Large effect",
                   ifelse(abs(cohens_d) >= 0.5, "Medium effect",
                          ifelse(abs(cohens_d) >= 0.2, "Small effect", "Negligible effect")))))


# ---- 9. Plot A: delta distribution histogram ----
p_hist <- ggplot(df_clean, aes(x = delta)) +
  geom_histogram(binwidth = 20, fill = "#4E79A7", colour = "white", alpha = 0.85) +
  geom_vline(xintercept = 0,
             linetype = "dashed", colour = "#E15759", linewidth = 0.8) +
  geom_vline(xintercept = mean(df_clean$delta),
             linetype = "solid", colour = "#F28E2B", linewidth = 0.9) +
  annotate("text",
           x = mean(df_clean$delta) + 5, y = Inf,
           label = sprintf("Mean delta = %.1f", mean(df_clean$delta)),
           hjust = 0, vjust = 1.5, colour = "#F28E2B", size = 3.5) +
  annotate("text",
           x = max(df_clean$delta) * 0.05, y = Inf,
           label = sprintf("t-test p = %.4f", t_result$p.value),
           hjust = 0, vjust = 3.2, colour = "#E15759", size = 3.5) +
  labs(
    title    = "Makespan Delta Distribution (Baseline − ControlBroker)",
    subtitle = "Positive = ControlBroker faster  |  Negative = Baseline faster",
    x        = "Delta (time units)",
    y        = "Count",
    caption  = sprintf("n = %d valid runs  |  varying VM MIPS seed, fixed cloudlet seed",
                       nrow(df_clean))
  ) +
  theme_minimal(base_size = 13) +
  theme(
    plot.title    = element_text(face = "bold"),
    plot.subtitle = element_text(colour = "grey40"),
    plot.caption  = element_text(colour = "grey50")
  )

ggsave("delta_distribution_vm.png", plot = p_hist, width = 8, height = 5, dpi = 150)
cat("Plot saved to: delta_distribution_vm.png\n")

# ---- 10. Plot B: paired makespan scatter ----
# Each point is one seed; above diagonal = ControlBroker faster
p_scatter <- ggplot(df_clean, aes(x = baseline_makespan, y = controlled_makespan)) +
  geom_abline(slope = 1, intercept = 0,
              linetype = "dashed", colour = "#E15759", linewidth = 0.7) +
  geom_point(aes(colour = delta > 0), size = 3, alpha = 0.85) +
  scale_colour_manual(
    values = c("TRUE" = "#4E79A7", "FALSE" = "#E15759"),
    labels = c("TRUE" = "CB faster", "FALSE" = "Baseline faster"),
    name   = NULL
  ) +
  labs(
    title    = "Paired Makespan: Baseline vs ControlBroker",
    subtitle = "Points below diagonal = ControlBroker faster",
    x        = "Baseline makespan (time units)",
    y        = "ControlBroker makespan (time units)",
    caption  = sprintf("n = %d  |  varying VM MIPS seed", nrow(df_clean))
  ) +
  theme_minimal(base_size = 13) +
  theme(
    plot.title    = element_text(face = "bold"),
    plot.subtitle = element_text(colour = "grey40"),
    plot.caption  = element_text(colour = "grey50"),
    legend.position = "top"
  )

ggsave("paired_makespan_vm.png", plot = p_scatter, width = 7, height = 6, dpi = 150)
cat("Plot saved to: paired_makespan_vm.png\n")