# ============================================================
# Broker Experiment Analysis
# Paired t-test + delta distribution plot
# ============================================================

library(ggplot2)

# ---- 1. Import ----
df <- read.csv("C:/CloudSim/cloudsim-7.0.1/experiment_results.csv")

cat("Total runs:", nrow(df), "\n")

# ---- 2. Exclude failures (CB returned -1.0) ----
df_clean <- df[df$makespan_cb != -1.0, ]

cat("Failed runs excluded:", nrow(df) - nrow(df_clean), "\n")
cat("Valid runs for analysis:", nrow(df_clean), "\n\n")

# ---- 3. Descriptive statistics ----
cat("=== Descriptive Statistics ===\n")
cat(sprintf("Round Robin  | Mean: %.2f  SD: %.2f  Min: %.2f  Max: %.2f\n",
            mean(df_clean$makespan_rr), sd(df_clean$makespan_rr),
            min(df_clean$makespan_rr), max(df_clean$makespan_rr)))
cat(sprintf("Control Broker| Mean: %.2f  SD: %.2f  Min: %.2f  Max: %.2f\n",
            mean(df_clean$makespan_cb), sd(df_clean$makespan_cb),
            min(df_clean$makespan_cb), max(df_clean$makespan_cb)))
cat(sprintf("\nDelta (RR - CB) | Mean: %.2f  SD: %.2f\n",
            mean(df_clean$delta), sd(df_clean$delta)))
cat(sprintf("CB faster than RR: %d / %d runs\n",
            sum(df_clean$delta > 0), nrow(df_clean)))
cat(sprintf("CB slower than RR: %d / %d runs\n",
            sum(df_clean$delta < 0), nrow(df_clean)))
cat(sprintf("Tied:             %d / %d runs\n\n",
            sum(df_clean$delta == 0), nrow(df_clean)))

# ---- 4. Paired t-test ----
cat("=== Paired t-test (H0: mean delta = 0) ===\n")
t_result <- t.test(df_clean$makespan_rr, df_clean$makespan_cb, paired = TRUE)
print(t_result)

cat(sprintf("\nConclusion: %s\n",
            ifelse(t_result$p.value < 0.05,
                   "Significant difference (p < 0.05) — ControlBroker meaningfully changes makespan.",
                   "No significant difference (p >= 0.05) — cannot conclude ControlBroker improves makespan.")))

wilcox.test(df_clean$makespan_rr, df_clean$makespan_cb, paired = TRUE)


# ---- 5. Plot: delta distribution ----
p <- ggplot(df_clean, aes(x = delta)) +
  geom_histogram(binwidth = 20, fill = "#4E79A7", colour = "white", alpha = 0.85) +
  geom_vline(xintercept = 0,
             linetype = "dashed", colour = "#E15759", linewidth = 0.8) +
  geom_vline(xintercept = mean(df_clean$delta),
             linetype = "solid", colour = "#F28E2B", linewidth = 0.9) +
  annotate("text",
           x = mean(df_clean$delta) + 8, y = Inf,
           label = sprintf("Mean = %.1f", mean(df_clean$delta)),
           hjust = 0, vjust = 1.5, colour = "#F28E2B", size = 3.5) +
  annotate("text",
           x = 2, y = Inf,
           label = sprintf("p = %.4f", t_result$p.value),
           hjust = 0, vjust = 3.2, colour = "#E15759", size = 3.5) +
  labs(
    title    = "Makespan Delta Distribution (RR − ControlBroker)",
    subtitle = "Positive = ControlBroker faster  |  Negative = RoundRobin faster",
    x        = "Delta (time units)",
    y        = "Count",
    caption  = sprintf("n = %d valid runs  |  %d runs excluded (CB error)",
                       nrow(df_clean), nrow(df) - nrow(df_clean))
  ) +
  theme_minimal(base_size = 13) +
  theme(
    plot.title    = element_text(face = "bold"),
    plot.subtitle = element_text(colour = "grey40"),
    plot.caption  = element_text(colour = "grey50")
  )

ggsave("delta_distribution.png", plot = p, width = 8, height = 5, dpi = 150)
cat("\nPlot saved to: delta_distribution.png\n")
