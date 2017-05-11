package ca.simonho.sensorrecord;

import java.text.DecimalFormat;
import java.util.LinkedList;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.SparseArray;
import android.util.TypedValue;

import com.androidplot.ui.SizeLayoutType;
import com.androidplot.ui.SizeMetrics;
import com.androidplot.ui.XLayoutStyle;
import com.androidplot.ui.YLayoutStyle;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYStepMode;

public class DynamicLinePlot
{
    private static final int VERTEX_WIDTH = 4;
    private static final int LINE_WIDTH = 4;

    private int maxPoints = 50;

    private double maxRange = 10;
    private double minRange = -10;
    private String yTitle;

    Context context;

    private XYPlot plot;
    private SparseArray<SimpleXYSeries> series;
    private SparseArray<LinkedList<Number>> history;


    public DynamicLinePlot(XYPlot plot, Context context, String title)
    {
        this.plot = plot;
        this.context = context;
        this.yTitle = title;

        series = new SparseArray<SimpleXYSeries>();
        history = new SparseArray<LinkedList<Number>>();

        initPlot();
    }


    public double getMaxRange()
    {
        return maxRange;
    }


    public double getMinRange()
    {
        return minRange;
    }


    public int getWindowSize()
    {
        return maxPoints;
    }


    public void setMaxRange(double maxRange)
    {
        this.maxRange = maxRange;
        plot.setRangeBoundaries(minRange, maxRange, BoundaryMode.FIXED);
    }


    public void setMinRange(double minRange)
    {
        this.minRange = minRange;
        plot.setRangeBoundaries(minRange, maxRange, BoundaryMode.FIXED);
    }


    public void setWindowSize(int windowSize)
    {
        this.maxPoints = windowSize;
    }


    public void setData(double data, int key)
    {
        if (history.get(key).size() > maxPoints)
        {
            history.get(key).removeFirst();
        }

        history.get(key).addLast(data);

        series.get(key).setModel(history.get(key),
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);
    }


    public synchronized void draw()
    {
        plot.redraw();
    }


    public void addSeriesPlot(String seriesName, int key, int color)
    {
        history.append(key, new LinkedList<Number>());

        series.append(key, new SimpleXYSeries(seriesName));

        LineAndPointFormatter formatter = new LineAndPointFormatter(Color.rgb(
                0, 153, 204), Color.rgb(0, 153, 204), Color.TRANSPARENT,
                new PointLabelFormatter(Color.TRANSPARENT));

        Paint linePaint = new Paint();
        linePaint.setAntiAlias(true);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setColor(color);
        linePaint.setStrokeWidth(LINE_WIDTH);

        formatter.setLinePaint(linePaint);

        Paint vertexPaint = new Paint();
        vertexPaint.setAntiAlias(true);
        vertexPaint.setStyle(Paint.Style.STROKE);
        vertexPaint.setColor(color);
        vertexPaint.setStrokeWidth(VERTEX_WIDTH);

        formatter.setVertexPaint(vertexPaint);

        plot.addSeries(series.get(key), formatter);

    }


    public void removeSeriesPlot(int key)
    {
        plot.removeSeries(series.get(key));

        history.get(key).removeAll(history.get(key));
        history.remove(key);

        series.remove(key);
    }


    private void initPlot()
    {
        this.plot.getGraphWidget().setDomainLabelPaint(null);
        this.plot.setDomainBoundaries(0, maxPoints, BoundaryMode.FIXED);
        this.plot.setDomainStep(XYStepMode.INCREMENT_BY_VAL, 10);

        this.plot.setRangeBoundaries(minRange, maxRange,
                BoundaryMode.FIXED);
        this.plot.setRangeLabel(this.yTitle);
        this.plot.getRangeLabelWidget().pack();
        this.plot.setRangeStep(XYStepMode.INCREMENT_BY_VAL, 2);
        this.plot.setRangeValueFormat(new DecimalFormat("#"));

        this.plot.setGridPadding(0, 0, 0, 0);

        plot.getLegendWidget().setVisible(false);

        //Colours
        Paint blackPaint = new Paint();
        blackPaint.setColor(Color.rgb(0, 0, 0));

        this.plot.getGraphWidget().setGridBackgroundPaint(null);
        this.plot.getGraphWidget().setBackgroundPaint(null);
        this.plot.getGraphWidget().setBorderPaint(null);
        this.plot.getGraphWidget().setDomainOriginLinePaint(blackPaint);
        this.plot.getGraphWidget().setRangeOriginLinePaint(blackPaint);

        this.plot.setBorderPaint(null);
        this.plot.setBackgroundPaint(null);

        plot.getRangeLabelWidget().getLabelPaint().setColor(Color.BLACK);
        plot.getGraphWidget().getRangeLabelPaint().setColor(Color.BLACK);

        //Size and position
        Resources r = context.getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12,
                r.getDisplayMetrics());

        plot.getGraphWidget().getRangeLabelPaint().setTextSize(px);
        plot.getRangeLabelWidget().getLabelPaint().setTextSize(px);
        plot.getRangeLabelWidget().pack();

        plot.getGraphWidget().setMarginBottom(10);
        plot.getGraphWidget().setMarginTop(40);
        plot.getGraphWidget().setMarginLeft(10);
        plot.getGraphWidget().setMarginRight(10);
        plot.getGraphWidget().position(0.03f, XLayoutStyle.RELATIVE_TO_LEFT,
                0.0f, YLayoutStyle.RELATIVE_TO_TOP);
        plot.getGraphWidget().setSize(
                new SizeMetrics(1.0f, SizeLayoutType.RELATIVE, 0.98f,
                        SizeLayoutType.RELATIVE));

        this.plot.redraw();
    }
}