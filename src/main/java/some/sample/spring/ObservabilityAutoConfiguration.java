


package some.sample.spring;


import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Hooks;


@AutoConfiguration
@Slf4j
public class ObservabilityAutoConfiguration {

    @Bean
    public ApplicationListener<ApplicationReadyEvent> autoContextPropagationListener() {
        log.info("Init autoContextPropagationListener ");
        return new AutoContextPropagationListener();
    }

    private static final class AutoContextPropagationListener implements ApplicationListener<ApplicationReadyEvent> {

        @Override
        public void onApplicationEvent(final ApplicationReadyEvent event) {
            log.info("enableAutomaticContextPropagation ");
            Hooks.enableAutomaticContextPropagation();
        }
    }
}
