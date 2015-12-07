package com.yudiandreanp.aware_d;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.*;
import android.os.Bundle;
import android.util.Log;

import com.androidplot.Plot;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.*;

import java.text.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class StatisticsActivity extends Activity
{

    private XYPlot mySimpleXYPlot;
    private Float[] numPoints;
    private String[] dateTime;
    private String coba;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        mySimpleXYPlot = (XYPlot) findViewById(R.id.mySimpleXYPlot);
        setNumbers();


        // create our series from our array of nums:
        XYSeries series2 = new SimpleXYSeries(
                Arrays.asList(numPoints),
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY,
                "Last 5 Drives Statistics");

        mySimpleXYPlot.getGraphWidget().getGridBackgroundPaint().setColor(Color.BLACK);
        //mySimpleXYPlot.getGraphWidget().getDomainGridLinePaint().setColor(Color.BLACK);
        //mySimpleXYPlot.getGraphWidget().getDomainGridLinePaint().setPathEffect(new DashPathEffect(new float[]{1,1}, 1));
        mySimpleXYPlot.getGraphWidget().getDomainOriginLinePaint().setColor(Color.WHITE);
        mySimpleXYPlot.getGraphWidget().getRangeOriginLinePaint().setColor(Color.WHITE);

        mySimpleXYPlot.setBorderStyle(Plot.BorderStyle.SQUARE, null, null);
        mySimpleXYPlot.getBorderPaint().setStrokeWidth(1);
        mySimpleXYPlot.getBorderPaint().setAntiAlias(false);
        mySimpleXYPlot.getBorderPaint().setColor(Color.BLACK);

        PointLabelFormatter plf = new PointLabelFormatter();

        // Create a formatter to use for drawing a series using LineAndPointRenderer:
        LineAndPointFormatter series1Format = new LineAndPointFormatter(
                Color.rgb(0, 100, 0),                   // line color
                Color.rgb(0, 100, 0),                   // point color
                Color.rgb(100, 200, 0),plf
                );                // fill color

        // setup our line fill paint to be a slightly transparent gradient:
        Paint lineFill = new Paint();
        lineFill.setAlpha(200);
        lineFill.setShader(new LinearGradient(0, 0, 0, 250, Color.WHITE, Color.GREEN, Shader.TileMode.MIRROR));

        LineAndPointFormatter formatter  = new LineAndPointFormatter(Color.rgb(0, 0,0), Color.BLUE, Color.RED, plf);
        formatter.setFillPaint(lineFill);
        mySimpleXYPlot.getGraphWidget().setPaddingRight(2);
        mySimpleXYPlot.addSeries(series2, formatter);

        // draw a domain tick for each year:
        mySimpleXYPlot.setDomainStep(XYStepMode.SUBDIVIDE, dateTime.length);

        // customize our domain/range labels
        mySimpleXYPlot.setDomainLabel("Date/Time");
        mySimpleXYPlot.setRangeLabel("Driving Points");

        // get rid of decimal points in our range labels:
        mySimpleXYPlot.setRangeValueFormat(new DecimalFormat("1"));

        mySimpleXYPlot.setDomainValueFormat(new NumberFormat() {
            @Override
            public StringBuffer format(double value, StringBuffer buffer, FieldPosition field) {
                return new StringBuffer(dateTime[(int) value]);
            }

            @Override
            public StringBuffer format(long value, StringBuffer buffer, FieldPosition field) {
                return null;
            }

            @Override
            public Number parse(String string, ParsePosition position) {
                return null;
            }
        });

        // by default, AndroidPlot displays developer guides to aid in laying out your plot.
        // To get rid of them call disableAllMarkup():
        //mySimpleXYPlot.disableAllMarkup();
    }

    public void setNumbers()
    {
        int lastIndex = getLastIndex();
        int i = lastIndex-5;
        DBReader mDbHelper = new DBReader(StatisticsActivity.this);
        mDbHelper.createDatabase();
        mDbHelper.open();
        ArrayList <String> array = new ArrayList <>();
        //TODO iterate through cursor
        if (i>0)
        {
            Cursor data = mDbHelper.getStats(i);
            data.moveToFirst();
            while (!data.isAfterLast()) {
                array.add(data.getString(0));
                array.add(data.getString(1));
                array.add(data.getString(2));
                array.add(data.getString(3));
                array.add(data.getString(4));
                array.add(data.getString(5));
                data.moveToNext();
            }
        }

        numPoints = new Float[5];
        //1st point
        if (Integer.valueOf(array.get(1)) == 0)
        {
            numPoints[0] = Float.valueOf(0);
        }
        else
        {
            numPoints [0] = ((Float.valueOf(array.get(3))-Float.valueOf(array.get(4)))/Float.valueOf(array.get(3))) * 100;
        }

        //2nd point
        if (Long.valueOf(array.get(7)) == 0)
        {
            numPoints[1] = Float.valueOf(0);
        }
        else
        {
            numPoints [1] = ((Float.valueOf(array.get(9))-Float.valueOf(array.get(10)))/Float.valueOf(array.get(9))) * 100;
        }

        //3rd point
        if (Long.valueOf(array.get(13)) == 0)
        {
            numPoints[2] = Float.valueOf(0);
        }
        else
        {
            numPoints [2] = ((Float.valueOf(array.get(15))-Float.valueOf(array.get(16)))/Float.valueOf(array.get(15))) * 100;
        }

        //4th point
        if (Integer.valueOf(array.get(19)) == 0)
        {
            numPoints[3] = Float.valueOf(0);;
        }
        else
        {
            numPoints [3] = ((Float.valueOf(array.get(21))-Float.valueOf(array.get(22)))/Float.valueOf(array.get(21))) * 100;
        }

        //5th point
        if (Long.valueOf(array.get(25)) == 0)
        {
            numPoints[4] = Float.valueOf(0);;
        }
        else
        {
            numPoints [4] = ((Float.valueOf(array.get(27))-Float.valueOf(array.get(28)))/Float.valueOf(array.get(27))) * 100;
        }

        dateTime = new String[5];
        Date resultdate;
        int count1 = 0;
        for (int k=5; k<30; k=k+6)
        {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd-HH:mm");
            resultdate = new Date(Long.valueOf(array.get(k)));
            dateTime[count1]=sdf.format(resultdate);
            count1++;
        }
        mDbHelper.close();
    }

    private String makeString(ArrayList<String> arraystring)
    {
        StringBuilder ret = new StringBuilder();
        for (String s : arraystring)
        {
            ret.append(s + ", ");
        }
        return ret.toString();
    }

    private int getLastIndex()
    {
        DBReader mDbHelper = new DBReader(StatisticsActivity.this);
        mDbHelper.createDatabase();
        mDbHelper.open();
        int ret = mDbHelper.getLastIndexStats().getInt(0);
        mDbHelper.close();
        return ret;
    }

}
