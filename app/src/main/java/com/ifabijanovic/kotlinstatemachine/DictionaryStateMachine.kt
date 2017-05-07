package com.ifabijanovic.kotlinstatemachine

import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject

/**
 * Created by Ivan Fabijanovic on 06/05/2017.
 */

class DictionaryStateMachine<Key, State>(scheduler: Scheduler, val effectsForKey: (Key) -> (Observable<State>) -> Observable<Command<Key, State>>) {
    private val commands: PublishSubject<Pair<Key, State>> = PublishSubject.create()
    private val stateSubscription: Disposable

    val state: Observable<Map<Key, State>>

    init {
        val userCommandsFeedback: (Observable<Map<Key, State>>) -> Observable<Command<Key, State>> = { _ ->
            this.commands.map { Command.Update(it) }
        }

        this.state = system(
                mutableMapOf<Key, State>(),
                ::reduce,
                scheduler,
                userCommandsFeedback, perKeyFeedbackLoop(this.effectsForKey)
        ).share().replay(1).refCount()

        this.stateSubscription = this.state.subscribe()
    }

    fun transition(to: Pair<Key, State>) {
        this.commands.onNext(to)
    }

    fun dispose() {
        this.stateSubscription.dispose()
    }
}

sealed class Command<out Key, out State> {
    data class Update<out Key, out State>(val state: Pair<Key, State>) : Command<Key, State>()
    data class Finish<out Key, out State>(val key: Key) : Command<Key, State>()
}

private sealed class MutationEvent<in Key, out State> {
    data class Started<Key, out State>(val state: Pair<Key, State>): MutationEvent<Key, State>()
    data class Updated<Key, out State>(val state: Pair<Key, State>): MutationEvent<Key, State>()
    data class Finished<Key, out State>(val key: Key): MutationEvent<Key, State>()

    fun isUpdate(key: Key): Boolean = when(this) {
        is Updated -> this.state.first == key
        else -> false
    }

    fun isFinished(key: Key): Boolean = when(this) {
        is Finished -> this.key == key
        else -> false
    }

    fun state(): State? = when(this) {
        is Started -> this.state.second
        is Updated -> this.state.second
        is Finished -> null
    }
}

private fun <Key, State> reduce(state: Map<Key, State>, command: Command<Key, State>): Map<Key, State> = when(command) {
    is Command.Update -> {
        val newState = HashMap(state)
        newState[command.state.first] = command.state.second
        newState
    }
    is Command.Finish -> {
        val newState = HashMap(state)
        newState.remove(command.key)
        newState
    }
}

private fun <Key, State> perKeyFeedbackLoop(effects: (Key) -> (Observable<State>) -> Observable<Command<Key, State>>): (Observable<Map<Key, State>>) -> Observable<Command<Key, State>> {
    return { state ->
        val events = scanAndMaybeEmit(
                observable = state,
                state = mapOf<Key, State>(),
                accumulator = { states ->
                    val oldState = states.first
                    val newState = states.second

                    val newKeys = newState.keys
                    val oldKeys = oldState.keys

                    val finishedEvents: List<MutationEvent<Key, State>> = oldKeys.subtract(newKeys).map { MutationEvent.Finished<Key, State>(it) }
                    val newEvents: List<MutationEvent<Key, State>> = newKeys.subtract(oldKeys).map { MutationEvent.Started(Pair(it, newState[it]!!)) }
                    val updatedEvents: List<MutationEvent<Key, State>> = newKeys.intersect(oldKeys).map { MutationEvent.Updated(Pair(it, newState[it]!!)) }

                    return@scanAndMaybeEmit Pair(newState, newEvents + updatedEvents + finishedEvents)
                })
                .flatMapIterable { it }

        events
                .flatMap { event ->
                    val started = event as? MutationEvent.Started<Key, State> ?: return@flatMap Observable.empty<Command<Key, State>>()

                    val keyState = started.state

                    val statePerKey = events
                            .filter { it.isUpdate(keyState.first) }
                            .startWith(event)
                            .flatMap { it.state()?.let { Observable.just(it) } ?: Observable.empty() }
                            .distinctUntilChanged()

                    return@flatMap effects(keyState.first)(statePerKey)
                            .takeUntil(events.filter { it.isFinished(keyState.first) })
                }
    }
}
