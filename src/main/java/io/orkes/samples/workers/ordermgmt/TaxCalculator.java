package io.orkes.samples.workers.ordermgmt;

import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import lombok.Data;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
public class TaxCalculator {

    @Data
    public static class TaxCalculatorInput {
        private String zipCode;
        private Number amount;
    }

    @WorkerTask("calculate_tax")
    @Tool(description = "Calculates tax based on amount and zip code")
    public Map<String, Object> executeTaxCalculation(
            @ToolParam(description = "Input parameters for tax calculation") TaxCalculatorInput input) {

        Map<String, Object> result = new HashMap<>();

        // Validate zipCode
        if (input.getZipCode() == null || StringUtils.isEmpty(input.getZipCode())) {
            throw new RuntimeException("Missing zip code");
        }

        // Validate amount
        if (input.getAmount() == null) {
            throw new RuntimeException("Missing amount");
        }

        // Calculate tax (10% tax)
        BigDecimal tax = new BigDecimal(input.getAmount().doubleValue()).multiply(new BigDecimal(0.1));
        result.put("tax", tax);

        return result;
    }
}