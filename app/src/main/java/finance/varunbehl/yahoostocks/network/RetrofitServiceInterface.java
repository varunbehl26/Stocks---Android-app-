
package finance.varunbehl.yahoostocks.network;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface RetrofitServiceInterface {

    String BASE_URL = "https://query.yahooapis.com";

    @GET("/v1/public/yql?" +
            "format=json&diagnostics=true&" +
            "env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=")
    Call<GetStocks> getStocks(@Query("q") String query);

    @GET("/v1/public/yql?" +
            "format=json&diagnostics=true&" +
            "env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=")
    Call<GetStock> getStock(@Query("q") String query);

    @GET("/v1/public/yql?" +
            "format=json&diagnostics=true&" +
            "env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=")
    Call<GetHistoricalData> getStockHistoricalData(@Query("q") String query);
}
