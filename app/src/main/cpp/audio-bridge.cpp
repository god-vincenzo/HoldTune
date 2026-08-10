#include <jni.h>
#include <string>
#include "AudioEngine.h"

// Static instance of our AudioEngine
static AudioEngine gAudioEngine;

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_holdtune_audio_AudioBridge_nativeStart(JNIEnv* env, jobject thiz, jstring filePath) {
    if (filePath == nullptr) {
        return JNI_FALSE;
    }
    const char* pathChars = env->GetStringUTFChars(filePath, nullptr);
    std::string path(pathChars);
    env->ReleaseStringUTFChars(filePath, pathChars);
    
    return gAudioEngine.start(path) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_holdtune_audio_AudioBridge_nativeStop(JNIEnv* env, jobject thiz) {
    gAudioEngine.stop();
}

}
