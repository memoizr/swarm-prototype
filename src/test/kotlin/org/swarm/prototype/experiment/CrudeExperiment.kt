package org.swarm.prototype.experiment

import java.io.*
import kotlin.concurrent.thread

fun main(args: Array<String>) {
    async {
        println("${currentThread()} 1")
        doWork {
            println("${currentThread()} 2")
            suspend = true
        }
        println("${currentThread()} 3")
        doWork {
            println("${currentThread()} 4")
            println(NonSerializable()) // no problem using non serializable objects
            suspend = true
        }
        doWork {
            println("${currentThread()} 5")
        }
        println("${currentThread()} 6")
    }
    println("break")
    thread {
        resumeWork()
        resumeWork()
    }

    /* prints
        org.swarm.prototype.experiment.main 1
        org.swarm.prototype.experiment.main 2
        org.swarm.prototype.experiment.main 3
        break
        Thread-0 4
        org.swarm.prototype.experiment.NonSerializable@127af155
        Thread-0 5
        Thread-0 6
     */
}

class NonSerializable()

private fun currentThread() = Thread.currentThread().name

private fun resumeWork() {
    val x = resumeWork<State<Any>>()
    x.lastComputation()
    x.continuation.resume(Unit)
}

fun async(coroutine f: Controller.() -> Continuation<Unit>) {
    val c = Controller()
    c.f().resume(Unit)
}

class Controller : Serializable {
    var suspend = false
    suspend fun <T> doWork(f: () -> T, c: Continuation<Unit>) {
        if (suspend) {
            suspend = false
            serialize(State(f, c))
        } else {
            f()
            c.resume(Unit)
        }
    }
}

data class State<T>(
        val lastComputation: () -> T,
        val continuation: Continuation<Unit>) : Serializable {
    //        val nonSerializable = org.swarm.prototype.experiment.NonSerializable() // Breaks serialization because field is not serializable
    val nonSerializable = { NonSerializable() } // Perfectly fine
}

fun serialize(serializable: Serializable) {
    val fileOutputStream = FileOutputStream("/tmp/coroutines.ser")
    val objectOutputStream = ObjectOutputStream(fileOutputStream)
    objectOutputStream.writeObject(serializable)
    objectOutputStream.close()
    fileOutputStream.close()
}

inline fun <reified T> resumeWork(): T {
    val fileInputStream = FileInputStream("/tmp/coroutines.ser")
    val objectInputStream = ObjectInputStream(fileInputStream)
    return objectInputStream.readObject() as T
}