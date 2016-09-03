package finance.varunbehl.yahoostocks.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import finance.varunbehl.yahoostocks.R;
import finance.varunbehl.yahoostocks.data.QuoteColumns;
import finance.varunbehl.yahoostocks.data.QuoteProvider;

/**
 * Created by varunbehl on 02/09/16.
 */
public class StockWidgetFactory implements RemoteViewsService.RemoteViewsFactory {
    private Cursor cursor;
    private Context context;
    int widgetId;

    public StockWidgetFactory(Context applicationContext, Intent intent) {
        context = applicationContext;
        widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    @Override
    public void onCreate() {

    }

    @Override
    public void onDataSetChanged() {
        if (cursor != null) {
            cursor.close();
        }
        cursor = context.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                null);
    }

    @Override
    public void onDestroy() {
        if (cursor != null) {
            cursor.close();
        }
    }

    @Override
    public int getCount() {
        return cursor.getCount();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.stock_widget_list_content);
        if (cursor.moveToPosition(position)) {
            rv.setTextViewText(R.id.stock_symbol,
                    cursor.getString(cursor.getColumnIndex(QuoteColumns.SYMBOL)));
            rv.setTextViewText(R.id.bid_price,
                    cursor.getString(cursor.getColumnIndex(QuoteColumns.BIDPRICE)));
            rv.setTextViewText(R.id.stock_change,
                    cursor.getString(cursor.getColumnIndex(QuoteColumns.CHANGE)));
        }
        return rv;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
