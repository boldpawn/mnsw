package nl.mnsw

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MnswApplication

fun main(args: Array<String>) {
    runApplication<MnswApplication>(*args)
}
