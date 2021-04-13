#include <bugsnag.h>
#include <jni.h>
#include <stdio.h>
#include <sys/mman.h>
#include <unistd.h>
#include <type_traits>

// Create templated functions to bloat the code section
template <int COUNT, typename std::enable_if<0 == COUNT>::type* type = nullptr> int count_sum() {
  volatile char *ptr = (char *)100;
  *ptr = 0;

  volatile int a = COUNT;
  return a;
}
template <int COUNT, typename std::enable_if<0 != COUNT>::type* type = nullptr> int count_sum() {
  volatile int a = COUNT;
  return a + count_sum<COUNT-1>();
}

extern "C" {

bool on_error(void* event_ptr) {
  volatile int size = bugsnag_event_get_stacktrace_size(event_ptr);
  printf("size: %d\n", size);
  return true;
}

int crash_write_read_only() {
  bugsnag_add_on_error(on_error);

  // Change memory mapping flags of `count_sum<0>`
  int64_t pagesize = getpagesize();
  void* addr = (void*)((int64_t)&count_sum<0> & ~(pagesize - 1));
  mprotect (addr, 1, PROT_READ | PROT_WRITE | PROT_EXEC);

  return count_sum<1000>();
}

volatile unsigned uint_f2wk124_dont_optimize_me_bro;

_Noreturn void trigger_anr() {
  for (unsigned i = 0;; i++) {
    uint_f2wk124_dont_optimize_me_bro = i;
  }
}

bool my_on_error_b(void *event) {
  bugsnag_event_set_user(event, "999999", "ndk override", "j@ex.co");
  bugsnag_event_add_metadata_string(event, "Native", "field", "value");
  bugsnag_event_add_metadata_bool(event, "Native", "field", true);
  return true;
}

JNIEXPORT void JNICALL
Java_com_example_bugsnag_android_ExampleApplication_performNativeBugsnagSetup(
    JNIEnv *env, jobject instance) {
  bugsnag_add_on_error(&my_on_error_b);
}

JNIEXPORT void JNICALL
Java_com_example_bugsnag_android_BaseCrashyActivity_crashFromCXX(
    JNIEnv *env, jobject instance) {
  crash_write_read_only();
}

JNIEXPORT void JNICALL
Java_com_example_bugsnag_android_BaseCrashyActivity_anrFromCXX(
    JNIEnv *env, jobject instance) {
  trigger_anr();
}

JNIEXPORT void JNICALL
Java_com_example_bugsnag_android_BaseCrashyActivity_notifyFromCXX(
    JNIEnv *env, jobject instance) {
  // Set the current user
  bugsnag_set_user_env(env, "124323", "joe mills", "j@ex.co");
  // Leave a breadcrumb
  bugsnag_leave_breadcrumb_env(env, "Critical failure", BSG_CRUMB_LOG);
  // Send an error report
  bugsnag_notify_env(env, "Oh no", "The mill!", BSG_SEVERITY_INFO);
}
}
