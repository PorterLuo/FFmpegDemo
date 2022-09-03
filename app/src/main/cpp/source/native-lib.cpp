#include <jni.h>
//#include <string>
#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/error.h>
#include <libavutil/log.h>
#include <libavformat/avio.h>
#include <libswscale/swscale.h>
#include <libavcodec/jni.h>
#include <libswresample/swresample.h>
#include <string.h>
}
#include<iostream>
using namespace std;
#define TAG  "lhb"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)

static double r2d(AVRational r) {
    return r.num == 0 || r.den == 0 ? 0 : (double) r.num / (double) r.den;
}

//当前时间戳
long long GetNowMs(){
    struct timeval tv;
    gettimeofday(&tv, NULL);
    int sec = tv.tv_sec % 360000;
    long long t = sec  * 1000 + tv.tv_usec/1000;
    return t;

}

extern "C"
JNIEXPORT
jint JNI_OnLoad(JavaVM *vm,void *res)
{
    av_jni_set_java_vm(vm,0);
    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_luohb_ffmpegdemo_MainActivity_ffmpegInfo(JNIEnv *env, jobject  /* this */) {

    return env->NewStringUTF(avcodec_configuration());
}


extern "C"
JNIEXPORT void JNICALL
Java_com_luohb_ffmpegdemo_XPlay_Open(JNIEnv *env, jobject thiz, jstring url, jobject surface) {
    const char *path = env->GetStringUTFChars(url, 0);
    //初始化解封装
    av_register_all();
    //初始化网络
    avformat_network_init();
    //初始化解码器
    avcodec_register_all();
    AVFormatContext *ic = NULL;

    int re = avformat_open_input(&ic, path, NULL, NULL);
    if (re != 0) {
        LOGD("avformat_open_input failed %s", av_err2str(re));
        return;
    }
    LOGD("avformat_open_input %s success!", path);
    //获取流信息
    re = avformat_find_stream_info(ic, 0);
    if (re != 0) {
        LOGD("avformat_find_stream_info failed!");
        return;
    }
    LOGD("duration = %lld, nb_streams = %d", ic->duration, ic->nb_streams);

    int fps = 0;
    int videoStream = 0;
    int audioStream = 1;

    for (int i = 0; i < ic-> nb_streams; ++i) {
        AVStream *as = ic->streams[i];
        if (as->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            LOGD("视频数据");
            videoStream = i;
            fps = r2d(as->avg_frame_rate);
            LOGD("fps = %d, width = %d , height = %d, codec_id = %d, format = %d", fps,
                    as->codecpar->width, as->codecpar->height,
                 as->codecpar->codec_id, as->codecpar->format);
        } else if (as->codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
            LOGD("音频数据");
            audioStream = i;
            LOGD("sample_rate = %d, channels = %d, format = %d", as->codecpar->sample_rate,
                 as->codecpar->channels, as->codecpar->format);
        }
    }
    //获取音频流信息
    audioStream = av_find_best_stream(ic, AVMEDIA_TYPE_AUDIO, -1, -1, NULL, 0);
    LOGD("av_find_best_stream audioStream = %d", audioStream);

    //打开视频解码器
    //软解码器
    AVCodec  *codec = avcodec_find_decoder(ic->streams[videoStream]->codecpar->codec_id);
    //硬解码器
//    codec = avcodec_find_decoder_by_name("h264_mediacodec");
    if (!codec) {
        LOGD("avcodec find video failed!");
        return;
    }
    //解码器初始化
    AVCodecContext  *vc = avcodec_alloc_context3(codec);
    avcodec_parameters_to_context(vc, ic->streams[videoStream]->codecpar);
    vc->thread_count = 8;
    //打开解码器
    re = avcodec_open2(vc, 0, 0);
    if (re != 0) {
        LOGD("avcodec_open2 video failed: %s", av_err2str(re));
        return;
    }

    //打开音频解码器
    //软解码器
    AVCodec  *acodec = avcodec_find_decoder(ic->streams[audioStream]->codecpar->codec_id);

    if (!acodec) {
        LOGD("avcodec find audio failed!");
        return;
    }
    //解码器初始化
    AVCodecContext  *ac = avcodec_alloc_context3(acodec);
    avcodec_parameters_to_context(ac, ic->streams[audioStream]->codecpar);
    ac->thread_count = 8;
    //打开解码器
    re = avcodec_open2(ac, 0, 0);
    if (re != 0) {
        LOGD("avcodec_open2 audio failed");
        return;
    }



    //读取帧数据
    AVPacket  *pkt = av_packet_alloc();
    AVFrame *frame = av_frame_alloc();
    long long start = GetNowMs();
    int frameCount= 0;

    //初始化像素格式转换的上下文
    SwsContext *vctx = NULL;
    int outWidth = 1280;
    int outHeight = 720;
    char *rgb = new char[1920*1080*4];
    char *pcm = new char[48000*4*2];
    //音频重采样上下文初始化
    SwrContext *actx = swr_alloc();
    actx = swr_alloc_set_opts(actx,
            av_get_default_channel_layout(2),
            AV_SAMPLE_FMT_S16, ac->sample_rate,
            av_get_default_channel_layout(ac->channels),
            ac->sample_fmt,
            ac->sample_rate,
            0,0);
    re = swr_init(actx);
    if (re != 0) {
        LOGD("swr_init failed!");
    } else {
        LOGD("swr_init sucess!");
    }

    //显示窗口初始化
    ANativeWindow *nwin = ANativeWindow_fromSurface(env, surface);
    ANativeWindow_setBuffersGeometry(nwin,outWidth , outHeight, WINDOW_FORMAT_RGBA_8888);
    ANativeWindow_Buffer wbuf;

    for (;;) {
        if (GetNowMs() - start >= 3000) {
            LOGD("now decode fps is %d", frameCount / 3);
            start = GetNowMs();
            frameCount = 0;
        }
        int re = av_read_frame(ic, pkt);
        if (re != 0) {
            LOGD("读取到结尾处");
            int pos = 20 * r2d(ic->streams[videoStream]->time_base);
            av_seek_frame(ic, videoStream, pos, AVSEEK_FLAG_BACKWARD | AVSEEK_FLAG_FRAME);
            break;
        }

        AVCodecContext *cc = vc;
        if (pkt->stream_index == audioStream) {
            cc = ac;
        }

        //发送到线程中解码
        re = avcodec_send_packet(cc, pkt);
        //清理
        av_packet_unref(pkt);
        if (re != 0) {
            LOGD("avcodec_send_packet failed");
            continue;
        }
        for (;;) {
            re = avcodec_receive_frame(cc, frame);
            if (re != 0) {
//                LOGD("avcodec_receive_frameme failed");
                break;
            }
//            LOGD("avcodec_receive_frame %lld", frame->pts);
            if (cc == vc) {
                frameCount++;
                vctx = sws_getCachedContext(vctx,
                        frame->width,
                        frame->height,
                        (AVPixelFormat)frame->format,
                        outWidth,
                        outHeight,
                        AV_PIX_FMT_RGBA,
                        SWS_FAST_BILINEAR,
                        0, 0, 0);
                if (!vctx) {
                    LOGD("sws_getCachedContext failed!");
                } else {
                    uint8_t *data[AV_NUM_DATA_POINTERS] = {0};
                    data[0] =(uint8_t *)rgb;
                    int lines[AV_NUM_DATA_POINTERS] = {0};
                    lines[0] = outWidth * 4;
                    int h = sws_scale(vctx, frame->data, frame->linesize, 0, frame->height, data, lines);
                    LOGD("sws_scale = %d", h);
                    if (h > 0) {
                        ANativeWindow_lock(nwin, &wbuf, 0);
                        auto *dst = wbuf.bits;
                        memcpy(dst, rgb, 1080);
                        ANativeWindow_unlockAndPost(nwin);
                    }
                }
            } else {
                //音频
                //音频重采样
                uint8_t  *out[2] = {0};
                out[0] = (uint8_t*)pcm;
                int len = swr_convert(actx, out, frame->nb_samples,
                                      (const uint8_t**)frame->data, frame->nb_samples );
                LOGD("swr_convert = %d", len);
            }

        }
    }
    delete rgb;
    delete pcm;

    //关闭上下文
    avformat_close_input(&ic);
    env->ReleaseStringUTFChars(url, path);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_luohb_ffmpegdemo_MainActivity_swrInit(JNIEnv *env, jobject thiz) {
    //初始化解封装
    av_register_all();
    //初始化网络
    avformat_network_init();
    //初始化解码器
    avcodec_register_all();

    int ret = 0;
    char *path = "/sdcard/test.aac";

    uint8_t **src_data = NULL;
    int src_linesize = 0;

    uint8_t **dts_data = NULL;
    int dts_linesize = 0;

    AVFormatContext *fmt_ctx = NULL;

    AVPacket *pkt = av_packet_alloc();


    ret = avformat_open_input(&fmt_ctx, path, NULL, NULL);
    if (ret != 0) {
        LOGD("avformat_open_input failed %s", av_err2str(ret));
        return;
    }
    LOGD("avformat_open_input %s success!", path);

    char *out = "/sdcard/1080TVsw.pcm";
    FILE *outfile = fopen(out, "wb+");

    SwrContext *swr_ctx = NULL;
    swr_ctx = swr_alloc_set_opts(NULL,
            AV_CH_LAYOUT_STEREO,
            AV_SAMPLE_FMT_S16,
            48000,
            AV_CH_LAYOUT_STEREO,
            AV_SAMPLE_FMT_S16,
            44110,
            0, NULL);
    if(!swr_ctx) {
        LOGD("swr_alloc_set_opts failed");
        return;
    }
    swr_init(swr_ctx);



    //创建输入缓冲区
    av_samples_alloc_array_and_samples(&src_data, //输入缓冲区地址
            &src_linesize,  //缓冲区的大小
            2,  //通道个数
            1024,    //单通道采样个数
            AV_SAMPLE_FMT_S16,  //采样格式
            0);

    //创建输出缓冲区
    av_samples_alloc_array_and_samples(&dts_data, //输入缓冲区地址
                                       &dts_linesize,  //缓冲区的大小
                                       2,  //通道个数
                                       1024,    //单通道采样个数
                                       AV_SAMPLE_FMT_S16,  //采样格式
                                       0);
    ret = av_read_frame(fmt_ctx, pkt);
//    while ((ret = av_read_frame(fmt_ctx, pkt)) == 0) {
//        memcpy(src_data[0], pkt.data, pkt.size);

//        swr_convert(swr_ctx,
//                dts_data,
//                dts_linesize,
//                (const uint8_t **)(src_data),
//                src_linesize);
//        fwrite(dts_data[0], 1, dts_linesize, outfile);
//        fflush(outfile);
//        av_packet_unref(&pkt);
//    }
    fclose(outfile);
    if(src_data) {
        av_freep(&src_data[0]);
    }
    av_freep(&src_data);

    if(dts_data) {
        av_freep(&dts_data[0]);
    }
    av_freep(&dts_data);
    avformat_close_input(&fmt_ctx);

}