package com.github.badoualy.mobile.stitcher.utils

/**
 * @return true if this iterable as at least [n] distinct items.
 *
 * Note: Use this function instead of `distinctBy().count() >= n` to avoid checking the whole iterable when it's not necessary
 */
inline fun <T, K> Iterable<T>.hasAtLeastDistinctBy(n: Int = 2, selector: (T) -> K): Boolean {
    val set = HashSet<K>(n)
    for (e in this) {
        set.add(selector(e))
        if (set.size >= n) return true
    }
    return false
}
