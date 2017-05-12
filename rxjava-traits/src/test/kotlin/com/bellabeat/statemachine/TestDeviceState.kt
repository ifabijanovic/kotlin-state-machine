package com.bellabeat.statemachine

import common.Event
import common.Event.Next
import common.createColdObservable
import io.reactivex.rxjava.traits.*
import rx.Observable
import rx.schedulers.TestScheduler

/**
 * Created by Ivan Fabijanovic on 07/05/2017.
 */

sealed class TestDeviceState {
  data class Pair(val state: PairState) : TestDeviceState()
  data class Sync(val state: SyncState) : TestDeviceState()
  data class Start(val newState: TestDeviceState) : TestDeviceState()
  object Cancel : TestDeviceState()
  data class Error(val error: Throwable) : TestDeviceState()
  data class PoweredOff(val nextState: TestDeviceState) : TestDeviceState()
  
  fun needsConnection(): Boolean = when (this) {
    is Pair -> true
    is Sync -> true
    is Start -> false
    is Cancel -> false
    is Error -> false
    is PoweredOff -> true
  }
}

sealed class PairState {
  object Read : PairState()
  data class Configure(val data: String) : PairState()
  object Reset : PairState()
  data class Error(val error: Throwable) : PairState()
}

sealed class SyncState {
  object Read : SyncState()
  data class Process(val data: String) : SyncState()
  data class Save(val data: String, val path: String) : SyncState()
  object Clear : SyncState()
  data class Error(val error: Throwable) : SyncState()
}

data class Device(val id: Int)
data class ConnectedDevice(val device: Device)

sealed class ConnectionResult {
  object PoweredOff : ConnectionResult()
  data class Success(val connectedDevice: ConnectedDevice) : ConnectionResult()
}

sealed class TestDeviceStateError : Throwable() {
  object Connection : TestDeviceStateError()
  object Sync : TestDeviceStateError()
}

class TestDeviceStateFeedbackLoops(
    val scheduler: TestScheduler, val pairData: String, val syncData: String,
    val syncSavePath: String, val connect: (Device) -> Observable<ConnectionResult>) {
  
  fun feedbackLoops(device: Device): (Driver<TestDeviceState>) -> Driver<Command<Device, TestDeviceState>> {
    return { state ->
      state
          .map { it.needsConnection() }
          .distinctUntilChanged()
          .switchMap { needsConnection ->
            if (!needsConnection) {
              return@switchMap state.switchMap {
                when (it) {
                  is TestDeviceState.Start ->
                    Driver.just<Command<Device, TestDeviceState>>(Command.Update(Pair(device,
                                                                                      it.newState)))
                  else -> Driver.just<Command<Device, TestDeviceState>>(Command.Finish(device))
                }
              }
            }
            return@switchMap connect(device, state)
          }
    }
  }
  
  private fun connect(device: Device,
                      state: Driver<TestDeviceState>): Driver<Command<Device, TestDeviceState>> =
      this.connect(device)
          .switchMap { connectionResult ->
            state.catchErrorAndComplete()
                .asObservable()
                .first()
                .switchMap<Command<Device, TestDeviceState>> { currentState ->
                  when (connectionResult) {
                    is ConnectionResult.PoweredOff ->
                      Observable.just(Command.Update(Pair(device,
                                                          TestDeviceState.PoweredOff(currentState))))
                    is ConnectionResult.Success -> {
                      val connectedDevice = connectionResult.connectedDevice
                      state.switchMap<TestDeviceState, DriverTraits.Companion, Command<Device, TestDeviceState>> {
                        when (it) {
                          is TestDeviceState.Pair -> {
                            PairStateFeedbackLoops.feedbackLoops(device,
                                                                 connectedDevice,
                                                                 it.state,
                                                                 scheduler,
                                                                 pairData)
                                .asDriver(onErrorDriveWith = {
                                  Driver.just(Command.Update(Pair(device,
                                                                  TestDeviceState.Pair(PairState.Error(
                                                                      it)))))
                                })
                          }
                          is TestDeviceState.Sync -> {
                            SyncStateFeedbackLoops.feedbackLoops(device,
                                                                 connectedDevice,
                                                                 it.state,
                                                                 scheduler,
                                                                 syncData,
                                                                 syncSavePath)
                                .asDriver(onErrorDriveWith = {
                                  Driver.just(Command.Update(Pair(device,
                                                                  TestDeviceState.Sync(SyncState.Error(
                                                                      it)))))
                                })
                          }
                          is TestDeviceState.Start ->
                            Driver.just(Command.Finish<Device, TestDeviceState>(device))
                          is TestDeviceState.Cancel ->
                            Driver.just(Command.Finish<Device, TestDeviceState>(device))
                          is TestDeviceState.Error ->
                            Driver.just(Command.Finish<Device, TestDeviceState>(device))
                          is TestDeviceState.PoweredOff ->
                            Driver.just(Command.Update(Pair(device, it.nextState)))
                        }
                      }
                          .catchErrorAndComplete()
                          .asObservable()
                    }
                  }
                }
          }
          .asDriver(onErrorDriveWith = {
            Driver.just(Command.Update(Pair(device, TestDeviceState.Error(it))))
          })
  
  object PairStateFeedbackLoops {
    fun feedbackLoops(device: Device,
                      connectedDevice: ConnectedDevice,
                      state: PairState,
                      scheduler: TestScheduler,
                      data: String): Observable<Command<Device, TestDeviceState>> =
        when (state) {
          PairState.Read -> update(10,
                                   device,
                                   TestDeviceState.Pair(PairState.Configure(data)),
                                   scheduler)
          is PairState.Configure -> update(40,
                                           device,
                                           TestDeviceState.Pair(PairState.Reset),
                                           scheduler)
          PairState.Reset -> finish(10, device, scheduler)
          is PairState.Error -> finish(0, device, scheduler)
        }
  }
  
  object SyncStateFeedbackLoops {
    fun feedbackLoops(device: Device,
                      connectedDevice: ConnectedDevice,
                      state: SyncState,
                      scheduler: TestScheduler,
                      data: String,
                      savePath: String): Observable<Command<Device, TestDeviceState>> =
        when (state) {
          SyncState.Read -> update(10,
                                   device,
                                   TestDeviceState.Sync(SyncState.Process(data)), scheduler)
          is SyncState.Process -> update(50,
                                         device,
                                         TestDeviceState.Sync(SyncState.Save(data, savePath)),
                                         scheduler)
          is SyncState.Save -> {
            if (state.data == "error") scheduler.createColdObservable(Event.Error(15,
                                                                                  TestDeviceStateError.Sync))
            else update(30, device, TestDeviceState.Sync(SyncState.Clear), scheduler)
          }
          SyncState.Clear -> finish(10, device, scheduler)
          is SyncState.Error -> finish(0, device, scheduler)
        }
  }
}

fun update(period: Long,
           key: Device,
           state: TestDeviceState,
           scheduler: TestScheduler): Observable<Command<Device, TestDeviceState>> =
    scheduler.createColdObservable<Command<Device, TestDeviceState>>(
        Next(period, Command.Update(Pair(key, state)))
    )

fun finish(period: Long,
           key: Device,
           scheduler: TestScheduler): Observable<Command<Device, TestDeviceState>> =
    scheduler.createColdObservable<Command<Device, TestDeviceState>>(
        Next(period, Command.Finish<Device, TestDeviceState>(key))
    )
