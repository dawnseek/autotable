package io.github.dawnseek.autotable.core.utils;

import java.util.UUID;

/**
 * Description:
 *
 * @author dawnseek
 * @version 2025/3/13
 */
public class UuidUtil {
    /**
     * generate short uuid
     * @return short uuid
     */
    public static String getUuid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0,16);
    }
}
