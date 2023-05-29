#include "android_streams.hpp"

void android_ofstream::open(const boost::filesystem::path &p, std::ios_base::openmode mode) {
  fileProxy_ = getOutputFile(p.string());
  path_ = getFileProxyPath(fileProxy_);
  stream_ = new boost::filesystem::ofstream(path_);
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

void android_ofstream::flush() {
  stream_->flush();
}

void android_ofstream::close() {
  stream_->close();
  closeFileProxy(fileProxy_);
}
