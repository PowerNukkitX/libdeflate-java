#include "./common.h"
#include "./jni_util.h"
#include "./libdeflate/libdeflate.h"

static jfieldID ctxFieldID;
static jfieldID availInFieldID;

LIBDEFLATEJAVA_PUBLIC JNIEXPORT void JNICALL Java_org_powernukkitx_libdeflate_LibdeflateDecompressor_initIDs(JNIEnv *env, jclass klass) {
    ctxFieldID = (*env)->GetFieldID(env, klass, "ctx", "J");
    availInFieldID = (*env)->GetFieldID(env, klass, "availInBytes", "J");
}

LIBDEFLATEJAVA_PUBLIC JNIEXPORT jlong JNICALL Java_org_powernukkitx_libdeflate_LibdeflateDecompressor_allocate(JNIEnv *env, jclass klass) {
    struct libdeflate_decompressor *decompressor = libdeflate_alloc_decompressor();
    if (decompressor == NULL) {
        // Out of memory!
        throwException(env, "java/lang/OutOfMemoryError", "libdeflate allocate decompressor");
        return 0;
    }
    return (jlong) decompressor;
}

LIBDEFLATEJAVA_PUBLIC JNIEXPORT void JNICALL Java_org_powernukkitx_libdeflate_LibdeflateDecompressor_free(JNIEnv *env, jclass klass, jlong ctx) {
    libdeflate_free_decompressor((struct libdeflate_decompressor *) ctx);
}

static enum libdeflate_result performDecompression(
    jlong ctx,
    jbyte* inBytes, jint inPos, jint inSize,
    jbyte* outBytes, jint outPos, jint outSize,
    jint type, jint knownSize,
    size_t* actualInBytes, size_t* actualOutBytes)
{
    struct libdeflate_decompressor *decompressor = (struct libdeflate_decompressor *) ctx;
    void *inStart = (void *) (inBytes + inPos);
    void *outStart = (void *) (outBytes + outPos);
    size_t availableOutBytes = knownSize == -1 ? outSize : knownSize;

    *actualInBytes = 0;
    *actualOutBytes = 0;

    switch (type) {
        case COMPRESSION_TYPE_DEFLATE:
            return libdeflate_deflate_decompress_ex(
                decompressor, inStart, inSize, outStart, availableOutBytes,
                actualInBytes, knownSize == -1 ? actualOutBytes : NULL);
        case COMPRESSION_TYPE_ZLIB:
            return libdeflate_zlib_decompress_ex(
                decompressor, inStart, inSize, outStart, availableOutBytes,
                actualInBytes, knownSize == -1 ? actualOutBytes : NULL);
        case COMPRESSION_TYPE_GZIP:
            return libdeflate_gzip_decompress_ex(
                decompressor, inStart, inSize, outStart, availableOutBytes,
                actualInBytes, knownSize == -1 ? actualOutBytes : NULL);
        default:
            return LIBDEFLATE_BAD_DATA;
    }
}

static jlong finishDecompression(
    JNIEnv *env, jobject self, enum libdeflate_result result,
    size_t actualInBytes, size_t actualOutBytes, jint knownSize)
{
    switch (result) {
        case LIBDEFLATE_SUCCESS:
            (*env)->SetLongField(env, self, availInFieldID, (jlong) actualInBytes);
            return (jlong) actualOutBytes;
        case LIBDEFLATE_BAD_DATA:
            throwException(env, "java/util/zip/DataFormatException", "input data is corrupted");
            return 0;
        case LIBDEFLATE_SHORT_OUTPUT:
            throwException(env, "java/util/zip/DataFormatException", "decompressed data is shorter than expected size");
            return 0;
        case LIBDEFLATE_INSUFFICIENT_SPACE:
            if (knownSize == -1) {
                return -1;
            }
            throwException(env, "java/util/zip/DataFormatException", "decompressed data would be too large for given output buffer");
            return 0;
        default:
            throwException(env, "java/util/zip/DataFormatException", "unknown libdeflate error");
            return 0;
    }
}

