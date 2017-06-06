package com.gojuno.composer

import org.apache.commons.io.input.Tailer
import org.apache.commons.io.input.TailerListener
import rx.Emitter.BackpressureMode
import rx.Observable
import java.io.File
import java.io.FileNotFoundException
import java.lang.Exception

fun tail(file: File): Observable<String> = Observable.create<String>(
        { emitter ->
            val o = None
          
            Tailer.create(file, object : TailerListener {
                override fun init(tailer: Tailer) = emitter.setCancellation { tailer.stop() }
                override fun handle(line: String) = emitter.onNext(line)
                override fun handle(e: Exception) = emitter.onError(e)
                override fun fileRotated() = emitter.onError(IllegalStateException("Output rotation detected $file"))
                override fun fileNotFound() = emitter.onError(FileNotFoundException("$file file was not found"))
            })
        },
        BackpressureMode.BUFFER
)


sealed class Optional<out T : Any>

data class Some<out T : Any>(val value: T) : Optional<T>()

object None : Optional<Nothing>()
