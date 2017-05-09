package io.reactivex.rxjava.traits

import rx.Observable

/**
 * Created by juraj on 09/05/2017.
 */

fun <Element> Observable<Element>.asDriver(onErrorJustReturn: Element): Driver<Element> =
    SharedSequence(this.onErrorReturn { onErrorJustReturn }
                       .observeOn(DriverTraits.scheduler), DriverTraits.Companion)

fun <Element> Observable<Element>.asDriver(onErrorDriveWith: (Throwable) -> Driver<Element>): Driver<Element> =
    SharedSequence(this.onErrorResumeNext { onErrorDriveWith(it).source }
                       .observeOn(DriverTraits.scheduler), DriverTraits.Companion)

fun <Element> Observable<Element>.asDriverIgnoreError(): Driver<Element> =
    SharedSequence(this.onErrorResumeNext { Observable.empty() }
                       .observeOn(DriverTraits.scheduler), DriverTraits.Companion)
