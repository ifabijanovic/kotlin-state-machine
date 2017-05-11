package io.reactivex.rxjava.traits

import common.advanceTimeBy
import common.scheduleAt
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import rx.Observable
import rx.observers.TestSubscriber
import rx.schedulers.TestScheduler
import java.util.*

/**
 * Created by juraj on 10/05/2017.
 */

class DriverTest {
  var scheduler = TestScheduler()
  var observer = TestSubscriber<Int>()
  
  @Before
  fun setUp() {
    this.scheduler = TestScheduler()
    this.observer = TestSubscriber()
  }
  
  @After
  fun tearDown() {
    this.observer.unsubscribe()
  }
  
  fun observableRange(): Observable<Int> =
      Observable
          .range(1, 10, scheduler)
  
  @Test
  fun driverCompleteOnError() {
    DriverTraits.schedulerIsNow({ this.scheduler }) {
      
      this.scheduler.scheduleAt(0) {
        observableRange()
            .map {
              if (it == 5) throw Exception()
              else it
            }
            .asDriverCompleteOnError()
            .catchErrorAndComplete()
            .drive(observer)
      }
      this.scheduler.advanceTimeBy(10)
      
      assertEquals(arrayListOf(1, 2, 3, 4), observer.onNextEvents)
    }
  }
  
  
  @Test
  fun driverOnErrorJustReturn() {
    DriverTraits.schedulerIsNow({ this.scheduler }) {
      
      val returnOnError = 7
      
      this.scheduler.scheduleAt(0) {
        observableRange()
            .map {
              if (it == 5) throw Exception()
              else it
            }
            .asDriver(returnOnError)
            .catchErrorAndComplete()
            .drive(observer)
      }
      this.scheduler.advanceTimeBy(10)
      
      assertEquals(arrayListOf(1, 2, 3, 4, 7), observer.onNextEvents)
    }
  }
  
  @Test
  fun driverOnErrorDriveWith() {
    DriverTraits.schedulerIsNow({ this.scheduler }) {
      
      this.scheduler.scheduleAt(0) {
        observableRange()
            .map {
              if (it == 5) throw Exception()
              else it
            }
            .asDriver(onErrorDriveWith = { observableRange().asDriverCompleteOnError() })
            .catchErrorAndComplete()
            .drive(observer)
      }
      this.scheduler.advanceTimeBy(10)
      
      assertEquals(arrayListOf(1, 2, 3, 4, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10), observer.onNextEvents)
    }
  }
  
  @Test
  fun defer() {
    DriverTraits.schedulerIsNow({ this.scheduler }) {
      
      this.scheduler.scheduleAt(0) {
        Driver.defer { observableRange().asDriverCompleteOnError() }
            .catchErrorAndComplete()
            .drive(observer)
      }
      this.scheduler.advanceTimeBy(10)
      
      assertEquals(arrayListOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), observer.onNextEvents)
    }
  }
  
  @Test
  fun deferOnErrorComplete() {
    DriverTraits.schedulerIsNow({ this.scheduler }) {
      
      this.scheduler.scheduleAt(0) {
        Driver.defer {
          if (true) throw Exception()
          else
            observableRange().asDriverCompleteOnError()
        }
            .catchErrorAndComplete()
            .drive(observer)
      }
      this.scheduler.advanceTimeBy(10)
      
      assertEquals(ArrayList<Int>(0), observer.onNextEvents)
    }
  }
  
  @Test
  fun deferOnErrorJustReturn() {
    DriverTraits.schedulerIsNow({ this.scheduler }) {
      
      val returnOnError = 7
      
      this.scheduler.scheduleAt(0) {
        Driver.defer {
          if (true) throw Exception()
          else
            observableRange().asDriverCompleteOnError()
        }
            .catchError(returnOnError)
            .drive(observer)
      }
      this.scheduler.advanceTimeBy(10)
      
      assertEquals(arrayListOf(returnOnError), observer.onNextEvents)
    }
  }
  
  @Test
  fun catchErrorAndCompleteWithoutError() {
    DriverTraits.schedulerIsNow({ this.scheduler }) {
      
      this.scheduler.scheduleAt(0) {
        observableRange()
            .asDriverCompleteOnError()
            .catchErrorAndComplete()
            .drive(observer)
      }
      this.scheduler.advanceTimeBy(10)
      
      assertEquals(arrayListOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), observer.onNextEvents)
    }
  }
  
  @Test
  fun catchErrorAndComplete() {
    DriverTraits.schedulerIsNow({ this.scheduler }) {
      
      this.scheduler.scheduleAt(0) {
        observableRange()
            .asDriverCompleteOnError()
            .map {
              if (it == 5)
                throw Exception()
              else it
            }
            .catchErrorAndComplete()
            .drive(observer)
      }
      this.scheduler.advanceTimeBy(10)
      assertEquals(arrayListOf(1, 2, 3, 4), observer.onNextEvents)
    }
  }
  
  @Test
  fun catchErrorAndReturn() {
    DriverTraits.schedulerIsNow({ this.scheduler }) {
      
      val returnOnError = 7
      
      this.scheduler.scheduleAt(0) {
        observableRange()
            .asDriverCompleteOnError()
            .map {
              if (it == 5)
                throw Exception()
              else it
            }
            .catchError(returnOnError)
            .drive(observer)
      }
      this.scheduler.advanceTimeBy(10)
      
      assertEquals(arrayListOf(1, 2, 3, 4, 7), observer.onNextEvents)
    }
  }
}
