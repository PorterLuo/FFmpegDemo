//
// Created by 罗海波 on 9/3/22.
//

#include <jni.h>
extern "C" {
#include "libavcodec/avcodec.h"
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_luohb_ffmpeg_ui_activity_FFmpegInfoActivity_getFFmpegVersion(JNIEnv *env, jobject thiz) {

    return env->NewStringUTF(av_version_info());
}