#include "AudioEngine.h"
#include "AudioDecoder.h"
#include <android/log.h>

#define LOG_TAG "AudioEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

AudioEngine::AudioEngine() {
    LOGI("AudioEngine initialized");
}

AudioEngine::~AudioEngine() {
    stop();
}

bool AudioEngine::start(const std::string& filePath) {
    stop(); // Ensure any running stream is closed first
    
    LOGI("Loading and decoding hold music from: %s", filePath.c_str());
    if (!AudioDecoder::decode(filePath, mAudioData, mSampleRate, mChannelCount)) {
        LOGE("Failed to decode hold track");
        return false;
    }

    if (mAudioData.empty()) {
        LOGE("Audio data is empty after decoding");
        return false;
    }

    mPlaybackIndex = 0;
    mIsPlaying = true;

    LOGI("Opening Oboe audio stream (Usage: VoiceCommunication, Format: Float)");
    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output)
           ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
           ->setSharingMode(oboe::SharingMode::Shared)
           ->setUsage(oboe::Usage::VoiceCommunication) // Local voice communication routing
           ->setFormat(oboe::AudioFormat::Float)
           ->setChannelCount(mChannelCount)
           ->setSampleRate(mSampleRate)
           ->setCallback(this);

    oboe::Result result = builder.openStream(mStream);
    if (result != oboe::Result::OK) {
        LOGE("Failed to open audio stream. Error: %s", oboe::convertToText(result));
        mIsPlaying = false;
        mAudioData.clear();
        return false;
    }

    result = mStream->requestStart();
    if (result != oboe::Result::OK) {
        LOGE("Failed to start audio stream. Error: %s", oboe::convertToText(result));
        mStream->close();
        mStream.reset();
        mIsPlaying = false;
        mAudioData.clear();
        return false;
    }

    LOGI("Audio stream started successfully");
    return true;
}

void AudioEngine::stop() {
    mIsPlaying = false;
    
    if (mStream) {
        LOGI("Stopping and closing audio stream");
        mStream->requestStop();
        mStream->close();
        mStream.reset();
    }
    
    mAudioData.clear();
    mPlaybackIndex = 0;
    LOGI("AudioEngine stopped");
}

oboe::DataCallbackResult AudioEngine::onAudioReady(
        oboe::AudioStream *audioStream, 
        void *audioData, 
        int32_t numFrames) {
    
    float* floatBuffer = static_cast<float*>(audioData);
    int32_t numChannels = audioStream->getChannelCount();
    int32_t totalSamples = numFrames * numChannels;

    if (!mIsPlaying || mAudioData.empty()) {
        // Write silence
        for (int32_t i = 0; i < totalSamples; ++i) {
            floatBuffer[i] = 0.0f;
        }
        return oboe::DataCallbackResult::Stop;
    }

    size_t dataSize = mAudioData.size();
    size_t currentIndex = mPlaybackIndex.load();

    for (int32_t i = 0; i < totalSamples; ++i) {
        if (currentIndex >= dataSize) {
            currentIndex = 0; // Loop playback
        }
        floatBuffer[i] = mAudioData[currentIndex++];
    }

    mPlaybackIndex.store(currentIndex);
    return oboe::DataCallbackResult::Continue;
}
