package com.example.lifet.core.common

import javax.inject.Qualifier

/** Dispatcher para E/S (Room, DataStore, red). Inyectar siempre; nunca `Dispatchers.IO` directo. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
public annotation class IoDispatcher

/** Dispatcher para trabajo de CPU (cálculos de métricas, mapeos pesados). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
public annotation class DefaultDispatcher
