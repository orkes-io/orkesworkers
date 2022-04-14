package io.orkes.samples.workers;

import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;
import org.springframework.stereotype.Component;

import javax.imageio.*;
import lombok.extern.slf4j.Slf4j;
import com.amazonaws.regions.Regions;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse.File;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import io.orkes.samples.utils.S3Utils;
import java.io.*;
import java.awt.*;
import java.awt.image.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;


@Component
public class widgetShipping implements Worker {
    @Override
    public String getTaskDefName() {
        return "widget_shipping";
    }

    @Override
    public TaskResult execute(Task task) {
        
        TaskResult result = new TaskResult(task);
            String name = (String) task.getInputData().get("name");
            String street = (String) task.getInputData().get("street");
            String city = (String) task.getInputData().get("city");
            String state = (String) task.getInputData().get("state");
            String zip = (String) task.getInputData().get("zip");
            String fullAddress = name + "\n"+ street + "\n"+ city + ", "+ state + " " + zip;
            try {
                //generate 16 number shipping label
                int eightdigit1 = (int)(Math.floor(Math.random()*100000000));
                int eightdigit2 = (int)(Math.floor(Math.random()*100000000));
                String tracking = Integer.toString(eightdigit1) + " " +Integer.toString(eightdigit2);

                //magic that creates the label and prints it in the shipping bay
                
                
                result.setStatus(TaskResult.Status.COMPLETED);
                result.addOutputData("fullAddress", fullAddress);
                result.addOutputData("trackingNumber", tracking);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        return result;
    } 
}
