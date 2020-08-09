
package com.crio.warmup.stock.portfolio;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.crio.warmup.stock.quotes.StockQuotesService;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {

  private StockQuotesService stockQuotesService;


  // Caution: Do not delete or modify the constructor, or else your build will break!
  // This is absolutely necessary for backward compatibility
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
  }

  // Note:
  // Make sure to exercise the tests inside PortfolioManagerTest using command below:
  // ./gradlew test --tests PortfolioManagerTest

  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades,
      LocalDate endDate) throws StockQuoteServiceException {
    List<AnnualizedReturn> returns = new ArrayList<AnnualizedReturn>();
    for (int i = 0; i < portfolioTrades.size(); i++) {
      PortfolioTrade trade = portfolioTrades.get(i);
      String symbol = trade.getSymbol();
      // get opening price at buytime and closing at sell time
      List<Candle> candles = null;
      candles = this.getStockQuote(symbol,  trade.getPurchaseDate(), endDate);
      Double purchasePrice = candles.get(0).getOpen();
      Double closingPrice = candles.get(candles.size() - 1).getClose();
      returns.add(calculateAnnualizedReturns(endDate, trade,purchasePrice, closingPrice));
    }
    Collections.sort(returns, this.getComparator());
    return returns;
  }

  public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate,
      PortfolioTrade trade, Double buyPrice, Double sellPrice) {
    Double totalReturn = (sellPrice - buyPrice) / buyPrice;
    LocalDate buyDate = trade.getPurchaseDate();
    Double totalYears = Double.valueOf(
        Duration.between(buyDate.atStartOfDay(), endDate.atStartOfDay()).toDays()
      ) / 365.0;
    Double annualizedReturn = Math.pow((1.0 + totalReturn), 1.0 / totalYears) - 1.0;
    return new AnnualizedReturn(trade.getSymbol(), annualizedReturn, totalReturn);
  }



  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  //CHECKSTYLE:OFF

  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws StockQuoteServiceException {
    return stockQuotesService.getStockQuote(symbol, from, to);
  }


  // ¶TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Modify the function #getStockQuote and start delegating to calls to
  //  stockQuoteService provided via newly added constructor of the class.
  //  You also have a liberty to completely get rid of that function itself, however, make sure
  //  that you do not delete the #getStockQuote function.
  protected PortfolioManagerImpl(StockQuotesService stockQuotesService) {
    this.stockQuotesService = stockQuotesService;
  }

  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturnParallel(List<PortfolioTrade> portfolioTrades, LocalDate endDate,
      int numThreads) throws InterruptedException, StockQuoteServiceException {
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    List<AnnualizedReturn> returns = new ArrayList<AnnualizedReturn>();
    // create tasks
    List<Callable<List<Candle>>> tasks = new ArrayList<>();
    for (int i = 0; i < portfolioTrades.size(); i++) {
      PortfolioTrade trade = portfolioTrades.get(i);
      String symbol = trade.getSymbol();
      Callable<List<Candle>> task = () -> {
        return this.getStockQuote(symbol,  trade.getPurchaseDate(), endDate);
      };
      tasks.add(task);
    }
    // execute tasks
    List<Future<List<Candle>>> futures = executor.invokeAll(tasks);
    // extract values
    for (int i = 0; i < portfolioTrades.size(); i++) {
      PortfolioTrade trade = portfolioTrades.get(i);
      try {
        List<Candle> candles = futures.get(i).get();
        Double purchasePrice = candles.get(0).getOpen();
        Double closingPrice = candles.get(candles.size() - 1).getClose();
        returns.add(calculateAnnualizedReturns(endDate, trade,purchasePrice, closingPrice));
      } catch (Exception e) {
        throw new StockQuoteServiceException("error", e.getCause());
      }
    }
    Collections.sort(returns, this.getComparator());
    return returns;
  }
}
