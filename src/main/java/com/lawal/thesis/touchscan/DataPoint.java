package com.lawal.thesis.touchscan;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by Lawal on 05/04/2015.
 */
public class DataPoint {

    private String name;
    private String unit;
    Map<Long, String> valueMap = new LinkedHashMap<>();


    public void add(String timeStamp, String val) {

        long ts = parseDate(timeStamp);
        valueMap.put(ts, val);

    }


    private static long parseDate(String timeStamp) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSSS a");

            Date parse = sdf.parse(timeStamp);
            return parse.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = parseName(name);
        this.unit = parseUnit(name);
    }

    private String parseName(String name) {

        int lf = name.lastIndexOf("(");
        if(lf <0)return name;

        return  name.substring(0, lf);
    }

    private String parseUnit(String ti) {
        int openIndex = ti.indexOf('(');
        if (openIndex != -1) {
            int closeIndex = ti.indexOf(')');
            String content = ti.substring(openIndex + 1, closeIndex);
            return content;
        }
        return null;
    }

    public String getUnit() {
        return unit;
    }


}
