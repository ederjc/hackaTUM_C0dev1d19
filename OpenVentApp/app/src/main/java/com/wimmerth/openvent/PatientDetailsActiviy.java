package com.wimmerth.openvent;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.wimmerth.openvent.connection.CallerMeassurement;
import com.wimmerth.openvent.connection.VentApi.OpenVentResponse;
import com.wimmerth.openvent.data.Measurement;
import com.wimmerth.openvent.data.Patient;
import com.wimmerth.openvent.ui.home.HomeFragment;

import java.util.List;

public class PatientDetailsActiviy extends AppCompatActivity implements CallerMeassurement {
    LineGraphSeries<DataPoint> dynSeries;
    private TextView rrTextView, o2TextView, co2TextView, MVeTextView, ventilationmodetv, PressureTextView;
    private TextView triggerFiO2, triggerHumidity, triggerPmax, triggerRR, triggerVT, triggerPEEP, triggerIE;
    LineChart[] charts = new LineChart[4];
    Patient p;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_details_activiy);
        Intent i = getIntent();
        p = null;
        List<Patient> patients = HomeFragment.patients;
        for (Patient patient : patients) {
            if (patient.getId() == i.getIntExtra("id", 0)) {
                p = patient;
                break;
            }
        }
        if (p == null)
            p = new Patient("max mustermann", 0);
        p.addCallback(this); // so we get a message if something changed
        TextView patientNameTextView = findViewById(R.id.patientName);
        TextView bedNumberTextView = findViewById(R.id.bedNumber);
        patientNameTextView.setText(p.getName());
        bedNumberTextView.setText("Bett Nr.: " + p.getId());

        this.rrTextView = findViewById(R.id.rr);
        this.o2TextView = findViewById(R.id.o2);
        this.co2TextView = findViewById(R.id.co2);
        this.MVeTextView = findViewById(R.id.mve);
        this.ventilationmodetv = findViewById(R.id.ventilationmode);
        this.PressureTextView = findViewById(R.id.pressure);

        /*GraphView graph = (GraphView) findViewById(R.id.graph1);
        dynSeries = new LineGraphSeries<>();
        graph.addSeries(dynSeries);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(40);
        graph.setTitle("Volume per minute (ml)");*/

        charts[0] = findViewById(R.id.chart1);
        setupChart(charts[0], 0);

        charts[1] = findViewById(R.id.chart2);
        setupChart(charts[1], 1);

        charts[2] = findViewById(R.id.chart3);
        setupChart(charts[2], 2);

        charts[3] = findViewById(R.id.chart4);
        setupChart(charts[3], 3);

        //Fill trigger data
        triggerFiO2 = findViewById(R.id.triggerFiO2);
        triggerHumidity = findViewById(R.id.triggerHumidity);
        triggerIE = findViewById(R.id.triggerIE);
        triggerPEEP = findViewById(R.id.triggerPEEP);
        triggerPmax = findViewById(R.id.triggerPmax);
        triggerRR = findViewById(R.id.triggerRR);
        triggerVT = findViewById(R.id.triggerVT);
    }

    private void setupChart(LineChart chart, int i) {
        // enable description text
        chart.getDescription().setEnabled(false);
        // enable touch gestures
        chart.setTouchEnabled(true);

        // enable scaling and dragging
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setDrawGridBackground(false);

        // if disabled, scaling can be done on x- and y-axis separately
        chart.setPinchZoom(true);

        // set an alternative background color
        chart.setBackgroundColor(Color.rgb(250, 250, 250));

        LineData data = new LineData();
        data.setValueTextColor(Color.BLACK);
        data.notifyDataChanged();

        // add empty data
        chart.setData(data);
        chart.getLegend().setEnabled(false);

        XAxis xl = chart.getXAxis();
        xl.setTextColor(Color.BLACK);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);
        xl.setPosition(XAxis.XAxisPosition.BOTTOM);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(Color.BLACK);
        leftAxis.setSpaceBottom(50);
        leftAxis.setSpaceTop(50);
        leftAxis.setDrawGridLines(true);
        //todo: maby leftAxis.setGranularity(100);

        if (i == 1) {
            LimitLine nullLine = new LimitLine(0f, "0");
            nullLine.setEnabled(true);
            nullLine.setLineWidth(1f);
            nullLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
            nullLine.setTextSize(10f);

            leftAxis.addLimitLine(nullLine);
        }

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);
    }


    @Override
    public void addData(final OpenVentResponse apiData, int p) {
        Log.d("joscha", apiData.toString());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                rrTextView.setText("" + apiData.getProcessed().getFrequency());
                o2TextView.setText("" + apiData.getProcessed().getExpiredO2());
                co2TextView.setText("" + apiData.getProcessed().getExpiredCO2());
                MVeTextView.setText("" + apiData.getProcessed().getMVe());
                ventilationmodetv.setText("" + apiData.getProcessed().getVentilationMode());
                PressureTextView.setText("" + apiData.getProcessed().getPressure());
            }
        });

        addEntry(apiData);
    }


    /*COMPLEX CHART:*/
    private void addEntry(OpenVentResponse apiData) {
        for (int i = 0; i < charts.length; i++) {
            LineData data = charts[i].getData();

            if (data != null) {

                ILineDataSet set = data.getDataSetByIndex(0);
                // set.addEntry(...); // can be called as well

                if (set == null) {
                    LineDataSet ldset = createSet();
                    ldset.setDrawFilled(true);
                    data.addDataSet(ldset);
                    ldset.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                    set = ldset;
                }

                switch (i) {
                    case 0:
                        data.addEntry(new Entry(set.getEntryCount(), (float) apiData.getProcessed().getPressure()), 0);
                        break;
                    case 1:
                        data.addEntry(new Entry(set.getEntryCount(), (float) apiData.getProcessed().getFlowrate()), 0);
                        break;
                    case 2:
                        data.addEntry(new Entry(set.getEntryCount(), (float) apiData.getProcessed().getVolumePerMovement()), 0);
                        break;
                    case 3:
                        data.addEntry(new Entry(set.getEntryCount(), Float.parseFloat(apiData.getProcessed().getExpiredCO2().toString())), 0);
                        break;
                    default:
                        break;
                }
                data.notifyDataChanged();

                // let the chart know it's data has changed
                charts[i].notifyDataSetChanged();

                // limit the number of visible entries

                int max = 50;
                if (i == 0) {
                    max = 20;
                }
                //charts[i].setVisibleXRangeMinimum(max);
                charts[i].setVisibleXRangeMaximum(max);
                charts[i].setAutoScaleMinMaxEnabled(true);

                //chart.setVisibleYRange(30, 30, YAxis.AxisDependency.LEFT);

                // move to the latest entry
                charts[i].moveViewToX(data.getEntryCount());

                // this automatically refreshes the chart (calls invalidate())
                // chart.moveViewTo(data.getXValCount()-7, 55f,
                // AxisDependency.LEFT);
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    triggerFiO2.setText(p.getApiData().getProcessed().getTriggerSettings().getFiO2()+"%");
                    triggerHumidity.setText(p.getApiData().getProcessed().getTriggerSettings().getHumidity()+"%");
                    triggerPmax.setText(p.getApiData().getProcessed().getTriggerSettings().getPressureMax()+" cmH₂O");
                    triggerRR.setText(p.getApiData().getProcessed().getTriggerSettings().getRR()+" 1/m");
                    triggerVT.setText(p.getApiData().getProcessed().getTriggerSettings().getVT()+" mL");
                    triggerPEEP.setText(p.getApiData().getProcessed().getTriggerSettings().getPEEP()+" cmH₂O");
                    triggerIE.setText(String.valueOf(p.getApiData().getProcessed().getTriggerSettings().getIE()));
                }
            });
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        /*if (p != null)
            p.close();*/
    }

    private LineDataSet createSet() {

        LineDataSet set = new LineDataSet(null, "Dynamic Data");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(ColorTemplate.getHoloBlue());
        set.setCircleColor(Color.BLACK);
        set.setLineWidth(1f);
        set.setDrawCircles(false);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.rgb("#89dfde"));
        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(9f);
        set.setDrawValues(false);
        return set;
    }
}
