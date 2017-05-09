package com.bellabeat.statemachine

import io.reactivex.rxjava.traits.Driver
import io.reactivex.rxjava.traits.DriverTraits
import io.reactivex.rxjava.traits.drive
import io.reactivex.rxjava.traits.empty
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import rx.Observable
import rx.observers.TestSubscriber
import rx.schedulers.TestScheduler
import java.util.concurrent.TimeUnit

/**
 * Created by Ivan Fabijanovic on 07/05/2017.
 */

class DictionaryStateMachineDeviceTests {
  var scheduler = TestScheduler()
  var observer = TestSubscriber<Map<Device, TestDeviceState>>()
  var stateMachine = DriverTraits.schedulerIsNow({ this.scheduler }) {
    DictionaryStateMachine<Device, TestDeviceState>({ _ -> { _ -> Driver.empty() } })
  }
  var connectionCount = 0
  
  val pairData = "pair data"
  val syncData = "sync data"
  val syncSavePath = "file://test.file"
  val errorSyncData = "error"
  
  @Before
  fun setUp() {
    this.scheduler = TestScheduler()
    this.observer = TestSubscriber()
    this.connectionCount = 0
  }
  
  @After
  fun tearDown() {
    this.observer.unsubscribe()
    this.stateMachine.dispose()
  }
  
  fun makeStateMachine(connect: (Device) -> Observable<ConnectionResult>,
                       syncData: String = this.syncData) {
    DriverTraits.schedulerIsNow({ this.scheduler }, {
      this.stateMachine = DictionaryStateMachine(TestDeviceStateFeedbackLoops(this.scheduler,
                                                                              this.pairData,
                                                                              syncData,
                                                                              this.syncSavePath,
                                                                              connect)::feedbackLoops)
      this.stateMachine.state.drive(this.observer)
    })
  }
  
  fun perfectConnection(device: Device): Observable<ConnectionResult> {
    return Observable
        .just(ConnectionResult.Success(ConnectedDevice(device)))
        .delay(10, TimeUnit.SECONDS, this.scheduler)
        .doOnNext { this.connectionCount++ }
        .map { it }
  }
  
  fun pair(device: Device) {
    this.stateMachine.transition(Pair(device,
                                      TestDeviceState.Start(TestDeviceState.Pair(TestDeviceState.PairState.Read))))
  }
  
  fun sync(device: Device) {
    this.stateMachine.transition(Pair(device,
                                      TestDeviceState.Start(TestDeviceState.Sync(TestDeviceState.SyncState.Read))))
  }
  
  @Test
  fun singleDeviceSingleOperation() {
    DriverTraits.schedulerIsNow({ this.scheduler }) {
      this.makeStateMachine(this::perfectConnection)
      val device = Device(1)
      
      assertEquals(this.connectionCount, 0)
      
      this.scheduler.createWorker().schedule({ this.sync(device) }, 300, TimeUnit.SECONDS)
      this.scheduler.advanceTimeBy(1000, TimeUnit.SECONDS)
      
      assertEquals(this.observer.onNextEvents, listOf(
          mapOf(),
          mapOf(Pair(device,
                     TestDeviceState.Start(TestDeviceState.Sync(TestDeviceState.SyncState.Read)))),
          mapOf(Pair(device, TestDeviceState.Sync(TestDeviceState.SyncState.Read))),
          mapOf(Pair(device,
                     TestDeviceState.Sync(TestDeviceState.SyncState.Process(this.syncData)))),
          mapOf(Pair(device,
                     TestDeviceState.Sync(TestDeviceState.SyncState.Save(this.syncData,
                                                                         this.syncSavePath)))),
          mapOf(Pair(device, TestDeviceState.Sync(TestDeviceState.SyncState.Clear))),
          mapOf()
      ))
      
      assertEquals(this.connectionCount, 1)
    }
  }
}
