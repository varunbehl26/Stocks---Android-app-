package finance.varunbehl.yahoostocks.ui;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import finance.varunbehl.yahoostocks.R;
import finance.varunbehl.yahoostocks.data.QuoteColumns;
import finance.varunbehl.yahoostocks.data.QuoteHistoricalDataColumns;
import finance.varunbehl.yahoostocks.data.QuoteProvider;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.view.LineChartView;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int CURSOR_LOADER_ID = 0;
    private static final int CURSOR_LOADER_ID_FOR_LINE_CHART = 1;
    String symbolReceived;
    Context context;

    @BindView(R.id.stock_symbol)
    TextView symbolView;
    @BindView(R.id.stock_bidprice)
    TextView bidPriceView;
    @BindView(R.id.stock_change)
    TextView changePercentage;
    @BindView(R.id.stock_name)
    TextView nameView;

    LineChartView lineChartView;

    public DetailActivityFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(QuoteColumns.SYMBOL)) {
            symbolReceived = getArguments().getString(QuoteColumns.SYMBOL);
        }
        context = getActivity();

        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);
        getLoaderManager().initLoader(CURSOR_LOADER_ID_FOR_LINE_CHART, null, this);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        ButterKnife.bind(this, rootView);
        lineChartView = (LineChartView) rootView.findViewById(R.id.chart);

        return rootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        if (id == CURSOR_LOADER_ID) {
            return new CursorLoader(context, QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                            QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP,
                            QuoteColumns.NAME},
                    QuoteColumns.SYMBOL + " = \"" + symbolReceived + "\"", null, null);
        } else if (id == CURSOR_LOADER_ID_FOR_LINE_CHART) {
            String sortOrder = QuoteColumns._ID + " ASC LIMIT 5";

            return new CursorLoader(context, QuoteProvider.QuotesHistoricData.CONTENT_URI,
                    new String[]{QuoteHistoricalDataColumns._ID, QuoteHistoricalDataColumns.SYMBOL,
                            QuoteHistoricalDataColumns.BIDPRICE, QuoteHistoricalDataColumns.DATE},
                    QuoteHistoricalDataColumns.SYMBOL + " = \"" + symbolReceived + "\"",
                    null, sortOrder);

        } else {
            throw new IllegalStateException();

        }
    }


    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (loader.getId() == CURSOR_LOADER_ID && cursor != null && cursor.moveToFirst()) {

            String symbol = cursor.getString(cursor.getColumnIndex(QuoteColumns.SYMBOL));
            symbolView.setText(symbol);

            String bitPrice = cursor.getString(cursor.getColumnIndex(QuoteColumns.BIDPRICE));
            bidPriceView.setText(bitPrice);

            String name = cursor.getString(cursor.getColumnIndex(QuoteColumns.NAME));
            nameView.setText(name);

            String change = cursor.getString(cursor.getColumnIndex(QuoteColumns.CHANGE));
            String percentChange = cursor.getString(cursor.getColumnIndex(QuoteColumns.PERCENT_CHANGE));
            String mixedChange = change + " (" + percentChange + ")";
            changePercentage.setText(mixedChange);

        } else if (loader.getId() == CURSOR_LOADER_ID_FOR_LINE_CHART && cursor != null &&
                cursor.moveToFirst()) {
            getDataForChart(cursor);
        }
    }

    private void getDataForChart(Cursor cursor) {
        List<AxisValue> axisValuesX = new ArrayList<>();
        List<PointValue> pointValues = new ArrayList<>();

        int counter = -1;
        do {
            counter++;

            String date = cursor.getString(cursor.getColumnIndex(
                    QuoteHistoricalDataColumns.DATE));
            String bidPrice = cursor.getString(cursor.getColumnIndex(
                    QuoteHistoricalDataColumns.BIDPRICE));

            // We have to show chart in right order.
            int x = cursor.getCount() - 1 - counter;

            // Point for line chart (date, price).
            PointValue pointValue = new PointValue(x, Float.valueOf(bidPrice));
            pointValue.setLabel(date);
            pointValues.add(pointValue);

            // Set labels for x-axis (we have to reduce its number to avoid overlapping text).
            if (counter != 0 && counter % (cursor.getCount() / 3) == 0) {
                AxisValue axisValueX = new AxisValue(x);
                axisValueX.setLabel(date);
                axisValuesX.add(axisValueX);
            }

        } while (cursor.moveToNext());

        // Prepare cursor for chart
        Line line = new Line(pointValues).setColor(Color.WHITE).setCubic(false);
        List<Line> lines = new ArrayList<>();
        lines.add(line);
        LineChartData lineChartData = new LineChartData();
        lineChartData.setLines(lines);

        // Init x-axis
        Axis axisX = new Axis(axisValuesX);
        axisX.setHasLines(true);
        axisX.setMaxLabelChars(4);
        lineChartData.setAxisXBottom(axisX);

        // Init y-axis
        Axis axisY = new Axis();
        axisY.setAutoGenerated(true);
        axisY.setHasLines(true);
        axisY.setMaxLabelChars(4);
        lineChartData.setAxisYLeft(axisY);

        // Update chart with new cursor.
        lineChartView.setInteractive(false);
        lineChartView.setLineChartData(lineChartData);

        // Show chart
        lineChartView.setVisibility(View.VISIBLE);

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }


}
