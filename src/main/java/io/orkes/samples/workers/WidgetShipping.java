package io.orkes.samples.workers;

import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import lombok.Data;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class WidgetShipping {

    @Data
    public static class WidgetShippingInput {
        private String name;
        private String street;
        private String city;
        private String state;
        private String zip;
    }

    @WorkerTask("widget_shipping")
    @Tool(description = "Handles shipping for widgets and generates tracking numbers")
    public Map<String, Object> executeWidgetShipping(
            @ToolParam(description = "Input parameters for widget shipping") WidgetShippingInput input) {

        Map<String, Object> result = new HashMap<>();

        String name = input.getName();
        String street = input.getStreet();
        String city = input.getCity();
        String state = input.getState();
        String zip = input.getZip();
        String fullAddress = name + "\n" + street + "\n" + city + ", " + state + " " + zip;

        try {
            //generate 16 number shipping label
            int eightdigit1 = (int)(Math.floor(Math.random()*100000000));
            int eightdigit2 = (int)(Math.floor(Math.random()*100000000));
            String tracking = Integer.toString(eightdigit1) + " " + Integer.toString(eightdigit2);

            //magic that creates the label and prints it in the shipping bay
            result.put("fullAddress", fullAddress);
            result.put("trackingNumber", tracking);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("error", e.getMessage());
            throw new RuntimeException("Failed to execute widget shipping: " + e.getMessage(), e);
        }

        return result;
    }
}