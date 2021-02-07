package com.github.badoualy.mobile.stitcher.utils

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

suspend fun <A, B> Iterable<A>.pmap(f: suspend (A) -> B): List<B> = coroutineScope {
    map { async { f(it) } }.awaitAll()
}

suspend fun <A> Iterable<A>.pforEach(f: suspend (A) -> Unit): Unit = coroutineScope {
    map { async { f(it) } }.awaitAll()
}
