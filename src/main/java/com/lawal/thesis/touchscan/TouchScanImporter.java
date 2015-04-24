package com.lawal.thesis.touchscan;

import com.iotticket.api.v1.IOTAPIClient;
import com.iotticket.api.v1.exception.ValidAPIParamException;
import com.iotticket.api.v1.model.Datanode.DatanodeWriteValue;
import com.iotticket.api.v1.model.Device;
import com.iotticket.api.v1.model.Device.DeviceDetails;
import com.iotticket.api.v1.model.PagedResult;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by Lawal on 04/04/2015.
 */
public class TouchScanImporter {

    private static final Logger LOG = Logger.getLogger(TouchScanImporter.class);

    final private String DUPLICATE_MAKER = "__duplicate__";
    final private String time_header = "Time";
    private final int BATCH_SIZE = 1000;


    private final String url;
    private final String userName;
    private final String password;

    public TouchScanImporter(String url, String userName, String password) {

        this.url = url;
        this.userName = userName;
        this.password = password;
    }


    private List<DataPoint> readFile(Path path) {


        String cleanFileContent = readAndCleanFileContent(path);

        CSVFormat format = CSVFormat.DEFAULT.withCommentMarker('#').withHeader();
        List<DataPoint> dataPoints = new LinkedList<>();
        try (CSVParser parser = CSVParser.parse(cleanFileContent, format)) {
            List<CSVRecord> records = parser.getRecords();
            for (Map.Entry<String, Integer> entry : parser.getHeaderMap().entrySet()) {
                String header = entry.getKey();
                if (header.equals(time_header)) continue;
                if (header.contains(DUPLICATE_MAKER)) continue;

                DataPoint dp = new DataPoint();
                dp.setName(header.trim());
                records.forEach(rec -> {
                    String tm = rec.get(time_header);
                    String val = rec.get(header);
                    dp.add(tm, val);

                });
                dataPoints.add(dp);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }


        return dataPoints;
    }


    /*Removes first comment in the file. The CSV parser also seems to have a problem ignoring commentMaker # */
    private String readAndCleanFileContent(Path path) {
        StringBuffer linesBuffer = new StringBuffer();

        try (InputStreamReader isr = new InputStreamReader(new FileInputStream(path.toFile()), StandardCharsets.UTF_8)) {
            try (BufferedReader bre = new BufferedReader(isr)) {


                String line = null;
                boolean headerProcessed = false;
                while ((line = bre.readLine()) != null) {
                    line = line.trim();
                    //TODO some charset messup here ??
                    int indexOf = line.indexOf('#');
                    final boolean startsWithChar = indexOf > 0;
                    boolean startsWithString = line.startsWith("#");
                    boolean startsWith = startsWithChar || startsWithString;
                    if (startsWith) {
                        //skip  comment
                        continue;
                    }
                    if (!headerProcessed) {
                        //this is header line;
                        line = processHeader(line);
                        headerProcessed = true;
                    }
                    linesBuffer.append(line);
                    linesBuffer.append("\n\r");
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return linesBuffer.toString();
    }


    //
    private String processHeader(String line) {


        Set<String> headers = new LinkedHashSet<>();

        String[] split = line.split(",");

        for (String header : split) {

            if (headers.contains(header)) {
                headers.add(header + DUPLICATE_MAKER);
            } else
                headers.add(header);
        }


        String noDuplicates = headers.stream().collect(Collectors.joining(","));

        return noDuplicates;

    }

    public boolean run(Path path, String deviceName) {


        LOG.info("processing file:" + path);
        try {
            List<DataPoint> dataPoints = readFile(path);
            sendToServer(dataPoints, deviceName);
        } catch (ValidAPIParamException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;

    }

    private void sendToServer(List<DataPoint> dataPoints, String deviceName) throws ValidAPIParamException, InterruptedException {

        List<DatanodeWriteValue> datanodeWriteValues = new ArrayList<>();
        for (DataPoint dataPoint : dataPoints) {
            String name = dataPoint.getName().replaceAll(" ", "");
            Map<Long, String> valueMap = dataPoint.valueMap;
            for (Entry<Long, String> e : valueMap.entrySet()) {
                DatanodeWriteValue wr = new DatanodeWriteValue();
                wr.setName(name);
                wr.setUnit(dataPoint.getUnit());
                wr.setValue(e.getValue());
                wr.setTimestampMiliseconds(e.getKey());
                datanodeWriteValues.add(wr);

            }
        }



        LOG.info("Number of values" + datanodeWriteValues.size());
        IOTAPIClient client = new IOTAPIClient(url, userName, password);
        DeviceDetails device = getOrCreateDevice(deviceName, client);


        for (List<DatanodeWriteValue> batch : ListUtils.partition(datanodeWriteValues, BATCH_SIZE)) {
            LOG.info("sending "+batch.size()+" to server. DeviceId "+device.getDeviceId());
            client.writeData(device.getDeviceId(), batch);
            TimeUnit.MILLISECONDS.sleep(200);

        }


    }

    private DeviceDetails getOrCreateDevice(String deviceName, IOTAPIClient client) throws ValidAPIParamException {
        PagedResult<DeviceDetails> deviceList = client.getDeviceList(0, 100);
        Optional<DeviceDetails> deviceDetailsOptional = deviceList.getResults().stream().filter(dev -> dev.getName().equals(deviceName)).findAny();
        if (deviceDetailsOptional.isPresent()) return deviceDetailsOptional.get();

        Device dev = new Device();
        dev.setName(deviceName);
        dev.setManufacturer(deviceName);

        DeviceDetails deviceDetails = client.registerDevice(dev);
        return deviceDetails;
    }


}
