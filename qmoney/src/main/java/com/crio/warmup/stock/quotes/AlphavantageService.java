
package com.crio.warmup.stock.quotes;

import com.crio.warmup.stock.dto.AlphavantageCandle;
import com.crio.warmup.stock.dto.AlphavantageDailyResponse;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.SortedMap;
import org.springframework.web.client.RestTemplate;

public class AlphavantageService implements StockQuotesService {
  private RestTemplate restTemplate;

  protected AlphavantageService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Override
  public List<Candle> getStockQuote(String symbol, LocalDate from, 
      LocalDate to) throws StockQuoteServiceException {
    List<Candle> candles = new ArrayList<Candle>();
    String respBodyString = "unknown error";
    try {
      respBodyString = this.restTemplate.getForObject(
        this.buildUri(symbol),String.class
      );
      AlphavantageDailyResponse respBody = 
          getObjectMapper().readValue(respBodyString, AlphavantageDailyResponse.class);
      SortedMap<LocalDate, AlphavantageCandle> filteredCandles = 
          respBody.getCandles().subMap(from, to.plusDays(1)); // from inclusive, to exclusive
      filteredCandles.forEach((k,v) -> {
        v.setDate(k);
        candles.add(v);
      });
      return candles;
    } catch (Exception e) {
      throw new StockQuoteServiceException(respBodyString, e);
    }
  }
  
  protected String buildUri(String symbol) {
    String token = "MPBDFXXO161ANQG1";
    return String.format(
      Locale.US,
      "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=%s&apikey=%s&outputsize=full",
      symbol,token
    );
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }
}

