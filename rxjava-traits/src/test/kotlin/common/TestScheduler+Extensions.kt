package common

import rx.Observable
import rx.lang.kotlin.merge
import rx.schedulers.TestScheduler
import java.util.concurrent.TimeUnit

/**
 * Created by juraj on 11/05/2017.
 */

sealed class Event<T>(val delayDuration: Long) {
  data class Next<T>(val delay: Long, val value: T) : Event<T>(delay)
  data class Error<T>(val delay: Long, val error: Throwable) : Event<T>(delay)
}

fun <T> TestScheduler.createColdObservable(vararg events: Event<T>): Observable<T> =
    events.map { event ->
      Observable.timer(event.delayDuration, TimeUnit.SECONDS, this)
          .map<T> {
            when (event) {
              is Event.Next -> event.value
              is Event.Error -> throw event.error
            }
          }
    }.merge()

fun TestScheduler.scheduleAt(delay: Long, action: () -> Unit) =
    this.createWorker().schedule(action, delay, TimeUnit.SECONDS)

fun TestScheduler.advanceTimeBy(delay: Long) =
    this.advanceTimeBy(delay, TimeUnit.SECONDS)
