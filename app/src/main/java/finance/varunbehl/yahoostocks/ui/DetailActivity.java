package finance.varunbehl.yahoostocks.ui;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import finance.varunbehl.yahoostocks.R;
import finance.varunbehl.yahoostocks.data.QuoteColumns;

public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
//        Passing intent from main activity to fragment as it cant be directly passed
        if (savedInstanceState == null) {
            Bundle arguments = new Bundle();
            arguments.putString(QuoteColumns.SYMBOL,
                    getIntent().getStringExtra(QuoteColumns.SYMBOL));
            DetailActivityFragment fragment = new DetailActivityFragment();

            fragment.setArguments(arguments);

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.stock_detail_container, fragment)
                    .commit();
        }

    }

}
