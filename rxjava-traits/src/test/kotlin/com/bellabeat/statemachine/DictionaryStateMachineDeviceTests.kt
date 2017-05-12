package com.bellabeat.statemachine

import common.Event.Error
import common.Event.Next
import common.advanceTimeBy
import common.createColdObservable
import common.scheduleAt
import io.reactivex.rxjava.traits.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import rx.Observable
import rx.observers.TestSubscriber
import rx.schedulers.TestScheduler

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
      this.stateMachine = DictionaryStateMachine(
          TestDeviceStateFeedbackLoops(this.scheduler, this.pairData, syncData,
                                       this.syncSavePath, connect)::feedbackLoops)
      this.stateMachine.state.catchErrorAndComplete().drive(this.observer)
    })
  }
  
  fun perfectConnection(device: Device): Observable<ConnectionResult> =
      scheduler.createColdObservable<ConnectionResult>(
          Next(10, ConnectionResult.Success(ConnectedDevice(device)))
      )
          .doOnSubscribe { this.connectionCount++ }
  
  fun connectionWithBootup(device: Device): Observable<ConnectionResult> =
      scheduler.createColdObservable<ConnectionResult>(
          Next(5, ConnectionResult.PoweredOff),
          Next(20, ConnectionResult.Success(ConnectedDevice(device)))
      )
          .doOnSubscribe { this.connectionCount++ }
  
  fun connectionWithError(device: Device): Observable<ConnectionResult> =
      scheduler.createColdObservable<ConnectionResult>(
          Next(10, ConnectionResult.Success(ConnectedDevice(device))),
          Error(50, TestDeviceStateError.Connection)
      )
          .doOnSubscribe { this.connectionCount++ }
  
  fun connectionWithInterrupt(device: Device): Observable<ConnectionResult> {
    val connectedDevice = ConnectedDevice(device)
    return scheduler.createColdObservable<ConnectionResult>(
        Next(10, ConnectionResult.Success(connectedDevice)),
        Next(40, ConnectionResult.PoweredOff),
        Next(60, ConnectionResult.Success(connectedDevice))
    )
        .doOnSubscribe { this.connectionCount++ }
  }
  
  fun pair(device: Device) {
    this.stateMachine.transition(Pair(device,
                                      TestDeviceState.Start(TestDeviceState.Pair(PairState.Read))))
  }
  
  fun sync(device: Device) {
    this.stateMachine.transition(Pair(device,
                                      TestDeviceState.Start(TestDeviceState.Sync(SyncState.Read))))
  }
  
  @Test
  fun singleDeviceSingleOperation() {
    DriverTraits.schedulerIsNow({ this.scheduler }) {
      this.makeStateMachine(this::perfectConnection)
      val device = Device(1)
      
      assertEquals(this.connectionCount, 0)
      
      this.scheduler.scheduleAt(300) { this.sync(device) }
      this.scheduler.advanceTimeBy(1000)
      
      assertEquals(listOf(
          mapOf(),
          mapOf(Pair(device, TestDeviceState.Start(TestDeviceState.Sync(SyncState.Read)))),
          mapOf(Pair(device, TestDeviceState.Sync(SyncState.Read))),
          mapOf(Pair(device, TestDeviceState.Sync(SyncState.Process(this.syncData)))),
          mapOf(Pair(device,
                     TestDeviceState.Sync(SyncState.Save(this.syncData, this.syncSavePath)))),
          mapOf(Pair(device, TestDeviceState.Sync(SyncState.Clear))),
          mapOf()
      ), this.observer.onNextEvents)
      
      assertEquals(1, this.connectionCount)
    }
  }
  
  @Test
  fun singleDeviceSingleOperationWithBootup() {
    DriverTraits.schedulerIsNow({ this.scheduler }) {
      this.makeStateMachine(this::connectionWithBootup)
      val device = Device(1)
      
      assertEquals(0, this.connectionCount)
      
      this.scheduler.scheduleAt(300) { this.sync(device) }
      this.scheduler.advanceTimeBy(1000)
      
      assertEquals(listOf(
          mapOf(),
          mapOf(Pair(device, TestDeviceState.Start(TestDeviceState.Sync(SyncState.Read)))),
          mapOf(Pair(device, TestDeviceState.Sync(SyncState.Read))),
          mapOf(Pair(device, TestDeviceState.PoweredOff(TestDeviceState.Sync(SyncState.Read)))),
          mapOf(Pair(device, TestDeviceState.Sync(SyncState.Read))),
          mapOf(Pair(device, TestDeviceState.Sync(SyncState.Process(this.syncData)))),
          mapOf(Pair(device,
                     TestDeviceState.Sync(SyncState.Save(this.syncData, this.syncSavePath)))),
          mapOf(Pair(device, TestDeviceState.Sync(SyncState.Clear))),
          mapOf()
      ), this.observer.onNextEvents)
      
      assertEquals(1, this.connectionCount)
    }
  }
  
  @Test
  fun singleDeviceSingleOperationWithConnectionError() {
    DriverTraits.schedulerIsNow({ this.scheduler }) {
      this.makeStateMachine(this::connectionWithError)
      val device = Device(1)
      
      assertEquals(0, this.connectionCount)
      
      this.scheduler.scheduleAt(300) { this.sync(device) }
      this.scheduler.advanceTimeBy(1000)
      
      assertEquals(listOf(
          mapOf(),
          mapOf(Pair(device, TestDeviceState.Start(TestDeviceState.Sync(SyncState.Read)))),
          mapOf(Pair(device, TestDeviceState.Sync(SyncState.Read))),
          mapOf(Pair(device, TestDeviceState.Sync(SyncState.Process(this.syncData)))),
          mapOf(Pair(device, TestDeviceState.Error(TestDeviceStateError.Connection))),
          mapOf()
      ), this.observer.onNextEvents)
      
      assertEquals(1, this.connectionCount)
    }
  }
  
  @Test
  fun singleDeviceSingleOperationWithConnectionInterrupt() {
    DriverTraits.schedulerIsNow({ this.scheduler }) {
      this.makeStateMachine(this::connectionWithInterrupt)
      val device = Device(1)
      
      assertEquals(0, this.connectionCount)
      
      this.scheduler.scheduleAt(300) { this.sync(device) }
      this.scheduler.advanceTimeBy(1000)
      
      assertEquals(listOf(
          mapOf(),
          mapOf(Pair(device, TestDeviceState.Start(TestDeviceState.Sync(SyncState.Read)))),
          mapOf(Pair(device, TestDeviceState.Sync(SyncState.Read))),
          mapOf(Pair(device, TestDeviceState.Sync(SyncState.Process(this.syncData)))),
          mapOf(Pair(device,
                     TestDeviceState.PoweredOff(TestDeviceState.Sync(SyncState.Process(this.syncData))))),
          mapOf(Pair(device, TestDeviceState.Sync(SyncState.Process(this.syncData)))),
          mapOf(Pair(device,
                     TestDeviceState.Sync(SyncState.Save(this.syncData, this.syncSavePath)))),
          mapOf(Pair(device, TestDeviceState.Sync(SyncState.Clear))),
          mapOf()
      ), this.observer.onNextEvents)
      
      assertEquals(1, this.connectionCount)
    }
  }
  
  @Test
  fun singleDeviceSingleFailingOperation() {
    DriverTraits.schedulerIsNow({ this.scheduler }) {
      this.makeStateMachine({ perfectConnection(it) }, errorSyncData)
      val device = Device(1)
      
      assertEquals(0, this.connectionCount)
      
      this.scheduler.scheduleAt(300) { this.sync(device) }
      this.scheduler.advanceTimeBy(1000)
      
      assertEquals(listOf(
          mapOf(),
          mapOf(Pair(device, TestDeviceState.Start(TestDeviceState.Sync(SyncState.Read)))),
          mapOf(Pair(device, TestDeviceState.Sync(SyncState.Read))),
          mapOf(Pair(device, TestDeviceState.Sync(SyncState.Process(this.errorSyncData)))),
          mapOf(Pair(device,
                     TestDeviceState.Sync(SyncState.Save(this.errorSyncData, this.syncSavePath)))),
          mapOf(Pair(device, TestDeviceState.Sync(SyncState.Error(TestDeviceStateError.Sync)))),
          mapOf()
      ), this.observer.onNextEvents)
      
      assertEquals(1, this.connectionCount)
    }
  }
  
  @Test
  fun singleDeviceMultipleSequentialOperations() {
    DriverTraits.schedulerIsNow({ this.scheduler }) {
      this.makeStateMachine(this::perfectConnection)
      val device = Device(1)
      
      assertEquals(0, this.connectionCount)
      
      this.scheduler.scheduleAt(300) { this.sync(device) }
      this.scheduler.scheduleAt(500) { this.pair(device) }
      this.scheduler.advanceTimeBy(1000)
      
      assertEquals(listOf(
          mapOf(),
          mapOf(Pair(device, TestDeviceState.Start(TestDeviceState.Sync(SyncState.Read)))),
          mapOf(Pair(device, TestDeviceState.Sync(SyncState.Read))),
          mapOf(Pair(device, TestDeviceState.Sync(SyncState.Process(this.syncData)))),
          mapOf(Pair(device,
                     TestDeviceState.Sync(SyncState.Save(this.syncData, this.syncSavePath)))),
          mapOf(Pair(device, TestDeviceState.Sync(SyncState.Clear))),
          mapOf(),
          mapOf(Pair(device, TestDeviceState.Start(TestDeviceState.Pair(PairState.Read)))),
          mapOf(Pair(device, TestDeviceState.Pair(PairState.Read))),
          mapOf(Pair(device, TestDeviceState.Pair(PairState.Configure(this.pairData)))),
          mapOf(Pair(device, TestDeviceState.Pair(PairState.Reset))),
          mapOf()
      ), this.observer.onNextEvents)
      
      assertEquals(2, this.connectionCount)
    }
  }
  
  @Test
  fun singleDeviceInterruptOperationWithSameOperation() {
    DriverTraits.schedulerIsNow({ this.scheduler }) {
      this.makeStateMachine(this::perfectConnection)
      val device = Device(1)
      
      assertEquals(0, this.connectionCount)
      
      this.scheduler.scheduleAt(300) { this.sync(device) }
      this.scheduler.scheduleAt(380) { this.sync(device) }
      this.scheduler.advanceTimeBy(1000)
      
      assertEquals(listOf(
          mapOf(),
          mapOf(Pair(device, TestDeviceState.Start(TestDeviceState.Sync(SyncState.Read)))),
          mapOf(Pair(device, TestDeviceState.Sync(SyncState.Read))),
          mapOf(Pair(device, TestDeviceState.Sync(SyncState.Process(this.syncData)))),
          mapOf(Pair(device,
                     TestDeviceState.Sync(SyncState.Save(this.syncData, this.syncSavePath)))),
          mapOf(Pair(device, TestDeviceState.Start(TestDeviceState.Sync(SyncState.Read)))),
          mapOf(Pair(device, TestDeviceState.Sync(SyncState.Read))),
          mapOf(Pair(device, TestDeviceState.Sync(SyncState.Process(this.syncData)))),
          mapOf(Pair(device,
                     TestDeviceState.Sync(SyncState.Save(this.syncData, this.syncSavePath)))),
          mapOf(Pair(device, TestDeviceState.Sync(SyncState.Clear))),
          mapOf()
      ), this.observer.onNextEvents)
      
      assertEquals(2, this.connectionCount)
    }
  }
  
  @Test
  fun singleDeviceInterruptOperationWithDifferentOperation() {
    DriverTraits.schedulerIsNow({ this.scheduler }) {
      this.makeStateMachine(this::perfectConnection)
      val device = Device(1)
      
      assertEquals(0, this.connectionCount)
      
      this.scheduler.scheduleAt(300) { this.sync(device) }
      this.scheduler.scheduleAt(380) { this.pair(device) }
      this.scheduler.advanceTimeBy(1000)
      
      assertEquals(listOf(
          mapOf(),
          mapOf(Pair(device, TestDeviceState.Start(TestDeviceState.Sync(SyncState.Read)))),
          mapOf(Pair(device, TestDeviceState.Sync(SyncState.Read))),
          mapOf(Pair(device, TestDeviceState.Sync(SyncState.Process(this.syncData)))),
          mapOf(Pair(device,
                     TestDeviceState.Sync(SyncState.Save(this.syncData, this.syncSavePath)))),
          mapOf(Pair(device, TestDeviceState.Start(TestDeviceState.Pair(PairState.Read)))),
          mapOf(Pair(device, TestDeviceState.Pair(PairState.Read))),
          mapOf(Pair(device, TestDeviceState.Pair(PairState.Configure(this.pairData)))),
          mapOf(Pair(device, TestDeviceState.Pair(PairState.Reset))),
          mapOf()
      ), this.observer.onNextEvents)
      
      assertEquals(2, this.connectionCount)
    }
  }
  
  @Test
  fun twoDevicesSameOperationSequential() {
    DriverTraits.schedulerIsNow({ this.scheduler }) {
      this.makeStateMachine(this::perfectConnection)
      val device1 = Device(1)
      val device2 = Device(2)
      
      assertEquals(0, this.connectionCount)
      
      this.scheduler.scheduleAt(300) { this.sync(device1) }
      this.scheduler.scheduleAt(500) { this.sync(device2) }
      this.scheduler.advanceTimeBy(1000)
      
      assertEquals(listOf(
          mapOf(),
          mapOf(Pair(device1, TestDeviceState.Start(TestDeviceState.Sync(SyncState.Read)))),
          mapOf(Pair(device1, TestDeviceState.Sync(SyncState.Read))),
          mapOf(Pair(device1, TestDeviceState.Sync(SyncState.Process(this.syncData)))),
          mapOf(Pair(device1,
                     TestDeviceState.Sync(SyncState.Save(this.syncData, this.syncSavePath)))),
          mapOf(Pair(device1, TestDeviceState.Sync(SyncState.Clear))),
          mapOf(),
          mapOf(Pair(device2, TestDeviceState.Start(TestDeviceState.Sync(SyncState.Read)))),
          mapOf(Pair(device2, TestDeviceState.Sync(SyncState.Read))),
          mapOf(Pair(device2, TestDeviceState.Sync(SyncState.Process(this.syncData)))),
          mapOf(Pair(device2,
                     TestDeviceState.Sync(SyncState.Save(this.syncData, this.syncSavePath)))),
          mapOf(Pair(device2, TestDeviceState.Sync(SyncState.Clear))),
          mapOf()
      ), this.observer.onNextEvents)
      
      assertEquals(2, this.connectionCount)
    }
  }
  
  @Test
  fun twoDevicesDifferentOperationOverlappingWithBootup() {
    DriverTraits.schedulerIsNow({ this.scheduler }) {
      this.makeStateMachine(this::connectionWithBootup)
      val device1 = Device(1)
      val device2 = Device(2)
      
      assertEquals(0, this.connectionCount)
      
      this.scheduler.scheduleAt(300) { this.sync(device1) }
      this.scheduler.scheduleAt(355) { this.pair(device2) }
      this.scheduler.advanceTimeBy(1000)
      
      assertEquals(listOf(
          mapOf(),
          mapOf(Pair(device1, TestDeviceState.Start(TestDeviceState.Sync(SyncState.Read)))),
          mapOf(Pair(device1, TestDeviceState.Sync(SyncState.Read))),
          mapOf(Pair(device1, TestDeviceState.PoweredOff(TestDeviceState.Sync(SyncState.Read)))),
          mapOf(Pair(device1, TestDeviceState.Sync(SyncState.Read))),
          mapOf(Pair(device1, TestDeviceState.Sync(SyncState.Process(this.syncData)))),
          mapOf(Pair(device1, TestDeviceState.Sync(SyncState.Process(this.syncData))),
                Pair(device2, TestDeviceState.Start(TestDeviceState.Pair(PairState.Read)))),
          mapOf(Pair(device1, TestDeviceState.Sync(SyncState.Process(this.syncData))),
                Pair(device2, TestDeviceState.Pair(PairState.Read))),
          mapOf(Pair(device1, TestDeviceState.Sync(SyncState.Process(this.syncData))),
                Pair(device2, TestDeviceState.PoweredOff(TestDeviceState.Pair(PairState.Read)))),
          mapOf(Pair(device1, TestDeviceState.Sync(SyncState.Process(this.syncData))),
                Pair(device2, TestDeviceState.Pair(PairState.Read))),
          mapOf(Pair(device1,
                     TestDeviceState.Sync(SyncState.Save(this.syncData, this.syncSavePath))),
                Pair(device2, TestDeviceState.Pair(PairState.Read))),
          mapOf(Pair(device1,
                     TestDeviceState.Sync(SyncState.Save(this.syncData, this.syncSavePath))),
                Pair(device2, TestDeviceState.Pair(PairState.Configure(pairData)))),
          mapOf(Pair(device1, TestDeviceState.Sync(SyncState.Clear)),
                Pair(device2, TestDeviceState.Pair(PairState.Configure(pairData)))),
          mapOf(Pair(device2, TestDeviceState.Pair(PairState.Configure(pairData)))),
          mapOf(Pair(device2, TestDeviceState.Pair(PairState.Reset))),
          mapOf()
      ), this.observer.onNextEvents)
      
      assertEquals(2, this.connectionCount)
    }
  }
}
