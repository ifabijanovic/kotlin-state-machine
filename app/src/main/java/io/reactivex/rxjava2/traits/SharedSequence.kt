package io.reactivex.rxjava2.traits

import io.reactivex.Observable
import io.reactivex.Scheduler

/**
 * Created by kzaher on 5/9/17.
 */

interface SharedSequenceTraits {
  val scheduler: Scheduler
  fun <Element> share(source: Observable<Element>): Observable<Element>
}

class SharedSequence<out Traits : SharedSequenceTraits, Element>(source: Observable<Element>,
                                                                 internal val traits: Traits) {
  
  internal val source: Observable<Element> = traits.share(source)
  
  companion object
  
  fun asObservable(): Observable<Element> {
    return source
  }
}

fun <Element, Traits : SharedSequenceTraits, Result> SharedSequence<Traits, Element>.map(selector: (Element) -> Result): SharedSequence<Traits, Result> {
  return SharedSequence(this.source.map(selector), this.traits)
}

fun <Element, Traits : SharedSequenceTraits> SharedSequence<Traits, Element>.filter(predicate: (Element) -> Boolean): SharedSequence<Traits, Element> {
  return SharedSequence(this.source.filter(predicate), this.traits)
}

fun <Element, Traits : SharedSequenceTraits> SharedSequence<Traits, Element>.distinctUntilChanged(): SharedSequence<Traits, Element> {
  return SharedSequence(this.source.distinctUntilChanged(), this.traits)
}

fun <Element, Traits : SharedSequenceTraits> SharedSequence<Traits, Element>.startWith(item: Element): SharedSequence<Traits, Element> {
  return SharedSequence(this.source.startWith(item), this.traits)
}

fun <Element, Traits : SharedSequenceTraits, Result> SharedSequence<Traits, Element>.scan(
    initialValue: Result,
    accumulator: (Result, Element) -> Result): SharedSequence<Traits, Result> {
  return SharedSequence(this.source.scan(initialValue, accumulator), this.traits)
}

fun <Element, Traits : SharedSequenceTraits> SharedSequence<Traits, Element>.doOnNext(onNext: (Element) -> Unit): SharedSequence<Traits, Element> {
  return SharedSequence(this.source.doOnNext(onNext), this.traits)
}

fun <Element, Traits : SharedSequenceTraits, Result> SharedSequence<Traits, Element>.flatMap(
    selector: (Element) -> Driver<Result>): SharedSequence<Traits, Result> {
  return SharedSequence(this.source.flatMap({ element: Element -> selector(element).source }),
                        this.traits)
}

fun <Element, Traits : SharedSequenceTraits, Result> SharedSequence<Traits, Element>.switchMap(
    selector: (Element) -> Driver<Result>): SharedSequence<Traits, Result> {
  return SharedSequence(this.source.switchMap({ element: Element -> selector(element).source }),
                        this.traits)
}

fun <Element, Traits : SharedSequenceTraits, Result> SharedSequence<Traits, Element>.flatMapIterable(
    selector: (Element) -> Iterable<Result>): SharedSequence<Traits, Result> {
  return SharedSequence(this.source.flatMapIterable(selector), this.traits)
}

fun <Element, Traits : SharedSequenceTraits> SharedSequence<Traits, Element>.debug(id: String): SharedSequence<Traits, Element> =
    SharedSequence(this.source.debug(id), this.traits)
