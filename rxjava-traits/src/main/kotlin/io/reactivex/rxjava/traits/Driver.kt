package io.reactivex.rxjava.traits

import rx.Observable
import rx.Observer
import rx.Scheduler
import rx.android.schedulers.AndroidSchedulers

/**
 * Created by juraj on 09/05/2017.
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
typealias SafeDriver<Element> = SharedSequence.Safe<DriverTraits.Companion, Element>

// elementary

fun <Element> SharedSequence.Companion.just(element: Element): Driver<Element> =
    SharedSequence(Observable.just(element).subscribeOn(DriverTraits.scheduler),
                   DriverTraits.Companion)

fun <Element> SharedSequence.Companion.empty(): Driver<Element> =
    SharedSequence(Observable.empty<Element>().subscribeOn(DriverTraits.scheduler),
                   DriverTraits.Companion)

fun <Element> SharedSequence.Companion.never(): Driver<Element> =
    SharedSequence(Observable.never<Element>().subscribeOn(DriverTraits.scheduler),
                   DriverTraits.Companion)

// operations

fun <Element> SharedSequence.Companion.defer(factory: () -> Driver<Element>): Driver<Element> =
    SharedSequence(Observable.defer { factory().source },
                   DriverTraits.Companion)

fun <Element> SharedSequence.Companion.merge(sources: Iterable<Driver<out Element>>): Driver<Element> =
    SharedSequence(Observable.merge(sources.map { it.source }),
                   DriverTraits.Companion)

fun <Element> SharedSequence.Safe<DriverTraits.Companion, Element>.drive(onNext: (Element) -> Unit) =
    this.asObservable().subscribe(onNext)

fun <Element> SharedSequence.Safe<DriverTraits.Companion, Element>.drive(observer: Observer<Element>) =
    this.asObservable().subscribe(observer)

fun <Element> SharedSequence.Safe<DriverTraits.Companion, Element>.drive() =
    this.asObservable().subscribe()
