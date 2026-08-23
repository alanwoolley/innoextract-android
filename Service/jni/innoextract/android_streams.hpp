#ifndef INNOEXTRACT_ANDROID_SERVICE_JNI_INNOEXTRACT_ANDROID_STREAMS_HPP_
#define INNOEXTRACT_ANDROID_SERVICE_JNI_INNOEXTRACT_ANDROID_STREAMS_HPP_

#include "native_interface.hpp"
#include <boost/filesystem/fstream.hpp>
#include <boost/filesystem/path.hpp>

class android_ofstream {
public:
    typedef typename boost::filesystem::ofstream::off_type off_type;

    void open(const boost::filesystem::path &p, std::ios_base::openmode mode);

    bool is_open();

    void write(const char *__s, std::streamsize __n);

    bool fail();

    void seekp(off_type __off, std::ios_base::seekdir __dir);

    void seekg(off_type __off, std::ios_base::seekdir __dir);

    void flush();

    void close();

    bool eof();

    std::basic_istream<char>& read(std::basic_istream<char>::char_type* type, std::streamsize n);

    boost::filesystem::fstream * stream() { return stream_; }

    ~android_ofstream() {
        delete (stream_);
        stream_ = nullptr;
    }

private:
    boost::filesystem::fstream *stream_;
    std::string path_;
    jobject fileProxy_;
};

#endif //INNOEXTRACT_ANDROID_SERVICE_JNI_INNOEXTRACT_ANDROID_STREAMS_HPP_
