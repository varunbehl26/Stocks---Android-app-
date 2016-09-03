package finance.varunbehl.yahoostocks.ui;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.melnykov.fab.FloatingActionButton;

import finance.varunbehl.yahoostocks.R;
import finance.varunbehl.yahoostocks.data.QuoteColumns;
import finance.varunbehl.yahoostocks.data.QuoteProvider;
import finance.varunbehl.yahoostocks.service.StockIntentService;
import finance.varunbehl.yahoostocks.adapter.QuoteCursorAdapter;
import finance.varunbehl.yahoostocks.service.StockTaskService;
import finance.varunbehl.yahoostocks.touch_helper.RecyclerViewItemClickListener;
import finance.varunbehl.yahoostocks.touch_helper.SimpleItemTouchHelperCallback;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    //Unique id for loader manager
    private static final int CURSOR_LOADER_ID = 0;
    boolean isConnected;
    Context context;
    Intent serviceIntent;
    QuoteCursorAdapter quoteCursorAdapter;
    ItemTouchHelper itemTouchHelper;
    private CharSequence mTitle;
    Cursor cursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        isConnected = checkNetworkConnectivity();
        // The intent service is for executing immediate pulls from the Yahoo API
        // GCMTaskService can only schedule tasks, they cannot execute immediately
        serviceIntent = new Intent(this, StockIntentService.class);

        //if there is nothing in the activity
        if (savedInstanceState == null) {
            // Run the initialize task service so that some stocks appear upon an empty database
            serviceIntent.putExtra("tag", "init");
            if (isConnected) {
                startService(serviceIntent);
            } else {
                networkToast();
            }
        }

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        //Starting loader manager
        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

        //Creating emtry adapter to fill data later on
        quoteCursorAdapter = new QuoteCursorAdapter(this, null);
        recyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
                new RecyclerViewItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View v, int position) {
                        //TODO:
                        // do something on item click
                        Intent detailIntent = new Intent(context, DetailActivity.class);
                        cursor.moveToPosition(position);
                        detailIntent.putExtra(StockIntentService.EXTRA_SYMBOL, cursor.getString(cursor.getColumnIndex(QuoteColumns.SYMBOL)));
                        context.startActivity(detailIntent);
                    }
                }));
        recyclerView.setAdapter(quoteCursorAdapter);


        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(quoteCursorAdapter);
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        mTitle = getTitle();
        if (isConnected) {
            long period = 3600L;
            long flex = 10L;
            String periodicTag = "periodic";

            // create a periodic task to pull stocks once every hour after the app has been opened. This
            // is so Widget data stays up to date.
            PeriodicTask periodicTask = new PeriodicTask.Builder()
                    .setService(StockTaskService.class)
                    .setPeriod(period)
                    .setFlex(flex)
                    .setTag(periodicTag)
                    .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                    .setRequiresCharging(false)
                    .build();
            // Schedule task with tag "periodic." This ensure that only the stocks present in the DB
            // are updated.
            GcmNetworkManager.getInstance(this).schedule(periodicTask);
        }
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.attachToRecyclerView(recyclerView);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isConnected) {
                    new MaterialDialog.Builder(context).title(R.string.symbol_search)
                            .content(R.string.content_test)
                            .inputType(InputType.TYPE_CLASS_TEXT)
                            .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                                @Override
                                public void onInput(MaterialDialog dialog, CharSequence input) {
                                    // On FAB click, receive user input. Make sure the stock doesn't already exist
                                    // in the DB and proceed accordingly
                                    Cursor c = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                                            new String[]{QuoteColumns.SYMBOL}, QuoteColumns.SYMBOL + "= ?",
                                            new String[]{input.toString()}, null);
                                    if (c.getCount() != 0) {
                                        Toast toast =
                                                Toast.makeText(MainActivity.this, "This stock is already saved!",
                                                        Toast.LENGTH_LONG);
                                        toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
                                        toast.show();
                                        return;
                                    } else {
                                        // Add the stock to DB
                                        serviceIntent.putExtra("tag", "add");
                                        serviceIntent.putExtra("symbol", input.toString());
                                        startService(serviceIntent);
                                    }
                                }
                            })
                            .show();
                } else {
                    networkToast();
                }

            }
        });
    }

    private void networkToast() {
        Toast.makeText(context, getString(R.string.network_toast), Toast.LENGTH_SHORT).show();

    }

    private Boolean checkNetworkConnectivity() {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        return isConnected;
    }


    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // This narrows the return to only the stocks that are most current.
        return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
                new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
//                QuoteColumns.ISCURRENT + " = ?",
//                new String[]{"1"},
                null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        quoteCursorAdapter.swapCursor(cursor);
        this.cursor = cursor;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        quoteCursorAdapter.swapCursor(null);
    }
}
