package com.tadeasfort.threadsapi.aspect;

import com.tadeasfort.threadsapi.service.ThreadsRateLimitService;
import com.tadeasfort.threadsapi.service.ThreadsRateLimitService.RateLimitStatus;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Aspect
@Component
public class RateLimitAspect {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitAspect.class);

    @Autowired
    private ThreadsRateLimitService rateLimitService;

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RateLimit {
        RateLimitType type() default RateLimitType.API_CALL;

        int userIdParamIndex() default 0; // Index of userId parameter
    }

    public enum RateLimitType {
        API_CALL,
        POST,
        REPLY
    }

    @Around("@annotation(rateLimit)")
    public Object enforceRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        Object[] args = joinPoint.getArgs();

        // Extract userId from method parameters
        String userId = extractUserId(args, rateLimit.userIdParamIndex());
        if (userId == null) {
            logger.warn("Could not extract userId for rate limiting from method: {}",
                    joinPoint.getSignature().getName());
            return joinPoint.proceed();
        }

        // Check rate limit based on type
        RateLimitStatus status = checkRateLimit(userId, rateLimit.type());

        if (!status.isAllowed()) {
            logger.warn("Rate limit exceeded for user {} on {}: {}", userId, rateLimit.type(), status.getReason());
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    String.format("Rate limit exceeded: %s. Retry after %d seconds",
                            status.getReason(), status.getRetryAfterSeconds()));
        }

        try {
            // Proceed with the method execution
            Object result = joinPoint.proceed();

            // Record the successful API call
            recordApiUsage(userId, rateLimit.type());

            return result;
        } catch (Exception e) {
            // Don't record failed calls against rate limit
            logger.debug("API call failed for user {}, not recording against rate limit: {}", userId, e.getMessage());
            throw e;
        }
    }

    private String extractUserId(Object[] args, int userIdParamIndex) {
        if (args == null || userIdParamIndex >= args.length) {
            return null;
        }

        Object userIdArg = args[userIdParamIndex];
        return userIdArg != null ? userIdArg.toString() : null;
    }

    private RateLimitStatus checkRateLimit(String userId, RateLimitType type) {
        switch (type) {
            case POST:
                return rateLimitService.checkPostLimit(userId);
            case REPLY:
                return rateLimitService.checkReplyLimit(userId);
            case API_CALL:
            default:
                return rateLimitService.checkApiCallLimit(userId);
        }
    }

    private void recordApiUsage(String userId, RateLimitType type) {
        switch (type) {
            case POST:
                rateLimitService.recordPost(userId);
                break;
            case REPLY:
                rateLimitService.recordReply(userId);
                break;
            case API_CALL:
            default:
                rateLimitService.recordApiCall(userId);
                break;
        }
    }
}