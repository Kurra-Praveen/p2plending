package com.llms.service.interest;

import com.llms.enums.InterestRateMode;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Factory for obtaining the appropriate interest calculator based on rate mode.
 */
@Component
public class InterestCalculatorFactory {

    private final Map<InterestRateMode, InterestCalculator> calculators;

    public InterestCalculatorFactory(List<InterestCalculator> calculatorList) {
        calculators = new EnumMap<>(InterestRateMode.class);
        for (InterestCalculator calculator : calculatorList) {
            calculators.put(calculator.getMode(), calculator);
        }
    }

    /**
     * Returns the calculator for the given interest rate mode.
     *
     * @param mode The interest rate mode
     * @return The appropriate calculator
     * @throws IllegalArgumentException if no calculator exists for the mode
     */
    public InterestCalculator getCalculator(InterestRateMode mode) {
        InterestCalculator calculator = calculators.get(mode);
        if (calculator == null) {
            throw new IllegalArgumentException("No calculator found for mode: " + mode);
        }
        return calculator;
    }

    /**
     * Returns the default calculator (ANNUAL_PERCENTAGE).
     */
    public InterestCalculator getDefaultCalculator() {
        return getCalculator(InterestRateMode.ANNUAL_PERCENTAGE);
    }
}
