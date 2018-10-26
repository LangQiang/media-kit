#include "ffmpeg.h"


#include <jni.h>
#include <android/log.h>
#include <string.h>
#define LOGE(format, ...) __android_log_print(ANDROID_LOG_ERROR,"ffmepg_ndk",format,##__VA_ARGS__)



static jclass jc;
static JNIEnv *genv;
static jobject jobj;
static int duration = -1;

JNIEXPORT jint JNICALL
Java_com_lq_mediakit_jni_MediaHelper_addGifWater(

        JNIEnv *env,
        jobject thiz,
        jstring mp4InputPath,
        jstring gif,
        jstring outPutPath,
        jstring waterW,
        jstring waterH,
        jstring xPercent,
        jstring yPercent
) {
//    char info[10000] = {0};
//    av_register_all();
//
//    sprintf(info, "%s\n", avcodec_configuration());

//    //LOGE("%s", info);
//    LOGE("%s", info);
    jc = (*env)->GetObjectClass(env, thiz);
    genv = env;
    jobj = (*env)->NewGlobalRef(env, thiz);

    const char *jinput = (char *) (*env)->GetStringUTFChars(env, mp4InputPath, 0);

    const char *jgif = (char *) (*env)->GetStringUTFChars(env, gif, 0);

    const char *jout = (char *) (*env)->GetStringUTFChars(env, outPutPath, 0);

    const char *jwaterW = (char *) (*env)->GetStringUTFChars(env, waterW, 0);

    const char *jwaterH = (char *) (*env)->GetStringUTFChars(env, waterH, 0);

    const char *jxPercent = (char *) (*env)->GetStringUTFChars(env, xPercent, 0);
    const char *jyPercent = (char *) (*env)->GetStringUTFChars(env, yPercent, 0);
    //ffmpeg -y -i b.mp4 -ignore_loop 0 -i bw.gif -filter_complex '[1:v]scale=300:300[wm];[0:v][wm]overlay=x=W*0.5:y=H*0.5:shortest=1' test_out8.mp4
    char filter[100];
    strcpy(filter, "[1:v]scale=");
    strcat(filter, jwaterW);
    strcat(filter, ":");
    strcat(filter, jwaterH);
    strcat(filter, "[wm];[0:v][wm]overlay=x=W*");
    strcat(filter, jxPercent);
    strcat(filter, ":y=H*");
    strcat(filter, jyPercent);
    strcat(filter, ":shortest=1");
    char *argv1[] = {"ffmepg",
                     "-y",
                     "-i",
                     jinput,
                     "-ignore_loop",
                     "0",
                     "-i",
                     jgif,
                     "-filter_complex",
                     filter,
                     "-c:v",
                     "libx264",
                     jout};
    int argc1 = 0;
    argc1 = sizeof(argv1) / sizeof(argv1[0]);


    //ffmpeg -y -i b.mp4 -ignore_loop 0 -i bw.gif -filter_complex '[1:v]scale=300:300[wm];[0:v][wm]overlay=x=W*0.5:y=H*0.5:shortest=1' test_out8.mp4
    //String cmd = "ffmpeg -i "+ mp4InputPath +" -ignore_loop 0 -i "+gif+" -filter_complex [1:v]scale="+waterW+":"+waterH+"[water1];[0:v][water1]overlay=x=W*"+xPercent+":y=H*" + yPercent + ":shortest=1 -y "+outPutPath;
    //final String[] cmds = cmd.split("\\s+");
/*
 int argc = (*env)->GetArrayLength(env,commands);
 char *argv[argc];
 int i;
 for (i = 0; i < argc; i++) {
     jstring js = (jstring) (*env)->GetObjectArrayElement(env,commands, i);
     argv[i] = (char *) (*env)->GetStringUTFChars(env,js, 0);
 }
 LOGE("%d %d",argc,argc1);
 */


    LOGE("ff_run");
    int ret = ff_run(argc1, argv1);
    (*env)->ReleaseStringUTFChars(env, mp4InputPath, jinput);
    (*env)->ReleaseStringUTFChars(env, gif, jgif);
    (*env)->ReleaseStringUTFChars(env, outPutPath, jout);
    (*env)->ReleaseStringUTFChars(env, waterW, jwaterW);
    (*env)->ReleaseStringUTFChars(env, waterH, jwaterH);
    (*env)->ReleaseStringUTFChars(env, xPercent, jxPercent);
    (*env)->ReleaseStringUTFChars(env, yPercent, jyPercent);
    jc = NULL;
    jobj = NULL;
    genv = NULL;

    return ret;

}

