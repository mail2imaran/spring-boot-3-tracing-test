
package some.sample.spring;

import static java.util.stream.Collectors.toList;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.DefaultErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class GlobalExceptionHandler extends DefaultErrorWebExceptionHandler {

    @Autowired
    public GlobalExceptionHandler(
        final ObjectProvider<ViewResolver> viewResolversProvider,
        final ServerCodecConfigurer serverCodecConfigurer,
        ErrorAttributes errorAttributes, final WebProperties webProperties,
        ServerProperties serverProperties,
        ApplicationContext applicationContext) {
        super(errorAttributes, webProperties.getResources(),
            serverProperties.getError(), applicationContext);
        super.setViewResolvers(viewResolversProvider.orderedStream()
            .collect(toList()));
        super.setMessageWriters(serverCodecConfigurer.getWriters());
        super.setMessageReaders(serverCodecConfigurer.getReaders());
        log.info(" GlobalExceptionHandler init");
    }

    protected Mono<ServerResponse>
        renderErrorResponse(final ServerRequest request) {
        final Throwable throwable = this.getError(request);
        final ServerWebExchange serverWebExchange = request.exchange();
        log.error("{}",
            Map.of("errorMessage", Optional.ofNullable(throwable.getMessage())
                .orElse("NullMessage")));
        return ServerResponse.status(HttpStatusCode.valueOf(500))
            .bodyValue("ERROR : " + throwable.getMessage());

    }

}
