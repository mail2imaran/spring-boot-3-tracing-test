

package some.sample.filter;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import lombok.extern.slf4j.Slf4j;



@Slf4j
public class CommonUtils {

    public static <T> Optional<T> safeGet(final Supplier<T> supplier) {

        try {
            T result = supplier.get();
            return Optional.ofNullable(result);

        }
        catch (Throwable e) {

            return Optional.empty();
        }
    }

    public static Map<String, Object> extractExceptionData(final Throwable throwable) {
        return new LinkedHashMap<>() {
            {
                put("exceptionClass", throwable.getClass()
                            .getSimpleName());
                put("exceptionMessage",
                            Objects.toString(throwable.getMessage()));
                put("exception", throwable);
            }
        };
    }
}
