#include "android_streams.hpp"

void android_ofstream::open(const boost::filesystem::path &p, std::ios_base::openmode mode) {
    fileProxy_ = getOutputFile(p.string());
    path_ = getFileProxyPath(fileProxy_);
    stream_ = new boost::filesystem::fstream(path_);
}

bool android_ofstream::is_open() {
    return stream_->is_open();
}

void android_ofstream::write(const char *__s, std::streamsize __n) {
    stream_->write(__s, __n);
}

bool android_ofstream::fail() {
    return stream_->fail();
}

void android_ofstream::seekp(boost::filesystem::ofstream::off_type __off,
                             std::ios_base::seekdir __dir) {
    stream_->seekp(__off, __dir);
}

void android_ofstream::seekg(boost::filesystem::fstream::off_type __off, std::ios_base::seekdir __dir) {
    stream_->seekg(__off, __dir);
}

void android_ofstream::flush() {
    stream_->flush();
}

void android_ofstream::close() {
    stream_->close();
    closeFileProxy(fileProxy_);
}

bool android_ofstream::eof() {
    return stream_->eof();
}

std::basic_istream<char> &
android_ofstream::read(std::basic_istream<char>::char_type *type, std::streamsize n) {
    return stream_->read(type, n);
}