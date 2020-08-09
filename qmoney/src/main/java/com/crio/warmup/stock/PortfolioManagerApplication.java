package com.crio.warmup.stock;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.dto.TotalReturnsDto;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.crio.warmup.stock.portfolio.PortfolioManager;
import com.crio.warmup.stock.portfolio.PortfolioManagerFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.client.RestTemplate;


public class PortfolioManagerApplication {

  public static List<String> mainReadFile(String[] args) throws IOException, URISyntaxException {
    List<PortfolioTrade> trades =  getObjectMapper().readValue(
        resolveFileFromResources(args[0]), 
        new TypeReference<List<PortfolioTrade>>(){}
    );
    List<String> symbols = new ArrayList<String>();
    for (int i = 0; i < trades.size(); i++) {
      symbols.add(trades.get(i).getSymbol());
    } 
    return symbols;
  }

  // Note:
  // Remember to confirm that you are getting same results for annualized returns as in Module 3.
  public static List<String> mainReadQuotes(String[] args) throws IOException, URISyntaxException {
    List<PortfolioTrade> trades =  getObjectMapper().readValue(
        resolveFileFromResources(args[0]), 
        new TypeReference<List<PortfolioTrade>>(){}
    );
    List<TotalReturnsDto> returns = new ArrayList<TotalReturnsDto>();
    for (int i = 0; i < trades.size(); i++) {
      RestTemplate rest = new RestTemplate();
      String symbol = trades.get(i).getSymbol();
      String purchaseDate = trades.get(i).getPurchaseDate().toString();
      TiingoCandle[] candles = rest.getForObject(
          resourceUrl(symbol, purchaseDate, args[1]),
          TiingoCandle[].class
      );
      if (candles == null) {
        throw new RuntimeException();
      } else {
        returns.add(new TotalReturnsDto(symbol, candles[candles.length - 1].getClose()));
      }
    }
    Collections.sort(returns, Comparator.comparing(TotalReturnsDto::getClosingPrice));
    List<String> sortedSymbols = new ArrayList<String>();
    for (int i = 0; i < returns.size(); i++) {
      sortedSymbols.add(returns.get(i).getSymbol());
    }
    return sortedSymbols;
  }

  private static String resourceUrl(String symbol, String startDate, String endDate) {
    String token = "356d2243964d4a71eccf73c77b78201930b5c1a7";
    return String.format(
        Locale.US,
        "https://api.tiingo.com/tiingo/daily/%s/prices?startDate=%s&endDate=%s&token=%s",
        symbol, startDate, endDate, token
    );
  }


  private static void printJsonObject(Object object) throws IOException {
    Logger logger = Logger.getLogger(PortfolioManagerApplication.class.getCanonicalName());
    ObjectMapper mapper = new ObjectMapper();
    logger.info(mapper.writeValueAsString(object));
  }

  private static File resolveFileFromResources(String filename) throws URISyntaxException {
    return Paths.get(
        Thread.currentThread().getContextClassLoader().getResource(filename).toURI()).toFile();
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }

  public static List<String> debugOutputs() {

    String valueOfArgument0 = "trades.json";
    String resultOfResolveFilePathArgs0 = 
        "/home/crio-user/workspace/mohitkumarsingh907-ME_QMONEY/qmoney/bin/main/trades.json";
    String toStringOfObjectMapper = "com.fasterxml.jackson.databind.ObjectMapper@46268f08";
    String functionNameFromTestFileInStackTrace =
        "PortfolioManagerApplicationTest.mainReadFile";
    String lineNumberFromTestFileInStackTrace = "22:1";


    return Arrays.asList(new String[]{valueOfArgument0, resultOfResolveFilePathArgs0,
        toStringOfObjectMapper, functionNameFromTestFileInStackTrace,
        lineNumberFromTestFileInStackTrace});
  }

  //  TODO: CRIO_TASK_MODULE_CALCULATIONS
  //  Now that you have the list of PortfolioTrade and their data, calculate annualized returns
  //  for the stocks provided in the Json.
  //  Use the function you just wrote #calculateAnnualizedReturns.
  //  Return the list of AnnualizedReturns sorted by annualizedReturns in descending order.

  public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] args)
      throws IOException, URISyntaxException {
    List<PortfolioTrade> trades =  getObjectMapper().readValue(
        resolveFileFromResources(args[0]), 
        new TypeReference<List<PortfolioTrade>>(){}
    );
    List<AnnualizedReturn> returns = new ArrayList<AnnualizedReturn>();
    for (int i = 0; i < trades.size(); i++) {
      RestTemplate rest = new RestTemplate();
      PortfolioTrade trade = trades.get(i);
      String symbol = trade.getSymbol();
      String purchaseDate = trades.get(i).getPurchaseDate().toString();
      // get opening price at buytime
      TiingoCandle[] startCandles = null;
      startCandles = rest.getForObject(
        resourceUrl(
          symbol, 
          purchaseDate, 
          purchaseDate
        ),
        TiingoCandle[].class
      );
      // get closing price at sell time
      TiingoCandle[] candles = null;
      Double purchasePrice = 0.0;
      if (startCandles == null) {
        throw new RuntimeException();
      } else {
        purchasePrice = startCandles[startCandles.length - 1].getOpen();
      }
      LocalDate endDate = LocalDate.parse(args[1]);
      LocalDate temp = endDate;
      while (candles == null) {
        candles = rest.getForObject(
          resourceUrl(symbol, temp.toString(), temp.toString()),
          TiingoCandle[].class
        );
        temp = temp.minusDays(1);
      }
      Double closingPrice = candles[candles.length - 1].getClose();
      returns.add(calculateAnnualizedReturns(endDate, trade,purchasePrice, closingPrice));
    }
    Collections.sort(returns, 
        Comparator.comparing(AnnualizedReturn::getAnnualizedReturn, Comparator.reverseOrder()));
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

  public static List<AnnualizedReturn> mainCalculateReturnsAfterRefactor(String[] args)
      throws Exception {
    List<PortfolioTrade> portfolioTrades =  getObjectMapper().readValue(
        resolveFileFromResources(args[0]), 
        new TypeReference<List<PortfolioTrade>>(){}
    );
    LocalDate endDate = LocalDate.parse(args[1]);
    RestTemplate restTemplate = new RestTemplate();
    PortfolioManager portfolioManager = PortfolioManagerFactory.getPortfolioManager(restTemplate);
    return portfolioManager.calculateAnnualizedReturn(portfolioTrades, endDate);
  }


  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    ThreadContext.put("runId", UUID.randomUUID().toString());
    printJsonObject(mainCalculateReturnsAfterRefactor(args));
  }
  
}

