package flowInterpreter;

import java.util.ArrayList;
import java.util.List;
class DamData {
    final String name;
    final double capacity;
    final double maxFlowRate;
    final double startLevel;
    final List<Double> ownRainfall;
    final int simulateDays;
    final List<PolicyRule> policyRules;
    
    // Computed data
    double currentLevel;
    final List<Double> damLevelPercentages;
    final List<Double> inflowPerDay;
    final List<Double> ownRainPerDay;
    final List<Double> outflowPerDay;
    
    static class PolicyRule {
        enum RuleType { THRESHOLD, DEFAULT }
        final RuleType type;
        final Double threshold;
        final Double releaseRate;
        
        PolicyRule(RuleType type, Double threshold, Double releaseRate) {
            this.type = type;
            this.threshold = threshold;
            this.releaseRate = releaseRate;
        }
    }
    
    DamData(String name, double capacity, double maxFlowRate, double startLevel, 
            List<Double> ownRainfall, int simulateDays, List<PolicyRule> policyRules, 
            List<List<Double>> inflowSources) {
        this.name = name;
        this.capacity = capacity;
        this.maxFlowRate = maxFlowRate;
        this.startLevel = startLevel;
        this.ownRainfall = new ArrayList<>(ownRainfall);
        this.simulateDays = simulateDays;
        this.policyRules = new ArrayList<>(policyRules);
        
        this.currentLevel = capacity * startLevel;
        this.damLevelPercentages = new ArrayList<>();
        this.inflowPerDay = new ArrayList<>();
        this.ownRainPerDay = new ArrayList<>();
        this.outflowPerDay = new ArrayList<>();
        
        // Compute simulation
        computeSimulation(inflowSources);
    }
    
    private void computeSimulation(List<List<Double>> inflowSources) {
        // Combine all inflow sources (already with decay applied from interpreter)
        List<Double> combinedInflow = combineInflows(inflowSources);
        
        // Simulate each day
        for (int day = 0; day < simulateDays; day++) {
            double inflow = day < combinedInflow.size() ? combinedInflow.get(day) : 0.0;
            double ownRain = day < ownRainfall.size() ? ownRainfall.get(day) : 0.0;
            double totalInflow = inflow + ownRain;
            
            // Add water to dam first
            currentLevel += totalInflow;
            currentLevel = Math.min(currentLevel, capacity); // Can't exceed capacity
            
            // Calculate release based on policy AFTER adding water
            double fillRatio = currentLevel / capacity;
            double releaseRate = evaluatePolicy(fillRatio);
            
            // Release is a percentage of max flow rate
            double desiredRelease = maxFlowRate * releaseRate;
            // But can't release more than what's in the dam
            double release = Math.min(desiredRelease, currentLevel);
            
            // Release water
            currentLevel -= release;
            currentLevel = Math.max(0, currentLevel); // Can't go negative
            
            // Record data
            damLevelPercentages.add((currentLevel / capacity) * 100.0);
            inflowPerDay.add(inflow);
            ownRainPerDay.add(ownRain);
            outflowPerDay.add(release);
        }
    }
    
    private List<Double> combineInflows(List<List<Double>> inflowSources) {
        if (inflowSources.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Find max days
        int maxDays = 0;
        for (List<Double> source : inflowSources) {
            maxDays = Math.max(maxDays, source.size());
        }
        
        // Combine all sources
        List<Double> combined = new ArrayList<>();
        for (int day = 0; day < maxDays; day++) {
            double total = 0.0;
            for (List<Double> source : inflowSources) {
                if (day < source.size()) {
                    total += source.get(day);
                }
            }
            combined.add(total);
        }
        
        return combined;
    }
    
    private double evaluatePolicy(double fillRatio) {
    // If no policy rules, pass through everything
    if (policyRules.isEmpty()) {
        return 1.0;
    }
    
    // Sort threshold rules from highest to lowest
    List<PolicyRule> thresholds = new ArrayList<>();
    PolicyRule defaultRule = null;
    
    for (PolicyRule rule : policyRules) {
        if (rule.type == PolicyRule.RuleType.THRESHOLD) {
        thresholds.add(rule);
        } else if (rule.type == PolicyRule.RuleType.DEFAULT) {
        defaultRule = rule;
        }
    }
    
    // Sort thresholds from highest to lowest
    thresholds.sort((a, b) -> Double.compare(b.threshold, a.threshold));
    
    // Find the first threshold that fillRatio meets or exceeds
    for (PolicyRule rule : thresholds) {
        if (fillRatio >= rule.threshold) {
        return rule.releaseRate;
        }
    }
    
    // If below all thresholds, use default (or 0.0 if no default)
    return defaultRule != null ? defaultRule.releaseRate : 0.0;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("\n");
        sb.append("Capacity: ").append(String.format("%.2fL", capacity)).append("\n");
        sb.append("Max Flow Rate: ").append(String.format("%.2fL/day", maxFlowRate)).append("\n");
        sb.append("Starting Level: ").append(String.format("%.2f%%", startLevel * 100)).append(" (")
          .append(String.format("%.2fL", capacity * startLevel)).append(")\n");
        sb.append("Simulated Days: ").append(simulateDays).append("\n\n");
        
        sb.append("Day-by-Day Breakdown:\n");
        sb.append(String.format("%-5s | %-13s | %-11s | %-12s | %-12s | %-11s\n",
            "Day", "Dam Level (%)", "Inflow (L)", "Own Rain (L)", "Total In (L)", "Outflow (L)"));
        sb.append("------|---------------|-------------|--------------|--------------|-------------\n");
        
        double totalIn = 0.0;
        double totalOut = 0.0;
        
        for (int i = 0; i < simulateDays; i++) {
            double inflow = inflowPerDay.get(i);
            double rain = ownRainPerDay.get(i);
            double totalDayIn = inflow + rain;
            double outflow = outflowPerDay.get(i);
            double levelPct = damLevelPercentages.get(i);
            
            totalIn += totalDayIn;
            totalOut += outflow;
            
            sb.append(String.format("%4d  | %11.2f%% | %10.2f  | %11.2f  | %11.2f  | %10.2f\n",
                i + 1, levelPct, inflow, rain, totalDayIn, outflow));
        }
        
        double storageChange = currentLevel - (capacity * startLevel);
        
        sb.append("\n");
        sb.append("Final Dam Level: ").append(String.format("%.2f%%", damLevelPercentages.get(simulateDays - 1)))
          .append(" (").append(String.format("%.2fL", currentLevel)).append(")\n");
        sb.append("Total Water In: ").append(String.format("%.2fL", totalIn)).append("\n");
        sb.append("Total Water Out: ").append(String.format("%.2fL", totalOut)).append("\n");
        sb.append("Net Storage Change: ").append(String.format("%.2fL", storageChange));
        if (storageChange > 0) {
            sb.append(" (gained)\n");
        } else if (storageChange < 0) {
            sb.append(" (lost)\n");
        } else {
            sb.append(" (no change)\n");
        }
        sb.append("\n");
        
        sb.append("Downstream Flow: [");
        for (int i = 0; i < outflowPerDay.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(String.format("%.2f", outflowPerDay.get(i)));
        }
        sb.append("]\n");
        
        return sb.toString();
    }
}