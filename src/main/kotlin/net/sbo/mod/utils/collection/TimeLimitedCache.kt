package net.sbo.mod.utils.collection

import com.google.common.cache.Cache
import com.google.common.cache.RemovalCause
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.toJavaDuration

/**
 * A cache map that automatically removes entries after a specified duration since their last write.
 *
 * @param K the type of keys maintained by this cache
 * @param V the type of mapped values
 * @param expireAfterWrite the duration after which an entry should be automatically removed after its last write
 * @param removalListener an optional listener that gets notified when an entry is removed
 *
 * Credits to this go fully to SkyHanni
 */
class TimeLimitedCache<K : Any, V : Any>(
    expireAfterWrite: Duration,
    removalListener: ((K?, V?, RemovalCause) -> Unit)? = null,
) : CacheMap<K, V>() {

    override val cache: Cache<K, V> = buildCache {
        expireAfterWrite(expireAfterWrite.toJavaDuration())
        setRemovalListener(removalListener)
    }
}
