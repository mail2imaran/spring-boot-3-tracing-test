
package some.sample.spring;


import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.embedded.netty.NettyServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import reactor.netty.http.server.HttpServer;

@Configuration
@ConditionalOnProperty(name = "netty.threadConfig.enabled", havingValue = "true", matchIfMissing = true)
public class NettyConfiguration {


    @Bean
    public NettyReactiveWebServerFactory nettyReactiveWebServerFactory() {
        final NettyReactiveWebServerFactory webServerFactory =
            new NettyReactiveWebServerFactory();
        webServerFactory.addServerCustomizers(new EventLoopNettyCustomizer());
        return webServerFactory;
    }

    private static class EventLoopNettyCustomizer
        implements NettyServerCustomizer {

        @Override
        public HttpServer apply(final HttpServer httpServer) {
            final EventLoopGroup eventLoopGroup = new NioEventLoopGroup(1);
            eventLoopGroup.register(new NioServerSocketChannel());
            return httpServer.runOn(eventLoopGroup);
        }
    }
}
