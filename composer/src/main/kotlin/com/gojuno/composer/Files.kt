package com.gojuno.composer

import io.reactivex.Observable
import org.apache.commons.io.input.Tailer
import org.apache.commons.io.input.TailerListener
import java.io.File
import java.io.FileNotFoundException

fun tail(file: File): Observable<String> = Observable.create { emitter ->
    Tailer.create(file, object : TailerListener {
        override fun init(tailer: Tailer) = emitter.setCancellable { tailer.stop() }
        override fun handle(line: String) = emitter.onNext(line)
        override fun handle(e: Exception) = emitter.onError(e)
        override fun fileRotated() = emitter.onError(IllegalStateException("Output rotation detected $file"))
        override fun fileNotFound() = emitter.onError(FileNotFoundException("$file file was not found"))
    })
}
