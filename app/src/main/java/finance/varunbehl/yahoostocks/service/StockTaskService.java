package finance.varunbehl.yahoostocks.service;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;

import java.io.IOError;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import finance.varunbehl.yahoostocks.adapter.Utils;
import finance.varunbehl.yahoostocks.data.QuoteColumns;
import finance.varunbehl.yahoostocks.data.QuoteHistoricalDataColumns;
import finance.varunbehl.yahoostocks.data.QuoteProvider;
import finance.varunbehl.yahoostocks.network.GetHistoricalData;
import finance.varunbehl.yahoostocks.network.GetStock;
import finance.varunbehl.yahoostocks.network.GetStocks;
import finance.varunbehl.yahoostocks.network.RetrofitServiceInterface;
import finance.varunbehl.yahoostocks.network.StockQuote;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by varunbehl on 30/08/16.
 */
public class StockTaskService extends GcmTaskService {

    private Context context;
    private static String LOG_TAG = StockTaskService.class.getSimpleName();
    private final static String INIT_QUOTES = "\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\"";
    public final static String TAG_PERIODIC = "periodic";
    boolean isUpdate;
    private StringBuilder storedSymbols = new StringBuilder();
    ContentResolver resolver;
    static int firstTime = 0;


    public StockTaskService() {

    }

    public StockTaskService(Context context) {
        this.context = context;
    }

    @Override
    public int onRunTask(TaskParams taskParams) {
        resolver = context.getContentResolver();
        if (context == null) {
            return GcmNetworkManager.RESULT_FAILURE;
        }

        // Load relevant data about stocks
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(RetrofitServiceInterface.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        RetrofitServiceInterface service = retrofit.create(RetrofitServiceInterface.class);
        String query = "select * from yahoo.finance.quotes where symbol in ("
                + buildUrl(taskParams)
                + ")";
        try {
            // JSON response is different, if we request data for multiple stocks and single stock.
            if (taskParams.getTag().equals(StockIntentService.ACTION_INIT)) {
                Call<GetStocks> call = service.getStocks(query);
                Response<GetStocks> response = call.execute();
                GetStocks responseGetStocks = response.body();
                if (firstTime == 0) {
                    saveToDatabase(responseGetStocks.getStockQuotes());
                } else {
                    firstTime = 1;
                }
            } else {
                Call<GetStock> call = service.getStock(query);
                Response<GetStock> response = call.execute();
                GetStock responseGetStock = response.body();
                saveToDatabase(responseGetStock.getStockQuotes());
            }
        } catch (IOException | RemoteException | OperationApplicationException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            return GcmNetworkManager.RESULT_FAILURE;
        }

        return GcmNetworkManager.RESULT_SUCCESS;

    }

    private void saveToDatabase(List<StockQuote> stockQuotes) throws RemoteException, OperationApplicationException {
        ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
        for (StockQuote quote : stockQuotes) {
            batchOperations.add(QuoteProvider.buildBatchOperation(quote));
        }
        if (isUpdate) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(QuoteColumns.ISCURRENT, 0);
            resolver.update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
                    null, null);
        }
        resolver.applyBatch(QuoteProvider.AUTHORITY, batchOperations);

        for (StockQuote quote : stockQuotes) {
            // Load historical data for the quote
            try {
                loadHistoricalData(quote);
            } catch (IOException | RemoteException | OperationApplicationException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
            }
        }
    }

    private String buildUrl(TaskParams taskParams) {
        if ((taskParams.getTag().equals(StockIntentService.ACTION_INIT) || taskParams.getTag().equals(TAG_PERIODIC))) {
            isUpdate = true;

            Cursor cursor = resolver.query(QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{"Distinct " + QuoteColumns.SYMBOL}, null,
                    null, null);

            if (cursor != null && cursor.getCount() == 0 || cursor == null) {
                // Init task. Populates DB with quotes for the symbols seen below
                return INIT_QUOTES;
            } else {
                DatabaseUtils.dumpCursor(cursor);
                cursor.moveToFirst();
                for (int i = 0; i < cursor.getCount(); i++) {
                    storedSymbols.append("\"");
                    storedSymbols.append(cursor.getString(
                            cursor.getColumnIndex(QuoteColumns.SYMBOL)));
                    storedSymbols.append("\",");
                    cursor.moveToNext();
                }
                storedSymbols.replace(storedSymbols.length() - 1, storedSymbols.length(), "");
                return storedSymbols.toString();
            }
        } else if (taskParams.getTag().equals(StockIntentService.ACTION_ADD)) {
            isUpdate = false;
            // Get symbol from params.getExtra and build query
            String stockInput = taskParams.getExtras().getString(StockIntentService.EXTRA_SYMBOL);
            return "\"" + stockInput + "\"";
        }
        return null;
    }

    private void loadHistoricalData(StockQuote quote) throws IOException, RemoteException,
            OperationApplicationException {

        // Load historic stock data
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date currentDate = new Date();

        Calendar calEnd = Calendar.getInstance();
        calEnd.setTime(currentDate);
        calEnd.add(Calendar.DATE, 0);

        Calendar calStart = Calendar.getInstance();
        calStart.setTime(currentDate);
        calStart.add(Calendar.MONTH, -1);

        String startDate = dateFormat.format(calStart.getTime());
        String endDate = dateFormat.format(calEnd.getTime());

        String query = "select * from yahoo.finance.historicaldata where symbol=\"" +
                quote.getSymbol() +
                "\" and startDate=\"" + startDate + "\" and endDate=\"" + endDate + "\"";

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(RetrofitServiceInterface.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        RetrofitServiceInterface service = retrofit.create(RetrofitServiceInterface.class);
        Call<GetHistoricalData> call = service.getStockHistoricalData(query);
        Response<GetHistoricalData> response;
        response = call.execute();
        GetHistoricalData responseGetHistoricalData = response.body();
        if (responseGetHistoricalData != null) {
            saveQuoteHistoricalData2Database(responseGetHistoricalData.getHistoricData());
        }
    }

    private void saveQuoteHistoricalData2Database(List<GetHistoricalData.Quote> quotes)
            throws RemoteException, OperationApplicationException {
        ContentResolver resolver = context.getContentResolver();
        ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
        for (GetHistoricalData.Quote quote : quotes) {

            // First, we have to delete outdated date from DB.
//            resolver.delete(QuoteProvider.QuotesHistoricData.CONTENT_URI,
//                    QuoteHistoricalDataColumns.SYMBOL + " = \"" + quote.getSymbol() + "\"", null);

            batchOperations.add(QuoteProvider.buildBatchOperation(quote));
        }

        resolver.applyBatch(QuoteProvider.AUTHORITY, batchOperations);
    }
}
