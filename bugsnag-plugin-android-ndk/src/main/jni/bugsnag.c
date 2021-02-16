/** \brief The public API
 */
#include "bugsnag_ndk.h"
#include "report.h"
#include "utils/stack_unwinder.h"
#include "utils/string.h"
#include "metadata.h"
#include "safejni.h"
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static JNIEnv *bsg_global_jni_env = NULL;

void bugsnag_set_binary_arch(JNIEnv *env);

void bugsnag_init(JNIEnv *env) { bsg_global_jni_env = env; }

void bugsnag_notify_env(JNIEnv *env, char *name, char *message,
                        bsg_severity_t severity);
void bugsnag_set_user_env(JNIEnv *env, char *id, char *email, char *name);
void bugsnag_leave_breadcrumb_env(JNIEnv *env, char *name,
                                  bsg_breadcrumb_t type);

void bugsnag_notify(char *name, char *message, bsg_severity_t severity) {
  if (bsg_global_jni_env != NULL) {
    bugsnag_notify_env(bsg_global_jni_env, name, message, severity);
  } else {
    BUGSNAG_LOG("Cannot bugsnag_notify before initializing with bugsnag_init");
  }
}

void bugsnag_set_user(char *id, char *email, char *name) {
  if (bsg_global_jni_env != NULL) {
    bugsnag_set_user_env(bsg_global_jni_env, id, email, name);
  } else {
    BUGSNAG_LOG(
        "Cannot bugsnag_set_user before initializing with bugsnag_init");
  }
}

void bugsnag_leave_breadcrumb(char *name, bsg_breadcrumb_t type) {
  if (bsg_global_jni_env != NULL) {
    bugsnag_leave_breadcrumb_env(bsg_global_jni_env, name, type);
  } else {
    BUGSNAG_LOG("Cannot bugsnag_leave_breadcrumb_env before initializing with "
                "bugsnag_init");
  }
}

jfieldID bsg_parse_jseverity(JNIEnv *env, bsg_severity_t severity,
                             jclass severity_class) {
  const char *severity_sig = "Lcom/bugsnag/android/Severity;";
  if (severity == BSG_SEVERITY_ERR) {
    return bsg_safe_get_static_field_id(env, severity_class, "ERROR", severity_sig);
  } else if (severity == BSG_SEVERITY_WARN) {
    return bsg_safe_get_static_field_id(env, severity_class, "WARNING",
                                        severity_sig);
  } else {
    return bsg_safe_get_static_field_id(env, severity_class, "INFO", severity_sig);
  }
}

void bsg_release_byte_ary(JNIEnv *env, jbyteArray array, char *original_text) {
  if (array != NULL) {
    (*env)->ReleaseByteArrayElements(env, array, (jbyte *)original_text, JNI_COMMIT);
  }
}

void bsg_populate_notify_stacktrace(JNIEnv *env, bsg_stackframe *stacktrace,
                                   ssize_t frame_count, jclass trace_class,
                                   jmethodID trace_constructor,
                                   jobjectArray trace) {
  for (int i = 0; i < frame_count; i++) {
    bsg_stackframe frame = stacktrace[i];

    // create Java string objects for class/filename/method
    jstring class = bsg_safe_new_string_utf(env, "");
    if (class == NULL) {
      goto exit;
    }

    jstring filename = bsg_safe_new_string_utf(env, frame.filename);
    jstring method;
    if (strlen(frame.method) == 0) {
      char *frame_address = malloc(sizeof(char) * 32);
      sprintf(frame_address, "0x%lx", (unsigned long)frame.frame_address);
      method = bsg_safe_new_string_utf(env, frame_address);
      free(frame_address);
    } else {
      method = bsg_safe_new_string_utf(env, frame.method);
    }

    // create StackTraceElement object
    jobject jframe =
        bsg_safe_new_object(env, trace_class, trace_constructor, class, method,
                            filename, frame.line_number);
    if (jframe == NULL) {
      goto exit;
    }

    bsg_safe_set_object_array_element(env, trace, i, jframe);
    goto exit;

    exit:
    (*env)->DeleteLocalRef(env, filename);
    (*env)->DeleteLocalRef(env, class);
  }
}

