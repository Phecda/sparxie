#include <jni.h>
#include <pthread.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>

#include "iperf.h"
#include "iperf_api.h"

#define RESULT_FINISHED 0
#define RESULT_STOPPED (-1)
#define RESULT_FAILED (-2)

#define DEFAULT_DURATION_SECONDS 10
#define DEFAULT_CONNECT_TIMEOUT_MS 3000
#define DEFAULT_INTERVAL_SECONDS 1

static JavaVM *g_vm = NULL;
static pthread_mutex_t g_session_mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_mutex_t g_callback_mutex = PTHREAD_MUTEX_INITIALIZER;
static struct iperf_test *g_prepared_test = NULL;
static struct iperf_test *g_active_test = NULL;
static int g_stop_requested = 0;
static int g_last_error_code = IENONE;
static char g_last_error_message[256] = "";
static jobject g_json_listener = NULL;
static jmethodID g_json_method = NULL;

static void set_last_error_locked(int code, const char *fallback) {
    const char *message = iperf_strerror(code);

    g_last_error_code = code;
    if (message == NULL || message[0] == '\0') {
        snprintf(g_last_error_message, sizeof(g_last_error_message), "%s",
                 fallback != NULL ? fallback : "iperf error");
    } else {
        snprintf(g_last_error_message, sizeof(g_last_error_message), "%s", message);
    }
}

static jint fail_with_error(int code, const char *fallback) {
    pthread_mutex_lock(&g_session_mutex);
    set_last_error_locked(code, fallback);
    pthread_mutex_unlock(&g_session_mutex);
    return RESULT_FAILED;
}

static void discard_prepared_test(struct iperf_test *test) {
    if (test == NULL) {
        return;
    }

    int should_free = 0;
    pthread_mutex_lock(&g_session_mutex);
    if (g_prepared_test == test) {
        g_prepared_test = NULL;
        should_free = 1;
    }
    pthread_mutex_unlock(&g_session_mutex);

    if (should_free) {
        iperf_free_test(test);
    }
}

static void clear_json_callback(void) {
    pthread_mutex_lock(&g_callback_mutex);
    g_json_listener = NULL;
    g_json_method = NULL;
    pthread_mutex_unlock(&g_callback_mutex);
}

static void install_json_callback(jobject listener, jmethodID method) {
    pthread_mutex_lock(&g_callback_mutex);
    g_json_listener = listener;
    g_json_method = method;
    pthread_mutex_unlock(&g_callback_mutex);
}

static void json_callback(struct iperf_test *test, char *payload) {
    (void)test;

    if (payload == NULL || g_vm == NULL) {
        return;
    }

    pthread_mutex_lock(&g_callback_mutex);
    if (g_json_listener != NULL && g_json_method != NULL) {
        JNIEnv *env = NULL;
        int attached = 0;
        jint env_status = (*g_vm)->GetEnv(g_vm, (void **)&env, JNI_VERSION_1_6);

        if (env_status == JNI_EDETACHED) {
            if ((*g_vm)->AttachCurrentThread(g_vm, (JNIEnv **)&env, NULL) != JNI_OK) {
                pthread_mutex_unlock(&g_callback_mutex);
                return;
            }
            attached = 1;
        } else if (env_status != JNI_OK) {
            pthread_mutex_unlock(&g_callback_mutex);
            return;
        }

        jstring json = (*env)->NewStringUTF(env, payload);
        if (json != NULL) {
            (*env)->CallVoidMethod(env, g_json_listener, g_json_method, json);
            (*env)->DeleteLocalRef(env, json);
        }

        if ((*env)->ExceptionCheck(env)) {
            (*env)->ExceptionClear(env);
        }

        if (attached) {
            (*g_vm)->DetachCurrentThread(g_vm);
        }
    }
    pthread_mutex_unlock(&g_callback_mutex);
}

