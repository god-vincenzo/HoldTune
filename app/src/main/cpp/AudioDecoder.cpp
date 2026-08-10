#include "AudioDecoder.h"
#include <media/NdkMediaExtractor.h>
#include <media/NdkMediaCodec.h>
#include <media/NdkMediaFormat.h>
#include <android/log.h>

#define LOG_TAG "AudioDecoder"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

bool AudioDecoder::decode(const std::string& filePath, 
                           std::vector<float>& outSamples, 
                           int32_t& outSampleRate, 
                           int32_t& outChannelCount) {
    LOGI("Decoding file: %s", filePath.c_str());
    
    AMediaExtractor* extractor = AMediaExtractor_new();
    if (!extractor) {
        LOGE("Failed to create MediaExtractor");
        return false;
    }

    media_status_t status = AMediaExtractor_setDataSource(extractor, filePath.c_str());
    if (status != AMEDIA_OK) {
        LOGE("Failed to set data source for path: %s (status: %d)", filePath.c_str(), status);
        AMediaExtractor_delete(extractor);
        return false;
    }

    int32_t trackCount = AMediaExtractor_getTrackCount(extractor);
    int32_t audioTrackIndex = -1;
    const char* mime = nullptr;

    for (int32_t i = 0; i < trackCount; ++i) {
        AMediaFormat* format = AMediaExtractor_getTrackFormat(extractor, i);
        if (AMediaFormat_getString(format, AMEDIAFORMAT_KEY_MIME, &mime)) {
            if (std::string(mime).find("audio/") == 0) {
                audioTrackIndex = i;
                AMediaFormat_delete(format);
                break;
            }
        }
        AMediaFormat_delete(format);
    }

    if (audioTrackIndex < 0) {
        LOGE("No audio track found in file");
        AMediaExtractor_delete(extractor);
        return false;
    }

    AMediaExtractor_selectTrack(extractor, audioTrackIndex);
    AMediaFormat* format = AMediaExtractor_getTrackFormat(extractor, audioTrackIndex);

    AMediaFormat_getInt32(format, AMEDIAFORMAT_KEY_SAMPLE_RATE, &outSampleRate);
    AMediaFormat_getInt32(format, AMEDIAFORMAT_KEY_CHANNEL_COUNT, &outChannelCount);
    LOGI("Mime: %s, Sample Rate: %d, Channels: %d", mime, outSampleRate, outChannelCount);

    AMediaCodec* codec = AMediaCodec_createDecoderByType(mime);
    if (!codec) {
        LOGE("Failed to create decoder");
        AMediaFormat_delete(format);
        AMediaExtractor_delete(extractor);
        return false;
    }

    if (AMediaCodec_configure(codec, format, nullptr, nullptr, 0) != AMEDIA_OK) {
        LOGE("Failed to configure decoder");
        AMediaCodec_delete(codec);
        AMediaFormat_delete(format);
        AMediaExtractor_delete(extractor);
        return false;
    }

    if (AMediaCodec_start(codec) != AMEDIA_OK) {
        LOGE("Failed to start decoder");
        AMediaCodec_delete(codec);
        AMediaFormat_delete(format);
        AMediaExtractor_delete(extractor);
        return false;
    }

    bool sawInputEOS = false;
    bool sawOutputEOS = false;
    constexpr int64_t kTimeoutUs = 2000; // 2ms timeout

    while (!sawOutputEOS) {
        if (!sawInputEOS) {
            ssize_t inputBufIdx = AMediaCodec_dequeueInputBuffer(codec, kTimeoutUs);
            if (inputBufIdx >= 0) {
                size_t bufSize;
                uint8_t* buf = AMediaCodec_getInputBuffer(codec, inputBufIdx, &bufSize);
                if (buf) {
                    ssize_t sampleSize = AMediaExtractor_readSampleData(extractor, buf, bufSize);
                    if (sampleSize < 0) {
                        sawInputEOS = true;
                        AMediaCodec_queueInputBuffer(codec, inputBufIdx, 0, 0, 0, 
                            AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM);
                    } else {
                        int64_t sampleTime = AMediaExtractor_getSampleTime(extractor);
                        AMediaCodec_queueInputBuffer(codec, inputBufIdx, 0, sampleSize, sampleTime, 0);
                        AMediaExtractor_advance(extractor);
                    }
                }
            }
        }

        AMediaCodecBufferInfo info;
        ssize_t outputBufIdx = AMediaCodec_dequeueOutputBuffer(codec, &info, kTimeoutUs);
        if (outputBufIdx >= 0) {
            if (info.flags & AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM) {
                sawOutputEOS = true;
            }

            size_t bufSize;
            uint8_t* buf = AMediaCodec_getOutputBuffer(codec, outputBufIdx, &bufSize);
            if (buf && info.size > 0) {
                // MediaCodec audio decoders typically output 16-bit PCM (signed 16-bit integers)
                const int16_t* pcmData = reinterpret_cast<const int16_t*>(buf + info.offset);
                size_t numSamples = info.size / sizeof(int16_t);
                
                outSamples.reserve(outSamples.size() + numSamples);
                for (size_t i = 0; i < numSamples; ++i) {
                    // Convert to float in range [-1.0, 1.0]
                    outSamples.push_back(pcmData[i] / 32768.0f);
                }
            }

            AMediaCodec_releaseOutputBuffer(codec, outputBufIdx, false);
        } else if (outputBufIdx == AMEDIACODEC_INFO_OUTPUT_FORMAT_CHANGED) {
            AMediaFormat* newFormat = AMediaCodec_getOutputFormat(codec);
            AMediaFormat_getInt32(newFormat, AMEDIAFORMAT_KEY_SAMPLE_RATE, &outSampleRate);
            AMediaFormat_getInt32(newFormat, AMEDIAFORMAT_KEY_CHANNEL_COUNT, &outChannelCount);
            LOGI("Output format changed: Sample Rate: %d, Channels: %d", outSampleRate, outChannelCount);
            AMediaFormat_delete(newFormat);
        }
    }

    AMediaCodec_stop(codec);
    AMediaCodec_delete(codec);
    AMediaFormat_delete(format);
    AMediaExtractor_delete(extractor);

    LOGI("Finished decoding. Total samples: %zu", outSamples.size());
    return !outSamples.empty();
}
