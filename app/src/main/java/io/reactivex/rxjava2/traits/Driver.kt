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
  companion object : SharedSequenceTraits {
    override val scheduler: Scheduler
      get() {
        return sharedScheduler()
      }
    
    private var sharedScheduler: () -> (Scheduler) = { AndroidSchedulers.mainThread() }
    
    fun <Result> schedulerIsNow(factory: () -> Scheduler, action: () -> Result): Result {
      val current = sharedScheduler
      try {
        sharedScheduler = factory
        return action()
      } finally {
        sharedScheduler = current
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

// operations

fun <Element> SharedSequence.Companion.defer(factory: () -> Driver<Element>): Driver<Element> {
  return SharedSequence(Observable.defer { factory().source }, DriverTraits.Companion)
}

fun <Element> SharedSequence.Companion.merge(sources: Iterable<Driver<out Element>>): Driver<Element> {
  return SharedSequence(Observable.merge(sources.map { it.source }), DriverTraits.Companion)
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
