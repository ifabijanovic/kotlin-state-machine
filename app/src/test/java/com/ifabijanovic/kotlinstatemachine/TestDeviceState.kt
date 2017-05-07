package com.ifabijanovic.kotlinstatemachine

import io.reactivex.Observable
import io.reactivex.schedulers.TestScheduler
import java.util.concurrent.TimeUnit

/**
 * Created by Ivan Fabijanovic on 07/05/2017.
 */

sealed class TestDeviceState {
    sealed class PairState {
        object Read: TestDeviceState.PairState()
        data class Configure(val data: String): TestDeviceState.PairState()
        object Reset: TestDeviceState.PairState()
    }

    sealed class SyncState {
        object Read: TestDeviceState.SyncState()
        data class Process(val data: String): TestDeviceState.SyncState()
        data class Save(val data: String, val path: String): TestDeviceState.SyncState()
        object Clear: TestDeviceState.SyncState()
    }

    data class Pair(val state: TestDeviceState.PairState): TestDeviceState()
    data class Sync(val state: TestDeviceState.SyncState): TestDeviceState()

    data class Start(val newState: TestDeviceState): TestDeviceState()
    object Cancel: TestDeviceState()
    data class Error(val error: Throwable): TestDeviceState()
    data class PoweredOff(val nextState: TestDeviceState): TestDeviceState()

    fun needsConnection(): Boolean = when(this) {
        is Pair -> true
        is Sync -> true
        is Start -> false
        is Cancel -> false
        is Error -> false
        is PoweredOff -> true
    }
}

data class Device(val id: Int)
data class ConnectedDevice(val device: Device)

sealed class ConnectionResult {
    object PoweredOff: ConnectionResult()
    data class Success(val connectedDevice: ConnectedDevice): ConnectionResult()
}

class TestDeviceStateFeedbackLoops(
        val scheduler: TestScheduler,
        val pairData: String,
        val syncData: String,
        val syncSavePath: String,
        val connect: (Device) -> Observable<ConnectionResult>
) {
    fun feedbackLoops(key: Device): (Observable<TestDeviceState>) -> Observable<Command<Device, TestDeviceState>> {
        return { state ->
            state
                    .map { it.needsConnection() }
                    .distinctUntilChanged()
                    .switchMap { needsConnection ->
                        if (!needsConnection) {
                            return@switchMap state.switchMap { state ->
                                when (state) {
                                    is TestDeviceState.Start -> Observable.just(Command.Update(Pair(key, state.newState)))
                                    else -> Observable.just(Command.Finish<Device, TestDeviceState>(key))
                                }
                            }
                        }

                        return@switchMap this
                                .connect(key, state, { connectedState ->
                                    val currentState = connectedState.first
                                    when (currentState) {
                                        is TestDeviceState.Pair -> this.handleState(key, connectedState.second, currentState.state)
                                        is TestDeviceState.Sync -> this.handleState(key, connectedState.second, currentState.state)
                                        is TestDeviceState.Start -> Observable.just(Command.Finish<Device, TestDeviceState>(key))
                                        is TestDeviceState.Cancel -> Observable.just(Command.Finish<Device, TestDeviceState>(key))
                                        is TestDeviceState.Error -> Observable.just(Command.Finish<Device, TestDeviceState>(key))
                                        is TestDeviceState.PoweredOff -> Observable.just(Command.Update(Pair(key, currentState.nextState)))
                                    }
                                })
                    }
        }
    }

    private fun connect(device: Device, state: Observable<TestDeviceState>, effects: (Pair<TestDeviceState, ConnectedDevice
            >) -> Observable<Command<Device, TestDeviceState>>): Observable<Command<Device, TestDeviceState>> {
        return this
                .connect(device)
                .switchMap { connectionResult ->
                    return@switchMap state
                            .take(1)
                            .flatMap { currentState ->
                                when (connectionResult) {
                                    is ConnectionResult.PoweredOff -> Observable.just(Command.Update(Pair(device, currentState)))
                                    is ConnectionResult.Success -> state.switchMap { effects(Pair(it, connectionResult.connectedDevice)) }
                                }
                            }
                }
                .onErrorReturn { Command.Update(Pair(device, TestDeviceState.Error(it))) }
    }

    private fun handleState(device: Device, connectedDevice: ConnectedDevice, state: TestDeviceState.PairState): Observable<Command<Device, TestDeviceState>> = when (state) {
        // connectedDevice would be used here
        is TestDeviceState.PairState.Read -> this.update(10, device, TestDeviceState.Pair(TestDeviceState.PairState.Configure(this.pairData)))
        is TestDeviceState.PairState.Configure -> this.update(40, device, TestDeviceState.Pair(TestDeviceState.PairState.Reset))
        is TestDeviceState.PairState.Reset -> this.finish(10, device)
    }

    private fun handleState(device: Device, connectedDevice: ConnectedDevice, state: TestDeviceState.SyncState): Observable<Command<Device, TestDeviceState>> = when (state) {
        // connectedDevice would be used here
        is TestDeviceState.SyncState.Read -> this.update(10, device, TestDeviceState.Sync(TestDeviceState.SyncState.Process(this.syncData)))
        is TestDeviceState.SyncState.Process -> this.update(50, device, TestDeviceState.Sync(TestDeviceState.SyncState.Save(this.syncData, this.syncSavePath)))
        is TestDeviceState.SyncState.Save -> if (state.data == "error") Observable.error(Exception()) else this.update(30, device, TestDeviceState.Sync(TestDeviceState.SyncState.Clear))
        is TestDeviceState.SyncState.Clear -> this.finish(10, device)
    }

    private fun update(period: Long, key: Device, state: TestDeviceState): Observable<Command<Device, TestDeviceState>> {
        return Observable
                .interval(period, TimeUnit.SECONDS, this.scheduler)
                .take(1)
                .map { Command.Update(Pair(key, state)) }
    }

    private fun finish(period: Long, key: Device): Observable<Command<Device, TestDeviceState>> {
        return Observable
                .interval(period, TimeUnit.SECONDS, this.scheduler)
                .take(1)
                .map { Command.Finish<Device, TestDeviceState>(key) }
    }
}
