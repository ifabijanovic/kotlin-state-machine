package io.reactivex.rxjava2.traits

import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable

/**
 * Created by kzaher on 5/9/17.
 */


class DriverTraits {
    companion object: SharedSequenceTraits {
        override val scheduler: Scheduler
            get()  {
                return _sharedScheduler()
            }

        private var _sharedScheduler: () -> (Scheduler) = { AndroidSchedulers.mainThread() }

        fun schedulerIsNow(factory: () -> Scheduler, action: () -> Unit) {
            val current = _sharedScheduler
            try {
                _sharedScheduler = factory
                action()
            }
            finally {
                _sharedScheduler = current
            }
        }

        override fun <Element> share(source: Observable<Element>): Observable<Element> {
            return source.replay(1).refCount()
        }
    }
}

typealias Driver<Element> = SharedSequence<DriverTraits.Companion, Element>

// elementary

fun <Element> SharedSequence.Companion.just(element: Element): Driver<Element> {
    return Driver(Observable.just(element), DriverTraits.Companion)
}

fun <Element> SharedSequence.Companion.empty(): Driver<Element> {
    return Driver(Observable.empty(), DriverTraits.Companion)
}

fun <Element> SharedSequence.Companion.never(): Driver<Element> {
    return Driver(Observable.never(), DriverTraits.Companion)
}

// observable

fun <Element> Observable<Element>.asDriver(onErrorJustReturn: Element): Driver<Element> {
    return SharedSequence(this.onErrorReturn { onErrorJustReturn }, DriverTraits.Companion)
}

fun <Element> Observable<Element>.asDriver(onErrorDriveWith: (Throwable) -> Driver<Element>): Driver<Element> {
    return SharedSequence(this.onErrorResumeNext { error: Throwable -> onErrorDriveWith(error)._source }, DriverTraits.Companion)
}

fun <Element> Observable<Element>.asDriverIgnoreError(): Driver<Element> {
    return SharedSequence(this.onErrorResumeNext { error: Throwable -> Observable.empty() }, DriverTraits.Companion)
}

fun <Element> SharedSequence.Companion.defer(factory: () -> Driver<Element>): Driver<Element> {
    return SharedSequence(Observable.defer { factory()._source }, DriverTraits.Companion)
}

// operations

fun <Element> SharedSequence.Companion.merge(sources: Iterable<Driver<out Element>>): Driver<Element> {
    return SharedSequence(Observable.merge(sources.map { it._source }), DriverTraits.Companion)
}

fun <Element> SharedSequence<DriverTraits.Companion, Element>.drive(onNext: (Element) -> Unit): Disposable {
    return this.asObservable().subscribe(onNext)
}

fun <Element> SharedSequence<DriverTraits.Companion, Element>.drive(observer: Observer<Element>) {
    this.asObservable().subscribe(observer)
}

fun <Element> SharedSequence<DriverTraits.Companion, Element>.drive(): Disposable {
    return this.asObservable().subscribe()
}
