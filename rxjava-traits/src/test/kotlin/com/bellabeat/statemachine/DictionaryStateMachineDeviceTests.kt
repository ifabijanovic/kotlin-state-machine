package com.bellabeat.statemachine

import io.reactivex.rxjava.traits.*
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
      this.stateMachine = DictionaryStateMachine(
          TestDeviceStateFeedbackLoops(this.scheduler, this.pairData, syncData,
                                       this.syncSavePath, connect)::feedbackLoops)
      this.stateMachine.state.catchErrorAndComplete().drive(this.observer)
    })
  }
  
  fun perfectConnection(device: Device): Observable<ConnectionResult> =
      Observable.timer(10, TimeUnit.SECONDS, this.scheduler)
          .map <ConnectionResult> { ConnectionResult.Success(ConnectedDevice(device)) }
          .doOnSubscribe { this.connectionCount++ }
  
  fun connectionWithBootup(device: Device): Observable<ConnectionResult> {
    val poweredOff = Observable.timer(5, TimeUnit.SECONDS, this.scheduler)
        .map { ConnectionResult.PoweredOff }
    
    val success = Observable.timer(20, TimeUnit.SECONDS, this.scheduler)
        .map { ConnectionResult.Success(ConnectedDevice(device)) }
    
    return Observable.merge(poweredOff, success)
        .doOnSubscribe { this.connectionCount++ }
  }
  
  fun connectionWithError(device: Device): Observable<ConnectionResult> {
    val success = Observable.timer(10, TimeUnit.SECONDS, this.scheduler)
        .map { ConnectionResult.Success(ConnectedDevice(device)) }
    
    val error = Observable.timer(50, TimeUnit.SECONDS, this.scheduler)
        .map<ConnectionResult> { throw TestDeviceStateError.Connection }
    
    return Observable.merge(success, error)
        .doOnSubscribe { this.connectionCount++ }
  }
  
  fun connectionWithInterrupt(device: Device): Observable<ConnectionResult> {
    val connectedDevice = ConnectedDevice(device)
    val success1 = Observable.timer(10, TimeUnit.SECONDS, this.scheduler)
        .map { ConnectionResult.Success(connectedDevice) }
    
    val poweredOff = Observable.timer(40, TimeUnit.SECONDS, this.scheduler)
        .map { ConnectionResult.PoweredOff }
    
    val success2 = Observable.timer(60, TimeUnit.SECONDS, this.scheduler)
        .map { ConnectionResult.Success(connectedDevice) }
    
    return Observable.merge(success1, poweredOff, success2)
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
      
      this.scheduler.createWorker().schedule({ this.sync(device) }, 300, TimeUnit.SECONDS)
      this.scheduler.advanceTimeBy(1000, TimeUnit.SECONDS)
      
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
      
      this.scheduler.createWorker().schedule({ this.sync(device) }, 300, TimeUnit.SECONDS)
      this.scheduler.advanceTimeBy(1000, TimeUnit.SECONDS)
      
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
      
      this.scheduler.createWorker().schedule({ this.sync(device) }, 300, TimeUnit.SECONDS)
      this.scheduler.advanceTimeBy(1000, TimeUnit.SECONDS)
      
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
      
      this.scheduler.createWorker().schedule({ this.sync(device) }, 300, TimeUnit.SECONDS)
      this.scheduler.advanceTimeBy(1000, TimeUnit.SECONDS)
      
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
}
