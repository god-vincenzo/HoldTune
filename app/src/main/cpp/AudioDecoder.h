#pragma once
#include <string>
#include <vector>
#include <cstdint>

class AudioDecoder {
public:
    static bool decode(const std::string& filePath, 
                       std::vector<float>& outSamples, 
                       int32_t& outSampleRate, 
                       int32_t& outChannelCount);
};