LIBDEFLATEJAVA_PUBLIC JNIEXPORT jlong JNICALL Java_org_powernukkitx_libdeflate_LibdeflateDecompressor_decompressBothHeap(
    JNIEnv *env, jobject self,
    jbyteArray in, jint inPos, jint inSize,
    jbyteArray out, jint outPos, jint outSize,
    jint type,
    jint knownSize)
{
    jlong ctx = (*env)->GetLongField(env, self, ctxFieldID);
    jbyte *inBytes = (*env)->GetPrimitiveArrayCritical(env, in, 0);
    jbyte *outBytes = (*env)->GetPrimitiveArrayCritical(env, out, 0);

    if (inBytes == NULL || outBytes == NULL) {
        if (outBytes != NULL) {
            (*env)->ReleasePrimitiveArrayCritical(env, out, outBytes, 0);
        }
        if (inBytes != NULL) {
            (*env)->ReleasePrimitiveArrayCritical(env, in, inBytes, JNI_ABORT);
        }
        return -1;
    }

    size_t actualInBytes;
    size_t actualOutBytes;
    enum libdeflate_result result = performDecompression(
        ctx, inBytes, inPos, inSize, outBytes, outPos, outSize,
        type, knownSize, &actualInBytes, &actualOutBytes);

    (*env)->ReleasePrimitiveArrayCritical(env, out, outBytes, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, in, inBytes, JNI_ABORT);

    return finishDecompression(env, self, result, actualInBytes, actualOutBytes, knownSize);
}

LIBDEFLATEJAVA_PUBLIC JNIEXPORT jlong JNICALL Java_org_powernukkitx_libdeflate_LibdeflateDecompressor_decompressBothDirect(
    JNIEnv *env, jobject self,
    jobject in, jint inPos, jint inSize,
    jobject out, jint outPos, jint outSize,
    jint type,
    jint knownSize)
{
    jlong ctx = (*env)->GetLongField(env, self, ctxFieldID);
    jbyte *inBytes = (*env)->GetDirectBufferAddress(env, in);
    jbyte *outBytes = (*env)->GetDirectBufferAddress(env, out);

    if (inBytes == NULL || outBytes == NULL) {
        throwException(env, "java/lang/IllegalArgumentException", "unable to obtain direct access to buffer");
        return -1;
    }

    size_t actualInBytes;
    size_t actualOutBytes;
    enum libdeflate_result result = performDecompression(
        ctx, inBytes, inPos, inSize, outBytes, outPos, outSize,
        type, knownSize, &actualInBytes, &actualOutBytes);

    return finishDecompression(env, self, result, actualInBytes, actualOutBytes, knownSize);
}

LIBDEFLATEJAVA_PUBLIC JNIEXPORT jlong JNICALL Java_org_powernukkitx_libdeflate_LibdeflateDecompressor_decompressOnlySourceDirect(
    JNIEnv *env, jobject self,
    jobject in, jint inPos, jint inSize,
    jbyteArray out, jint outPos, jint outSize,
    jint type,
    jint knownSize)
{
    jlong ctx = (*env)->GetLongField(env, self, ctxFieldID);
    jbyte *inBytes = (*env)->GetDirectBufferAddress(env, in);
    if (inBytes == NULL) {
        throwException(env, "java/lang/IllegalArgumentException", "unable to obtain direct access to input buffer");
        return -1;
    }

    jbyte *outBytes = (*env)->GetPrimitiveArrayCritical(env, out, 0);
    if (outBytes == NULL) {
        return -1;
    }

    size_t actualInBytes;
    size_t actualOutBytes;
    enum libdeflate_result result = performDecompression(
        ctx, inBytes, inPos, inSize, outBytes, outPos, outSize,
        type, knownSize, &actualInBytes, &actualOutBytes);

    (*env)->ReleasePrimitiveArrayCritical(env, out, outBytes, 0);

    return finishDecompression(env, self, result, actualInBytes, actualOutBytes, knownSize);
}

LIBDEFLATEJAVA_PUBLIC JNIEXPORT jlong JNICALL Java_org_powernukkitx_libdeflate_LibdeflateDecompressor_decompressOnlyDestinationDirect(
    JNIEnv *env, jobject self,
    jbyteArray in, jint inPos, jint inSize,
    jobject out, jint outPos, jint outSize,
    jint type,
    jint knownSize)
{
    jlong ctx = (*env)->GetLongField(env, self, ctxFieldID);
    jbyte *outBytes = (*env)->GetDirectBufferAddress(env, out);
    if (outBytes == NULL) {
        throwException(env, "java/lang/IllegalArgumentException", "unable to obtain direct access to output buffer");
        return -1;
    }

    jbyte *inBytes = (*env)->GetPrimitiveArrayCritical(env, in, 0);
    if (inBytes == NULL) {
        return -1;
    }

    size_t actualInBytes;
    size_t actualOutBytes;
    enum libdeflate_result result = performDecompression(
        ctx, inBytes, inPos, inSize, outBytes, outPos, outSize,
        type, knownSize, &actualInBytes, &actualOutBytes);

    (*env)->ReleasePrimitiveArrayCritical(env, in, inBytes, JNI_ABORT);

    return finishDecompression(env, self, result, actualInBytes, actualOutBytes, knownSize);
}
