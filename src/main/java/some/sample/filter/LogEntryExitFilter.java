
package some.sample.filter;

import static java.util.Collections.EMPTY_MAP;

import java.util.ArrayList;


import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
//import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
//import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher.MatchResult;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.tracing.BaggageInScope;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.Tracer.SpanInScope;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.util.context.Context;

@Order(Ordered.LOWEST_PRECEDENCE)
@Slf4j
@RequiredArgsConstructor
@Service
public class LogEntryExitFilter implements WebFilter {

    private final ConfigurableEnvironment env;

    private final Tracer tracer;


    public static final String HOST_PORT = "server.port";

    public static final String DATA_CENTER = "application.datacenter";

    public static final String ENV = "application.env";

    public static final String COMMON_LOGS_KEY = "COMMON_LOGS_KEY";

    public static final String SUCCESS = "SUCCESS";

    public static final String FAILURE = "FAILURE";

    public static final String EXCEPTION_KEY = "Exception";

    public static final String X_TXN_ID = "X-Txn-ID";

    @Override
    public Mono<Void> filter(final ServerWebExchange exchange,
        final WebFilterChain chain) {
        return logAndProceed(exchange, chain);

    }

    private Mono<Void> logAndProceed(final ServerWebExchange exchange,
        final WebFilterChain chain) {
        long startTime = System.currentTimeMillis();
        log.info("event=LogBeforeAddingBaggage");
        List<BaggageInScope> baggages = new ArrayList<>();
        return getCommonLogMap(exchange).flatMap(commonLogs -> {
            exchange.getAttributes()
                .put(COMMON_LOGS_KEY, commonLogs);
            commonLogs.entrySet()
                .stream()
                .forEach(entry -> {
                    BaggageInScope baggage = tracer
                        .createBaggageInScope(entry.getKey(), entry.getValue());
                    baggages.add(baggage);
                });

            log.info("{}", composeEntryLogMap(exchange, EMPTY_MAP));
            return chain.filter(exchange);

        })
            .doOnError(throwable -> exchange.getAttributes()
                .put(EXCEPTION_KEY, throwable))

            .doFinally(
                signal -> logRequestExitEvent(exchange, signal, startTime,
                    baggages));

    }


    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Map composeEntryLogMap(final ServerWebExchange exchange,
        final Map<String, String> commonLogs) {
        Map loggerInfo = new LinkedHashMap<>() {
            {
                put("event", "RequestEntryEvent");
                put("baseUrl", exchange.getRequest()
                    .getURI()
                    .getHost());
                put("queryParams",
                    exchange.getRequest()
                        .getQueryParams());
                putAll(commonLogs);
            }
        };

        return loggerInfo;
    }

    public Mono<Map<String, String>>
        getCommonLogMap(ServerWebExchange serverWebExchange) {


        return Mono.fromSupplier(() -> {
            var uri = serverWebExchange.getRequest()
                .getURI()
                .getPath();
            var httpMethod = serverWebExchange.getRequest()
                .getMethod()
                .name();

            Map<String, String> logMap = new LinkedHashMap<>();

            logMap.put("DC", env.getProperty("datacenter", "AsiaPacific"));
            logMap.put("ENV", env.getProperty("env", "DEV"));
            logMap.put("appName", env.getProperty("appName", "SampleApp"));
            logMap.put("HostPort", env.getProperty("server.port", "8080"));
            logMap.put("httpMethod", httpMethod);
            logMap.put("uri", uri);


            CommonUtils.safeGet(() -> serverWebExchange.getRequest()
                .getHeaders()
                .get("X-Client-App-ID")
                .stream()
                .findAny()).flatMap(Function.identity())
                    .ifPresent(val -> {
                        logMap.put("clientAppId", val);
                    });


            return logMap;
        });


    }



    private void logRequestExitEvent(final ServerWebExchange webExchange,
        final SignalType signal, final long startTime,
        final List<BaggageInScope> baggages) {
        try {
            // Map<String, String> commonLogs = webExchange
            // .getAttributeOrDefault(COMMON_LOGS_KEY, Collections.emptyMap());

            final Optional<Throwable> throwable =
                Optional.ofNullable((Throwable)webExchange.getAttributes()
                    .get(EXCEPTION_KEY));

            var response = webExchange.getResponse();
            final int statusCode = Optional.ofNullable(response.getStatusCode())
                .map(HttpStatusCode::value)
                .orElse(-1);

            final String responseStatus = response.getStatusCode()
                .is2xxSuccessful() ? SUCCESS : FAILURE;

            Map<String, Object> resHeaders = Collections.emptyMap();

            final String baseUrl = webExchange.getRequest()
                .getURI()
                .getHost();
            var timeTakenMillis = (System.currentTimeMillis() - startTime);
            var loggerInfo = new LinkedHashMap<>() {
                {
                    put("event", "RequestExitEvent");
                    put("responseStatus", responseStatus);
                    put("statusCode", statusCode);
                    put("signal", signal);
                    put("timeTaken", timeTakenMillis);
                    put("baseUrl", baseUrl);
                    putAll(resHeaders);
                    // .putAll(exceptionInfo)
                }
            };
            // addCommonLogs(webExchange, signal, loggerInfo);
            log.info("{}", loggerInfo);

            // addCommonLogs(webExchange, signal, errorLoggerInfo);
            throwable.ifPresent(th -> {
                var errorLoggerInfo =
                    Map.of("event", "RequestExitEventStackTrace", "signal", signal);
                log.error("{}", errorLoggerInfo, th);
            });
            
            baggages.forEach(BaggageInScope::close);
        }
        catch (Throwable th) {
            logError(webExchange, th);
        }
    }


    private void logError(final ServerWebExchange webExchange, Throwable th) {
        var exceptionInfo = CommonUtils.extractExceptionData(th);
        log.error("{}", new LinkedHashMap<>() {
            {
                put("event", "RequestErrorEvent");
                put("baseUrl", webExchange.getRequest()
                    .getURI()
                    .getHost());
                put("apiStatus", FAILURE);
                put("httpMethod", webExchange.getRequest()
                    .getMethod()
                    .name());
                putAll(exceptionInfo);
            }
        });
        log.error("event=RequestErrorEventStackTrace", th);
    }


}
