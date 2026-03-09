package flowInterpreter;

import java.util.ArrayList;
import java.util.List;

class RiverData {
    final String name;          // River name
    final double multiplier;    // Multiplier (L per mm)
    final List<Double> rainfall; // Original rainfall input
    final List<Double> flowArray; // Computed flow (multiplier × rainfall + incoming flows)
    
    RiverData(String name, double multiplier, List<Double> rainfall, List<Double> flowArray) {
        this.name = name;
        this.multiplier = multiplier;
        this.rainfall = new ArrayList<>(rainfall);
        this.flowArray = new ArrayList<>(flowArray);
    }
    
    // Constructor for creating from base rainfall only
    RiverData(String name, double multiplier, List<Double> rainfall) {
        this.name = name;
        this.multiplier = multiplier;
        this.rainfall = new ArrayList<>(rainfall);
        this.flowArray = new ArrayList<>();
        
        // Calculate base flow: multiplier × rainfall
        for (double rain : rainfall) {
            flowArray.add(multiplier * rain);
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(": [");
        for (int i = 0; i < flowArray.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(String.format("%.2f", flowArray.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }
}