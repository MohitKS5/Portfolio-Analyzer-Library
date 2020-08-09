
package com.crio.warmup.stock.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.SortedMap;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AlphavantageDailyResponse {

  @JsonProperty(value = "Time Series (Daily)")
  private SortedMap<LocalDate, AlphavantageCandle> candles;

  public SortedMap<LocalDate, AlphavantageCandle> getCandles() {
    return candles;
  }

  public void setCandles(SortedMap<LocalDate, AlphavantageCandle> candles) {
    this.candles = candles;
  }
}
