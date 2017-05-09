package io.reactivex.rxjava2.traits

import io.reactivex.Observable

/**
 * Created by kzaher on 5/9/17.
 */

fun log(line: String) {
  println(line)
}

fun <Element> Observable<Element>.debug(id: String): Observable<Element> =
    this.doOnNext { log("$id -> next $it") }
        .doOnError { log("$id -> error $it") }
        .doOnComplete { log("$id -> completed") }
        .doOnSubscribe { log("$id -> subscribe") }
        .doOnDispose { log("$id -> dispose") }
