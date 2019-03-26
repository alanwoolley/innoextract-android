package uk.co.armedpineapple.innoextract.service

class SpeedCalculator {


    private var lastTime: Long = 0
    private var buffer: LongArray = LongArray(size=3)
    private var bufferIdx: Int = 0
    private var lastValue: Long = 0
    private var lastAverage: Long = 0
    private var bufferFilled = false


    fun reset() {
        bufferIdx =0
        lastTime=0
        lastValue=0
        lastAverage=0
        bufferFilled = false
    }

    fun update(progress: Long): Long {
        val now = System.currentTimeMillis()

            if (lastTime == 0L) {
                lastTime = now
            } else if (now - lastTime > MIN_TIME) {

                val secondsPassed: Float = ((now - lastTime) * 1.0f) / 1000
                val bps = ((progress - lastValue) * 1.0f) / secondsPassed

                lastTime = now
                lastValue = progress
                bufferIdx = (bufferIdx + 1) % buffer.size
                bufferFilled = (bufferFilled || bufferIdx == 0)
                buffer[bufferIdx] = bps.toLong()

                if (bufferFilled) {
                   lastAverage = buffer.average().toLong()
                }
            }

            return lastAverage

    }

    companion object {
        private const val MIN_TIME = 5000
    }

}
