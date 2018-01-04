package uk.co.armedpineapple.innoextract

class SpeedCalculator {

    private val MIN_TIME = 2500
    private var lastTime: Long = 0
    private var lastValue: Long = 0


    fun update(progress: Long): Long {
        val now = System.currentTimeMillis()

        try {

            if (lastTime == 0L) {
                return 0
            }

            val secondsPassed: Float = ((now - lastTime) * 1.0f) / 1000
            val bps = ((progress - lastValue) * 1.0f) / secondsPassed
            return bps.toLong()

        } finally {
            if (now - lastTime > MIN_TIME) {
                lastTime = now
                lastValue = progress
            }
        }

    }

}
