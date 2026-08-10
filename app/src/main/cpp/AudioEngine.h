#pragma once
#include <vector>
#include <memory>
#include <atomic>
#include <string>
#include <oboe/Oboe.h>

class AudioEngine : public oboe::AudioStreamDataCallback {
public:
    AudioEngine();
    virtual ~AudioEngine();

    bool start(const std::string& filePath);
    void stop();

    oboe::DataCallbackResult onAudioReady(
            oboe::AudioStream *audioStream, 
            void *audioData, 
            int32_t numFrames) override;

private:
    std::shared_ptr<oboe::AudioStream> mStream;
    std::vector<float> mAudioData;
    std::atomic<size_t> mPlaybackIndex{0};
    std::atomic<bool> mIsPlaying{false};
    int32_t mSampleRate = 48000;
    int32_t mChannelCount = 2;
};
