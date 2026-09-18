// tour 专用 SDL3 shim（LD_PRELOAD，m3-263-increment / harness/lib）。
// 拦截 dlsym("SDL_GL_SetAttribute")（含 LWJGL 的库级 scoped dlsym），丢弃
// SDL_GL_FRAMEBUFFER_SRGB_CAPABLE(22)=1。根因：Xvfb swrast GLX 无 sRGB fbconfig
//（glxprobe 实测 0/80），renderpearl GlBackend.java:68 置 22/1 → glXChooseFBConfig
// 必空 → "Couldn't find matching GLX visual" → 26.3 client 无法建后端。
// 仅走查环境装载（生产 jar 不含）；真机上 sRGB fbconfig 存在，本 shim 不部署。
#define _GNU_SOURCE
#include <dlfcn.h>
#include <string.h>
typedef int (*fnSet)(int, int);
typedef void* (*fnDlsym)(void*, const char*);
static void* (*volatile real_dlsym)(void*, const char*);
static fnSet real_setattr;
static int shim_setattr(int attr, int value);
static fnDlsym get_real_dlsym(void) {
    void* f = real_dlsym;
    if (f) return f;
    // dlvsym 与 dlsym 不同符号：这里调用不会递归进本包装器
    f = dlvsym(RTLD_NEXT, "dlsym", "GLIBC_2.34");
    if (!f) f = dlvsym(RTLD_NEXT, "dlsym", "GLIBC_2.2.5");
    if (!f) __builtin_abort();
    __atomic_store_n(&real_dlsym, f, __ATOMIC_RELEASE);
    return f;
}
static void* dlsym_impl_fallback;
void* dlsym(void* handle, const char* name) {
    void* r = get_real_dlsym()(handle, name);
    if (name && r && strcmp(name, "SDL_GL_SetAttribute") == 0) {
        Dl_info info;
        if (dladdr(r, &info) && info.dli_fname && strstr(info.dli_fname, "libSDL3")) {
            real_setattr = (fnSet)r;
            return (void*)shim_setattr;
        }
    }
    return r;
}
static int shim_setattr(int attr, int value) {
    if (attr == 22 && value == 1) return 0;
    return real_setattr(attr, value);
}