int getDuration(char *ret) {
    int result = 0;
    if (duration == -2) {
        char str[17] = {0};
        strncpy(str, ret, 16);
        LOGE("%s", str);
        //for (int i = 0; i < strlen(str); i++) {
        //    LOGE("%c",str[i]);
        //}
        int h = (str[0] - '0') * 10 + (str[1] - '0');
        int m = (str[3] - '0') * 10 + (str[4] - '0');
        int s = (str[6] - '0') * 10 + (str[7] - '0');
        int ms = (str[9] - '0') * 100 + (str[10] - '0') * 10;
        result = ms + (s + m * 60 + h * 60 * 60) * 1000;
        return result;
    }

    char timeStr[10] = "Duration:";
    char *q = strstr(ret, timeStr);
    if (q != NULL) {
        return -2;
    }
    if (q != NULL) {

    } else {
        return -1;
    }
}

void callJavaMethod(char *ret) {
    if (duration == -1 || duration == -2) {
        duration = getDuration(ret);
    }
    if (duration <= 0) {
        return;
    }
    int result = 0;

    char timeStr[10] = "kB time=";
    char *q = strstr(ret, timeStr);
    if (q != NULL) {
        int i = 8;
        char str[20] = {0};
        strncpy(str, q, 19);

        int h = (str[i + 0] - '0') * 10 + (str[i + 1] - '0');
        int m = (str[i + 3] - '0') * 10 + (str[i + 4] - '0');
        int s = (str[i + 6] - '0') * 10 + (str[i + 7] - '0');
        int ms = (str[i + 9] - '0') * 100 + (str[i + 10] - '0') * 10;
        result = ms + (s + m * 60 + h * 60 * 60) * 1000;
    } else {
        return;
    }
    jmethodID methodID = (*genv)->GetMethodID(genv, jc, "onProgress", "(II)V");
    //调用该方法
    (*genv)->CallVoidMethod(genv, jobj, methodID, result, duration);
}

JNIEXPORT jint JNICALL
Java_com_lq_mediakit_jni_MediaHelper_videoClips(

        JNIEnv *env,
        jobject thiz,
        jstring mp4InputPath,
        jstring outPutPath,
        jstring startTime,
        jstring duration
) {
//    char info[10000] = {0};
//    av_register_all();
//
//    sprintf(info, "%s\n", avcodec_configuration());
//
//    //LOGE("%s", info);
//    LOGE("%s", info);
    jc = (*env)->GetObjectClass(env, thiz);
    genv = env;
    jobj = (*env)->NewGlobalRef(env, thiz);

    const char *jinput = (char *) (*env)->GetStringUTFChars(env, mp4InputPath, 0);

    const char *jout = (char *) (*env)->GetStringUTFChars(env, outPutPath, 0);

    const char *jstartTime = (char *) (*env)->GetStringUTFChars(env, startTime, 0);

    const char *jduration = (char *) (*env)->GetStringUTFChars(env, duration, 0);

    char *argv1[] = {"ffmepg",
                     "-y",
                     "-i",
                     jinput,
                     "-ss",
                     jstartTime,
                     "-t",
                     jduration,
                     jout};
    int argc1 = 0;
    argc1 = sizeof(argv1) / sizeof(argv1[0]);



    LOGE("ff_run");
    int ret = ff_run(argc1, argv1);
    (*env)->ReleaseStringUTFChars(env, mp4InputPath, jinput);
    (*env)->ReleaseStringUTFChars(env, outPutPath, jout);
    (*env)->ReleaseStringUTFChars(env, duration, jduration);
    (*env)->ReleaseStringUTFChars(env, startTime, jstartTime);
    jc = NULL;
    jobj = NULL;
    genv = NULL;

    return ret;
}


JNIEXPORT jstring JNICALL Java_com_lq_mediakit_jni_MediaHelper_getFFmpegVersion(
        JNIEnv *env,
        jobject obj) {
    return (*env)->NewStringUTF(env, "v3.1.2");
}








