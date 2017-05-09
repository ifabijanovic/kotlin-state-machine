package io.reactivex.rxjava2.traits

import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.Scheduler
import io.reactivex.internal.schedulers.ImmediateThinScheduler
import io.reactivex.schedulers.Schedulers

/**
 * Created by kzaher on 5/9/17.
 */

public interface SharedSequenceTraits {
    val scheduler: Scheduler
    fun <Element> share(source: Observable<Element>): Observable<Element>
}


public class SharedSequence<Traits: SharedSequenceTraits, Element> {
    internal val _source: Observable<Element>
    internal val _traits: Traits

    companion object {

    }

    constructor(source: Observable<Element>, traits: Traits) {
        this._source = traits.share(source)
        this._traits = traits
    }

    fun asObservable(): Observable<Element> {
        return _source
    }
}

fun <Element, Traits: SharedSequenceTraits, Result> SharedSequence<Traits, Element>.map(selector: (Element) -> Result): SharedSequence<Traits, Result> {
    return SharedSequence(this._source.map(selector), this._traits)
}

fun <Element, Traits: SharedSequenceTraits> SharedSequence<Traits, Element>.filter(predicate: (Element) -> Boolean): SharedSequence<Traits, Element> {
    return SharedSequence(this._source.filter(predicate), this._traits)
}

fun <Element, Traits: SharedSequenceTraits> SharedSequence<Traits, Element>.distinctUntilChanged(): SharedSequence<Traits, Element> {
    return SharedSequence(this._source.distinctUntilChanged(), this._traits)
}

fun <Element, Traits: SharedSequenceTraits> SharedSequence<Traits, Element>.startWith(item: Element): SharedSequence<Traits, Element> {
    return SharedSequence(this._source.startWith(item), this._traits)
}

fun <Element, Traits: SharedSequenceTraits, Result> SharedSequence<Traits, Element>.scan(initialValue: Result, accumulator: (Result, Element) -> Result): SharedSequence<Traits, Result> {
    return SharedSequence(this._source.scan(initialValue, accumulator), this._traits)
}

fun <Element, Traits: SharedSequenceTraits> SharedSequence<Traits, Element>.doOnNext(onNext: (Element) -> Unit): SharedSequence<Traits, Element> {
    return SharedSequence(this._source.doOnNext(onNext), this._traits)
}

fun <Element, Traits: SharedSequenceTraits, Result> SharedSequence<Traits, Element>.flatMap(selector: (Element) -> Driver<Result>): SharedSequence<Traits, Result> {
    return SharedSequence(this._source.flatMap({ element: Element -> selector(element)._source }), this._traits)
}

fun <Element, Traits: SharedSequenceTraits, Result> SharedSequence<Traits, Element>.switchMap(selector: (Element) -> Driver<Result>): SharedSequence<Traits, Result> {
    return SharedSequence(this._source.switchMap({ element: Element -> selector(element)._source }), this._traits)
}

fun <Element, Traits: SharedSequenceTraits, Result> SharedSequence<Traits, Element>.flatMapIterable(selector: (Element) -> Iterable<Result>): SharedSequence<Traits, Result> {
    return SharedSequence(this._source.flatMapIterable(selector), this._traits)
}

fun <Element, Traits: SharedSequenceTraits> SharedSequence<Traits, Element>.debug(id: String): SharedSequence<Traits, Element> =
        SharedSequence(this._source.debug(id), this._traits)
