package com.crio.warmup.stock.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AlphavantageCandle implements Candle {
  @JsonProperty("1. open")
  private Double open;
  @JsonProperty("4. close")
  private Double close;
  @JsonProperty("2. high")
  private Double high;
  @JsonProperty("3. low")
  private Double low;
  private LocalDate date;

  @Override
  public Double getOpen() {
    return open;
  }

  @Override
  public Double getClose() {
    return close;
  }

  @Override
  public Double getHigh() {
    return high;
  }

  @Override
  public Double getLow() {
    return low;
  }

  @Override
  public LocalDate getDate() {
    return date;
  }

  public void setDate(LocalDate timeStamp) {
    this.date = timeStamp;
  }
}

