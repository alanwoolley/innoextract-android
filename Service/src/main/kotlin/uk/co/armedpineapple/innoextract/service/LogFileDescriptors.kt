package uk.co.armedpineapple.innoextract.service

enum class LogFileDescriptors(val fd: Int) {
    STDERR(2),
    STDOUT(1)
}