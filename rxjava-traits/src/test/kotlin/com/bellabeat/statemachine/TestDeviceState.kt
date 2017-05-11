package com.bellabeat.statemachine

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
  //    data class Error(val error: Throwable) : TestDeviceState.PairState()
}

sealed class SyncState {
  object Read : SyncState()
  data class Process(val data: String) : SyncState()
  data class Save(val data: String, val path: String) : SyncState()
  object Clear : SyncState()
  //    data class Error(val error: Throwable) : TestDeviceState.SyncState()
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
  
  fun feedbackLoops(key: Device): (Driver<TestDeviceState>) -> Driver<Command<Device, TestDeviceState>> {
    return { state ->
      state
          .map { it.needsConnection() }
          .distinctUntilChanged()
          .switchMap { needsConnection ->
            if (!needsConnection) {
              return@switchMap state.switchMap<TestDeviceState, DriverTraits.Companion, Command<Device, TestDeviceState>> {
                when (it) {
                  is TestDeviceState.Start -> Driver.just(Command.Update(Pair(key, it.newState)))
                  else -> Driver.just(Command.Finish(key))
                }
              }
            }
            
            return@switchMap this
                .connect(key, state, { (currentState, connectedDevice) ->
                  when (currentState) {
                    is TestDeviceState.Pair ->
                      this.handleState(key, connectedDevice, currentState.state)
                    is TestDeviceState.Sync ->
                      this.handleState(key, connectedDevice, currentState.state)
                    is TestDeviceState.Start ->
                      Observable.just(Command.Finish<Device, TestDeviceState>(key))
                    is TestDeviceState.Cancel ->
                      Observable.just(Command.Finish<Device, TestDeviceState>(key))
                    is TestDeviceState.Error ->
                      Observable.just(Command.Finish<Device, TestDeviceState>(key))
                    is TestDeviceState.PoweredOff ->
                      Observable.just(Command.Update(Pair(key, currentState.nextState)))
                  }
                })
                .asDriver(onErrorDriveWith = {
                  Driver.just(Command.Update(Pair(key, TestDeviceState.Error(it))))
                })
          }
    }
  }
  
  private fun connect(device: Device,
                      state: Driver<TestDeviceState>,
                      effects: (Pair<TestDeviceState, ConnectedDevice>) -> Observable<Command<Device, TestDeviceState>>): Observable<Command<Device, TestDeviceState>> {
    return this
        .connect(device)
        .switchMap { connectionResult ->
          state
              .catchErrorAndComplete()
              .asObservable()
              .first()
              .flatMap { currentState ->
                when (connectionResult) {
                  is ConnectionResult.PoweredOff -> Observable.just(Command.Update(Pair(device,
                                                                                        TestDeviceState.PoweredOff(
                                                                                            currentState))))
                  is ConnectionResult.Success -> state.catchErrorAndComplete().asObservable().switchMap {
                    effects(Pair(it, connectionResult.connectedDevice))
                  }
                }
              }
        }
  }
  
  private fun handleState(device: Device, connectedDevice: ConnectedDevice,
                          state: PairState): Observable<Command<Device, TestDeviceState>> = when (state) {
  // connectedDevice would be used here
    is PairState.Read -> this.update(10, device, TestDeviceState.Pair(PairState.Configure(
        this.pairData)))
    is PairState.Configure -> this.update(40, device, TestDeviceState.Pair(PairState.Reset))
    is PairState.Reset -> this.finish(10, device)
  }
  
  private fun handleState(device: Device, connectedDevice: ConnectedDevice,
                          state: SyncState): Observable<Command<Device, TestDeviceState>> = when (state) {
  // connectedDevice would be used here
    is SyncState.Read -> this.update(10, device,
                                     TestDeviceState.Sync(SyncState.Process(this.syncData)))
    is SyncState.Process -> this.update(50, device,
                                        TestDeviceState.Sync(SyncState.Save(this.syncData,
                                                                            this.syncSavePath)))
    is SyncState.Save -> if (state.data == "error") Observable.error(Exception()) else this.update(
        30, device, TestDeviceState.Sync(SyncState.Clear))
    is SyncState.Clear -> this.finish(10, device)
  }
  
  private fun update(period: Long, key: Device,
                     state: TestDeviceState): Observable<Command<Device, TestDeviceState>> =
      scheduler.createColdObservable<Command<Device, TestDeviceState>>(
          Next(period, Command.Update(Pair(key, state)))
      )
  
  private fun finish(period: Long, key: Device): Observable<Command<Device, TestDeviceState>> =
      scheduler.createColdObservable<Command<Device, TestDeviceState>>(
          Next(period, Command.Finish<Device, TestDeviceState>(key))
      )
}
