package dev.alinco8.xmmp.client.sync

import kotlinx.coroutines.Job

class ExclusiveJob(
    private val block: () -> Job,
) {
    private var job: Job? = null

    fun launch() {
        if (job?.isActive == true) return
        job = block()
    }
}