void bugsnag_notify_env(JNIEnv *env, char *name, char *message,
                        bsg_severity_t severity) {
  jclass interface_class = NULL;
  jmethodID notify_method = NULL;
  jclass trace_class = NULL;
  jclass severity_class = NULL;
  jmethodID trace_constructor = NULL;
  jobjectArray trace = NULL;
  jfieldID severity_field = NULL;
  jobject jseverity = NULL;
  jbyteArray jname = NULL;
  jbyteArray jmessage = NULL;

  bsg_stackframe stacktrace[BUGSNAG_FRAMES_MAX];
  ssize_t frame_count =
      bsg_unwind_stack(bsg_configured_unwind_style(), stacktrace, NULL, NULL);

  // lookup com/bugsnag/android/NativeInterface
  interface_class =
      bsg_safe_find_class(env, "com/bugsnag/android/NativeInterface");
  if (interface_class == NULL) {
    goto exit;
  }

  // lookup NativeInterface.notify()
  notify_method = bsg_safe_get_static_method_id(
      env, interface_class, "notify",
      "([B[BLcom/bugsnag/android/Severity;[Ljava/lang/StackTraceElement;)V");
  if (notify_method == NULL) {
    goto exit;
  }

  // lookup java/lang/StackTraceElement
  trace_class = bsg_safe_find_class(env, "java/lang/StackTraceElement");
  if (trace_class == NULL) {
    goto exit;
  }

  // lookup com/bugsnag/android/Severity
  severity_class = bsg_safe_find_class(env, "com/bugsnag/android/Severity");
  if (severity_class == NULL) {
    goto exit;
  }

  // lookup StackTraceElement constructor
  trace_constructor = bsg_safe_get_method_id(
      env, trace_class, "<init>",
      "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V");
  if (trace_constructor == NULL) {
    goto exit;
  }

  // create StackTraceElement array
  trace = bsg_safe_new_object_array(env, frame_count, trace_class);
  if (trace == NULL) {
    goto exit;
  }

  // populate stacktrace object
  bsg_populate_notify_stacktrace(env, stacktrace, frame_count, trace_class,
                                 trace_constructor, trace);

  // get the severity field
  severity_field = bsg_parse_jseverity(env, severity, severity_class);
  if (severity_field == NULL) {
    goto exit;
  }
  // get the error severity object
  jseverity =
      bsg_safe_get_static_object_field(env, severity_class, severity_field);
  if (jseverity == NULL) {
    goto exit;
  }

  jname = bsg_byte_ary_from_string(env, name);
  jmessage = bsg_byte_ary_from_string(env, message);

  // set application's binary arch
  bugsnag_set_binary_arch(env);

  bsg_safe_call_static_void_method(env, interface_class, notify_method, jname,
                                   jmessage, jseverity, trace);

  goto exit;

  exit:
  if (jname != NULL) {
    bsg_release_byte_ary(env, jname, name);
  }
  if (jmessage != NULL) {
    bsg_release_byte_ary(env, jmessage, message);
  }
  (*env)->DeleteLocalRef(env, jname);
  (*env)->DeleteLocalRef(env, jmessage);

  (*env)->DeleteLocalRef(env, interface_class);
  (*env)->DeleteLocalRef(env, trace_class);
  (*env)->DeleteLocalRef(env, severity_class);
  (*env)->DeleteLocalRef(env, trace);
  (*env)->DeleteLocalRef(env, jseverity);
}

void bugsnag_set_binary_arch(JNIEnv *env) {
  jclass interface_class = NULL;
  jmethodID set_arch_method = NULL;
  jstring arch = NULL;

  // lookup com/bugsnag/android/NativeInterface
  interface_class =
      bsg_safe_find_class(env, "com/bugsnag/android/NativeInterface");
  if (interface_class == NULL) {
    goto exit;
  }

  // lookup NativeInterface.setBinaryArch()
  set_arch_method = bsg_safe_get_static_method_id(
      env, interface_class, "setBinaryArch", "(Ljava/lang/String;)V");
  if (set_arch_method == NULL) {
    goto exit;
  }

  // call NativeInterface.setBinaryArch()
  arch = bsg_safe_new_string_utf(env, bsg_binary_arch());
  if (arch != NULL) {
    bsg_safe_call_static_void_method(env, interface_class, set_arch_method,
                                     arch);
  }

  goto exit;

  exit:
  (*env)->DeleteLocalRef(env, arch);
  (*env)->DeleteLocalRef(env, interface_class);
}

