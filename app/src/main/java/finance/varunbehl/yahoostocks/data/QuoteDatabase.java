package finance.varunbehl.yahoostocks.data;

import net.simonvt.schematic.annotation.Database;
import net.simonvt.schematic.annotation.Table;

/**
 * Created by varunbehl on 30/08/16.
 */
@Database(version = QuoteDatabase.VERSION)
public class QuoteDatabase {
    private QuoteDatabase() {
    }

    public static final int VERSION = 11;

    @Table(QuoteHistoricalDataColumns.class)
    public static final String QUOTES_HISTORICAL_DATA = "quotes_historical_data";

    @Table(QuoteColumns.class)
    public static final String QUOTES = "quotes";



}