static jlong prepare_test(
    JNIEnv *env,
    jint port,
    jchar role,
    const char *server_address,
    jint parallel_streams
) {
    (void)env;

    if (port < 1 || port > 65535) {
        pthread_mutex_lock(&g_session_mutex);
        set_last_error_locked(IENEWTEST, "Server Port must be between 1 and 65535.");
        pthread_mutex_unlock(&g_session_mutex);
        return 0;
    }

    if (role == 'c' && (server_address == NULL || server_address[0] == '\0')) {
        pthread_mutex_lock(&g_session_mutex);
        set_last_error_locked(IENEWTEST, "Server Address must not be empty.");
        pthread_mutex_unlock(&g_session_mutex);
        return 0;
    }

    if (role == 'c' && (parallel_streams < 1 || parallel_streams > MAX_STREAMS)) {
        pthread_mutex_lock(&g_session_mutex);
        set_last_error_locked(IENEWTEST, "Parallel Streams must be between 1 and 128.");
        pthread_mutex_unlock(&g_session_mutex);
        return 0;
    }

    pthread_mutex_lock(&g_session_mutex);
    if (g_prepared_test != NULL || g_active_test != NULL) {
        set_last_error_locked(IENEWTEST, "An iperf session is already active.");
        pthread_mutex_unlock(&g_session_mutex);
        return 0;
    }
    pthread_mutex_unlock(&g_session_mutex);

    struct iperf_test *test = iperf_new_test();
    if (test == NULL) {
        pthread_mutex_lock(&g_session_mutex);
        set_last_error_locked(i_errno, "Unable to create an iperf test.");
        pthread_mutex_unlock(&g_session_mutex);
        return 0;
    }

    if (iperf_defaults(test) < 0) {
        int code = i_errno;
        iperf_free_test(test);
        pthread_mutex_lock(&g_session_mutex);
        set_last_error_locked(code, "Unable to apply iperf defaults.");
        pthread_mutex_unlock(&g_session_mutex);
        return 0;
    }

    if (set_protocol(test, Ptcp) < 0) {
        int code = i_errno;
        iperf_free_test(test);
        pthread_mutex_lock(&g_session_mutex);
        set_last_error_locked(code, "Unable to select TCP.");
        pthread_mutex_unlock(&g_session_mutex);
        return 0;
    }

    iperf_set_test_role(test, role);
    iperf_set_test_server_port(test, port);
    iperf_set_test_reporter_interval(test, DEFAULT_INTERVAL_SECONDS);
    iperf_set_test_stats_interval(test, DEFAULT_INTERVAL_SECONDS);
    iperf_set_test_json_output(test, 1);
    iperf_set_test_json_stream(test, 1);
    iperf_set_test_json_callback(test, json_callback);

    if (role == 'c') {
        iperf_set_test_server_hostname(test, server_address);
        iperf_set_test_duration(test, DEFAULT_DURATION_SECONDS);
        iperf_set_test_num_streams(test, parallel_streams);
        iperf_set_test_connect_timeout(test, DEFAULT_CONNECT_TIMEOUT_MS);
        iperf_set_test_reverse(test, 0);
    } else {
        iperf_set_test_one_off(test, 0);
    }

    pthread_mutex_lock(&g_session_mutex);
    if (g_prepared_test != NULL || g_active_test != NULL) {
        set_last_error_locked(IENEWTEST, "An iperf session is already active.");
        pthread_mutex_unlock(&g_session_mutex);
        iperf_free_test(test);
        return 0;
    }

    g_prepared_test = test;
    pthread_mutex_unlock(&g_session_mutex);

    return (jlong)(intptr_t)test;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    (void)reserved;
    g_vm = vm;
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL
Java_me_phecda_iperf3_Iperf3Native_reset(JNIEnv *env, jobject thiz) {
    (void)env;
    (void)thiz;

    pthread_mutex_lock(&g_session_mutex);
    if (g_prepared_test == NULL && g_active_test == NULL) {
        g_stop_requested = 0;
        g_last_error_code = IENONE;
        g_last_error_message[0] = '\0';
    }
    pthread_mutex_unlock(&g_session_mutex);
}

JNIEXPORT jlong JNICALL
Java_me_phecda_iperf3_Iperf3Native_prepareClient(
    JNIEnv *env,
    jobject thiz,
    jstring server_address,
    jint server_port,
    jint parallel_streams
) {
    (void)thiz;

    if (server_address == NULL) {
        pthread_mutex_lock(&g_session_mutex);
        set_last_error_locked(IENEWTEST, "Server Address must not be empty.");
        pthread_mutex_unlock(&g_session_mutex);
        return 0;
    }

    const char *address = (*env)->GetStringUTFChars(env, server_address, NULL);
    if (address == NULL) {
        return 0;
    }

    jlong handle = prepare_test(env, server_port, 'c', address, parallel_streams);
    (*env)->ReleaseStringUTFChars(env, server_address, address);
    return handle;
}

JNIEXPORT jlong JNICALL
Java_me_phecda_iperf3_Iperf3Native_prepareServer(
    JNIEnv *env,
    jobject thiz,
    jint server_port
) {
    (void)thiz;
    return prepare_test(env, server_port, 's', NULL, 1);
}

JNIEXPORT jint JNICALL
Java_me_phecda_iperf3_Iperf3Native_run(
    JNIEnv *env,
    jobject thiz,
    jlong handle,
    jobject listener
) {
    (void)thiz;

    struct iperf_test *test = (struct iperf_test *)(intptr_t)handle;
    if (test == NULL) {
        return fail_with_error(IENEWTEST, "Invalid iperf session.");
    }

    if (listener == NULL) {
        discard_prepared_test(test);
        return fail_with_error(IENEWTEST, "Invalid iperf session.");
    }

    jclass listener_class = (*env)->GetObjectClass(env, listener);
    if (listener_class == NULL) {
        if ((*env)->ExceptionCheck(env)) {
            (*env)->ExceptionClear(env);
        }
        discard_prepared_test(test);
        return fail_with_error(IENEWTEST, "Unable to access the JSON event listener.");
    }

    jmethodID listener_method = (*env)->GetMethodID(
        env,
        listener_class,
        "onJson",
        "(Ljava/lang/String;)V"
    );
    (*env)->DeleteLocalRef(env, listener_class);

    if (listener_method == NULL) {
        if ((*env)->ExceptionCheck(env)) {
            (*env)->ExceptionClear(env);
        }
        discard_prepared_test(test);
        return fail_with_error(IENEWTEST, "Unable to access the JSON event listener.");
    }

    jobject listener_ref = (*env)->NewGlobalRef(env, listener);
    if (listener_ref == NULL) {
        if ((*env)->ExceptionCheck(env)) {
            (*env)->ExceptionClear(env);
        }
        discard_prepared_test(test);
        return fail_with_error(IENEWTEST, "Unable to retain the JSON event listener.");
    }

    pthread_mutex_lock(&g_session_mutex);
    if (g_prepared_test != test) {
        pthread_mutex_unlock(&g_session_mutex);
        (*env)->DeleteGlobalRef(env, listener_ref);
        return fail_with_error(IENEWTEST, "Invalid iperf session.");
    }

    if (g_stop_requested) {
        g_prepared_test = NULL;
        set_last_error_locked(IENONE, "Stopped before the session started.");
        pthread_mutex_unlock(&g_session_mutex);
        (*env)->DeleteGlobalRef(env, listener_ref);
        iperf_free_test(test);
        return RESULT_STOPPED;
    }

    g_prepared_test = NULL;
    g_active_test = test;
    pthread_mutex_unlock(&g_session_mutex);

    install_json_callback(listener_ref, listener_method);

    i_errno = IENONE;
    int result = test->role == 'c' ? iperf_run_client(test) : iperf_run_server(test);
    int error_code = i_errno;

    pthread_mutex_lock(&g_session_mutex);
    g_active_test = NULL;
    int stopped = g_stop_requested;
    if (stopped) {
        set_last_error_locked(IENONE, "Stopped.");
    } else if (result < 0) {
        set_last_error_locked(error_code, "iperf session failed.");
    } else {
        set_last_error_locked(IENONE, "Finished.");
    }
    pthread_mutex_unlock(&g_session_mutex);

    clear_json_callback();
    (*env)->DeleteGlobalRef(env, listener_ref);
    iperf_free_test(test);

    if (stopped) {
        return RESULT_STOPPED;
    }
    if (result < 0) {
        return RESULT_FAILED;
    }
    return RESULT_FINISHED;
}

JNIEXPORT jboolean JNICALL
Java_me_phecda_iperf3_Iperf3Native_interrupt(JNIEnv *env, jobject thiz) {
    (void)env;
    (void)thiz;

    pthread_mutex_lock(&g_session_mutex);
    g_stop_requested = 1;
    int active = g_active_test != NULL;
    if (active) {
        iperf_interrupt(g_active_test);
    }
    pthread_mutex_unlock(&g_session_mutex);

    return active ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_me_phecda_iperf3_Iperf3Native_lastErrorCode(JNIEnv *env, jobject thiz) {
    (void)env;
    (void)thiz;

    pthread_mutex_lock(&g_session_mutex);
    jint code = g_last_error_code;
    pthread_mutex_unlock(&g_session_mutex);
    return code;
}

JNIEXPORT jstring JNICALL
Java_me_phecda_iperf3_Iperf3Native_lastErrorMessage(
    JNIEnv *env,
    jobject thiz,
    jint code
) {
    (void)thiz;
    (void)code;

    pthread_mutex_lock(&g_session_mutex);
    char message[sizeof(g_last_error_message)];
    snprintf(message, sizeof(message), "%s", g_last_error_message);
    pthread_mutex_unlock(&g_session_mutex);

    return (*env)->NewStringUTF(env, message);
}