void bugsnag_set_user_env(JNIEnv *env, char *id, char *email, char *name) {
  // lookup com/bugsnag/android/NativeInterface
  jclass interface_class = NULL;
  jmethodID set_user_method = NULL;

  interface_class =
      bsg_safe_find_class(env, "com/bugsnag/android/NativeInterface");
  if (interface_class == NULL) {
    goto exit;
  }

  // lookup NativeInterface.setUser()
  set_user_method = bsg_safe_get_static_method_id(env, interface_class,
                                                  "setUser", "([B[B[B)V");
  if (set_user_method == NULL) {
    goto exit;
  }

  jbyteArray jid = bsg_byte_ary_from_string(env, id);
  jbyteArray jemail = bsg_byte_ary_from_string(env, email);
  jbyteArray jname = bsg_byte_ary_from_string(env, name);

  bsg_safe_call_static_void_method(env, interface_class, set_user_method, jid,
                                   jemail, jname);

  bsg_release_byte_ary(env, jid, id);
  bsg_release_byte_ary(env, jemail, email);
  bsg_release_byte_ary(env, jname, name);

  (*env)->DeleteLocalRef(env, jid);
  (*env)->DeleteLocalRef(env, jemail);
  (*env)->DeleteLocalRef(env, jname);

  goto exit;

  exit:
  (*env)->DeleteLocalRef(env, interface_class);
}

jfieldID bsg_parse_jcrumb_type(JNIEnv *env, bsg_breadcrumb_t type,
                               jclass type_class) {
  const char *type_sig = "Lcom/bugsnag/android/BreadcrumbType;";
  if (type == BSG_CRUMB_USER) {
    return bsg_safe_get_static_field_id(env, type_class, "USER", type_sig);
  } else if (type == BSG_CRUMB_ERROR) {
    return bsg_safe_get_static_field_id(env, type_class, "ERROR", type_sig);
  } else if (type == BSG_CRUMB_LOG) {
    return bsg_safe_get_static_field_id(env, type_class, "LOG", type_sig);
  } else if (type == BSG_CRUMB_NAVIGATION) {
    return bsg_safe_get_static_field_id(env, type_class, "NAVIGATION",
                                        type_sig);
  } else if (type == BSG_CRUMB_PROCESS) {
    return bsg_safe_get_static_field_id(env, type_class, "PROCESS", type_sig);
  } else if (type == BSG_CRUMB_REQUEST) {
    return bsg_safe_get_static_field_id(env, type_class, "REQUEST", type_sig);
  } else if (type == BSG_CRUMB_STATE) {
    return bsg_safe_get_static_field_id(env, type_class, "STATE", type_sig);
  } else { // MANUAL is the default type
    return bsg_safe_get_static_field_id(env, type_class, "MANUAL", type_sig);
  }
}

void bugsnag_leave_breadcrumb_env(JNIEnv *env, char *message,
                                  bsg_breadcrumb_t type) {
  jclass interface_class = NULL;
  jmethodID leave_breadcrumb_method = NULL;
  jclass type_class = NULL;
  jfieldID crumb_type = NULL;
  jobject jtype = NULL;
  jbyteArray jmessage = NULL;

  // lookup com/bugsnag/android/NativeInterface
  interface_class =
      bsg_safe_find_class(env, "com/bugsnag/android/NativeInterface");
  if (interface_class == NULL) {
    goto exit;
  }

  // lookup NativeInterface.leaveBreadcrumb()
  leave_breadcrumb_method = bsg_safe_get_static_method_id(
      env, interface_class, "leaveBreadcrumb",
      "([BLcom/bugsnag/android/BreadcrumbType;)V");
  if (leave_breadcrumb_method == NULL) {
    goto exit;
  }

  // lookup com/bugsnag/android/BreadcrumbType
  type_class = bsg_safe_find_class(env, "com/bugsnag/android/BreadcrumbType");
  if (type_class == NULL) {
    goto exit;
  }

  // get breadcrumb type fieldID
  crumb_type = bsg_parse_jcrumb_type(env, type, type_class);
  if (crumb_type == NULL) {
    goto exit;
  }

  // get the breadcrumb type
  jtype = bsg_safe_get_static_object_field(env, type_class, crumb_type);
  if (jtype == NULL) {
    goto exit;
  }
  jmessage = bsg_byte_ary_from_string(env, message);
  bsg_safe_call_static_void_method(env, interface_class,
                                   leave_breadcrumb_method, jmessage, jtype);

  goto exit;

  exit:
  bsg_release_byte_ary(env, jmessage, message);
  (*env)->DeleteLocalRef(env, interface_class);
  (*env)->DeleteLocalRef(env, type_class);
  (*env)->DeleteLocalRef(env, jtype);
  (*env)->DeleteLocalRef(env, jmessage);
}
