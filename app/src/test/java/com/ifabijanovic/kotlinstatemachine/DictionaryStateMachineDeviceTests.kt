package com.ifabijanovic.kotlinstatemachine

import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.TestScheduler
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Created by Ivan Fabijanovic on 07/05/2017.
 */

class DictionaryStateMachineDeviceTests {
    var scheduler = TestScheduler()
    var observer = TestObserver<Map<Device, TestDeviceState>>()
    var stateMachine = DictionaryStateMachine<Device,TestDeviceState>(this.scheduler, { _ -> { _ -> Observable.empty() } })
    var connectionCount = 0

    val pairData = "pair data"
    val syncData = "sync data"
    val syncSavePath = "file://test.file"
    val errorSyncData = "error"

    @Before
    fun setUp() {
        this.scheduler = TestScheduler()
        this.observer = TestObserver()
        this.connectionCount = 0
    }

    @After
    fun tearDown() {
        this.observer.dispose()
        this.stateMachine.dispose()
    }

    fun makeStateMachine(connect: (Device) -> Observable<ConnectionResult>, syncData: String = this.syncData) {
        this.stateMachine = DictionaryStateMachine(this.scheduler, TestDeviceStateFeedbackLoops(this.scheduler, this.pairData, syncData, this.syncSavePath, connect)::feedbackLoops)
        this.stateMachine.state.subscribe(this.observer)
    }

    fun perfectConnection(device: Device): Observable<ConnectionResult> {
        return Observable
                .just(ConnectionResult.Success(ConnectedDevice(device)))
                .delay(10, TimeUnit.SECONDS, this.scheduler)
                .doOnNext { this.connectionCount++ }
                .map { it }
    }

    fun pair(device: Device) {
        this.stateMachine.transition(Pair(device, TestDeviceState.Start(TestDeviceState.Pair(TestDeviceState.PairState.Read))))
    }

    fun sync(device: Device) {
        this.stateMachine.transition(Pair(device, TestDeviceState.Start(TestDeviceState.Sync(TestDeviceState.SyncState.Read))))
    }

    @Test
    fun singleDeviceSingleOperation() {
        this.makeStateMachine(this::perfectConnection)
        val device = Device(1)

        assertEquals(this.connectionCount, 0)

        this.scheduler.createWorker().schedule({ this.sync(device) }, 300, TimeUnit.SECONDS)
        this.scheduler.advanceTimeBy(1000, TimeUnit.SECONDS)

        assertEquals(this.observer.values(), listOf(
                mapOf(),
                mapOf(Pair(device, TestDeviceState.Start(TestDeviceState.Sync(TestDeviceState.SyncState.Read)))),
                mapOf(Pair(device, TestDeviceState.Sync(TestDeviceState.SyncState.Read))),
                mapOf(Pair(device, TestDeviceState.Sync(TestDeviceState.SyncState.Process(this.syncData)))),
                mapOf(Pair(device, TestDeviceState.Sync(TestDeviceState.SyncState.Save(this.syncData, this.syncSavePath)))),
                mapOf(Pair(device, TestDeviceState.Sync(TestDeviceState.SyncState.Clear))),
                mapOf()
        ))

        assertEquals(this.connectionCount, 1)
    }
}
