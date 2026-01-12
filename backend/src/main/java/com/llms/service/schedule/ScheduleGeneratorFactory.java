package com.llms.service.schedule;

import com.llms.enums.LoanFrequency;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Factory for obtaining the appropriate schedule generator based on loan frequency.
 */
@Component
public class ScheduleGeneratorFactory {

    private final Map<LoanFrequency, ScheduleGenerator> generators;

    public ScheduleGeneratorFactory(List<ScheduleGenerator> generatorList) {
        generators = new EnumMap<>(LoanFrequency.class);
        for (ScheduleGenerator generator : generatorList) {
            generators.put(generator.getFrequency(), generator);
        }
    }

    /**
     * Returns the generator for the given loan frequency.
     *
     * @param frequency The loan frequency
     * @return The appropriate generator
     * @throws IllegalArgumentException if no generator exists for the frequency
     */
    public ScheduleGenerator getGenerator(LoanFrequency frequency) {
        ScheduleGenerator generator = generators.get(frequency);
        if (generator == null) {
            throw new IllegalArgumentException("No generator found for frequency: " + frequency);
        }
        return generator;
    }

    /**
     * Returns the default generator (MONTHLY).
     */
    public ScheduleGenerator getDefaultGenerator() {
        return getGenerator(LoanFrequency.MONTHLY);
    }
}
