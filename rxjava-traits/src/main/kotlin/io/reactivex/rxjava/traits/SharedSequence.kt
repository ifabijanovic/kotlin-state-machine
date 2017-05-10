package io.reactivex.rxjava.traits

import rx.Observable
import rx.Scheduler

/**
 * Created by juraj on 09/05/2017.
 */

interface SharedSequenceTraits {
  val scheduler: Scheduler
  fun <Element> share(source: Observable<Element>): Observable<Element>
}

open class SharedSequence<Traits : SharedSequenceTraits, Element>(source: Observable<Element>,
                                                                  internal val traits: Traits) {
  companion object {}
  
  internal val source: Observable<Element> = traits.share(source)
  
  class Safe<Traits : SharedSequenceTraits, Element>(source: Observable<Element>,
                                                     traits: Traits) : SharedSequence<Traits, Element>(
      source, traits) {
    fun asObservable() = source
  }
}

fun <Element, Traits : SharedSequenceTraits> SharedSequence<Traits, Element>.catchErrorAndComplete(): SharedSequence.Safe<Traits, Element> =
    SharedSequence.Safe(this.source.onErrorResumeNext { Observable.empty() }, this.traits)

fun <Element, Traits : SharedSequenceTraits> SharedSequence<Traits, Element>.catchError(
    onErrorJustReturn: Element): SharedSequence.Safe<Traits, Element> =
    SharedSequence.Safe(this.source.onErrorReturn { onErrorJustReturn }, this.traits)

//fun <Element, Traits : SharedSequenceTraits, Result> SharedSequence<Traits, Element>.retryWithInitialInterval(initialInteval: Int): SharedSequence.SafeSharedSequence<Traits, Result> =
//    SharedSequence.SafeSharedSequence(this.source.retry { initialInteval }, this.traits)

fun <Element, Traits : SharedSequenceTraits, Result> SharedSequence<Traits, Element>.map(selector: (Element) -> Result): SharedSequence<Traits, Result> =
    SharedSequence(this.source.map(selector), this.traits)

fun <Element, Traits : SharedSequenceTraits> SharedSequence<Traits, Element>.filter(predicate: (Element) -> Boolean): SharedSequence<Traits, Element> =
    SharedSequence(this.source.filter(predicate), this.traits)

fun <Element, Traits : SharedSequenceTraits> SharedSequence<Traits, Element>.distinctUntilChanged(): SharedSequence<Traits, Element> =
    SharedSequence(this.source.distinctUntilChanged(), this.traits)

fun <Element, Traits : SharedSequenceTraits> SharedSequence<Traits, Element>.startWith(item: Element): SharedSequence<Traits, Element> =
    SharedSequence(this.source.startWith(item), this.traits)

fun <Element, Traits : SharedSequenceTraits, Result> SharedSequence<Traits, Element>.scan(
    initialValue: Result,
    accumulator: (Result, Element) -> Result): SharedSequence<Traits, Result> =
    SharedSequence(this.source.scan(initialValue, accumulator), this.traits)

fun <Element, Traits : SharedSequenceTraits> SharedSequence<Traits, Element>.doOnNext(onNext: (Element) -> Unit): SharedSequence<Traits, Element> =
    SharedSequence(this.source.doOnNext(onNext), this.traits)

fun <Element, Traits : SharedSequenceTraits, Result> SharedSequence<Traits, Element>.flatMap(
    selector: (Element) -> SharedSequence<Traits, Result>): SharedSequence<Traits, Result> =
    SharedSequence(this.source.flatMap({ element: Element -> selector(element).source }),
                   this.traits)

fun <Element, Traits : SharedSequenceTraits, Result> SharedSequence<Traits, Element>.switchMap(
    selector: (Element) -> SharedSequence<Traits, Result>): SharedSequence<Traits, Result> =
    SharedSequence(this.source.switchMap({ element: Element -> selector(element).source }),
                   this.traits)

fun <Element, Traits : SharedSequenceTraits, Result> SharedSequence<Traits, Element>.flatMapIterable(
    selector: (Element) -> Iterable<Result>): SharedSequence<Traits, Result> =
    SharedSequence(this.source.flatMapIterable(selector), this.traits)

fun <Element, Traits : SharedSequenceTraits> SharedSequence<Traits, Element>.debug(id: String): SharedSequence<Traits, Element> =
    SharedSequence(this.source.debug(id), this.traits)
