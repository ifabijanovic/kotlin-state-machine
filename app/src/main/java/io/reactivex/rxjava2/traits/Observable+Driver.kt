package io.reactivex.rxjava2.traits

import io.reactivex.Observable

/**
 * Created by kzaher on 5/9/17.
 */


fun <Element> Observable<Element>.asDriver(onErrorJustReturn: Element): Driver<Element> {
    return SharedSequence(this.onErrorReturn { onErrorJustReturn }.observeOn(DriverTraits.scheduler), DriverTraits.Companion)
}

fun <Element> Observable<Element>.asDriver(onErrorDriveWith: (Throwable) -> Driver<Element>): Driver<Element> {
    return SharedSequence(this.onErrorResumeNext { error: Throwable -> onErrorDriveWith(error)._source }.observeOn(DriverTraits.scheduler), DriverTraits.Companion)
}

fun <Element> Observable<Element>.asDriverIgnoreError(): Driver<Element> {
    return SharedSequence(this.onErrorResumeNext { error: Throwable -> Observable.empty() }.observeOn(DriverTraits.scheduler), DriverTraits.Companion)
}
