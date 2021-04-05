package io.github.kimmking.gateway.filter;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import com.google.common.util.concurrent.RateLimiter;

/**
 * @author luojun
 */
public class LimitHttpRequestFilter implements HttpRequestFilter {
    //创建一个限流器，参数代表每秒生成的令牌数
    private static final RateLimiter RATE_LIMITER = RateLimiter.create(50);

    @Override
    public void filter(FullHttpRequest fullRequest, ChannelHandlerContext ctx) {
        if(!RATE_LIMITER.tryAcquire()){
            ctx.close();
        }

    }
}
