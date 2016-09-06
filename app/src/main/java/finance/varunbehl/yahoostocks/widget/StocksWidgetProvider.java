package finance.varunbehl.yahoostocks.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import finance.varunbehl.yahoostocks.R;

/**
 * Created by varunbehl on 02/09/16.
 */
public class StocksWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        for (int appWidgetId : appWidgetIds) {
            RemoteViews rv = new RemoteViews(context.getPackageName(),
                    R.layout.stock_widget);

            Intent intent = new Intent(context, StockWidgetService.class);
//            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            rv.setRemoteAdapter(R.id.stock_list, intent);


            appWidgetManager.updateAppWidget(appWidgetId, rv);
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId,
                    R.id.stock_list);
        }

    }
}
