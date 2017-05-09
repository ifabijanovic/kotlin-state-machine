package io.reactivex.rxjava.traits

import rx.Observable

/**
 * Created by juraj on 09/05/2017.
 */

fun log(line: String) {
  println(line)
}

fun <Element> Observable<Element>.debug(id: String): Observable<Element> =
    this.doOnNext { log("$id -> next $it") }
        .doOnError { log("$id -> error $it") }
        .doOnCompleted { log("$id -> completed") }
        .doOnSubscribe { log("$id -> subscribe") }
        .doOnUnsubscribe { log("$id -> dispose") }
